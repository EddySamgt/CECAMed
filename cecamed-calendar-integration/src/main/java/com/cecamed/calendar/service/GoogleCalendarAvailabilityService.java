package com.cecamed.calendar.service;

import com.cecamed.calendar.client.GoogleCalendarClient;
import com.cecamed.calendar.dto.TimeSlotDto;
import com.cecamed.core.model.appointment.DoctorSchedule;
import com.cecamed.core.repository.AppointmentRepository;
import com.cecamed.core.repository.DoctorScheduleRepository;
import com.cecamed.core.repository.ScheduleBlockRepository;
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

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GoogleCalendarAvailabilityService {

    private final DoctorScheduleRepository doctorScheduleRepository;
    private final ScheduleBlockRepository scheduleBlockRepository;
    private final AppointmentRepository appointmentRepository;
    private final GoogleCalendarClient googleCalendarClient;

    /**
     * Calcula turnos unificados cruzando la configuración de horarios de la clínica,
     * las citas activas en base de datos y los eventos externos ocupados en Google Calendar.
     */
    public List<TimeSlotDto> getUnifiedSlots(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        Optional<DoctorSchedule> scheduleOpt = doctorScheduleRepository.findByDayOfWeekAndIsActiveTrue(dayOfWeek);

        if (scheduleOpt.isEmpty()) {
            return Collections.emptyList();
        }

        DoctorSchedule schedule = scheduleOpt.get();
        List<TimeSlotDto> result = new ArrayList<>();

        LocalTime current = schedule.getStartTime();
        int slotDuration = schedule.getSlotDurationMinutes();

        LocalDateTime dayStart = LocalDateTime.of(date, schedule.getStartTime());
        LocalDateTime dayEnd = LocalDateTime.of(date, schedule.getEndTime());

        // Obtener eventos ocupados de Google Calendar de todo el día de una sola vez (optimización)
        List<TimeSlotDto> googleBusySlots = Collections.emptyList();
        if (googleCalendarClient.isClientAvailable()) {
            try {
                googleBusySlots = googleCalendarClient.getBusyTimeSlots(dayStart, dayEnd);
            } catch (Exception e) {
                log.warn("No se pudo consultar FreeBusy de Google Calendar para el día {}: {}", date, e.getMessage());
            }
        }

        while (!current.plusMinutes(slotDuration).isAfter(schedule.getEndTime())) {
            LocalDateTime slotStart = LocalDateTime.of(date, current);
            LocalDateTime slotEnd = slotStart.plusMinutes(slotDuration);

            boolean isBlocked = scheduleBlockRepository.isTimeRangeBlocked(slotStart, slotEnd);
            boolean hasAppointment = appointmentRepository.hasOverlappingAppointment(slotStart, slotEnd, null);

            final LocalDateTime startCheck = slotStart;
            final LocalDateTime endCheck = slotEnd;
            boolean hasGoogleEvent = googleBusySlots.stream().anyMatch(gSlot ->
                    gSlot.getStart().isBefore(endCheck) && gSlot.getEnd().isAfter(startCheck)
            );

            boolean isBusy = isBlocked || hasAppointment || hasGoogleEvent;
            String reason = null;
            if (isBlocked) {
                reason = "Bloqueo clínico de agenda";
            } else if (hasAppointment) {
                reason = "Cita médica existente en clínica";
            } else if (hasGoogleEvent) {
                reason = "Compromiso externo en Google Calendar";
            }

            result.add(TimeSlotDto.builder()
                    .start(slotStart)
                    .end(slotEnd)
                    .busy(isBusy)
                    .description(reason)
                    .build());

            current = current.plusMinutes(slotDuration);
        }

        return result;
    }
}
