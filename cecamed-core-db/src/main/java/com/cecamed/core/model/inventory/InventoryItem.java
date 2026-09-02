package com.cecamed.core.model.inventory;

import com.cecamed.core.audit.AuditableEntity;
import com.cecamed.core.model.inventory.enums.ItemCategory;
import com.cecamed.core.model.inventory.enums.UnitOfMeasure;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Representa un insumo o medicamento dentro del catálogo de inventario de la clínica.
 */
@Entity
@Table(
    name = "inventory_items",
    indexes = {
        @Index(name = "idx_item_code", columnList = "code", unique = true),
        @Index(name = "idx_item_name", columnList = "name"),
        @Index(name = "idx_item_category", columnList = "category"),
        @Index(name = "idx_item_stock_alert", columnList = "current_stock, min_stock_alert"),
        @Index(name = "idx_item_expiration", columnList = "expiration_date")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "movements")
public class InventoryItem extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El código o SKU es obligatorio")
    @Size(max = 50, message = "El código no puede exceder 50 caracteres")
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @NotBlank(message = "El nombre del insumo/medicamento es obligatorio")
    @Size(max = 150, message = "El nombre no puede exceder 150 caracteres")
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Size(max = 150, message = "El nombre genérico no puede exceder 150 caracteres")
    @Column(name = "generic_name", length = 150)
    private String genericName;

    @NotNull(message = "La categoría es obligatoria")
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 40)
    private ItemCategory category;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "La unidad de medida es obligatoria")
    @Enumerated(EnumType.STRING)
    @Column(name = "unit_of_measure", nullable = false, length = 30)
    private UnitOfMeasure unitOfMeasure;

    @NotNull(message = "El stock actual es obligatorio")
    @DecimalMin(value = "0.00", message = "El stock actual no puede ser negativo")
    @Column(name = "current_stock", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal currentStock = BigDecimal.ZERO;

    @NotNull(message = "El stock mínimo de alerta es obligatorio")
    @DecimalMin(value = "0.00", message = "El stock mínimo no puede ser negativo")
    @Column(name = "min_stock_alert", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal minStockAlert = BigDecimal.ZERO;

    @DecimalMin(value = "0.00", message = "El stock máximo no puede ser negativo")
    @Column(name = "max_stock", precision = 12, scale = 2)
    private BigDecimal maxStock;

    @DecimalMin(value = "0.00", message = "El costo unitario no puede ser negativo")
    @Column(name = "unit_cost", precision = 12, scale = 2)
    private BigDecimal unitCost;

    @DecimalMin(value = "0.00", message = "El precio de venta no puede ser negativo")
    @Column(name = "selling_price", precision = 12, scale = 2)
    private BigDecimal sellingPrice;

    @Size(max = 100)
    @Column(name = "location_in_clinic", length = 100)
    private String locationInClinic;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Size(max = 50)
    @Column(name = "lot_number", length = 50)
    private String lotNumber;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Builder.Default
    @OneToMany(mappedBy = "inventoryItem", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("movementDateTime DESC")
    private List<InventoryMovement> movements = new ArrayList<>();

    // Métodos utilitarios de negocio
    public boolean isLowStock() {
        if (currentStock == null || minStockAlert == null) return false;
        return currentStock.compareTo(minStockAlert) <= 0;
    }

    public boolean isExpired() {
        if (expirationDate == null) return false;
        return expirationDate.isBefore(LocalDate.now());
    }

    public boolean isExpiringSoon(int daysThreshold) {
        if (expirationDate == null) return false;
        LocalDate now = LocalDate.now();
        return !expirationDate.isBefore(now) && expirationDate.isBefore(now.plusDays(daysThreshold));
    }

    public void addMovement(InventoryMovement movement) {
        movements.add(movement);
        movement.setInventoryItem(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InventoryItem that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
