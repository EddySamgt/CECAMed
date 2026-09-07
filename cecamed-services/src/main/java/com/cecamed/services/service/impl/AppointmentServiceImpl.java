package com.cecamed.services.service.impl;

import com.cecamed.core.model.appointment.Appointment;
import com.cecamed.core.model.appointment.DoctorSchedule;
import com.cecamed.core.model.appointment.enums.AppointmentStatus;
import com.cecamed.core.model.appointment.enums.GoogleSyncStatus;
import com.cecamed.core.model.patient.Patient;
import com.cecamed.core.repository.AppointmentRepository;
import com.cecamed.core.repository.DoctorScheduleRepository;
import com.cecamed.core.repository.PatientRepository;
import com.cecamed.core.repository.ScheduleBlockRepository;
import com.cecamed.services.dto.appointment.AppointmentRequestDto;
import com.cecamed.services.dto.appointment.AppointmentRescheduleDto;
import com.cecamed.services.dto.appointment.AppointmentResponseDto;
import com.cecamed.services.dto.appointment.AvailableSlotDto;
import com.cecamed.services.exception.BusinessRuleException;
import com.cecamed.services.exception.ResourceNotFoundException;
import com.cecamed.services.mapper.AppointmentMapper;
import com.cecamed.services.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.cecamed.calendar.rules.DoctorScheduleValidationRule;
import com.cecamed.calendar.service.AppointmentCalendarSyncService;
import com.cecamed.calendar.service.GoogleCalendarAvailabilityService;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorScheduleRepository doctorScheduleRepository;
    private final ScheduleBlockRepository scheduleBlockRepository;
    private final PatientRepository patientRepository;
    private final AppointmentMapper appointmentMapper;

    @Autowired(required = false)
    private AppointmentCalendarSyncService appointmentCalendarSyncService;

    @Autowired(required = false)
    private DoctorScheduleValidationRule doctorScheduleValidationRule;

    @Autowired(required = false)
    private GoogleCalendarAvailabilityService googleCalendarAvailabilityService;

    @Override
    @Transactional
    public AppointmentResponseDto createAppointment(AppointmentRequestDto dto) {
        log.info("Creando cita para el paciente ID: {} entre {} y {}",
                dto.getPatientId(), dto.getStartTime(), dto.getEndTime());

        validateAppointmentTime(dto.getStartTime(), dto.getEndTime());

        Patient patient = patientRepository.findById(dto.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", "id", dto.getPatientId()));

        if (!patient.getActive()) {
            throw new BusinessRuleException("No se pueden agendar citas a pacientes inactivos");
        }

        // Validación de conflictos de agenda y bloqueos
        validateNoConflict(dto.getStartTime(), dto.getEndTime(), null);

        Appointment appointment = appointmentMapper.toEntity(dto);
        appointment.setPatient(patient);

        Appointment saved = appointmentRepository.save(appointment);
        log.info("Cita creada exitosamente con ID: {}", saved.getId());

        if (appointmentCalendarSyncService != null) {
            try {
                appointmentCalendarSyncService.syncAppointment(saved);
            } catch (Exception e) {
                log.warn("No se pudo sincronizar automáticamente con Google Calendar: {}", e.getMessage());
            }
        }

        return appointmentMapper.toResponseDto(saved);
    }

    @Override
    @Transactional
    public AppointmentResponseDto rescheduleAppointment(Long appointmentId, AppointmentRescheduleDto dto) {
        log.info("Reprogramando cita ID: {} a nuevo intervalo: {} - {}",
                appointmentId, dto.getNewStartTime(), dto.getNewEndTime());

        validateAppointmentTime(dto.getNewStartTime(), dto.getNewEndTime());

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Cita", "id", appointmentId));

        if (appointment.getStatus() == AppointmentStatus.CANCELADA) {
            throw new BusinessRuleException("No se puede reprogramar una cita cancelada");
        }
        if (appointment.getStatus() == AppointmentStatus.ATENDIDA) {
            throw new BusinessRuleException("No se puede reprogramar una cita ya atendida");
        }

        validateNoConflict(dto.getNewStartTime(), dto.getNewEndTime(), appointmentId);

        appointment.setStartTime(dto.getNewStartTime());
        appointment.setEndTime(dto.getNewEndTime());
        appointment.setStatus(AppointmentStatus.REPROGRAMADA);
        appointment.setGoogleSyncStatus(GoogleSyncStatus.PENDING);
        if (dto.getReason() != null && !dto.getReason().isBlank()) {
            String currentNotes = appointment.getNotes() != null ? appointment.getNotes() + "\n" : "";
            appointment.setNotes(currentNotes + "Motivo de reprogramación: " + dto.getReason());
        }

        Appointment updated = appointmentRepository.save(appointment);

        if (appointmentCalendarSyncService != null) {
            try {
                appointmentCalendarSyncService.syncAppointment(updated);
            } catch (Exception e) {
                log.warn("No se pudo sincronizar reprogramación con Google Calendar: {}", e.getMessage());
            }
        }

        return appointmentMapper.toResponseDto(updated);
    }

    @Override
    @Transactional
    public AppointmentResponseDto cancelAppointment(Long appointmentId, String cancellationReason) {
        log.info("Cancelando cita ID: {} por motivo: {}", appointmentId, cancellationReason);

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Cita", "id", appointmentId));

        if (appointment.getStatus() == AppointmentStatus.CANCELADA) {
            throw new BusinessRuleException("La cita ya se encuentra cancelada");
        }

        appointment.setStatus(AppointmentStatus.CANCELADA);
        appointment.setCancellationReason(cancellationReason);
        appointment.setGoogleSyncStatus(GoogleSyncStatus.PENDING);

        Appointment updated = appointmentRepository.save(appointment);

        if (appointmentCalendarSyncService != null) {
            try {
                appointmentCalendarSyncService.cancelAppointmentEvent(updated);
            } catch (Exception e) {
                log.warn("No se pudo sincronizar cancelación con Google Calendar: {}", e.getMessage());
            }
        }

        return appointmentMapper.toResponseDto(updated);
    }

    @Override
    @Transactional
    public AppointmentResponseDto completeAppointment(Long appointmentId) {
        log.info("Marcando cita ID: {} como atendida", appointmentId);

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Cita", "id", appointmentId));

        appointment.setStatus(AppointmentStatus.ATENDIDA);
        Appointment updated = appointmentRepository.save(appointment);
        return appointmentMapper.toResponseDto(updated);
    }

    @Override
    public AppointmentResponseDto getAppointmentById(Long id) {
        return appointmentRepository.findById(id)
                .map(appointmentMapper::toResponseDto)
                .orElseThrow(() -> new ResourceNotFoundException("Cita", "id", id));
    }

    @Override
    public List<AppointmentResponseDto> getAppointmentsByPatient(Long patientId) {
        if (!patientRepository.existsById(patientId)) {
            throw new ResourceNotFoundException("Paciente", "id", patientId);
        }
        return appointmentRepository.findAllByPatientIdOrderByStartTimeDesc(patientId).stream()
                .map(appointmentMapper::toResponseDto)
                .toList();
    }

    @Override
    public List<AppointmentResponseDto> getAppointmentsByDateRange(LocalDateTime start, LocalDateTime end) {
        return appointmentRepository.findAllByStartTimeBetweenOrderByStartTimeAsc(start, end).stream()
                .map(appointmentMapper::toResponseDto)
                .toList();
    }

    @Override
    public List<AvailableSlotDto> getAvailableSlotsForDate(LocalDate date) {
        if (googleCalendarAvailabilityService != null) {
            return googleCalendarAvailabilityService.getUnifiedSlots(date).stream()
                    .map(slot -> AvailableSlotDto.builder()
                            .startTime(slot.getStart())
                            .endTime(slot.getEnd())
                            .available(!slot.isBusy())
                            .reasonIfNotAvailable(slot.getDescription())
                            .build())
                    .toList();
        }

        DayOfWeek dayOfWeek = date.getDayOfWeek();
        Optional<DoctorSchedule> scheduleOpt = doctorScheduleRepository.findByDayOfWeekAndIsActiveTrue(dayOfWeek);

        if (scheduleOpt.isEmpty()) {
            return Collections.emptyList();
        }

        DoctorSchedule schedule = scheduleOpt.get();
        List<AvailableSlotDto> slots = new ArrayList<>();

        LocalTime current = schedule.getStartTime();
        int slotDuration = schedule.getSlotDurationMinutes();

        while (!current.plusMinutes(slotDuration).isAfter(schedule.getEndTime())) {
            LocalDateTime slotStart = LocalDateTime.of(date, current);
            LocalDateTime slotEnd = slotStart.plusMinutes(slotDuration);

            boolean isBlocked = scheduleBlockRepository.isTimeRangeBlocked(slotStart, slotEnd);
            boolean hasAppointment = appointmentRepository.hasOverlappingAppointment(slotStart, slotEnd, null);

            boolean available = !isBlocked && !hasAppointment;
            String reason = null;
            if (isBlocked) {
                reason = "Horario bloqueado por descanso/actividad programada";
            } else if (hasAppointment) {
                reason = "Horario ocupado por otra cita médica";
            }

            slots.add(AvailableSlotDto.builder()
                    .startTime(slotStart)
                    .endTime(slotEnd)
                    .available(available)
                    .reasonIfNotAvailable(reason)
                    .build());

            current = current.plusMinutes(slotDuration);
        }

        return slots;
    }

    private void validateAppointmentTime(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            throw new BusinessRuleException("Los horarios de inicio y fin son obligatorios");
        }
        if (!startTime.isBefore(endTime)) {
            throw new BusinessRuleException("La hora de inicio debe ser estrictamente anterior a la hora de fin");
        }
    }

    private void validateNoConflict(LocalDateTime startTime, LocalDateTime endTime, Long excludeAppointmentId) {
        if (doctorScheduleValidationRule != null) {
            var ruleResult = doctorScheduleValidationRule.validateSlot(startTime, endTime);
            if (!ruleResult.isValid()) {
                throw new BusinessRuleException(ruleResult.getErrorMessage());
            }
        } else {
            // Verificación interna de bloqueos clínicos
            if (scheduleBlockRepository.isTimeRangeBlocked(startTime, endTime)) {
                throw new BusinessRuleException("El intervalo seleccionado coincide con un período de bloqueo de agenda médica");
            }
        }

        // Verificar solapamiento con otras citas activas
        if (appointmentRepository.hasOverlappingAppointment(startTime, endTime, excludeAppointmentId)) {
            throw new BusinessRuleException("Ya existe una cita programada en ese horario que genera conflicto");
        }
    }
}
