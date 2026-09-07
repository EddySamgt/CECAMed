package com.cecamed.calendar.client;

import com.cecamed.calendar.config.GoogleCalendarProperties;
import com.cecamed.calendar.dto.CalendarEventDto;
import com.cecamed.calendar.dto.TimeSlotDto;
import com.cecamed.calendar.exception.CalendarSyncException;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import com.google.api.services.calendar.model.FreeBusyCalendar;
import com.google.api.services.calendar.model.FreeBusyRequest;
import com.google.api.services.calendar.model.FreeBusyRequestItem;
import com.google.api.services.calendar.model.FreeBusyResponse;
import com.google.api.services.calendar.model.TimePeriod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class GoogleCalendarClientImpl implements GoogleCalendarClient {

    private final Calendar googleCalendar;
    private final GoogleCalendarProperties properties;
    private final ZoneId zoneId;

    @Autowired
    public GoogleCalendarClientImpl(@Nullable Calendar googleCalendar, GoogleCalendarProperties properties) {
        this.googleCalendar = googleCalendar;
        this.properties = properties;
        this.zoneId = ZoneId.of(properties.getTimeZone());
    }

    @Override
    public boolean isClientAvailable() {
        return googleCalendar != null && properties.isEnabled();
    }

    @Override
    public CalendarEventDto createEvent(CalendarEventDto eventDto) {
        if (!isClientAvailable()) {
            log.warn("Google Calendar no está disponible. Creando evento simulado local para {}", eventDto.getSummary());
            return CalendarEventDto.builder()
                    .id("mock-event-" + UUID.randomUUID())
                    .summary(eventDto.getSummary())
                    .description(eventDto.getDescription())
                    .location(eventDto.getLocation())
                    .startDateTime(eventDto.getStartDateTime())
                    .endDateTime(eventDto.getEndDateTime())
                    .htmlLink("https://calendar.google.com/calendar/r/eventedit/mock")
                    .status("confirmed")
                    .build();
        }

        try {
            Event event = toGoogleEvent(eventDto);
            Event created = googleCalendar.events()
                    .insert(properties.getCalendarId(), event)
                    .execute();

            log.info("Evento creado exitosamente en Google Calendar con ID: {}", created.getId());
            return toDto(created);

        } catch (IOException e) {
            log.error("Error al crear evento en Google Calendar: {}", e.getMessage(), e);
            throw new CalendarSyncException("Fallo al crear evento en Google Calendar: " + e.getMessage(), e);
        }
    }

    @Override
    public CalendarEventDto updateEvent(String eventId, CalendarEventDto eventDto) {
        if (!isClientAvailable()) {
            log.warn("Google Calendar no está disponible. Simulación de actualización para evento ID: {}", eventId);
            return eventDto;
        }

        try {
            Event event = toGoogleEvent(eventDto);
            Event updated = googleCalendar.events()
                    .patch(properties.getCalendarId(), eventId, event)
                    .execute();

            log.info("Evento ID {} actualizado exitosamente en Google Calendar", eventId);
            return toDto(updated);

        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == 404) {
                log.warn("El evento con ID {} no existe en Google Calendar. Se creará de nuevo.", eventId);
                return createEvent(eventDto);
            }
            throw new CalendarSyncException("Error al actualizar evento en Google Calendar: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new CalendarSyncException("Error de comunicación con Google Calendar: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteEvent(String eventId) {
        if (!isClientAvailable()) {
            log.warn("Google Calendar no disponible. Simulación de eliminación para evento ID: {}", eventId);
            return;
        }

        try {
            googleCalendar.events()
                    .delete(properties.getCalendarId(), eventId)
                    .execute();
            log.info("Evento ID {} eliminado de Google Calendar", eventId);

        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == 404) {
                log.warn("El evento ID {} no existe en Google Calendar (ya fue eliminado)", eventId);
                return;
            }
            throw new CalendarSyncException("Error al eliminar evento en Google Calendar: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new CalendarSyncException("Error de comunicación al eliminar evento en Google Calendar", e);
        }
    }

    @Override
    public Optional<CalendarEventDto> getEvent(String eventId) {
        if (!isClientAvailable()) {
            return Optional.empty();
        }

        try {
            Event event = googleCalendar.events()
                    .get(properties.getCalendarId(), eventId)
                    .execute();
            return Optional.of(toDto(event));

        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == 404) {
                return Optional.empty();
            }
            throw new CalendarSyncException("Error al consultar evento en Google Calendar: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new CalendarSyncException("Error de comunicación al consultar evento en Google Calendar", e);
        }
    }

    @Override
    public List<CalendarEventDto> listEvents(LocalDateTime start, LocalDateTime end) {
        if (!isClientAvailable()) {
            return Collections.emptyList();
        }

        try {
            Events events = googleCalendar.events()
                    .list(properties.getCalendarId())
                    .setTimeMin(toDateTime(start))
                    .setTimeMax(toDateTime(end))
                    .setSingleEvents(true)
                    .setOrderBy("startTime")
                    .execute();

            if (events.getItems() == null) {
                return Collections.emptyList();
            }

            return events.getItems().stream()
                    .map(this::toDto)
                    .toList();

        } catch (IOException e) {
            log.error("Error al listar eventos de Google Calendar: {}", e.getMessage(), e);
            throw new CalendarSyncException("Error al listar eventos de Google Calendar", e);
        }
    }

    @Override
    public List<TimeSlotDto> getBusyTimeSlots(LocalDateTime start, LocalDateTime end) {
        if (!isClientAvailable()) {
            return Collections.emptyList();
        }

        try {
            FreeBusyRequest request = new FreeBusyRequest()
                    .setTimeMin(toDateTime(start))
                    .setTimeMax(toDateTime(end))
                    .setTimeZone(properties.getTimeZone())
                    .setItems(List.of(new FreeBusyRequestItem().setId(properties.getCalendarId())));

            FreeBusyResponse response = googleCalendar.freebusy()
                    .query(request)
                    .execute();

            FreeBusyCalendar cal = response.getCalendars().get(properties.getCalendarId());
            if (cal == null || cal.getBusy() == null) {
                return Collections.emptyList();
            }

            List<TimeSlotDto> busySlots = new ArrayList<>();
            for (TimePeriod period : cal.getBusy()) {
                busySlots.add(TimeSlotDto.builder()
                        .start(toLocalDateTime(period.getStart()))
                        .end(toLocalDateTime(period.getEnd()))
                        .busy(true)
                        .description("Evento externo en Google Calendar")
                        .build());
            }

            return busySlots;

        } catch (IOException e) {
            log.error("Error al consultar disponibilidad FreeBusy en Google Calendar: {}", e.getMessage(), e);
            throw new CalendarSyncException("Error al consultar FreeBusy en Google Calendar", e);
        }
    }

    private Event toGoogleEvent(CalendarEventDto dto) {
        Event event = new Event();
        event.setSummary(dto.getSummary());
        event.setDescription(dto.getDescription());
        event.setLocation(dto.getLocation());

        EventDateTime start = new EventDateTime()
                .setDateTime(toDateTime(dto.getStartDateTime()))
                .setTimeZone(properties.getTimeZone());
        event.setStart(start);

        EventDateTime end = new EventDateTime()
                .setDateTime(toDateTime(dto.getEndDateTime()))
                .setTimeZone(properties.getTimeZone());
        event.setEnd(end);

        if (dto.getAttendeeEmails() != null && !dto.getAttendeeEmails().isEmpty()) {
            List<EventAttendee> attendees = dto.getAttendeeEmails().stream()
                    .map(email -> new EventAttendee().setEmail(email))
                    .toList();
            event.setAttendees(attendees);
        }

        return event;
    }

    private CalendarEventDto toDto(Event event) {
        LocalDateTime start = null;
        if (event.getStart() != null) {
            DateTime dt = event.getStart().getDateTime();
            if (dt == null) {
                dt = event.getStart().getDate();
            }
            if (dt != null) {
                start = toLocalDateTime(dt);
            }
        }

        LocalDateTime end = null;
        if (event.getEnd() != null) {
            DateTime dt = event.getEnd().getDateTime();
            if (dt == null) {
                dt = event.getEnd().getDate();
            }
            if (dt != null) {
                end = toLocalDateTime(dt);
            }
        }

        List<String> attendees = Collections.emptyList();
        if (event.getAttendees() != null) {
            attendees = event.getAttendees().stream()
                    .map(EventAttendee::getEmail)
                    .toList();
        }

        return CalendarEventDto.builder()
                .id(event.getId())
                .summary(event.getSummary())
                .description(event.getDescription())
                .location(event.getLocation())
                .startDateTime(start)
                .endDateTime(end)
                .htmlLink(event.getHtmlLink())
                .status(event.getStatus())
                .attendeeEmails(attendees)
                .build();
    }

    private DateTime toDateTime(LocalDateTime ldt) {
        if (ldt == null) return null;
        Instant instant = ldt.atZone(zoneId).toInstant();
        return new DateTime(Date.from(instant));
    }

    private LocalDateTime toLocalDateTime(DateTime dt) {
        if (dt == null) return null;
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(dt.getValue()), zoneId);
    }
}
