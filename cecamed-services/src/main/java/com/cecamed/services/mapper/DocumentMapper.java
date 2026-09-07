package com.cecamed.services.mapper;

import com.cecamed.core.model.patient.PatientDocument;
import com.cecamed.services.dto.document.PatientDocumentResponseDto;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class DocumentMapper {

    public PatientDocumentResponseDto toResponseDto(PatientDocument document) {
        if (document == null) return null;

        return PatientDocumentResponseDto.builder()
                .id(document.getId())
                .patientId(document.getPatient() != null ? document.getPatient().getId() : null)
                .consultationId(document.getConsultation() != null ? document.getConsultation().getId() : null)
                .fileName(document.getFileName())
                .originalFileName(document.getOriginalFileName())
                .fileType(document.getFileType())
                .documentType(document.getDocumentType())
                .filePath(document.getFilePath())
                .fileSizeBytes(document.getFileSizeBytes())
                .checksumSha256(document.getChecksumSha256())
                .description(document.getDescription())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }

    public List<PatientDocumentResponseDto> toResponseDtoList(List<PatientDocument> documents) {
        if (documents == null) return Collections.emptyList();
        return documents.stream().map(this::toResponseDto).toList();
    }
}
