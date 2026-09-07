package com.cecamed.services.service;

import com.cecamed.services.dto.inventory.InventoryItemRequestDto;
import com.cecamed.services.dto.inventory.InventoryItemResponseDto;
import com.cecamed.services.dto.inventory.InventoryMovementRequestDto;
import com.cecamed.services.dto.inventory.InventoryMovementResponseDto;
import com.cecamed.services.dto.inventory.KardexReportDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface InventoryService {

    InventoryItemResponseDto createItem(InventoryItemRequestDto dto);

    InventoryItemResponseDto updateItem(Long id, InventoryItemRequestDto dto);

    InventoryItemResponseDto getItemById(Long id);

    InventoryItemResponseDto getItemByCode(String code);

    List<InventoryItemResponseDto> getAllActiveItems();

    List<InventoryItemResponseDto> getLowStockAlerts();

    List<InventoryItemResponseDto> getExpiringItems(int daysThreshold);

    List<InventoryItemResponseDto> searchItems(String query);

    Page<InventoryItemResponseDto> searchItemsPaged(String query, Pageable pageable);

    void deactivateItem(Long id);

    InventoryMovementResponseDto registerMovement(InventoryMovementRequestDto dto);

    KardexReportDto getKardexReport(Long itemId, LocalDateTime startDate, LocalDateTime endDate);

    List<InventoryMovementResponseDto> getMovementsByItem(Long itemId);

    List<InventoryMovementResponseDto> getMovementsByPatient(Long patientId);
}
