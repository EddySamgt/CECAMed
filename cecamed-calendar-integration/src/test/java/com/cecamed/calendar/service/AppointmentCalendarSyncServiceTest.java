package com.cecamed.calendar.service;

import com.cecamed.calendar.client.GoogleCalendarClient;
import com.cecamed.calendar.config.GoogleCalendarProperties;
import com.cecamed.calendar.dto.CalendarEventDto;
import com.cecamed.calendar.dto.CalendarSyncResultDto;
import com.cecamed.calendar.exception.CalendarSyncException;
import com.cecamed.core.model.appointment.Appointment;
import com.cecamed.core.model.appointment.ScheduleBlock;
import com.cecamed.core.model.appointment.enums.AppointmentStatus;
import com.cecamed.core.model.appointment.enums.BlockType;
import com.cecamed.core.model.appointment.enums.GoogleSyncStatus;
import com.cecamed.core.model.patient.Patient;
import com.cecamed.core.repository.AppointmentRepository;
import com.cecamed.core.repository.ScheduleBlockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AppointmentCalendarSyncService — pruebas unitarias")
class AppointmentCalendarSyncServiceTest {

    @Mock
    private GoogleCalendarClient googleCalendarClient;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private ScheduleBlockRepository scheduleBlockRepository;

    @Mock
    private GoogleCalendarProperties properties;

    private AppointmentCalendarSyncService syncService;

    private Patient patient;
    private Appointment appointment;

