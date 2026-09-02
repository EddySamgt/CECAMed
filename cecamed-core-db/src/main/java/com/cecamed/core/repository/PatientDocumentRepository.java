package com.cecamed.core.repository;

import com.cecamed.core.model.patient.PatientDocument;
import com.cecamed.core.model.patient.enums.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PatientDocumentRepository extends JpaRepository<PatientDocument, Long> {

    List<PatientDocument> findAllByPatientIdOrderByCreatedAtDesc(Long patientId);

    List<PatientDocument> findAllByConsultationIdOrderByCreatedAtDesc(Long consultationId);

    List<PatientDocument> findAllByPatientIdAndDocumentTypeOrderByCreatedAtDesc(Long patientId, DocumentType documentType);
}
