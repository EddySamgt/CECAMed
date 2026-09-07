package com.cecamed.services.dto.inventory;

import com.cecamed.core.model.inventory.enums.ItemCategory;
import com.cecamed.core.model.inventory.enums.UnitOfMeasure;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryItemRequestDto {

    @NotBlank(message = "El código o SKU es obligatorio")
    @Size(max = 50, message = "El código no puede exceder 50 caracteres")
    private String code;

    @NotBlank(message = "El nombre del insumo/medicamento es obligatorio")
    @Size(max = 150, message = "El nombre no puede exceder 150 caracteres")
    private String name;

    @Size(max = 150, message = "El nombre genérico no puede exceder 150 caracteres")
    private String genericName;

    @NotNull(message = "La categoría es obligatoria")
    private ItemCategory category;

    private String description;

    @NotNull(message = "La unidad de medida es obligatoria")
    private UnitOfMeasure unitOfMeasure;

    @NotNull(message = "El stock inicial o actual es obligatorio")
    @DecimalMin(value = "0.00", message = "El stock no puede ser negativo")
    @Builder.Default
    private BigDecimal currentStock = BigDecimal.ZERO;

    @NotNull(message = "El stock mínimo de alerta es obligatorio")
    @DecimalMin(value = "0.00", message = "El stock mínimo no puede ser negativo")
    @Builder.Default
    private BigDecimal minStockAlert = BigDecimal.ZERO;

    @DecimalMin(value = "0.00", message = "El stock máximo no puede ser negativo")
    private BigDecimal maxStock;

    @DecimalMin(value = "0.00", message = "El costo unitario no puede ser negativo")
    private BigDecimal unitCost;

    @DecimalMin(value = "0.00", message = "El precio de venta no puede ser negativo")
    private BigDecimal sellingPrice;

    @Size(max = 100, message = "La ubicación no puede exceder 100 caracteres")
    private String locationInClinic;

    private LocalDate expirationDate;

    @Size(max = 50, message = "El número de lote no puede exceder 50 caracteres")
    private String lotNumber;
}
