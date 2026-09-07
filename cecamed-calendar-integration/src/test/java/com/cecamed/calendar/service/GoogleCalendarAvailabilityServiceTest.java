package com.cecamed.calendar.service;

import com.cecamed.calendar.client.GoogleCalendarClient;
import com.cecamed.calendar.dto.TimeSlotDto;
import com.cecamed.calendar.exception.CalendarSyncException;
import com.cecamed.core.model.appointment.DoctorSchedule;
import com.cecamed.core.repository.AppointmentRepository;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GoogleCalendarAvailabilityService — pruebas unitarias")
class GoogleCalendarAvailabilityServiceTest {

    @Mock
    private DoctorScheduleRepository doctorScheduleRepository;

    @Mock
    private ScheduleBlockRepository scheduleBlockRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private GoogleCalendarClient googleCalendarClient;

    @InjectMocks
    private GoogleCalendarAvailabilityService availabilityService;

    // Lunes de prueba (2026-10-05 es un Lunes)
    private final LocalDate testMonday = LocalDate.of(2026, 10, 5);
    private DoctorSchedule mondaySchedule;

    @BeforeEach
    void setUp() {
        mondaySchedule = new DoctorSchedule();
        mondaySchedule.setDayOfWeek(DayOfWeek.MONDAY);
        mondaySchedule.setStartTime(LocalTime.of(9, 0));
        mondaySchedule.setEndTime(LocalTime.of(11, 0)); // 2 horas = 4 slots de 30 mins
        mondaySchedule.setSlotDurationMinutes(30);
        mondaySchedule.setIsActive(true);
    }

    @Test
    @DisplayName("getUnifiedSlots() sin jornada configurada devuelve lista vacía")
    void getUnifiedSlots_noSchedule_returnsEmptyList() {
        when(doctorScheduleRepository.findByDayOfWeekAndIsActiveTrue(DayOfWeek.MONDAY))
                .thenReturn(Optional.empty());

        List<TimeSlotDto> slots = availabilityService.getUnifiedSlots(testMonday);

        assertThat(slots).isEmpty();
        verify(googleCalendarClient, never()).getBusyTimeSlots(any(), any());
    }

    @Test
    @DisplayName("getUnifiedSlots() sin bloqueos, citas ni eventos Google retorna todos los slots libres")
    void getUnifiedSlots_allFree_returnsAllAvailableSlots() {
        when(doctorScheduleRepository.findByDayOfWeekAndIsActiveTrue(DayOfWeek.MONDAY))
                .thenReturn(Optional.of(mondaySchedule));
        when(googleCalendarClient.isClientAvailable()).thenReturn(true);
        when(googleCalendarClient.getBusyTimeSlots(any(), any())).thenReturn(Collections.emptyList());
        when(scheduleBlockRepository.isTimeRangeBlocked(any(), any())).thenReturn(false);
        when(appointmentRepository.hasOverlappingAppointment(any(), any(), eq(null))).thenReturn(false);

        List<TimeSlotDto> slots = availabilityService.getUnifiedSlots(testMonday);

        assertThat(slots).hasSize(4);
        assertThat(slots).allMatch(s -> !s.isBusy());
        assertThat(slots.get(0).getStart()).isEqualTo(LocalDateTime.of(testMonday, LocalTime.of(9, 0)));
        assertThat(slots.get(0).getEnd()).isEqualTo(LocalDateTime.of(testMonday, LocalTime.of(9, 30)));
        assertThat(slots.get(3).getEnd()).isEqualTo(LocalDateTime.of(testMonday, LocalTime.of(11, 0)));
    }

    @Test
    @DisplayName("getUnifiedSlots() detecta slot bloqueado por bloqueo clínico de agenda")
    void getUnifiedSlots_scheduleBlock_marksSlotBusy() {
        when(doctorScheduleRepository.findByDayOfWeekAndIsActiveTrue(DayOfWeek.MONDAY))
                .thenReturn(Optional.of(mondaySchedule));
        when(googleCalendarClient.isClientAvailable()).thenReturn(false);

        LocalDateTime blockStart = LocalDateTime.of(testMonday, LocalTime.of(9, 30));
        LocalDateTime blockEnd   = LocalDateTime.of(testMonday, LocalTime.of(10, 0));

        when(scheduleBlockRepository.isTimeRangeBlocked(any(), any())).thenAnswer(inv -> {
            LocalDateTime s = inv.getArgument(0);
            return s.equals(blockStart);
        });
        when(appointmentRepository.hasOverlappingAppointment(any(), any(), eq(null))).thenReturn(false);

        List<TimeSlotDto> slots = availabilityService.getUnifiedSlots(testMonday);

        assertThat(slots).hasSize(4);
        TimeSlotDto blockedSlot = slots.get(1); // 09:30 - 10:00
        assertThat(blockedSlot.isBusy()).isTrue();
        assertThat(blockedSlot.getDescription()).containsIgnoringCase("bloqueo clínico");

        TimeSlotDto freeSlot = slots.get(0); // 09:00 - 09:30
        assertThat(freeSlot.isBusy()).isFalse();
    }

