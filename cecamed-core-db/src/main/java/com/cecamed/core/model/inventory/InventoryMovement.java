package com.cecamed.core.model.inventory;

import com.cecamed.core.audit.AuditableEntity;
import com.cecamed.core.model.inventory.enums.MovementType;
import com.cecamed.core.model.patient.MedicalConsultation;
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
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Registro de auditoría (Kardex) para cada movimiento de entrada o salida de inventario.
 */
@Entity
@Table(
    name = "inventory_movements",
    indexes = {
        @Index(name = "idx_movement_item_id", columnList = "inventory_item_id"),
        @Index(name = "idx_movement_date_time", columnList = "movement_date_time"),
        @Index(name = "idx_movement_type", columnList = "movement_type")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"inventoryItem", "patient", "consultation"})
public class InventoryMovement extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "El insumo/medicamento es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private InventoryItem inventoryItem;

    @NotNull(message = "El tipo de movimiento es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 30)
    private MovementType movementType;

    @NotNull(message = "La cantidad del movimiento es obligatoria")
    @DecimalMin(value = "0.01", message = "La cantidad debe ser mayor a cero")
    @Column(name = "quantity", nullable = false, precision = 12, scale = 2)
    private BigDecimal quantity;

    @NotNull(message = "El stock previo es obligatorio")
    @Column(name = "stock_before", nullable = false, precision = 12, scale = 2)
    private BigDecimal stockBefore;

    @NotNull(message = "El stock posterior es obligatorio")
    @Column(name = "stock_after", nullable = false, precision = 12, scale = 2)
    private BigDecimal stockAfter;

    @Column(name = "unit_cost_at_moment", precision = 12, scale = 2)
    private BigDecimal unitCostAtMoment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consultation_id")
    private MedicalConsultation consultation;

    @Column(name = "reason_notes", columnDefinition = "TEXT")
    private String reasonNotes;

    @NotNull(message = "La fecha y hora del movimiento es obligatoria")
    @Column(name = "movement_date_time", nullable = false)
    private LocalDateTime movementDateTime;

    @Size(max = 100)
    @Column(name = "performed_by", length = 100)
    private String performedBy;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InventoryMovement that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
