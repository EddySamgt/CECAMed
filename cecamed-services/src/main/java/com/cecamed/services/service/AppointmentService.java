package com.cecamed.services.service;

import com.cecamed.services.dto.appointment.AppointmentRequestDto;
import com.cecamed.services.dto.appointment.AppointmentRescheduleDto;
import com.cecamed.services.dto.appointment.AppointmentResponseDto;
import com.cecamed.services.dto.appointment.AvailableSlotDto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentService {

    AppointmentResponseDto createAppointment(AppointmentRequestDto dto);

    AppointmentResponseDto rescheduleAppointment(Long appointmentId, AppointmentRescheduleDto dto);

    AppointmentResponseDto cancelAppointment(Long appointmentId, String cancellationReason);

    AppointmentResponseDto completeAppointment(Long appointmentId);

    AppointmentResponseDto updateAppointmentStatus(Long appointmentId, com.cecamed.core.model.appointment.enums.AppointmentStatus status);

    AppointmentResponseDto getAppointmentById(Long id);

    List<AppointmentResponseDto> getAppointmentsByPatient(Long patientId);

    List<AppointmentResponseDto> getAppointmentsByDateRange(LocalDateTime start, LocalDateTime end);

    List<AvailableSlotDto> getAvailableSlotsForDate(LocalDate date);
}
