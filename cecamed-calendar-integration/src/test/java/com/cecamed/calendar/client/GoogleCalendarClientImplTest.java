package com.cecamed.calendar.client;

import com.cecamed.calendar.config.GoogleCalendarProperties;
import com.cecamed.calendar.dto.CalendarEventDto;
import com.cecamed.calendar.dto.TimeSlotDto;
import com.cecamed.calendar.exception.CalendarSyncException;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import com.google.api.services.calendar.model.FreeBusyCalendar;
import com.google.api.services.calendar.model.FreeBusyResponse;
import com.google.api.services.calendar.model.TimePeriod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GoogleCalendarClientImpl — pruebas unitarias")
class GoogleCalendarClientImplTest {

    @Mock
    private Calendar mockCalendar;

    @Mock
    private GoogleCalendarProperties properties;

    private GoogleCalendarClientImpl clientWithCalendar;
    private GoogleCalendarClientImpl clientOffline;

    private static final String CALENDAR_ID = "test-calendar-id";
    private static final String TIME_ZONE   = "America/Mexico_City";

    @BeforeEach
    void setUp() {
        // Cliente con Google Calendar disponible (lenient para permitir pruebas offline y casos deshabilitados)
        lenient().when(properties.getTimeZone()).thenReturn(TIME_ZONE);
        lenient().when(properties.getCalendarId()).thenReturn(CALENDAR_ID);
        lenient().when(properties.isEnabled()).thenReturn(true);

        clientWithCalendar = new GoogleCalendarClientImpl(mockCalendar, properties);
        clientOffline      = new GoogleCalendarClientImpl(null, properties);
    }

    // ─────────────────────────────────────────────────────────────────
    // Modo offline / sin credenciales
    // ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Modo offline (Calendar bean == null)")
    class OfflineMode {

        @Test
        @DisplayName("isClientAvailable() devuelve false cuando no hay bean Calendar")
        void isClientAvailable_returnsFalseWhenNull() {
            assertThat(clientOffline.isClientAvailable()).isFalse();
        }

        @Test
        @DisplayName("createEvent() en modo offline devuelve evento simulado con ID mock-event-*")
        void createEvent_offlineReturnsMockEvent() {
            CalendarEventDto dto = buildEventDto();
            CalendarEventDto result = clientOffline.createEvent(dto);

            assertThat(result.getId()).startsWith("mock-event-");
            assertThat(result.getSummary()).isEqualTo(dto.getSummary());
            assertThat(result.getStartDateTime()).isEqualTo(dto.getStartDateTime());
        }

        @Test
        @DisplayName("updateEvent() en modo offline devuelve el mismo DTO de entrada")
        void updateEvent_offlineReturnsInputDto() {
            CalendarEventDto dto = buildEventDto();
            CalendarEventDto result = clientOffline.updateEvent("some-id", dto);
            assertThat(result).isSameAs(dto);
        }

        @Test
        @DisplayName("deleteEvent() en modo offline no lanza excepción")
        void deleteEvent_offlineDoesNotThrow() {
            // No debe lanzar excepción
            clientOffline.deleteEvent("some-id");
        }

        @Test
        @DisplayName("getEvent() en modo offline devuelve Optional vacío")
        void getEvent_offlineReturnsEmpty() {
            assertThat(clientOffline.getEvent("some-id")).isEmpty();
        }

        @Test
        @DisplayName("listEvents() en modo offline devuelve lista vacía")
        void listEvents_offlineReturnsEmptyList() {
            LocalDateTime start = LocalDateTime.now();
            LocalDateTime end   = start.plusHours(2);
            assertThat(clientOffline.listEvents(start, end)).isEmpty();
        }

