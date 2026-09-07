package com.cecamed.services.dto.inventory;

import com.cecamed.core.model.inventory.enums.MovementType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryMovementRequestDto {

    @NotNull(message = "El ID del insumo/medicamento es obligatorio")
    private Long inventoryItemId;

    @NotNull(message = "El tipo de movimiento es obligatorio (ENTRADA_COMPRA, SALIDA_CONSULTA, etc.)")
    private MovementType movementType;

    @NotNull(message = "La cantidad del movimiento es obligatoria")
    @DecimalMin(value = "0.01", message = "La cantidad debe ser mayor a 0")
    private BigDecimal quantity;

    @DecimalMin(value = "0.00", message = "El costo unitario no puede ser negativo")
    private BigDecimal unitCostAtMoment;

    private Long patientId;
    private Long consultationId;

    private String reasonNotes;

    @Size(max = 100, message = "El responsable no puede exceder 100 caracteres")
    private String performedBy;
}
