package com.cecamed.calendar.rules;

import com.cecamed.calendar.client.GoogleCalendarClient;
import com.cecamed.calendar.dto.TimeSlotDto;
import com.cecamed.calendar.rules.DoctorScheduleValidationRule.ScheduleValidationResult;
import com.cecamed.core.model.appointment.DoctorSchedule;
import com.cecamed.core.repository.DoctorScheduleRepository;
import com.cecamed.core.repository.ScheduleBlockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DoctorScheduleValidationRule — pruebas unitarias")
class DoctorScheduleValidationRuleTest {

    @Mock
    private DoctorScheduleRepository doctorScheduleRepository;

    @Mock
    private ScheduleBlockRepository scheduleBlockRepository;

    @Mock
    private GoogleCalendarClient googleCalendarClient;

    @InjectMocks
    private DoctorScheduleValidationRule rule;

    // Lunes 09:00 – 18:00 como jornada de referencia
    private DoctorSchedule mondaySchedule;

    @BeforeEach
    void setUp() {
        mondaySchedule = new DoctorSchedule();
        mondaySchedule.setDayOfWeek(DayOfWeek.MONDAY);
        mondaySchedule.setStartTime(LocalTime.of(9, 0));
        mondaySchedule.setEndTime(LocalTime.of(18, 0));
        mondaySchedule.setIsActive(true);
    }

