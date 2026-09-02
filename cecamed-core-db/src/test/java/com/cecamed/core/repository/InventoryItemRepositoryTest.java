package com.cecamed.core.repository;

import com.cecamed.core.model.inventory.InventoryItem;
import com.cecamed.core.model.inventory.enums.ItemCategory;
import com.cecamed.core.model.inventory.enums.UnitOfMeasure;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("InventoryItemRepository - Pruebas de stock y alertas de inventario")
class InventoryItemRepositoryTest {

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    private InventoryItem createItem(String code, String name, ItemCategory category, BigDecimal stock, BigDecimal minAlert, LocalDate exp) {
        return InventoryItem.builder()
                .code(code)
                .name(name)
                .category(category)
                .unitOfMeasure(UnitOfMeasure.CAJA)
                .currentStock(stock)
                .minStockAlert(minAlert)
                .expirationDate(exp)
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Debe consultar items con stock bajo o igual al límite mínimo (alertas de stock)")
    void shouldFindLowStockAlerts() {
        InventoryItem lowStockItem = createItem("MED-001", "Paracetamol 500mg", ItemCategory.MEDICAMENTO, new BigDecimal("5.00"), new BigDecimal("10.00"), LocalDate.now().plusMonths(6));
        InventoryItem exactMinStockItem = createItem("MED-002", "Ibuprofeno 400mg", ItemCategory.MEDICAMENTO, new BigDecimal("10.00"), new BigDecimal("10.00"), LocalDate.now().plusMonths(6));
        InventoryItem healthyStockItem = createItem("MED-003", "Amoxicilina 500mg", ItemCategory.MEDICAMENTO, new BigDecimal("50.00"), new BigDecimal("10.00"), LocalDate.now().plusMonths(6));

        inventoryItemRepository.saveAll(List.of(lowStockItem, exactMinStockItem, healthyStockItem));

        List<InventoryItem> alerts = inventoryItemRepository.findLowStockAlerts();
        assertThat(alerts).hasSize(2);
        assertThat(alerts).extracting(InventoryItem::getCode).containsExactly("MED-001", "MED-002");
    }

    @Test
    @DisplayName("Debe consultar items próximos a vencer")
    void shouldFindExpiringItems() {
        LocalDate today = LocalDate.now();
        InventoryItem expiringSoon = createItem("MED-EXP1", "Vacuna Antigripal", ItemCategory.MEDICAMENTO, new BigDecimal("20.00"), new BigDecimal("5.00"), today.plusDays(15));
        InventoryItem expiringLater = createItem("MED-EXP2", "Suero Fisiológico", ItemCategory.MEDICAMENTO, new BigDecimal("20.00"), new BigDecimal("5.00"), today.plusMonths(6));

        inventoryItemRepository.saveAll(List.of(expiringSoon, expiringLater));

        List<InventoryItem> expiringList = inventoryItemRepository.findExpiringItems(today.plusDays(30));
        assertThat(expiringList).hasSize(1);
        assertThat(expiringList.get(0).getCode()).isEqualTo("MED-EXP1");
    }
}