    @BeforeEach
    void setUp() {
        lenient().when(properties.isEnabled()).thenReturn(true);
        lenient().when(properties.getCalendarId()).thenReturn("primary");

        syncService = new AppointmentCalendarSyncService(
                googleCalendarClient,
                appointmentRepository,
                scheduleBlockRepository,
                properties
        );

        patient = Patient.builder()
                .id(1L)
                .firstName("Carlos")
                .lastName("Gómez")
                .identificationNumber("ID-987654")
                .phone("555-4321")
                .email("carlos@example.com")
                .build();

        appointment = Appointment.builder()
                .id(100L)
                .patient(patient)
                .startTime(LocalDateTime.now().plusDays(2).withHour(10).withMinute(0))
                .endTime(LocalDateTime.now().plusDays(2).withHour(10).withMinute(30))
                .reasonForVisit("Consulta periódica")
                .status(AppointmentStatus.PROGRAMADA)
                .googleSyncStatus(GoogleSyncStatus.PENDING)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────
    // syncAppointment
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("syncAppointment() con integración deshabilitada marca como NOT_APPLICABLE")
    void syncAppointment_disabled_marksNotApplicable() {
        when(properties.isEnabled()).thenReturn(false);

        CalendarSyncResultDto result = syncService.syncAppointment(appointment);

        assertThat(result.getStatus()).isEqualTo(GoogleSyncStatus.NOT_APPLICABLE);
        assertThat(appointment.getGoogleSyncStatus()).isEqualTo(GoogleSyncStatus.NOT_APPLICABLE);
        verify(appointmentRepository).save(appointment);
        verify(googleCalendarClient, never()).createEvent(any());
    }

    @Test
    @DisplayName("syncAppointment() con cliente no disponible marca como PENDING")
    void syncAppointment_clientUnavailable_marksPending() {
        when(properties.isEnabled()).thenReturn(true);
        when(googleCalendarClient.isClientAvailable()).thenReturn(false);

        CalendarSyncResultDto result = syncService.syncAppointment(appointment);

        assertThat(result.getStatus()).isEqualTo(GoogleSyncStatus.PENDING);
        assertThat(appointment.getGoogleSyncStatus()).isEqualTo(GoogleSyncStatus.PENDING);
        verify(appointmentRepository).save(appointment);
        verify(googleCalendarClient, never()).createEvent(any());
    }

    @Test
    @DisplayName("syncAppointment() sin googleEventId previo crea evento y marca SYNCED")
    void syncAppointment_newAppointment_createsEventAndMarksSynced() {
        when(properties.isEnabled()).thenReturn(true);
        when(googleCalendarClient.isClientAvailable()).thenReturn(true);

        CalendarEventDto createdEvent = CalendarEventDto.builder()
                .id("g-event-123")
                .htmlLink("https://calendar.google.com/event/123")
                .build();
        when(googleCalendarClient.createEvent(any(CalendarEventDto.class))).thenReturn(createdEvent);

        CalendarSyncResultDto result = syncService.syncAppointment(appointment);

        assertThat(result.getStatus()).isEqualTo(GoogleSyncStatus.SYNCED);
        assertThat(result.getGoogleEventId()).isEqualTo("g-event-123");
        assertThat(appointment.getGoogleEventId()).isEqualTo("g-event-123");
        assertThat(appointment.getGoogleHtmlLink()).isEqualTo("https://calendar.google.com/event/123");
        assertThat(appointment.getGoogleSyncStatus()).isEqualTo(GoogleSyncStatus.SYNCED);
        assertThat(appointment.getGoogleLastSyncedAt()).isNotNull();
        verify(appointmentRepository).save(appointment);
    }

    @Test
    @DisplayName("syncAppointment() con googleEventId existente actualiza evento y marca SYNCED")
    void syncAppointment_existingGoogleEventId_updatesEventAndMarksSynced() {
        appointment.setGoogleEventId("g-event-existing");
        when(properties.isEnabled()).thenReturn(true);
        when(googleCalendarClient.isClientAvailable()).thenReturn(true);

        CalendarEventDto updatedEvent = CalendarEventDto.builder()
                .id("g-event-existing")
                .htmlLink("https://calendar.google.com/event/existing")
                .build();
        when(googleCalendarClient.updateEvent(eq("g-event-existing"), any(CalendarEventDto.class)))
                .thenReturn(updatedEvent);

        CalendarSyncResultDto result = syncService.syncAppointment(appointment);

        assertThat(result.getStatus()).isEqualTo(GoogleSyncStatus.SYNCED);
        assertThat(appointment.getGoogleSyncStatus()).isEqualTo(GoogleSyncStatus.SYNCED);
        verify(googleCalendarClient).updateEvent(eq("g-event-existing"), any(CalendarEventDto.class));
        verify(googleCalendarClient, never()).createEvent(any());
    }

    @Test
    @DisplayName("syncAppointment() ante error de API marca como FAILED")
    void syncAppointment_apiError_marksFailed() {
        when(properties.isEnabled()).thenReturn(true);
        when(googleCalendarClient.isClientAvailable()).thenReturn(true);
        when(googleCalendarClient.createEvent(any(CalendarEventDto.class)))
                .thenThrow(new CalendarSyncException("Error de red con Google"));

        CalendarSyncResultDto result = syncService.syncAppointment(appointment);

        assertThat(result.getStatus()).isEqualTo(GoogleSyncStatus.FAILED);
        assertThat(appointment.getGoogleSyncStatus()).isEqualTo(GoogleSyncStatus.FAILED);
        verify(appointmentRepository).save(appointment);
    }

    // ─────────────────────────────────────────────────────────────────
    // cancelAppointmentEvent
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("cancelAppointmentEvent() sin googleEventId marca como NOT_APPLICABLE")
    void cancelAppointment_noGoogleEventId_marksNotApplicable() {
        appointment.setGoogleEventId(null);

        CalendarSyncResultDto result = syncService.cancelAppointmentEvent(appointment);

        assertThat(result.getStatus()).isEqualTo(GoogleSyncStatus.NOT_APPLICABLE);
        verify(googleCalendarClient, never()).deleteEvent(anyString());
    }

    @Test
    @DisplayName("cancelAppointmentEvent() con integración deshabilitada limpia evento y marca NOT_APPLICABLE")
    void cancelAppointment_disabled_clearsAndMarksNotApplicable() {
        appointment.setGoogleEventId("g-event-to-cancel");
        when(properties.isEnabled()).thenReturn(false);

        CalendarSyncResultDto result = syncService.cancelAppointmentEvent(appointment);

        assertThat(result.getStatus()).isEqualTo(GoogleSyncStatus.NOT_APPLICABLE);
        assertThat(appointment.getGoogleEventId()).isNull();
        verify(googleCalendarClient, never()).deleteEvent(anyString());
    }

    @Test
    @DisplayName("cancelAppointmentEvent() con cliente no disponible marca PENDING sin borrar ID")
    void cancelAppointment_clientUnavailable_marksPending() {
        appointment.setGoogleEventId("g-event-to-cancel");
        when(properties.isEnabled()).thenReturn(true);
        when(googleCalendarClient.isClientAvailable()).thenReturn(false);

        CalendarSyncResultDto result = syncService.cancelAppointmentEvent(appointment);

        assertThat(result.getStatus()).isEqualTo(GoogleSyncStatus.PENDING);
        assertThat(appointment.getGoogleEventId()).isEqualTo("g-event-to-cancel");
        verify(googleCalendarClient, never()).deleteEvent(anyString());
    }

    @Test
    @DisplayName("cancelAppointmentEvent() con cliente disponible elimina evento y marca SYNCED")
    void cancelAppointment_clientAvailable_deletesEventAndMarksSynced() {
        appointment.setGoogleEventId("g-event-to-cancel");
        when(properties.isEnabled()).thenReturn(true);
        when(googleCalendarClient.isClientAvailable()).thenReturn(true);

        CalendarSyncResultDto result = syncService.cancelAppointmentEvent(appointment);

        assertThat(result.getStatus()).isEqualTo(GoogleSyncStatus.SYNCED);
        assertThat(appointment.getGoogleEventId()).isNull();
        assertThat(appointment.getGoogleHtmlLink()).isNull();
        verify(googleCalendarClient).deleteEvent("g-event-to-cancel");
        verify(appointmentRepository).save(appointment);
    }

    @Test
    @DisplayName("cancelAppointmentEvent() ante error al eliminar evento marca FAILED")
    void cancelAppointment_deleteFails_marksFailed() {
        appointment.setGoogleEventId("g-event-to-cancel");
        when(properties.isEnabled()).thenReturn(true);
        when(googleCalendarClient.isClientAvailable()).thenReturn(true);
        doThrow(new CalendarSyncException("Fallo al eliminar"))
                .when(googleCalendarClient).deleteEvent("g-event-to-cancel");

        CalendarSyncResultDto result = syncService.cancelAppointmentEvent(appointment);

        assertThat(result.getStatus()).isEqualTo(GoogleSyncStatus.FAILED);
        assertThat(appointment.getGoogleSyncStatus()).isEqualTo(GoogleSyncStatus.FAILED);
        verify(appointmentRepository).save(appointment);
    }

    // ─────────────────────────────────────────────────────────────────
    // syncScheduleBlock y deleteScheduleBlockEvent
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("syncScheduleBlock() con integración deshabilitada no hace nada")
    void syncScheduleBlock_disabled_doesNothing() {
        when(properties.isEnabled()).thenReturn(false);
        ScheduleBlock block = ScheduleBlock.builder().id(1L).title("Vacaciones").build();

        syncService.syncScheduleBlock(block);

        verify(googleCalendarClient, never()).createEvent(any());
        verify(scheduleBlockRepository, never()).save(any());
    }

    @Test
    @DisplayName("syncScheduleBlock() con cliente disponible crea evento y asigna ID")
    void syncScheduleBlock_createsEventAndSavesBlock() {
        when(properties.isEnabled()).thenReturn(true);
        when(googleCalendarClient.isClientAvailable()).thenReturn(true);

        ScheduleBlock block = ScheduleBlock.builder()
                .id(1L)
                .title("Capacitación Médica")
                .reason("Congreso de Cardiología")
                .blockType(BlockType.CAPACITACION)
                .startDateTime(LocalDateTime.now().plusDays(5).withHour(8).withMinute(0))
                .endDateTime(LocalDateTime.now().plusDays(5).withHour(14).withMinute(0))
                .build();

        CalendarEventDto created = CalendarEventDto.builder().id("g-block-001").build();
        when(googleCalendarClient.createEvent(any(CalendarEventDto.class))).thenReturn(created);

        syncService.syncScheduleBlock(block);

        assertThat(block.getGoogleEventId()).isEqualTo("g-block-001");
        verify(scheduleBlockRepository).save(block);
    }

    @Test
    @DisplayName("deleteScheduleBlockEvent() elimina evento de Google Calendar si tiene ID")
    void deleteScheduleBlockEvent_deletesEventIfExists() {
        when(properties.isEnabled()).thenReturn(true);
        when(googleCalendarClient.isClientAvailable()).thenReturn(true);

        ScheduleBlock block = ScheduleBlock.builder()
                .id(1L)
                .title("Feriado")
                .googleEventId("g-block-999")
                .build();

        syncService.deleteScheduleBlockEvent(block);

        verify(googleCalendarClient).deleteEvent("g-block-999");
    }

    // ─────────────────────────────────────────────────────────────────
    // retryPendingAndFailedSyncs
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("retryPendingAndFailedSyncs() procesa tanto citas pendientes como fallidas")
    void retryPendingAndFailedSyncs_retriesBothCategories() {
        when(properties.isEnabled()).thenReturn(true);
        when(googleCalendarClient.isClientAvailable()).thenReturn(true);

        Appointment pending = Appointment.builder()
                .id(201L)
                .patient(patient)
                .startTime(LocalDateTime.now().plusDays(1).withHour(9).withMinute(0))
                .endTime(LocalDateTime.now().plusDays(1).withHour(9).withMinute(30))
                .reasonForVisit("Consulta 1")
                .status(AppointmentStatus.PROGRAMADA)
                .googleSyncStatus(GoogleSyncStatus.PENDING)
                .build();

        Appointment failed = Appointment.builder()
                .id(202L)
                .patient(patient)
                .startTime(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0))
                .endTime(LocalDateTime.now().plusDays(1).withHour(10).withMinute(30))
                .reasonForVisit("Consulta 2")
                .status(AppointmentStatus.PROGRAMADA)
                .googleSyncStatus(GoogleSyncStatus.FAILED)
                .build();

        when(appointmentRepository.findAllByGoogleSyncStatus(GoogleSyncStatus.PENDING))
                .thenReturn(List.of(pending));
        when(appointmentRepository.findAllByGoogleSyncStatus(GoogleSyncStatus.FAILED))
                .thenReturn(List.of(failed));

        when(googleCalendarClient.createEvent(any(CalendarEventDto.class)))
                .thenReturn(CalendarEventDto.builder().id("g-new-id").build());

        int syncedCount = syncService.retryPendingAndFailedSyncs();

        assertThat(syncedCount).isEqualTo(2);
        assertThat(pending.getGoogleSyncStatus()).isEqualTo(GoogleSyncStatus.SYNCED);
        assertThat(failed.getGoogleSyncStatus()).isEqualTo(GoogleSyncStatus.SYNCED);
    }
}
