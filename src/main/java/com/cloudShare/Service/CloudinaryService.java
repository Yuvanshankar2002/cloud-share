package com.cloudShare.Service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {
    private final Cloudinary cloudinary;

    // ─────────────────────────────────────────────────────────────────────────
    // UPLOAD FILE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Uploads any file to Cloudinary as a raw resource.
     *
     * WHY resource_type = "raw"?
     *   Cloudinary defaults to "image". For PDFs, ZIPs, DOCs, etc.
     *   you must explicitly set "raw" — otherwise upload will fail
     *   or be misclassified.
     *
     * WHY folder = "cloudshare/{clerkId}"?
     *   Isolates each user's files in their own Cloudinary folder.
     *   Makes it easy to audit or bulk-delete a user's files.
     *
     * WHY use_filename = true + unique_filename = true?
     *   Preserves the original filename in the public_id while
     *   appending a random suffix to avoid collisions.
     *   e.g. cloudshare/user123/resume_abc123.pdf
     *
     * @param file     The multipart file from the HTTP request
     * @param clerkId  The authenticated user's Clerk ID (used as folder name)
     * @return Map with keys: publicId, secureUrl, resourceType, bytes
     * @throws IOException if Cloudinary upload fails or file cannot be read
     */
    public Map<String, Object> uploadFile(MultipartFile file, String clerkId) throws IOException {

        log.info("Uploading file to Cloudinary: name={}, size={} bytes, type={}",
                file.getOriginalFilename(),
                file.getSize(),
                file.getContentType()
        );

        // Cloudinary upload() accepts byte[] or File.
        // We use file.getBytes() since MultipartFile is already in memory.
        Map<?, ?> result = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        // Store under cloudshare/{clerkId}/ folder in Cloudinary
                        "folder","cloudshare/" + clerkId,

                        // "raw" handles all non-image file types
                        // For images you can use "image", but "raw" works for everything
                        "resource_type","raw",

                        // Use original filename as base for the public_id
                        "use_filename",true,

                        // Append random chars to avoid name collisions
                        "unique_filename",true,

                        // Don't overwrite if same name already exists
                        "overwrite",false
                )
        );

        // Extract the fields we need from Cloudinary's response map
        String publicId  = result.get("public_id").toString();
        String secureUrl = result.get("secure_url").toString();
        String resType   = result.get("resource_type").toString();
        long   bytes     = Long.parseLong(result.get("bytes").toString());

        log.info("Cloudinary upload success: publicId={}, url={}", publicId, secureUrl);

        // Return only what FileMetaDataService needs — keeps coupling minimal
        return Map.of(
                "publicId",      publicId,   // stored in MongoDB for delete/signedUrl
                "secureUrl",     secureUrl,  // stored in MongoDB for public file view
                "resourceType",  resType,
                "bytes",         bytes
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE FILE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Deletes a file from Cloudinary using its public_id.
     *
     * WHY resource_type = "raw"?
     *   Cloudinary's destroy() also defaults to "image".
     *   If you uploaded as "raw" but delete as "image", Cloudinary
     *   returns "not found" even though the file exists.
     *   Always match the resource_type used during upload.
     *
     * WHY return boolean instead of throwing?
     *   If a file was manually deleted from the Cloudinary dashboard,
     *   we still want MongoDB cleanup to proceed. Returning false lets
     *   the caller (FileMetaDataService) log a warning without blocking.
     *
     * @param publicId  The Cloudinary public_id stored in MongoDB
     * @return true if deleted successfully, false if not found or failed
     */
    public boolean deleteFile(String publicId) {
        try {
            log.info("Deleting file from Cloudinary: publicId={}", publicId);

            Map<?, ?> result = cloudinary.uploader().destroy(
                    publicId,
                    ObjectUtils.asMap(
                            // Must match upload resource_type — "raw" for non-images
                            "resource_type", "raw"
                    )
            );

            // Cloudinary returns { "result": "ok" } on success
            // or { "result": "not found" } if publicId doesn't exist
            String outcome = result.get("result").toString();
            boolean success = "ok".equals(outcome);

            if (success) {
                log.info("Cloudinary delete success: publicId={}", publicId);
            } else {
                log.warn("Cloudinary delete returned '{}' for publicId={}", outcome, publicId);
            }

            return success;

        } catch (IOException e) {
            // Network or SDK error — log but don't crash the delete flow
            log.error("IOException deleting file from Cloudinary: publicId={}", publicId, e);
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GENERATE SIGNED DOWNLOAD URL
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Generates a time-limited signed URL for secure file download.
     *
     * WHY signed URLs instead of the plain secureUrl?
     *   The secureUrl stored in MongoDB is a permanent public URL.
     *   If the file is private or you want download expiry,
     *   you need a signed URL with a timestamp-based signature.
     *   This prevents hotlinking and unauthorized permanent access.
     *
     * WHY 1 hour expiry?
     *   Long enough for the user to complete the download,
     *   short enough to prevent link sharing abuse.
     *   Adjust SIGNED_URL_EXPIRY_SECONDS as needed.
     *
     * HOW it works:
     *   Cloudinary signs the URL using your api_secret.
     *   The signature includes the expiry timestamp.
     *   After expiry, Cloudinary returns 401 Unauthorized.
     *
     * @param publicId  The Cloudinary public_id stored in MongoDB
     * @return A signed HTTPS URL valid for 1 hour
     * @throws RuntimeException if Cloudinary API call fails
     */
    public String generateSignedUrl(String publicId) {

        // Expiry = current epoch seconds + 3600 (1 hour)
        // Change this constant to adjust expiry window
        final long SIGNED_URL_EXPIRY_SECONDS = 3600L;
        long expiresAt = (System.currentTimeMillis() / 1000L) + SIGNED_URL_EXPIRY_SECONDS;

        try {
            log.info("Generating signed URL: publicId={}, expiresAt={}", publicId, expiresAt);

            // cloudinary.api().resource() fetches metadata + signed URL in one call
            Map<?, ?> result = cloudinary.api().resource(
                    publicId,
                    ObjectUtils.asMap(
                            "resource_type", "raw",
                            "sign_url",      true,         // tell Cloudinary to sign the URL
                            "expires_at",    expiresAt     // Unix timestamp for expiry
                    )
            );

            String signedUrl = result.get("secure_url").toString();
            log.info("Signed URL generated successfully for publicId={}", publicId);
            return signedUrl;

        } catch (Exception e) {
            // Cloudinary API errors (network, invalid publicId, auth failure)
            log.error("Failed to generate signed URL for publicId={}", publicId, e);
            throw new RuntimeException("Could not generate download URL. Please try again.", e);
        }
    }
}
