package com.cecamed.services.mapper;

import com.cecamed.core.model.inventory.InventoryItem;
import com.cecamed.core.model.inventory.InventoryMovement;
import com.cecamed.services.dto.inventory.InventoryItemRequestDto;
import com.cecamed.services.dto.inventory.InventoryItemResponseDto;
import com.cecamed.services.dto.inventory.InventoryMovementResponseDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class InventoryMapper {

    public InventoryItem toEntity(InventoryItemRequestDto dto) {
        if (dto == null) return null;

        return InventoryItem.builder()
                .code(dto.getCode())
                .name(dto.getName())
                .genericName(dto.getGenericName())
                .category(dto.getCategory())
                .description(dto.getDescription())
                .unitOfMeasure(dto.getUnitOfMeasure())
                .currentStock(dto.getCurrentStock() != null ? dto.getCurrentStock() : BigDecimal.ZERO)
                .minStockAlert(dto.getMinStockAlert() != null ? dto.getMinStockAlert() : BigDecimal.ZERO)
                .maxStock(dto.getMaxStock())
                .unitCost(dto.getUnitCost())
                .sellingPrice(dto.getSellingPrice())
                .locationInClinic(dto.getLocationInClinic())
                .expirationDate(dto.getExpirationDate())
                .lotNumber(dto.getLotNumber())
                .isActive(true)
                .build();
    }

    public void updateEntityFromDto(InventoryItemRequestDto dto, InventoryItem item) {
        if (dto == null || item == null) return;

        item.setCode(dto.getCode());
        item.setName(dto.getName());
        item.setGenericName(dto.getGenericName());
        item.setCategory(dto.getCategory());
        item.setDescription(dto.getDescription());
        item.setUnitOfMeasure(dto.getUnitOfMeasure());
        if (dto.getCurrentStock() != null) {
            item.setCurrentStock(dto.getCurrentStock());
        }
        if (dto.getMinStockAlert() != null) {
            item.setMinStockAlert(dto.getMinStockAlert());
        }
        item.setMaxStock(dto.getMaxStock());
        item.setUnitCost(dto.getUnitCost());
        item.setSellingPrice(dto.getSellingPrice());
        item.setLocationInClinic(dto.getLocationInClinic());
        item.setExpirationDate(dto.getExpirationDate());
        item.setLotNumber(dto.getLotNumber());
    }

    public InventoryItemResponseDto toResponseDto(InventoryItem item) {
        if (item == null) return null;

        return InventoryItemResponseDto.builder()
                .id(item.getId())
                .code(item.getCode())
                .name(item.getName())
                .genericName(item.getGenericName())
                .category(item.getCategory())
                .description(item.getDescription())
                .unitOfMeasure(item.getUnitOfMeasure())
                .currentStock(item.getCurrentStock())
                .minStockAlert(item.getMinStockAlert())
                .maxStock(item.getMaxStock())
                .unitCost(item.getUnitCost())
                .sellingPrice(item.getSellingPrice())
                .locationInClinic(item.getLocationInClinic())
                .expirationDate(item.getExpirationDate())
                .lotNumber(item.getLotNumber())
                .isActive(item.getIsActive())
                .lowStock(item.isLowStock())
                .expired(item.isExpired())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }

    public InventoryMovementResponseDto toMovementResponseDto(InventoryMovement movement) {
        if (movement == null) return null;

        return InventoryMovementResponseDto.builder()
                .id(movement.getId())
                .inventoryItemId(movement.getInventoryItem() != null ? movement.getInventoryItem().getId() : null)
                .inventoryItemCode(movement.getInventoryItem() != null ? movement.getInventoryItem().getCode() : null)
                .inventoryItemName(movement.getInventoryItem() != null ? movement.getInventoryItem().getName() : null)
                .movementType(movement.getMovementType())
                .quantity(movement.getQuantity())
                .stockBefore(movement.getStockBefore())
                .stockAfter(movement.getStockAfter())
                .unitCostAtMoment(movement.getUnitCostAtMoment())
                .patientId(movement.getPatient() != null ? movement.getPatient().getId() : null)
                .patientFullName(movement.getPatient() != null ? movement.getPatient().getFullName() : null)
                .consultationId(movement.getConsultation() != null ? movement.getConsultation().getId() : null)
                .reasonNotes(movement.getReasonNotes())
                .movementDateTime(movement.getMovementDateTime())
                .performedBy(movement.getPerformedBy())
                .build();
    }
}
