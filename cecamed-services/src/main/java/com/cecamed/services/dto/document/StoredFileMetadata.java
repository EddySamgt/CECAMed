package com.cecamed.services.dto.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoredFileMetadata {
    private String savedFileName;
    private String originalFileName;
    private String relativePath;
    private String fileType;
    private Long fileSizeBytes;
    private String checksumSha256;
}
