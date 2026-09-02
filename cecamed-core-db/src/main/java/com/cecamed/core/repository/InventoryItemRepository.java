package com.cecamed.core.repository;

import com.cecamed.core.model.inventory.InventoryItem;
import com.cecamed.core.model.inventory.enums.ItemCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long>, JpaSpecificationExecutor<InventoryItem> {

    Optional<InventoryItem> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);

    List<InventoryItem> findAllByIsActiveTrueOrderByNameAsc();

    List<InventoryItem> findAllByCategoryAndIsActiveTrueOrderByNameAsc(ItemCategory category);

    /**
     * Consulta insumos cuyo stock actual está en o por debajo del stock mínimo configurado.
     */
    @Query("""
        SELECT i FROM InventoryItem i
        WHERE i.isActive = true
          AND i.currentStock <= i.minStockAlert
        ORDER BY (i.currentStock - i.minStockAlert) ASC, i.name ASC
    """)
    List<InventoryItem> findLowStockAlerts();

    /**
     * Consulta insumos próximos a vencer o ya vencidos antes de una fecha límite.
     */
    @Query("""
        SELECT i FROM InventoryItem i
        WHERE i.isActive = true
          AND i.expirationDate IS NOT NULL
          AND i.expirationDate <= :thresholdDate
        ORDER BY i.expirationDate ASC
    """)
    List<InventoryItem> findExpiringItems(@Param("thresholdDate") LocalDate thresholdDate);

    @Query("""
        SELECT i FROM InventoryItem i
        WHERE i.isActive = true
          AND (
            LOWER(i.name) LIKE LOWER(CONCAT('%', :term, '%'))
            OR LOWER(i.code) LIKE LOWER(CONCAT('%', :term, '%'))
            OR (i.genericName IS NOT NULL AND LOWER(i.genericName) LIKE LOWER(CONCAT('%', :term, '%')))
          )
        ORDER BY i.name ASC
    """)
    List<InventoryItem> searchActiveItems(@Param("term") String term);

    @Query("""
        SELECT i FROM InventoryItem i
        WHERE (
            LOWER(i.name) LIKE LOWER(CONCAT('%', :term, '%'))
            OR LOWER(i.code) LIKE LOWER(CONCAT('%', :term, '%'))
            OR (i.genericName IS NOT NULL AND LOWER(i.genericName) LIKE LOWER(CONCAT('%', :term, '%')))
          )
    """)
    Page<InventoryItem> searchItemsPaged(@Param("term") String term, Pageable pageable);
}
