package com.cecamed.services.service;

import com.cecamed.core.model.patient.enums.DocumentType;
import com.cecamed.services.dto.document.PatientDocumentResponseDto;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PatientDocumentService {

    PatientDocumentResponseDto uploadDocument(
            Long patientId,
            Long consultationId,
            DocumentType documentType,
            String description,
            MultipartFile file
    );

    PatientDocumentResponseDto getDocumentMetadata(Long documentId);

    Resource loadDocumentFile(Long documentId);

    List<PatientDocumentResponseDto> getDocumentsByPatient(Long patientId);

    List<PatientDocumentResponseDto> getDocumentsByConsultation(Long consultationId);

    List<PatientDocumentResponseDto> getDocumentsByPatientAndType(Long patientId, DocumentType documentType);

    void deleteDocument(Long documentId);
}
