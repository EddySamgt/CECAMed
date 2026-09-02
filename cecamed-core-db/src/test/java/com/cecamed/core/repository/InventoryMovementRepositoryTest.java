package com.cecamed.core.repository;

import com.cecamed.core.model.inventory.InventoryItem;
import com.cecamed.core.model.inventory.InventoryMovement;
import com.cecamed.core.model.inventory.enums.ItemCategory;
import com.cecamed.core.model.inventory.enums.MovementType;
import com.cecamed.core.model.inventory.enums.UnitOfMeasure;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("InventoryMovementRepository - Pruebas de trazabilidad y Kardex")
class InventoryMovementRepositoryTest {

    @Autowired
    private InventoryMovementRepository inventoryMovementRepository;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Test
    @DisplayName("Debe registrar y consultar movimientos de Kardex para un insumo")
    void shouldFindKardexReport() {
        InventoryItem item = inventoryItemRepository.save(
                InventoryItem.builder()
                        .code("MAT-001")
                        .name("Gasas Estériles")
                        .category(ItemCategory.MATERIAL_CURACION)
                        .unitOfMeasure(UnitOfMeasure.PAQUETE)
                        .currentStock(new BigDecimal("100.00"))
                        .minStockAlert(new BigDecimal("20.00"))
                        .build()
        );

        LocalDateTime t1 = LocalDateTime.of(2026, 9, 1, 8, 0);
        LocalDateTime t2 = LocalDateTime.of(2026, 9, 1, 14, 0);

        InventoryMovement m1 = InventoryMovement.builder()
                .inventoryItem(item)
                .movementType(MovementType.ENTRADA_COMPRA)
                .quantity(new BigDecimal("100.00"))
                .stockBefore(BigDecimal.ZERO)
                .stockAfter(new BigDecimal("100.00"))
                .movementDateTime(t1)
                .performedBy("Admin Dr.")
                .reasonNotes("Compra mensual inicial")
                .build();

        InventoryMovement m2 = InventoryMovement.builder()
                .inventoryItem(item)
                .movementType(MovementType.SALIDA_CONSULTA)
                .quantity(new BigDecimal("5.00"))
                .stockBefore(new BigDecimal("100.00"))
                .stockAfter(new BigDecimal("95.00"))
                .movementDateTime(t2)
                .performedBy("Admin Dr.")
                .reasonNotes("Curación menor")
                .build();

        inventoryMovementRepository.saveAll(List.of(m1, m2));

        List<InventoryMovement> movements = inventoryMovementRepository.findKardexReport(
                item.getId(),
                LocalDateTime.of(2026, 9, 1, 0, 0),
                LocalDateTime.of(2026, 9, 1, 23, 59)
        );

        assertThat(movements).hasSize(2);
        assertThat(movements.get(0).getMovementType()).isEqualTo(MovementType.SALIDA_CONSULTA);
        assertThat(movements.get(1).getMovementType()).isEqualTo(MovementType.ENTRADA_COMPRA);
    }
}
