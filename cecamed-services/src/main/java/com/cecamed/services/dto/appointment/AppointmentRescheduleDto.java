package com.cecamed.services.dto.appointment;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
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
public class AppointmentRescheduleDto {

    @NotNull(message = "La nueva hora de inicio es obligatoria")
    @FutureOrPresent(message = "La nueva hora de inicio debe ser en el presente o futuro")
    private LocalDateTime newStartTime;

    @NotNull(message = "La nueva hora de fin es obligatoria")
    private LocalDateTime newEndTime;

    private String reason;
}
