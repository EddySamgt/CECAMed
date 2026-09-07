package com.cecamed.calendar.dto;

import com.cecamed.core.model.appointment.enums.GoogleSyncStatus;
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
public class CalendarSyncResultDto {
    private Long appointmentId;
    private String googleEventId;
    private String htmlLink;
    private GoogleSyncStatus status;
    private String message;
    private LocalDateTime syncedAt;
}
