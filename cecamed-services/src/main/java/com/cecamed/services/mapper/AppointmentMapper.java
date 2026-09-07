package com.cecamed.services.mapper;

import com.cecamed.core.model.appointment.Appointment;
import com.cecamed.core.model.appointment.DoctorSchedule;
import com.cecamed.core.model.appointment.ScheduleBlock;
import com.cecamed.core.model.appointment.enums.AppointmentStatus;
import com.cecamed.core.model.appointment.enums.GoogleSyncStatus;
import com.cecamed.services.dto.appointment.AppointmentRequestDto;
import com.cecamed.services.dto.appointment.AppointmentResponseDto;
import com.cecamed.services.dto.appointment.DoctorScheduleDto;
import com.cecamed.services.dto.appointment.ScheduleBlockRequestDto;
import com.cecamed.services.dto.appointment.ScheduleBlockResponseDto;
import org.springframework.stereotype.Component;

@Component
public class AppointmentMapper {

    public Appointment toEntity(AppointmentRequestDto dto) {
        if (dto == null) return null;

        return Appointment.builder()
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .reasonForVisit(dto.getReasonForVisit())
                .notes(dto.getNotes())
                .status(AppointmentStatus.PROGRAMADA)
                .googleSyncStatus(GoogleSyncStatus.PENDING)
                .build();
    }

    public AppointmentResponseDto toResponseDto(Appointment appointment) {
        if (appointment == null) return null;

        return AppointmentResponseDto.builder()
                .id(appointment.getId())
                .patientId(appointment.getPatient() != null ? appointment.getPatient().getId() : null)
                .patientFullName(appointment.getPatient() != null ? appointment.getPatient().getFullName() : null)
                .patientIdentificationNumber(appointment.getPatient() != null ? appointment.getPatient().getIdentificationNumber() : null)
                .patientPhone(appointment.getPatient() != null ? appointment.getPatient().getPhone() : null)
                .startTime(appointment.getStartTime())
                .endTime(appointment.getEndTime())
                .status(appointment.getStatus())
                .reasonForVisit(appointment.getReasonForVisit())
                .cancellationReason(appointment.getCancellationReason())
                .notes(appointment.getNotes())
                .googleEventId(appointment.getGoogleEventId())
                .googleSyncStatus(appointment.getGoogleSyncStatus())
                .googleHtmlLink(appointment.getGoogleHtmlLink())
                .googleLastSyncedAt(appointment.getGoogleLastSyncedAt())
                .createdAt(appointment.getCreatedAt())
                .updatedAt(appointment.getUpdatedAt())
                .build();
    }

    public DoctorSchedule toEntity(DoctorScheduleDto dto) {
        if (dto == null) return null;

        return DoctorSchedule.builder()
                .id(dto.getId())
                .dayOfWeek(dto.getDayOfWeek())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .slotDurationMinutes(dto.getSlotDurationMinutes() != null ? dto.getSlotDurationMinutes() : 30)
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .build();
    }

    public DoctorScheduleDto toDto(DoctorSchedule schedule) {
        if (schedule == null) return null;

        return DoctorScheduleDto.builder()
                .id(schedule.getId())
                .dayOfWeek(schedule.getDayOfWeek())
                .startTime(schedule.getStartTime())
                .endTime(schedule.getEndTime())
                .slotDurationMinutes(schedule.getSlotDurationMinutes())
                .isActive(schedule.getIsActive())
                .build();
    }

    public ScheduleBlock toEntity(ScheduleBlockRequestDto dto) {
        if (dto == null) return null;

        return ScheduleBlock.builder()
                .title(dto.getTitle())
                .startDateTime(dto.getStartDateTime())
                .endDateTime(dto.getEndDateTime())
                .blockType(dto.getBlockType())
                .allDay(dto.getAllDay() != null ? dto.getAllDay() : false)
                .reason(dto.getReason())
                .build();
    }

    public ScheduleBlockResponseDto toResponseDto(ScheduleBlock block) {
        if (block == null) return null;

        return ScheduleBlockResponseDto.builder()
                .id(block.getId())
                .title(block.getTitle())
                .startDateTime(block.getStartDateTime())
                .endDateTime(block.getEndDateTime())
                .blockType(block.getBlockType())
                .allDay(block.getAllDay())
                .reason(block.getReason())
                .googleEventId(block.getGoogleEventId())
                .createdAt(block.getCreatedAt())
                .build();
    }
}
