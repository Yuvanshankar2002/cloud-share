package com.cloudShare.Entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "files")
@AllArgsConstructor
@Data
@NoArgsConstructor
@Builder
public class FileMetaDataDocument {

    @Id
    private String id;
    private String name;
    private String type;
    private String cloudinaryPublicId;
    private String fileUrl;
    private Long size;
    private String clerkId;
    private Boolean isPublic;
    private LocalDateTime uploadedAt;

}