        @Test
        @DisplayName("getBusyTimeSlots() en modo offline devuelve lista vacía")
        void getBusySlots_offlineReturnsEmptyList() {
            LocalDateTime start = LocalDateTime.now();
            LocalDateTime end   = start.plusHours(2);
            assertThat(clientOffline.getBusyTimeSlots(start, end)).isEmpty();
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Modo online — isClientAvailable
    // ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Modo online (Calendar bean disponible)")
    class OnlineMode {

        @Test
        @DisplayName("isClientAvailable() devuelve true cuando Calendar no es null y enabled=true")
        void isClientAvailable_returnsTrueWhenCalendarNotNull() {
            assertThat(clientWithCalendar.isClientAvailable()).isTrue();
        }

        @Test
        @DisplayName("isClientAvailable() devuelve false si enabled=false aunque Calendar exista")
        void isClientAvailable_returnsFalseWhenDisabled() {
            when(properties.isEnabled()).thenReturn(false);
            // Recrea el cliente con el mismo mockCalendar pero enabled=false
            GoogleCalendarClientImpl disabledClient =
                    new GoogleCalendarClientImpl(mockCalendar, properties);
            assertThat(disabledClient.isClientAvailable()).isFalse();
        }

        // ── createEvent ──

        @Test
        @DisplayName("createEvent() delega en googleCalendar.events().insert() y mapea la respuesta")
        void createEvent_successfullyInsertsEvent() throws IOException {
            CalendarEventDto dto = buildEventDto();
            Event fakeCreated = buildGoogleEvent("google-id-001", dto.getSummary());

            Calendar.Events events = mock(Calendar.Events.class);
            Calendar.Events.Insert insert = mock(Calendar.Events.Insert.class);

            when(mockCalendar.events()).thenReturn(events);
            when(events.insert(anyString(), any(Event.class))).thenReturn(insert);
            when(insert.execute()).thenReturn(fakeCreated);

            CalendarEventDto result = clientWithCalendar.createEvent(dto);

            assertThat(result.getId()).isEqualTo("google-id-001");
            assertThat(result.getSummary()).isEqualTo(dto.getSummary());
            verify(events).insert(eq(CALENDAR_ID), any(Event.class));
        }

        @Test
        @DisplayName("createEvent() convierte IOException en CalendarSyncException")
        void createEvent_ioExceptionWrappedAsCalendarSyncException() throws IOException {
            Calendar.Events events = mock(Calendar.Events.class);
            Calendar.Events.Insert insert = mock(Calendar.Events.Insert.class);

            when(mockCalendar.events()).thenReturn(events);
            when(events.insert(anyString(), any(Event.class))).thenReturn(insert);
            when(insert.execute()).thenThrow(new IOException("timeout"));

            assertThatThrownBy(() -> clientWithCalendar.createEvent(buildEventDto()))
                    .isInstanceOf(CalendarSyncException.class)
                    .hasMessageContaining("timeout");
        }

        // ── updateEvent ──

        @Test
        @DisplayName("updateEvent() con 404 vuelve a llamar createEvent() en lugar de fallar")
        void updateEvent_notFoundFallsBackToCreate() throws IOException {
            CalendarEventDto dto = buildEventDto();
            Event fakeCreated = buildGoogleEvent("new-id-fallback", dto.getSummary());

            Calendar.Events events = mock(Calendar.Events.class);
            Calendar.Events.Patch patch = mock(Calendar.Events.Patch.class);
            Calendar.Events.Insert insert = mock(Calendar.Events.Insert.class);

            when(mockCalendar.events()).thenReturn(events);
            when(events.patch(anyString(), anyString(), any(Event.class))).thenReturn(patch);
            when(patch.execute()).thenThrow(build404Exception());

            when(events.insert(anyString(), any(Event.class))).thenReturn(insert);
            when(insert.execute()).thenReturn(fakeCreated);

            CalendarEventDto result = clientWithCalendar.updateEvent("old-event-id", dto);
            assertThat(result.getId()).isEqualTo("new-id-fallback");
        }

        // ── deleteEvent ──

        @Test
        @DisplayName("deleteEvent() con 404 no lanza excepción (idempotente)")
        void deleteEvent_notFoundIsIdempotent() throws IOException {
            Calendar.Events events = mock(Calendar.Events.class);
            Calendar.Events.Delete delete = mock(Calendar.Events.Delete.class);

            when(mockCalendar.events()).thenReturn(events);
            when(events.delete(anyString(), anyString())).thenReturn(delete);
            doThrow(build404Exception()).when(delete).execute();

            // No debe lanzar excepción
            clientWithCalendar.deleteEvent("non-existent-id");
        }

        // ── getEvent ──

        @Test
        @DisplayName("getEvent() con 404 devuelve Optional vacío")
        void getEvent_notFoundReturnsEmpty() throws IOException {
            Calendar.Events events = mock(Calendar.Events.class);
            Calendar.Events.Get get = mock(Calendar.Events.Get.class);

            when(mockCalendar.events()).thenReturn(events);
            when(events.get(anyString(), anyString())).thenReturn(get);
            when(get.execute()).thenThrow(build404Exception());

            Optional<CalendarEventDto> result = clientWithCalendar.getEvent("unknown-id");
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("getEvent() exitoso devuelve Optional con el evento mapeado")
        void getEvent_successReturnsPresent() throws IOException {
            Event googleEvent = buildGoogleEvent("evt-123", "Consulta Dr. Pérez");

            Calendar.Events events = mock(Calendar.Events.class);
            Calendar.Events.Get get = mock(Calendar.Events.Get.class);

            when(mockCalendar.events()).thenReturn(events);
            when(events.get(anyString(), anyString())).thenReturn(get);
            when(get.execute()).thenReturn(googleEvent);

            Optional<CalendarEventDto> result = clientWithCalendar.getEvent("evt-123");
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo("evt-123");
        }

        // ── listEvents ──

        @Test
        @DisplayName("listEvents() con items null devuelve lista vacía sin NPE")
        void listEvents_nullItemsReturnsEmptyList() throws IOException {
            Events emptyEvents = new Events();
            emptyEvents.setItems(null);

            Calendar.Events events = mock(Calendar.Events.class);
            Calendar.Events.List list = mock(Calendar.Events.List.class);

            when(mockCalendar.events()).thenReturn(events);
            when(events.list(anyString())).thenReturn(list);
            when(list.setTimeMin(any())).thenReturn(list);
            when(list.setTimeMax(any())).thenReturn(list);
            when(list.setSingleEvents(true)).thenReturn(list);
            when(list.setOrderBy("startTime")).thenReturn(list);
            when(list.execute()).thenReturn(emptyEvents);

            List<CalendarEventDto> result = clientWithCalendar.listEvents(
                    LocalDateTime.now(), LocalDateTime.now().plusHours(1));
            assertThat(result).isEmpty();
        }

        // ── getBusyTimeSlots ──

        @Test
        @DisplayName("getBusyTimeSlots() mapea correctamente los periodos ocupados de FreeBusy")
        void getBusyTimeSlots_mapsPeriodsCorrectly() throws IOException {
            long nowMs = System.currentTimeMillis();
            long hourMs = 3_600_000L;

            TimePeriod period = new TimePeriod()
                    .setStart(new DateTime(nowMs))
                    .setEnd(new DateTime(nowMs + hourMs));

            FreeBusyCalendar cal = new FreeBusyCalendar();
            cal.setBusy(List.of(period));

            FreeBusyResponse freeBusyResponse = new FreeBusyResponse();
            freeBusyResponse.setCalendars(Map.of(CALENDAR_ID, cal));

            Calendar.Freebusy freebusy = mock(Calendar.Freebusy.class);
            Calendar.Freebusy.Query query = mock(Calendar.Freebusy.Query.class);

            when(mockCalendar.freebusy()).thenReturn(freebusy);
            when(freebusy.query(any())).thenReturn(query);
            when(query.execute()).thenReturn(freeBusyResponse);

            List<TimeSlotDto> busySlots = clientWithCalendar.getBusyTimeSlots(
                    LocalDateTime.now(), LocalDateTime.now().plusHours(2));

            assertThat(busySlots).hasSize(1);
            assertThat(busySlots.get(0).isBusy()).isTrue();
        }

        @Test
        @DisplayName("getBusyTimeSlots() con calendarioId no encontrado en respuesta devuelve lista vacía")
        void getBusyTimeSlots_calendarNotInResponseReturnsEmpty() throws IOException {
            FreeBusyResponse freeBusyResponse = new FreeBusyResponse();
            freeBusyResponse.setCalendars(Collections.emptyMap());

            Calendar.Freebusy freebusy = mock(Calendar.Freebusy.class);
            Calendar.Freebusy.Query query = mock(Calendar.Freebusy.Query.class);

            when(mockCalendar.freebusy()).thenReturn(freebusy);
            when(freebusy.query(any())).thenReturn(query);
            when(query.execute()).thenReturn(freeBusyResponse);

            List<TimeSlotDto> result = clientWithCalendar.getBusyTimeSlots(
                    LocalDateTime.now(), LocalDateTime.now().plusHours(2));
            assertThat(result).isEmpty();
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────

    private CalendarEventDto buildEventDto() {
        return CalendarEventDto.builder()
                .summary("Consulta - Juan Pérez")
                .description("Primera consulta")
                .location("Consultorio 1")
                .startDateTime(LocalDateTime.now().plusDays(1).withHour(9).withMinute(0))
                .endDateTime(LocalDateTime.now().plusDays(1).withHour(9).withMinute(30))
                .timeZone(TIME_ZONE)
                .build();
    }

    private Event buildGoogleEvent(String id, String summary) {
        long nowMs = System.currentTimeMillis();
        Event event = new Event();
        event.setId(id);
        event.setSummary(summary);
        event.setStatus("confirmed");
        event.setStart(new EventDateTime().setDateTime(new DateTime(nowMs)));
        event.setEnd(new EventDateTime().setDateTime(new DateTime(nowMs + 1_800_000L)));
        return event;
    }

    /** Construye una GoogleJsonResponseException con código HTTP 404. */
    private GoogleJsonResponseException build404Exception() {
        GoogleJsonError error = new GoogleJsonError();
        error.setCode(404);
        error.setMessage("Not Found");
        HttpResponseException.Builder builder =
                new HttpResponseException.Builder(404, "Not Found", new HttpHeaders());
        return new GoogleJsonResponseException(builder, error);
    }
}
