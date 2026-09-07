package com.cecamed.services.dto.inventory;

import com.cecamed.core.model.inventory.enums.ItemCategory;
import com.cecamed.core.model.inventory.enums.UnitOfMeasure;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryItemResponseDto {
    private Long id;
    private String code;
    private String name;
    private String genericName;
    private ItemCategory category;
    private String description;
    private UnitOfMeasure unitOfMeasure;
    private BigDecimal currentStock;
    private BigDecimal minStockAlert;
    private BigDecimal maxStock;
    private BigDecimal unitCost;
    private BigDecimal sellingPrice;
    private String locationInClinic;
    private LocalDate expirationDate;
    private String lotNumber;
    private Boolean isActive;
    private boolean lowStock;
    private boolean expired;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
