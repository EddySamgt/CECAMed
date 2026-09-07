package com.cecamed.calendar.service;

import com.cecamed.calendar.client.GoogleCalendarClient;
import com.cecamed.calendar.config.GoogleCalendarProperties;
import com.cecamed.calendar.dto.CalendarEventDto;
import com.cecamed.calendar.dto.CalendarSyncResultDto;
import com.cecamed.core.model.appointment.Appointment;
import com.cecamed.core.model.appointment.ScheduleBlock;
import com.cecamed.core.model.appointment.enums.AppointmentStatus;
import com.cecamed.core.model.appointment.enums.GoogleSyncStatus;
import com.cecamed.core.repository.AppointmentRepository;
import com.cecamed.core.repository.ScheduleBlockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentCalendarSyncService {

    private final GoogleCalendarClient googleCalendarClient;
    private final AppointmentRepository appointmentRepository;
    private final ScheduleBlockRepository scheduleBlockRepository;
    private final GoogleCalendarProperties properties;

    @Transactional
    public CalendarSyncResultDto syncAppointment(Appointment appointment) {
        log.info("Sincronizando cita ID: {} con Google Calendar", appointment.getId());

        if (!properties.isEnabled()) {
            log.info("Sincronización con Google Calendar desactivada; marcando como NOT_APPLICABLE");
            appointment.setGoogleSyncStatus(GoogleSyncStatus.NOT_APPLICABLE);
            appointmentRepository.save(appointment);

            return CalendarSyncResultDto.builder()
                    .appointmentId(appointment.getId())
                    .status(GoogleSyncStatus.NOT_APPLICABLE)
                    .message("Integración con Google Calendar deshabilitada")
                    .syncedAt(LocalDateTime.now())
                    .build();
        }

        if (!googleCalendarClient.isClientAvailable()) {
            log.warn("Google Calendar habilitado pero cliente no disponible/conectado.");
            appointment.setGoogleSyncStatus(GoogleSyncStatus.PENDING);
            appointmentRepository.save(appointment);

            return CalendarSyncResultDto.builder()
                    .appointmentId(appointment.getId())
                    .status(GoogleSyncStatus.PENDING)
                    .message("Cliente de Google Calendar no disponible; sincronización pendiente")
                    .syncedAt(LocalDateTime.now())
                    .build();
        }

        try {
            CalendarEventDto eventDto = buildEventDtoFromAppointment(appointment);
            CalendarEventDto resultEvent;

            if (appointment.getGoogleEventId() != null && !appointment.getGoogleEventId().isBlank()) {
                resultEvent = googleCalendarClient.updateEvent(appointment.getGoogleEventId(), eventDto);
            } else {
                resultEvent = googleCalendarClient.createEvent(eventDto);
            }

            appointment.setGoogleEventId(resultEvent.getId());
            appointment.setGoogleHtmlLink(resultEvent.getHtmlLink());
            appointment.setGoogleCalendarId(properties.getCalendarId());
            appointment.setGoogleSyncStatus(GoogleSyncStatus.SYNCED);
            appointment.setGoogleLastSyncedAt(LocalDateTime.now());

            appointmentRepository.save(appointment);
            log.info("Cita ID {} sincronizada exitosamente con Google Calendar (Event ID: {})",
                    appointment.getId(), resultEvent.getId());

            return CalendarSyncResultDto.builder()
                    .appointmentId(appointment.getId())
                    .googleEventId(resultEvent.getId())
                    .htmlLink(resultEvent.getHtmlLink())
                    .status(GoogleSyncStatus.SYNCED)
                    .message("Sincronización completada con éxito")
                    .syncedAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Error al sincronizar cita ID {} con Google Calendar: {}", appointment.getId(), e.getMessage());

            appointment.setGoogleSyncStatus(GoogleSyncStatus.FAILED);
            appointmentRepository.save(appointment);

            return CalendarSyncResultDto.builder()
                    .appointmentId(appointment.getId())
                    .status(GoogleSyncStatus.FAILED)
                    .message("Error de sincronización: " + e.getMessage())
                    .syncedAt(LocalDateTime.now())
                    .build();
        }
    }

    @Transactional
    public CalendarSyncResultDto cancelAppointmentEvent(Appointment appointment) {
        log.info("Sincronizando cancelación de cita ID: {} en Google Calendar", appointment.getId());

        if (appointment.getGoogleEventId() == null || appointment.getGoogleEventId().isBlank()) {
            appointment.setGoogleSyncStatus(GoogleSyncStatus.NOT_APPLICABLE);
            appointmentRepository.save(appointment);
            return CalendarSyncResultDto.builder()
                    .appointmentId(appointment.getId())
                    .status(GoogleSyncStatus.NOT_APPLICABLE)
                    .message("La cita no tenía evento asociado en Google Calendar")
                    .syncedAt(LocalDateTime.now())
                    .build();
        }

        if (!properties.isEnabled()) {
            log.info("Integración con Google Calendar deshabilitada; limpiando referencia de evento");
            appointment.setGoogleEventId(null);
            appointment.setGoogleHtmlLink(null);
            appointment.setGoogleSyncStatus(GoogleSyncStatus.NOT_APPLICABLE);
            appointmentRepository.save(appointment);
            return CalendarSyncResultDto.builder()
                    .appointmentId(appointment.getId())
                    .status(GoogleSyncStatus.NOT_APPLICABLE)
                    .message("Integración con Google Calendar deshabilitada")
                    .syncedAt(LocalDateTime.now())
                    .build();
        }

        if (!googleCalendarClient.isClientAvailable()) {
            log.warn("Google Calendar habilitado pero cliente no disponible para cancelar evento; cancelación pendiente");
            appointment.setGoogleSyncStatus(GoogleSyncStatus.PENDING);
            appointmentRepository.save(appointment);
            return CalendarSyncResultDto.builder()
                    .appointmentId(appointment.getId())
                    .status(GoogleSyncStatus.PENDING)
                    .message("Cliente de Google Calendar no disponible; cancelación pendiente")
                    .syncedAt(LocalDateTime.now())
                    .build();
        }

        try {
            // Eliminar el evento en Google Calendar
            googleCalendarClient.deleteEvent(appointment.getGoogleEventId());

            appointment.setGoogleEventId(null);
            appointment.setGoogleHtmlLink(null);
            appointment.setGoogleSyncStatus(GoogleSyncStatus.SYNCED);
            appointment.setGoogleLastSyncedAt(LocalDateTime.now());
            appointmentRepository.save(appointment);

            log.info("Evento de Google Calendar eliminado para la cita ID: {}", appointment.getId());

            return CalendarSyncResultDto.builder()
                    .appointmentId(appointment.getId())
                    .status(GoogleSyncStatus.SYNCED)
                    .message("Evento eliminado de Google Calendar")
                    .syncedAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Error al eliminar evento en Google Calendar para la cita ID {}: {}",
                    appointment.getId(), e.getMessage());

            appointment.setGoogleSyncStatus(GoogleSyncStatus.FAILED);
            appointmentRepository.save(appointment);

            return CalendarSyncResultDto.builder()
                    .appointmentId(appointment.getId())
                    .status(GoogleSyncStatus.FAILED)
                    .message("Error al eliminar evento: " + e.getMessage())
                    .syncedAt(LocalDateTime.now())
                    .build();
        }
    }

    @Transactional
    public void syncScheduleBlock(ScheduleBlock block) {
        log.info("Sincronizando bloqueo de agenda ID: {} con Google Calendar", block.getId());

        if (!properties.isEnabled() || !googleCalendarClient.isClientAvailable()) {
            return;
        }

        try {
            CalendarEventDto eventDto = CalendarEventDto.builder()
                    .summary("[BLOQUEO CLÍNICO] " + block.getTitle())
                    .description("Motivo: " + (block.getReason() != null ? block.getReason() : "No especificado")
                            + "\nTipo de bloqueo: " + block.getBlockType())
                    .startDateTime(block.getStartDateTime())
                    .endDateTime(block.getEndDateTime())
                    .build();

            CalendarEventDto result;
            if (block.getGoogleEventId() != null && !block.getGoogleEventId().isBlank()) {
                result = googleCalendarClient.updateEvent(block.getGoogleEventId(), eventDto);
            } else {
                result = googleCalendarClient.createEvent(eventDto);
            }

            block.setGoogleEventId(result.getId());
            scheduleBlockRepository.save(block);
            log.info("Bloqueo de agenda sincronizado con Google Calendar ID: {}", result.getId());

        } catch (Exception e) {
            log.error("Error al sincronizar bloqueo de agenda ID {} con Google Calendar: {}", block.getId(), e.getMessage());
        }
    }

    @Transactional
    public void deleteScheduleBlockEvent(ScheduleBlock block) {
        if (block == null || block.getGoogleEventId() == null || block.getGoogleEventId().isBlank()) {
            return;
        }

        if (!properties.isEnabled() || !googleCalendarClient.isClientAvailable()) {
            return;
        }

        try {
            googleCalendarClient.deleteEvent(block.getGoogleEventId());
            log.info("Evento de bloqueo de agenda eliminado de Google Calendar ID: {}", block.getGoogleEventId());
        } catch (Exception e) {
            log.error("Error al eliminar evento de bloqueo en Google Calendar ID {}: {}",
                    block.getGoogleEventId(), e.getMessage());
        }
    }

    @Transactional
    public int retryPendingAndFailedSyncs() {
        log.info("Iniciando reintento de sincronización para citas pendientes y fallidas...");

        List<Appointment> pending = appointmentRepository.findAllByGoogleSyncStatus(GoogleSyncStatus.PENDING);
        List<Appointment> failed = appointmentRepository.findAllByGoogleSyncStatus(GoogleSyncStatus.FAILED);

        List<Appointment> toRetry = new ArrayList<>();
        toRetry.addAll(pending);
        toRetry.addAll(failed);

        int successCount = 0;
        for (Appointment appointment : toRetry) {
            if (appointment.getStatus() == AppointmentStatus.CANCELADA) {
                CalendarSyncResultDto result = cancelAppointmentEvent(appointment);
                if (result.getStatus() == GoogleSyncStatus.SYNCED) successCount++;
            } else {
                CalendarSyncResultDto result = syncAppointment(appointment);
                if (result.getStatus() == GoogleSyncStatus.SYNCED) successCount++;
            }
        }

        log.info("Reintento finalizado: {}/{} citas sincronizadas exitosamente", successCount, toRetry.size());
        return successCount;
    }

    private CalendarEventDto buildEventDtoFromAppointment(Appointment appointment) {
        String patientName = appointment.getPatient() != null ? appointment.getPatient().getFullName() : "Paciente";
        String patientDni = appointment.getPatient() != null ? appointment.getPatient().getIdentificationNumber() : "N/A";
        String patientPhone = appointment.getPatient() != null ? appointment.getPatient().getPhone() : "N/A";
        String patientEmail = appointment.getPatient() != null ? appointment.getPatient().getEmail() : null;

        String summary = String.format("Cita Médica: %s - %s", patientName, appointment.getReasonForVisit());

        StringBuilder description = new StringBuilder();
        description.append("--- FICHA DE CITA MÉDICA (CECAMed) ---\n")
                .append("ID Cita: ").append(appointment.getId()).append("\n")
                .append("Paciente: ").append(patientName).append("\n")
                .append("Identificación: ").append(patientDni).append("\n")
                .append("Teléfono de contacto: ").append(patientPhone).append("\n")
                .append("Motivo de consulta: ").append(appointment.getReasonForVisit()).append("\n")
                .append("Estado: ").append(appointment.getStatus()).append("\n");

        if (appointment.getNotes() != null && !appointment.getNotes().isBlank()) {
            description.append("Notas adicionales: ").append(appointment.getNotes()).append("\n");
        }

        List<String> attendees = new ArrayList<>();
        if (patientEmail != null && !patientEmail.isBlank()) {
            attendees.add(patientEmail);
        }

        return CalendarEventDto.builder()
                .summary(summary)
                .description(description.toString())
                .startDateTime(appointment.getStartTime())
                .endDateTime(appointment.getEndTime())
                .attendeeEmails(attendees)
                .build();
    }
}