    @Test
    @DisplayName("getUnifiedSlots() detecta slot ocupado por cita existente en BD")
    void getUnifiedSlots_existingAppointment_marksSlotBusy() {
        when(doctorScheduleRepository.findByDayOfWeekAndIsActiveTrue(DayOfWeek.MONDAY))
                .thenReturn(Optional.of(mondaySchedule));
        when(googleCalendarClient.isClientAvailable()).thenReturn(false);

        LocalDateTime apptStart = LocalDateTime.of(testMonday, LocalTime.of(10, 0));
        LocalDateTime apptEnd   = LocalDateTime.of(testMonday, LocalTime.of(10, 30));

        when(scheduleBlockRepository.isTimeRangeBlocked(any(), any())).thenReturn(false);
        when(appointmentRepository.hasOverlappingAppointment(any(), any(), eq(null))).thenAnswer(inv -> {
            LocalDateTime s = inv.getArgument(0);
            return s.equals(apptStart);
        });

        List<TimeSlotDto> slots = availabilityService.getUnifiedSlots(testMonday);

        TimeSlotDto busySlot = slots.get(2); // 10:00 - 10:30
        assertThat(busySlot.isBusy()).isTrue();
        assertThat(busySlot.getDescription()).containsIgnoringCase("cita médica");
    }

    @Test
    @DisplayName("getUnifiedSlots() detecta slot ocupado por evento externo en Google Calendar")
    void getUnifiedSlots_googleCalendarBusy_marksSlotBusy() {
        when(doctorScheduleRepository.findByDayOfWeekAndIsActiveTrue(DayOfWeek.MONDAY))
                .thenReturn(Optional.of(mondaySchedule));
        when(googleCalendarClient.isClientAvailable()).thenReturn(true);

        LocalDateTime gStart = LocalDateTime.of(testMonday, LocalTime.of(10, 30));
        LocalDateTime gEnd   = LocalDateTime.of(testMonday, LocalTime.of(11, 0));

        TimeSlotDto googleBusy = TimeSlotDto.builder()
                .start(gStart)
                .end(gEnd)
                .busy(true)
                .description("Evento externo")
                .build();

        when(googleCalendarClient.getBusyTimeSlots(any(), any())).thenReturn(List.of(googleBusy));
        when(scheduleBlockRepository.isTimeRangeBlocked(any(), any())).thenReturn(false);
        when(appointmentRepository.hasOverlappingAppointment(any(), any(), eq(null))).thenReturn(false);

        List<TimeSlotDto> slots = availabilityService.getUnifiedSlots(testMonday);

        TimeSlotDto busySlot = slots.get(3); // 10:30 - 11:00
        assertThat(busySlot.isBusy()).isTrue();
        assertThat(busySlot.getDescription()).containsIgnoringCase("Google Calendar");
    }

    @Test
    @DisplayName("getUnifiedSlots() maneja excepción en Google Calendar sin interrumpir cálculo de BD")
    void getUnifiedSlots_googleApiThrows_fallsBackGracefully() {
        when(doctorScheduleRepository.findByDayOfWeekAndIsActiveTrue(DayOfWeek.MONDAY))
                .thenReturn(Optional.of(mondaySchedule));
        when(googleCalendarClient.isClientAvailable()).thenReturn(true);
        when(googleCalendarClient.getBusyTimeSlots(any(), any()))
                .thenThrow(new CalendarSyncException("Fallo en FreeBusy"));

        when(scheduleBlockRepository.isTimeRangeBlocked(any(), any())).thenReturn(false);
        when(appointmentRepository.hasOverlappingAppointment(any(), any(), eq(null))).thenReturn(false);

        List<TimeSlotDto> slots = availabilityService.getUnifiedSlots(testMonday);

        assertThat(slots).hasSize(4);
        assertThat(slots).allMatch(s -> !s.isBusy());
    }
}
