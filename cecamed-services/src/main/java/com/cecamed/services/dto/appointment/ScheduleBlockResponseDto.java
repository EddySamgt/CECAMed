package com.cecamed.services.dto.appointment;

import com.cecamed.core.model.appointment.enums.BlockType;
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
public class ScheduleBlockResponseDto {
    private Long id;
    private String title;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private BlockType blockType;
    private Boolean allDay;
    private String reason;
    private String googleEventId;
    private LocalDateTime createdAt;
}
