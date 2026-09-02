package com.cecamed.core.model.appointment;

import com.cecamed.core.audit.AuditableEntity;
import com.cecamed.core.model.appointment.enums.BlockType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
 * Bloqueo de agenda (días no laborales, feriados, capacitaciones, vacaciones).
 */
@Entity
@Table(
    name = "schedule_blocks",
    indexes = {
        @Index(name = "idx_block_time_range", columnList = "start_date_time, end_date_time"),
        @Index(name = "idx_block_type", columnList = "block_type")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ScheduleBlock extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El título del bloqueo es obligatorio")
    @Size(max = 150)
    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @NotNull(message = "La fecha y hora de inicio es obligatoria")
    @Column(name = "start_date_time", nullable = false)
    private LocalDateTime startDateTime;

    @NotNull(message = "La fecha y hora de fin es obligatoria")
    @Column(name = "end_date_time", nullable = false)
    private LocalDateTime endDateTime;

    @NotNull(message = "El tipo de bloqueo es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "block_type", nullable = false, length = 30)
    @Builder.Default
    private BlockType blockType = BlockType.PERSONAL;

    @Builder.Default
    @Column(name = "all_day", nullable = false)
    private Boolean allDay = false;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "google_event_id", length = 255)
    private String googleEventId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ScheduleBlock that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
