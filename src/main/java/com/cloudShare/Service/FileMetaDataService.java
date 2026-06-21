package com.cloudShare.Service;

import com.cloudShare.Dto.FileMetaDataDocumentDto;
import com.cloudShare.Entity.FileMetaDataDocument;
import com.cloudShare.Entity.ProfileDocument;
import com.cloudShare.Exception.CloudShareException;
import com.cloudShare.Exception.ErrorCode;
import com.cloudShare.Repository.FileMetaDataDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileMetaDataService {
    private final ProfileService profileService;
    private final UserCreditService userCreditService;
    private final FileMetaDataDocumentRepository fileMetaDataDocumentRepository;
    private final CloudinaryService cloudinaryService;

    public List<FileMetaDataDocumentDto> uploadFiles(MultipartFile files[]) throws IOException {
        ProfileDocument currentProfile = profileService.getCurrentProfile();
        if (!userCreditService.hasEnoughCredits(files.length)) {
            throw new CloudShareException(ErrorCode.NOT_ENOUGH_CREDENDENTIALS);
        }
        List<FileMetaDataDocument> savedFiles = new ArrayList<>();
        Path uploadPath = Paths.get("upload").toAbsolutePath().normalize();
        Files.createDirectories(uploadPath);

        for (var file : files) {

            // REMOVED: UUID-based local filename generation
            // Previously:
            //   String fileName = UUID.randomUUID() + "." + StringUtils.getFilename(file.getOriginalFilename());
            //   Path targetLocation = uploadPath.resolve(fileName);
            //   Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            // Reason: Cloudinary handles naming and storage internally.

            // ADDED: Upload file bytes to Cloudinary under folder = clerkId
            // Returns a Map with: publicId, secureUrl, format, bytes
            // clerkId is used as the Cloudinary folder so each user's files are isolated
            Map<String, Object> uploadResult = cloudinaryService.uploadFile(
                    file,
                    currentProfile.getClerkId()
            );

            log.info("Uploaded file to Cloudinary: publicId={}, url={}",
                    uploadResult.get("publicId"),
                    uploadResult.get("secureUrl")
            );

            FileMetaDataDocument fileMetaDataDocument = FileMetaDataDocument.builder()

                    // REMOVED: .fileLocation(targetLocation.toString())
                    // Previously stored the absolute local path like /app/upload/uuid.pdf
                    // Replaced by two Cloudinary-specific fields below:

                    // ADDED: Cloudinary public_id — needed for delete and signed URL generation
                    .cloudinaryPublicId(uploadResult.get("publicId").toString())

                    // ADDED: Permanent HTTPS URL from Cloudinary (used for public file view)
                    .fileUrl(uploadResult.get("secureUrl").toString())

                    .name(file.getOriginalFilename())
                    .size(file.getSize())
                    .type(file.getContentType())
                    .clerkId(currentProfile.getClerkId())
                    .isPublic(false)
                    .uploadedAt(LocalDateTime.now())
                    .build();

            userCreditService.consumeCredit();
            savedFiles.add(fileMetaDataDocumentRepository.save(fileMetaDataDocument));
        }

        return savedFiles.stream()
                .map(this::maptoDto)
                .collect(Collectors.toList());
    }


    private FileMetaDataDocumentDto maptoDto(FileMetaDataDocument fileMetaDataDocument) {
        return FileMetaDataDocumentDto.builder()

                // CHANGED: fileLocation replaced by fileUrl
                // Previously: .fileLocation(fileMetaDataDocument.getFileLocation())
                // Reason: fileLocation was a local disk path (useless to frontend).
                //         fileUrl is the Cloudinary HTTPS URL the frontend can actually use.
                .fileUrl(fileMetaDataDocument.getFileUrl())

                .size(fileMetaDataDocument.getSize())
                .type(fileMetaDataDocument.getType())
                .name(fileMetaDataDocument.getName())
                .clerkId(fileMetaDataDocument.getClerkId())
                .isPublic(fileMetaDataDocument.getIsPublic())
                .uploadedAt(fileMetaDataDocument.getUploadedAt())

                // CHANGED: ID serialization
                // Previously: .id(fileMetaDataDocument.getId().toHexString())
                // toHexString() was needed because id was ObjectId type.
                // Now id is a plain String in the entity, so .getId() works directly.
                // This also fixes the [object Object] bug on the frontend.
                .id(fileMetaDataDocument.getId())

                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
// GET ALL FILES FOR CURRENT USER
// ─────────────────────────────────────────────────────────────────────────
    public List<FileMetaDataDocumentDto> getFiles() {
        // NO CHANGE in logic — still fetches by clerkId
        ProfileDocument currentProfile = profileService.getCurrentProfile();
        List<FileMetaDataDocument> files = fileMetaDataDocumentRepository
                .findByClerkId(currentProfile.getClerkId());
        return files.stream().map(this::maptoDto).collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
// GET PUBLIC FILE (no auth required — for share link)
// ─────────────────────────────────────────────────────────────────────────
    public FileMetaDataDocumentDto getPublicFile(String id) {

        // CHANGED: ObjectId lookup → plain String lookup
        // Previously:
        //   ObjectId objectId = new ObjectId(id);
        //   Optional<FileMetaDataDocument> fileOptional = fileMetaDataDocumentRepository.findById(objectId);
        // Reason: id field is now String in the entity, repository uses String directly.

        Optional<FileMetaDataDocument> fileOptional = fileMetaDataDocumentRepository.findById(id);

        if (fileOptional.isEmpty() || !fileOptional.get().getIsPublic()) {
            throw new CloudShareException(ErrorCode.FILE_NOT_FOUND);
        }
        return maptoDto(fileOptional.get());
    }

    // ─────────────────────────────────────────────────────────────────────────
// GET DOWNLOADABLE FILE (auth required — generates signed Cloudinary URL)
// ─────────────────────────────────────────────────────────────────────────
    public String getDownloadableFile(String id) {

        // CHANGED: Return type was FileMetaDataDocumentDto, now returns String (signed URL)
        // Previously returned the DTO and the controller read fileLocation from disk.
        // Now we generate a Cloudinary signed URL that expires in 1 hour.
        // The controller will redirect (302) to this URL — browser downloads directly from Cloudinary.

        // CHANGED: ObjectId lookup → plain String lookup (same reason as above)
        FileMetaDataDocument file = fileMetaDataDocumentRepository.findById(id)
                .orElseThrow(() -> new CloudShareException(ErrorCode.FILE_NOT_FOUND));

        // ADDED: Generate signed URL using the stored cloudinaryPublicId
        // Signed URL expires in 1 hour — prevents hotlinking indefinitely
        String signedUrl = cloudinaryService.generateSignedUrl(file.getCloudinaryPublicId());

        log.info("Generated signed download URL for fileId={}", id);
        return signedUrl;
    }

    // ─────────────────────────────────────────────────────────────────────────
// DELETE FILE
// ─────────────────────────────────────────────────────────────────────────
    public void deleteFile(String id) {
        try {
            // CHANGED: ObjectId lookup → plain String lookup
            // Previously: ObjectId objectId = new ObjectId(id);
            ProfileDocument currentProfile = profileService.getCurrentProfile();

            FileMetaDataDocument file = fileMetaDataDocumentRepository.findById(id)
                    .orElseThrow(() -> new CloudShareException(ErrorCode.FILE_NOT_FOUND));

            if (!file.getClerkId().equals(currentProfile.getClerkId())) {
                throw new CloudShareException(ErrorCode.INVALID_USER);
            }

            // REMOVED: Local file deletion
            // Previously:
            //   Path filePath = Paths.get(file.getFileLocation());
            //   Files.deleteIfExists(filePath);
            // Reason: File no longer exists on disk — it's in Cloudinary.

            // ADDED: Delete from Cloudinary using the stored publicId
            // cloudinaryService.deleteFile returns true if deleted, false if not found
            boolean deleted = cloudinaryService.deleteFile(file.getCloudinaryPublicId());

            if (!deleted) {
                // Log warning but don't block MongoDB cleanup
                // Cloudinary file may have been manually removed from dashboard
                log.warn("Cloudinary delete returned non-ok for publicId={}", file.getCloudinaryPublicId());
            }

            // CHANGED: deleteById now takes String directly (not ObjectId)
            fileMetaDataDocumentRepository.deleteById(id);

            log.info("Deleted file: id={}, cloudinaryPublicId={}", id, file.getCloudinaryPublicId());

        } catch (CloudShareException ex) {
            // Re-throw known business exceptions as-is (FILE_NOT_FOUND, INVALID_USER)
            throw ex;
        } catch (Exception ex) {
            // Catch unexpected errors (Cloudinary SDK, network issues etc.)
            log.error("Unexpected error deleting file id={}", id, ex);
            throw new CloudShareException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
// TOGGLE PUBLIC / PRIVATE
// ─────────────────────────────────────────────────────────────────────────
    public FileMetaDataDocumentDto togglePublic(String id) {

        // CHANGED: ObjectId lookup → plain String lookup
        // Previously: ObjectId objectId = new ObjectId(id);
        FileMetaDataDocument file = fileMetaDataDocumentRepository.findById(id)
                .orElseThrow(() -> new CloudShareException(ErrorCode.FILE_NOT_FOUND));

        file.setIsPublic(!file.getIsPublic());
        fileMetaDataDocumentRepository.save(file);

        log.info("Toggled file visibility: id={}, isPublic={}", id, file.getIsPublic());

        // NO CHANGE in logic — toggle works the same way
        return maptoDto(file);
    }
}
