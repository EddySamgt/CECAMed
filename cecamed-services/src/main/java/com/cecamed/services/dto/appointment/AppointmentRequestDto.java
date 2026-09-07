package com.cecamed.services.dto.appointment;

import jakarta.validation.constraints.FutureOrPresent;
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
public class AppointmentRequestDto {

    @NotNull(message = "El paciente es obligatorio")
    private Long patientId;

    @NotNull(message = "La hora de inicio es obligatoria")
    @FutureOrPresent(message = "La hora de inicio debe ser en el presente o futuro")
    private LocalDateTime startTime;

    @NotNull(message = "La hora de fin es obligatoria")
    private LocalDateTime endTime;

    @NotBlank(message = "El motivo de la consulta es obligatorio")
    @Size(max = 255, message = "El motivo no puede exceder 255 caracteres")
    private String reasonForVisit;

    private String notes;
}
