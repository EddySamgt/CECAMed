package com.cecamed.services.service.impl;

import com.cecamed.core.model.inventory.InventoryItem;
import com.cecamed.core.model.inventory.InventoryMovement;
import com.cecamed.core.model.inventory.enums.MovementType;
import com.cecamed.core.model.patient.MedicalConsultation;
import com.cecamed.core.model.patient.Patient;
import com.cecamed.core.repository.InventoryItemRepository;
import com.cecamed.core.repository.InventoryMovementRepository;
import com.cecamed.core.repository.MedicalConsultationRepository;
import com.cecamed.core.repository.PatientRepository;
import com.cecamed.services.dto.inventory.InventoryItemRequestDto;
import com.cecamed.services.dto.inventory.InventoryItemResponseDto;
import com.cecamed.services.dto.inventory.InventoryMovementRequestDto;
import com.cecamed.services.dto.inventory.InventoryMovementResponseDto;
import com.cecamed.services.dto.inventory.KardexReportDto;
import com.cecamed.services.exception.BusinessRuleException;
import com.cecamed.services.exception.ResourceNotFoundException;
import com.cecamed.services.mapper.InventoryMapper;
import com.cecamed.services.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryServiceImpl implements InventoryService {

    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final PatientRepository patientRepository;
    private final MedicalConsultationRepository consultationRepository;
    private final InventoryMapper inventoryMapper;

    @Override
    @Transactional
    public InventoryItemResponseDto createItem(InventoryItemRequestDto dto) {
        log.info("Registrando nuevo insumo de inventario con código: {}", dto.getCode());

        if (inventoryItemRepository.existsByCode(dto.getCode())) {
            throw new BusinessRuleException("Ya existe un insumo registrado con el código: " + dto.getCode());
        }

        InventoryItem item = inventoryMapper.toEntity(dto);
        InventoryItem saved = inventoryItemRepository.save(item);
        log.info("Insumo creado exitosamente con ID: {}", saved.getId());

        // Si el stock inicial es mayor a cero, registrar el movimiento de kardex correspondiente
        if (dto.getCurrentStock() != null && dto.getCurrentStock().compareTo(BigDecimal.ZERO) > 0) {
            InventoryMovement initialMovement = InventoryMovement.builder()
                    .inventoryItem(saved)
                    .movementType(MovementType.ENTRADA_COMPRA)
                    .quantity(dto.getCurrentStock())
                    .stockBefore(BigDecimal.ZERO)
                    .stockAfter(dto.getCurrentStock())
                    .unitCostAtMoment(dto.getUnitCost())
                    .reasonNotes("Carga inicial de inventario")
                    .movementDateTime(LocalDateTime.now())
                    .performedBy("SYSTEM")
                    .build();
            inventoryMovementRepository.save(initialMovement);
        }

        return inventoryMapper.toResponseDto(saved);
    }

    @Override
    @Transactional
    public InventoryItemResponseDto updateItem(Long id, InventoryItemRequestDto dto) {
        log.info("Actualizando insumo de inventario ID: {}", id);

        InventoryItem item = inventoryItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Insumo de inventario", "id", id));

        if (inventoryItemRepository.existsByCodeAndIdNot(dto.getCode(), id)) {
            throw new BusinessRuleException("El código " + dto.getCode() + " ya está asignado a otro insumo");
        }

        inventoryMapper.updateEntityFromDto(dto, item);
        InventoryItem updated = inventoryItemRepository.save(item);

        return inventoryMapper.toResponseDto(updated);
    }

    @Override
    public InventoryItemResponseDto getItemById(Long id) {
        return inventoryItemRepository.findById(id)
                .map(inventoryMapper::toResponseDto)
                .orElseThrow(() -> new ResourceNotFoundException("Insumo de inventario", "id", id));
    }

    @Override
    public InventoryItemResponseDto getItemByCode(String code) {
        return inventoryItemRepository.findByCode(code)
                .map(inventoryMapper::toResponseDto)
                .orElseThrow(() -> new ResourceNotFoundException("Insumo de inventario", "code", code));
    }

    @Override
    public List<InventoryItemResponseDto> getAllActiveItems() {
        return inventoryItemRepository.findAllByIsActiveTrueOrderByNameAsc().stream()
                .map(inventoryMapper::toResponseDto)
                .toList();
    }

    @Override
    public List<InventoryItemResponseDto> getLowStockAlerts() {
        return inventoryItemRepository.findLowStockAlerts().stream()
                .map(inventoryMapper::toResponseDto)
                .toList();
    }

    @Override
    public List<InventoryItemResponseDto> getExpiringItems(int daysThreshold) {
        LocalDate threshold = LocalDate.now().plusDays(daysThreshold);
        return inventoryItemRepository.findExpiringItems(threshold).stream()
                .map(inventoryMapper::toResponseDto)
                .toList();
    }

    @Override
    public List<InventoryItemResponseDto> searchItems(String query) {
        if (query == null || query.isBlank()) {
            return getAllActiveItems();
        }
        return inventoryItemRepository.searchActiveItems(query.trim()).stream()
                .map(inventoryMapper::toResponseDto)
                .toList();
    }

    @Override
    public Page<InventoryItemResponseDto> searchItemsPaged(String query, Pageable pageable) {
        String term = (query != null) ? query.trim() : "";
        return inventoryItemRepository.searchItemsPaged(term, pageable)
                .map(inventoryMapper::toResponseDto);
    }

    @Override
    @Transactional
    public void deactivateItem(Long id) {
        InventoryItem item = inventoryItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Insumo de inventario", "id", id));
        item.setIsActive(false);
        inventoryItemRepository.save(item);
        log.info("Insumo con ID {} desactivado", id);
    }

    @Override
    @Transactional
    public InventoryMovementResponseDto registerMovement(InventoryMovementRequestDto dto) {
        log.info("Registrando movimiento de inventario para el insumo ID: {}, tipo: {}, cantidad: {}",
                dto.getInventoryItemId(), dto.getMovementType(), dto.getQuantity());

        InventoryItem item = inventoryItemRepository.findById(dto.getInventoryItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Insumo de inventario", "id", dto.getInventoryItemId()));

        BigDecimal stockBefore = item.getCurrentStock() != null ? item.getCurrentStock() : BigDecimal.ZERO;
        BigDecimal stockAfter;

        boolean isEntry = isEntryMovement(dto.getMovementType());
        if (isEntry) {
            stockAfter = stockBefore.add(dto.getQuantity());
        } else {
            if (stockBefore.compareTo(dto.getQuantity()) < 0) {
                throw new BusinessRuleException(String.format(
                        "Stock insuficiente para el insumo '%s'. Disponible: %s, Solicitado: %s",
                        item.getName(), stockBefore, dto.getQuantity()
                ));
            }
            stockAfter = stockBefore.subtract(dto.getQuantity());
        }

        Patient patient = null;
        if (dto.getPatientId() != null) {
            patient = patientRepository.findById(dto.getPatientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Paciente", "id", dto.getPatientId()));
        }

        MedicalConsultation consultation = null;
        if (dto.getConsultationId() != null) {
            consultation = consultationRepository.findById(dto.getConsultationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Consulta médica", "id", dto.getConsultationId()));
        }

        // Actualizar stock del catálogo
        item.setCurrentStock(stockAfter);
        inventoryItemRepository.save(item);

        // Crear registro en kardex
        InventoryMovement movement = InventoryMovement.builder()
                .inventoryItem(item)
                .movementType(dto.getMovementType())
                .quantity(dto.getQuantity())
                .stockBefore(stockBefore)
                .stockAfter(stockAfter)
                .unitCostAtMoment(dto.getUnitCostAtMoment() != null ? dto.getUnitCostAtMoment() : item.getUnitCost())
                .patient(patient)
                .consultation(consultation)
                .reasonNotes(dto.getReasonNotes())
                .movementDateTime(LocalDateTime.now())
                .performedBy(dto.getPerformedBy())
                .build();

        InventoryMovement saved = inventoryMovementRepository.save(movement);
        log.info("Movimiento de kardex registrado con ID: {}. Stock previo: {}, Nuevo stock: {}",
                saved.getId(), stockBefore, stockAfter);

        return inventoryMapper.toMovementResponseDto(saved);
    }

    @Override
    public KardexReportDto getKardexReport(Long itemId, LocalDateTime startDate, LocalDateTime endDate) {
        InventoryItem item = inventoryItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Insumo de inventario", "id", itemId));

        List<InventoryMovement> movements = inventoryMovementRepository.findKardexReport(itemId, startDate, endDate);

        BigDecimal totalEntries = movements.stream()
                .filter(m -> isEntryMovement(m.getMovementType()))
                .map(InventoryMovement::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExits = movements.stream()
                .filter(m -> !isEntryMovement(m.getMovementType()))
                .map(InventoryMovement::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return KardexReportDto.builder()
                .itemId(item.getId())
                .itemCode(item.getCode())
                .itemName(item.getName())
                .startDate(startDate)
                .endDate(endDate)
                .currentStock(item.getCurrentStock())
                .totalEntries(totalEntries)
                .totalExits(totalExits)
                .movements(movements.stream().map(inventoryMapper::toMovementResponseDto).toList())
                .build();
    }

    @Override
    public List<InventoryMovementResponseDto> getMovementsByItem(Long itemId) {
        if (!inventoryItemRepository.existsById(itemId)) {
            throw new ResourceNotFoundException("Insumo de inventario", "id", itemId);
        }
        return inventoryMovementRepository.findAllByInventoryItemIdOrderByMovementDateTimeDesc(itemId).stream()
                .map(inventoryMapper::toMovementResponseDto)
                .toList();
    }

    @Override
    public List<InventoryMovementResponseDto> getMovementsByPatient(Long patientId) {
        if (!patientRepository.existsById(patientId)) {
            throw new ResourceNotFoundException("Paciente", "id", patientId);
        }
        return inventoryMovementRepository.findAllByPatientIdOrderByMovementDateTimeDesc(patientId).stream()
                .map(inventoryMapper::toMovementResponseDto)
                .toList();
    }

    private boolean isEntryMovement(MovementType type) {
        return type == MovementType.ENTRADA_COMPRA
                || type == MovementType.ENTRADA_DONACION
                || type == MovementType.ENTRADA_AJUSTE;
    }
}
