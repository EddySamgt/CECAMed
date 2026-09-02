package com.cecamed.core.model.appointment;

import com.cecamed.core.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Define la jornada y horario habitual de atención de consultas por día de la semana.
 */
@Entity
@Table(
    name = "doctor_schedules",
    indexes = {
        @Index(name = "idx_schedule_day_active", columnList = "day_of_week, is_active")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class DoctorSchedule extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "El día de la semana es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 15)
    private DayOfWeek dayOfWeek;

    @NotNull(message = "La hora de inicio de atención es obligatoria")
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @NotNull(message = "La hora de finalización de atención es obligatoria")
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @NotNull(message = "La duración de turno en minutos es obligatoria")
    @Min(value = 5, message = "La duración mínima es 5 minutos")
    @Max(value = 240, message = "La duración máxima es 240 minutos")
    @Builder.Default
    @Column(name = "slot_duration_minutes", nullable = false)
    private Integer slotDurationMinutes = 30;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DoctorSchedule that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
