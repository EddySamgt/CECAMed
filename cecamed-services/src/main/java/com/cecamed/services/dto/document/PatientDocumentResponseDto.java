package com.cecamed.services.dto.document;

import com.cecamed.core.model.patient.enums.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientDocumentResponseDto {
    private Long id;
    private Long patientId;
    private Long consultationId;
    private String fileName;
    private String originalFileName;
    private String fileType;
    private DocumentType documentType;
    private String filePath;
    private Long fileSizeBytes;
    private String checksumSha256;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
