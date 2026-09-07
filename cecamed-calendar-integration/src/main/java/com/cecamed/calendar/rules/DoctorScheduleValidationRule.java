package com.cecamed.calendar.rules;

import com.cecamed.calendar.client.GoogleCalendarClient;
import com.cecamed.calendar.dto.TimeSlotDto;
import com.cecamed.core.model.appointment.DoctorSchedule;
import com.cecamed.core.repository.DoctorScheduleRepository;
import com.cecamed.core.repository.ScheduleBlockRepository;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DoctorScheduleValidationRule {

    private final DoctorScheduleRepository doctorScheduleRepository;
    private final ScheduleBlockRepository scheduleBlockRepository;
    private final GoogleCalendarClient googleCalendarClient;

    @Getter
    @Builder
    public static class ScheduleValidationResult {
        private final boolean valid;
        private final String errorMessage;

        public static ScheduleValidationResult success() {
            return ScheduleValidationResult.builder().valid(true).build();
        }

        public static ScheduleValidationResult failure(String message) {
            return ScheduleValidationResult.builder().valid(false).errorMessage(message).build();
        }
    }

    /**
     * Valida si un intervalo [startTime, endTime) cumple con las jornadas del médico,
     * no colisiona con bloqueos de agenda clínica y no coincide con eventos ocupados en Google Calendar.
     */
    public ScheduleValidationResult validateSlot(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            return ScheduleValidationResult.failure("Los horarios de inicio y fin son requeridos");
        }

        if (!startTime.isBefore(endTime)) {
            return ScheduleValidationResult.failure("La hora de inicio debe ser anterior a la hora de finalización");
        }

        DayOfWeek dayOfWeek = startTime.getDayOfWeek();
        Optional<DoctorSchedule> scheduleOpt = doctorScheduleRepository.findByDayOfWeekAndIsActiveTrue(dayOfWeek);

        if (scheduleOpt.isEmpty()) {
            return ScheduleValidationResult.failure("El médico no tiene jornada laboral configurada para el día " + dayOfWeek);
        }

        DoctorSchedule schedule = scheduleOpt.get();
        LocalTime slotStartTime = startTime.toLocalTime();
        LocalTime slotEndTime = endTime.toLocalTime();

        if (slotStartTime.isBefore(schedule.getStartTime()) || slotEndTime.isAfter(schedule.getEndTime())) {
            return ScheduleValidationResult.failure(String.format(
                    "El turno solicitado (%s - %s) está fuera del horario laboral del médico (%s - %s)",
                    slotStartTime, slotEndTime, schedule.getStartTime(), schedule.getEndTime()
            ));
        }

        // Validación contra bloqueos de agenda en base de datos
        if (scheduleBlockRepository.isTimeRangeBlocked(startTime, endTime)) {
            return ScheduleValidationResult.failure("El horario coincide con un bloqueo clínico de agenda (feriado, descanso o capacitación)");
        }

        // Validación contra disponibilidad FreeBusy en Google Calendar (si está conectado)
        if (googleCalendarClient.isClientAvailable()) {
            List<TimeSlotDto> busySlots = googleCalendarClient.getBusyTimeSlots(startTime, endTime);
            boolean hasGoogleConflict = busySlots.stream().anyMatch(slot ->
                    slot.getStart().isBefore(endTime) && slot.getEnd().isAfter(startTime)
            );

            if (hasGoogleConflict) {
                return ScheduleValidationResult.failure("El horario coincide con un compromiso externo en Google Calendar");
            }
        }

        return ScheduleValidationResult.success();
    }
}
