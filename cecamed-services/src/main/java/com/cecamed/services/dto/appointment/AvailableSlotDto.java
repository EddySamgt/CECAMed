package com.cecamed.services.dto.appointment;

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
public class AvailableSlotDto {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private boolean available;
    private String reasonIfNotAvailable;
}
