package com.cloudShare.Controller;

import com.cloudShare.Dto.FileMetaDataDocumentDto;
import com.cloudShare.Dto.UploadFilesDto;
import com.cloudShare.Entity.UserCredits;
import com.cloudShare.Response.ApiResponse;
import com.cloudShare.Service.FileMetaDataService;
import com.cloudShare.Service.UserCreditService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class FileController {
    private final FileMetaDataService fileMetaDataService;
    private final UserCreditService userCreditService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<?>> uploadFiles(@RequestPart("files") MultipartFile files[]) throws IOException {
        List<FileMetaDataDocumentDto> fileMetaDataDocumentDtoList = fileMetaDataService.uploadFiles(files);
        UserCredits finalCredits = userCreditService.getUserCredits();
        UploadFilesDto uploadFilesDto = UploadFilesDto.builder()
                .fileMetaDataDocumentDtoList(fileMetaDataDocumentDtoList)
                .finalCredits(finalCredits.getCredits())
                .build();


        return new ResponseEntity<>(ApiResponse.success(uploadFilesDto, "Successfully Uploaded Files"), HttpStatus.OK);


    }

    @GetMapping("/get-files")
    public ResponseEntity<ApiResponse<?>> getFileForCurrentUser() {
        List<FileMetaDataDocumentDto> files = fileMetaDataService.getFiles();

        return new ResponseEntity<>(ApiResponse.success(files, "Successfully  Files Fetched"), HttpStatus.OK);


    }

    @GetMapping("/public/files/{id}")
    public ResponseEntity<ApiResponse<?>> getPublicFile(@PathVariable String id) {
        FileMetaDataDocumentDto file = fileMetaDataService.getPublicFile(id);


        return new ResponseEntity<>(ApiResponse.success(file, "Successfully  Fetched Public Files"), HttpStatus.OK);


    }

    @GetMapping("/download/{id}")
    public ResponseEntity<?> download(@PathVariable String id) throws IOException {
        // CHANGED: Was FileMetaDataDocumentDto — now returns String (signed Cloudinary URL)
        // Previously read file from local disk using Path + UrlResource.
        // Now CloudinaryService generates a signed URL and we redirect the browser to it.
        // Browser downloads directly from Cloudinary CDN — zero bandwidth on your server.
        String signedUrl = fileMetaDataService.getDownloadableFile(id);

        // CHANGED: Was ResponseEntity.ok() with OCTET_STREAM body
        // Now 302 redirect to Cloudinary signed URL (expires in 1 hour)
        // REMOVED: Path, Resource, UrlResource, MediaType, CONTENT_DISPOSITION header
        // — all unnecessary since Cloudinary serves the file directly.
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, signedUrl)
                .build();


    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) throws IOException {
        fileMetaDataService.deleteFile(id);


        return ResponseEntity.noContent().build();


    }

    @PatchMapping("/toggle-public/{id}")
    public ResponseEntity<ApiResponse<?>> togglPublic(@PathVariable String id) {
        FileMetaDataDocumentDto file = fileMetaDataService.togglePublic(id);


        return new ResponseEntity<>(ApiResponse.success(file, "Successfully Toggle Executed"), HttpStatus.OK);


    }
}
