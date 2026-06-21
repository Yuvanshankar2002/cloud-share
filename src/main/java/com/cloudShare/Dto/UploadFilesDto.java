package com.cloudShare.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class UploadFilesDto {
    private List<FileMetaDataDocumentDto> fileMetaDataDocumentDtoList;
    private Integer finalCredits;
}
