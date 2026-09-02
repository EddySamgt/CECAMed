package com.cecamed.core.repository;

import com.cecamed.core.model.inventory.InventoryMovement;
import com.cecamed.core.model.inventory.enums.MovementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {

    List<InventoryMovement> findAllByInventoryItemIdOrderByMovementDateTimeDesc(Long itemId);

    Page<InventoryMovement> findAllByInventoryItemIdOrderByMovementDateTimeDesc(Long itemId, Pageable pageable);

    List<InventoryMovement> findAllByMovementDateTimeBetweenOrderByMovementDateTimeDesc(
        LocalDateTime startDate,
        LocalDateTime endDate
    );

    List<InventoryMovement> findAllByPatientIdOrderByMovementDateTimeDesc(Long patientId);

    List<InventoryMovement> findAllByConsultationIdOrderByMovementDateTimeDesc(Long consultationId);

    List<InventoryMovement> findAllByMovementTypeOrderByMovementDateTimeDesc(MovementType movementType);

    @Query("""
        SELECT m FROM InventoryMovement m
        WHERE m.inventoryItem.id = :itemId
          AND m.movementDateTime BETWEEN :startDate AND :endDate
        ORDER BY m.movementDateTime DESC
    """)
    List<InventoryMovement> findKardexReport(
        @Param("itemId") Long itemId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
}
