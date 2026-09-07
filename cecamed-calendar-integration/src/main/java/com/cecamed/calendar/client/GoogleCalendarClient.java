package com.cecamed.calendar.client;

import com.cecamed.calendar.dto.CalendarEventDto;
import com.cecamed.calendar.dto.TimeSlotDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface GoogleCalendarClient {

    /**
     * Indica si el cliente está conectado y autenticado con la API de Google Calendar.
     */
    boolean isClientAvailable();

    /**
     * Inserta un nuevo evento en Google Calendar.
     */
    CalendarEventDto createEvent(CalendarEventDto eventDto);

    /**
     * Actualiza un evento existente en Google Calendar.
     */
    CalendarEventDto updateEvent(String eventId, CalendarEventDto eventDto);

    /**
     * Elimina un evento de Google Calendar por su identificador.
     */
    void deleteEvent(String eventId);

    /**
     * Obtiene el detalle de un evento por su ID.
     */
    Optional<CalendarEventDto> getEvent(String eventId);

    /**
     * Obtiene los eventos programados en un rango de fechas.
     */
    List<CalendarEventDto> listEvents(LocalDateTime start, LocalDateTime end);

    /**
     * Consulta períodos ocupados (FreeBusy query) en Google Calendar.
     */
    List<TimeSlotDto> getBusyTimeSlots(LocalDateTime start, LocalDateTime end);
}
