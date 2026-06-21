package com.cloudShare.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@Data
@NoArgsConstructor
@Builder
public class FileMetaDataDocumentDto {

    private String id;
    private String name;
    private String type;
    private Long size;
    private String clerkId;
    private Boolean isPublic;
    private LocalDateTime uploadedAt;
    private String fileUrl;
    private String cloudinaryPublicId;

}