    // ─────────────────────────────────────────────────────────────────
    // Validaciones de entrada nula / inválida
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("validateSlot() falla con null en startTime")
    void validate_nullStart_fails() {
        ScheduleValidationResult result = rule.validateSlot(null, LocalDateTime.now().plusHours(1));
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).containsIgnoringCase("requeridos");
    }

    @Test
    @DisplayName("validateSlot() falla con null en endTime")
    void validate_nullEnd_fails() {
        ScheduleValidationResult result = rule.validateSlot(LocalDateTime.now(), null);
        assertThat(result.isValid()).isFalse();
    }

    @Test
    @DisplayName("validateSlot() falla si startTime >= endTime")
    void validate_startAfterEnd_fails() {
        LocalDateTime start = LocalDateTime.now().plusHours(2);
        LocalDateTime end   = LocalDateTime.now().plusHours(1);
        ScheduleValidationResult result = rule.validateSlot(start, end);
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).containsIgnoringCase("anterior");
    }

    // ─────────────────────────────────────────────────────────────────
    // Sin jornada configurada
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("validateSlot() falla si no hay jornada activa para ese día")
    void validate_noScheduleForDay_fails() {
        LocalDateTime start = nextMonday(10, 0);
        LocalDateTime end   = start.plusMinutes(30);

        when(doctorScheduleRepository.findByDayOfWeekAndIsActiveTrue(DayOfWeek.MONDAY))
                .thenReturn(Optional.empty());

        ScheduleValidationResult result = rule.validateSlot(start, end);
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).containsIgnoringCase("jornada laboral");
    }

    // ─────────────────────────────────────────────────────────────────
    // Fuera de horario laboral
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("validateSlot() falla si el turno empieza antes del horario laboral")
    void validate_slotBeforeWorkHours_fails() {
        LocalDateTime start = nextMonday(7, 30);  // antes de las 09:00
        LocalDateTime end   = start.plusMinutes(30);

        when(doctorScheduleRepository.findByDayOfWeekAndIsActiveTrue(DayOfWeek.MONDAY))
                .thenReturn(Optional.of(mondaySchedule));

        ScheduleValidationResult result = rule.validateSlot(start, end);
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).containsIgnoringCase("fuera del horario laboral");
    }

    @Test
    @DisplayName("validateSlot() falla si el turno termina después del horario laboral")
    void validate_slotAfterWorkHours_fails() {
        LocalDateTime start = nextMonday(17, 45);  // termina a las 18:15 — fuera
        LocalDateTime end   = start.plusMinutes(30);

        when(doctorScheduleRepository.findByDayOfWeekAndIsActiveTrue(DayOfWeek.MONDAY))
                .thenReturn(Optional.of(mondaySchedule));

        ScheduleValidationResult result = rule.validateSlot(start, end);
        assertThat(result.isValid()).isFalse();
    }

    // ─────────────────────────────────────────────────────────────────
    // Bloqueos de agenda en BD
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("validateSlot() falla si hay bloqueo de agenda en ese horario")
    void validate_blockedPeriod_fails() {
        LocalDateTime start = nextMonday(10, 0);
        LocalDateTime end   = start.plusMinutes(30);

        when(doctorScheduleRepository.findByDayOfWeekAndIsActiveTrue(DayOfWeek.MONDAY))
                .thenReturn(Optional.of(mondaySchedule));
        when(scheduleBlockRepository.isTimeRangeBlocked(start, end)).thenReturn(true);

        ScheduleValidationResult result = rule.validateSlot(start, end);
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).containsIgnoringCase("bloqueo");
    }

    // ─────────────────────────────────────────────────────────────────
    // Conflicto con Google Calendar
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("validateSlot() falla si Google Calendar indica que el slot está ocupado")
    void validate_googleCalendarBusy_fails() {
        LocalDateTime start = nextMonday(11, 0);
        LocalDateTime end   = start.plusMinutes(30);

        when(doctorScheduleRepository.findByDayOfWeekAndIsActiveTrue(DayOfWeek.MONDAY))
                .thenReturn(Optional.of(mondaySchedule));
        when(scheduleBlockRepository.isTimeRangeBlocked(start, end)).thenReturn(false);
        when(googleCalendarClient.isClientAvailable()).thenReturn(true);

        // Google Calendar reporta conflicto exacto
        TimeSlotDto busySlot = TimeSlotDto.builder()
                .start(start)
                .end(end)
                .busy(true)
                .description("Reunión externa")
                .build();
        when(googleCalendarClient.getBusyTimeSlots(start, end)).thenReturn(List.of(busySlot));

        ScheduleValidationResult result = rule.validateSlot(start, end);
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).containsIgnoringCase("Google Calendar");
    }

    @Test
    @DisplayName("validateSlot() es válido aunque Google Calendar no esté disponible")
    void validate_googleCalendarUnavailable_stillValid() {
        LocalDateTime start = nextMonday(10, 0);
        LocalDateTime end   = start.plusMinutes(30);

        when(doctorScheduleRepository.findByDayOfWeekAndIsActiveTrue(DayOfWeek.MONDAY))
                .thenReturn(Optional.of(mondaySchedule));
        when(scheduleBlockRepository.isTimeRangeBlocked(start, end)).thenReturn(false);
        when(googleCalendarClient.isClientAvailable()).thenReturn(false);

        ScheduleValidationResult result = rule.validateSlot(start, end);
        assertThat(result.isValid()).isTrue();

        // No debe consultar FreeBusy si el cliente no está disponible
        verify(googleCalendarClient, never()).getBusyTimeSlots(any(), any());
    }

    // ─────────────────────────────────────────────────────────────────
    // Caso exitoso completo
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("validateSlot() retorna válido cuando cumple jornada, sin bloqueos y sin conflicto Google")
    void validate_allConditionsMet_success() {
        LocalDateTime start = nextMonday(10, 0);
        LocalDateTime end   = start.plusMinutes(30);

        when(doctorScheduleRepository.findByDayOfWeekAndIsActiveTrue(DayOfWeek.MONDAY))
                .thenReturn(Optional.of(mondaySchedule));
        when(scheduleBlockRepository.isTimeRangeBlocked(start, end)).thenReturn(false);
        when(googleCalendarClient.isClientAvailable()).thenReturn(true);
        when(googleCalendarClient.getBusyTimeSlots(start, end)).thenReturn(Collections.emptyList());

        ScheduleValidationResult result = rule.validateSlot(start, end);
        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("ScheduleValidationResult.failure() genera un resultado inválido con mensaje")
    void scheduleValidationResult_failureFactory() {
        ScheduleValidationResult failure = ScheduleValidationResult.failure("Error de prueba");
        assertThat(failure.isValid()).isFalse();
        assertThat(failure.getErrorMessage()).isEqualTo("Error de prueba");
    }

    @Test
    @DisplayName("ScheduleValidationResult.success() genera un resultado válido sin mensaje")
    void scheduleValidationResult_successFactory() {
        ScheduleValidationResult success = ScheduleValidationResult.success();
        assertThat(success.isValid()).isTrue();
        assertThat(success.getErrorMessage()).isNull();
    }

    // ─────────────────────────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────────────────────────

    /** Devuelve el próximo lunes a la hora indicada, siempre en el futuro. */
    private LocalDateTime nextMonday(int hour, int minute) {
        LocalDateTime base = LocalDateTime.now().plusDays(1);
        while (base.getDayOfWeek() != DayOfWeek.MONDAY) {
            base = base.plusDays(1);
        }
        return base.withHour(hour).withMinute(minute).withSecond(0).withNano(0);
    }
}
