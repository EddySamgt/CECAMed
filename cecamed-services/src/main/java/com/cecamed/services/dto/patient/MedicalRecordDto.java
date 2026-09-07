package com.cecamed.services.dto.patient;

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
public class MedicalRecordDto {
    private Long id;

    @NotNull(message = "El ID del paciente es obligatorio")
    private Long patientId;

    @NotBlank(message = "El número de expediente es obligatorio")
    @Size(max = 50, message = "El número de expediente no puede superar los 50 caracteres")
    private String recordNumber;

    private String allergies;
    private String pathologicalHistory;
    private String nonPathologicalHistory;
    private String familyHistory;
    private String surgicalHistory;
    private String currentMedications;
    private String generalObservations;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
