package com.cecamed.services.dto.inventory;

import com.cecamed.core.model.inventory.enums.MovementType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryMovementResponseDto {
    private Long id;
    private Long inventoryItemId;
    private String inventoryItemCode;
    private String inventoryItemName;
    private MovementType movementType;
    private BigDecimal quantity;
    private BigDecimal stockBefore;
    private BigDecimal stockAfter;
    private BigDecimal unitCostAtMoment;
    private Long patientId;
    private String patientFullName;
    private Long consultationId;
    private String reasonNotes;
    private LocalDateTime movementDateTime;
    private String performedBy;
}
