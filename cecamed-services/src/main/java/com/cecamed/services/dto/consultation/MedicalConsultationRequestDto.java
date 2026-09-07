package com.cecamed.services.dto.consultation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class MedicalConsultationRequestDto {

    @NotNull(message = "El ID del paciente es obligatorio")
    private Long patientId;

    @NotNull(message = "La fecha y hora de la consulta es obligatoria")
    private LocalDateTime consultationDateTime;

    @NotBlank(message = "El motivo de la consulta es obligatorio")
    @Size(max = 255, message = "El motivo no puede exceder 255 caracteres")
    private String reason;

    private String symptoms;
    private String physicalExamination;

    @Valid
    private VitalSignsDto vitalSigns;

    @NotBlank(message = "El diagnóstico es obligatorio")
    private String diagnosis;

    @Size(max = 20, message = "El código CIE-10 no puede exceder 20 caracteres")
    private String icd10Code;

    private String treatmentPlan;
    private String privateNotes;
}
