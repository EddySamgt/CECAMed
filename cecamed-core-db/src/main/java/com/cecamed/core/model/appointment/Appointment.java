package com.cecamed.core.model.appointment;

import com.cecamed.core.audit.AuditableEntity;
import com.cecamed.core.model.appointment.enums.AppointmentStatus;
import com.cecamed.core.model.appointment.enums.GoogleSyncStatus;
import com.cecamed.core.model.patient.Patient;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Representa una cita médica programada con un paciente y su sincronización con Google Calendar.
 */
@Entity
@Table(
    name = "appointments",
    indexes = {
        @Index(name = "idx_appointment_patient_id", columnList = "patient_id"),
        @Index(name = "idx_appointment_time_range", columnList = "start_time, end_time"),
        @Index(name = "idx_appointment_status", columnList = "status"),
        @Index(name = "idx_appointment_google_event_id", columnList = "google_event_id"),
        @Index(name = "idx_appointment_sync_status", columnList = "google_sync_status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "patient")
public class Appointment extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "El paciente es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @NotNull(message = "La hora de inicio es obligatoria")
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @NotNull(message = "La hora de fin es obligatoria")
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @NotNull(message = "El estado de la cita es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private AppointmentStatus status = AppointmentStatus.PROGRAMADA;

    @NotBlank(message = "El motivo de la cita es obligatorio")
    @Size(max = 255, message = "El motivo no puede exceder 255 caracteres")
    @Column(name = "reason_for_visit", nullable = false, length = 255)
    private String reasonForVisit;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // Sincronización con Google Calendar API v3
    @Column(name = "google_event_id", length = 255)
    private String googleEventId;

    @Column(name = "google_calendar_id", length = 255)
    private String googleCalendarId;

    @Enumerated(EnumType.STRING)
    @Column(name = "google_sync_status", nullable = false, length = 30)
    @Builder.Default
    private GoogleSyncStatus googleSyncStatus = GoogleSyncStatus.PENDING;

    @Column(name = "google_html_link", length = 500)
    private String googleHtmlLink;

    @Column(name = "google_last_synced_at")
    private LocalDateTime googleLastSyncedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Appointment that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
