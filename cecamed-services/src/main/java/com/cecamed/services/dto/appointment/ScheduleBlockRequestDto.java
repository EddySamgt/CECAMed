package com.cecamed.services.dto.appointment;

import com.cecamed.core.model.appointment.enums.BlockType;
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
public class ScheduleBlockRequestDto {

    @NotBlank(message = "El título del bloqueo es obligatorio")
    @Size(max = 150, message = "El título no puede exceder 150 caracteres")
    private String title;

    @NotNull(message = "La fecha y hora de inicio es obligatoria")
    private LocalDateTime startDateTime;

    @NotNull(message = "La fecha y hora de fin es obligatoria")
    private LocalDateTime endDateTime;

    @NotNull(message = "El tipo de bloqueo es obligatorio")
    @Builder.Default
    private BlockType blockType = BlockType.PERSONAL;

    @Builder.Default
    private Boolean allDay = false;

    private String reason;
}
