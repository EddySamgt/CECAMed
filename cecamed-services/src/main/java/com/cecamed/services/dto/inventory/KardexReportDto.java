package com.cecamed.services.dto.inventory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KardexReportDto {
    private Long itemId;
    private String itemCode;
    private String itemName;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private BigDecimal currentStock;
    private BigDecimal totalEntries;
    private BigDecimal totalExits;
    private List<InventoryMovementResponseDto> movements;
}
