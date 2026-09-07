package com.cecamed.services.dto.appointment;

import com.cecamed.core.model.appointment.enums.AppointmentStatus;
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
public class AppointmentResponseDto {
    private Long id;
    private Long patientId;
    private String patientFullName;
    private String patientIdentificationNumber;
    private String patientPhone;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AppointmentStatus status;
    private String reasonForVisit;
    private String cancellationReason;
    private String notes;
    private String googleEventId;
    private GoogleSyncStatus googleSyncStatus;
    private String googleHtmlLink;
    private LocalDateTime googleLastSyncedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
