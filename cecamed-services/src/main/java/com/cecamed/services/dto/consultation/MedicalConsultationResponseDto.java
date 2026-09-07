package com.cecamed.services.dto.consultation;

import com.cecamed.services.dto.document.PatientDocumentResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalConsultationResponseDto {
    private Long id;
    private Long patientId;
    private String patientFullName;
    private String patientIdentificationNumber;
    private LocalDateTime consultationDateTime;
    private String reason;
    private String symptoms;
    private String physicalExamination;
    private VitalSignsDto vitalSigns;
    private String diagnosis;
    private String icd10Code;
    private String treatmentPlan;
    private String privateNotes;
    private List<PatientDocumentResponseDto> attachedDocuments;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
