package com.cecamed.services.service;

import com.cecamed.core.model.inventory.InventoryItem;
import com.cecamed.core.model.inventory.InventoryMovement;
import com.cecamed.core.model.inventory.enums.ItemCategory;
import com.cecamed.core.model.inventory.enums.MovementType;
import com.cecamed.core.model.inventory.enums.UnitOfMeasure;
import com.cecamed.core.repository.InventoryItemRepository;
import com.cecamed.core.repository.InventoryMovementRepository;
import com.cecamed.core.repository.MedicalConsultationRepository;
import com.cecamed.core.repository.PatientRepository;
import com.cecamed.services.dto.inventory.InventoryMovementRequestDto;
import com.cecamed.services.dto.inventory.InventoryMovementResponseDto;
import com.cecamed.services.exception.BusinessRuleException;
import com.cecamed.services.mapper.InventoryMapper;
import com.cecamed.services.service.impl.InventoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryItemRepository inventoryItemRepository;
    @Mock
    private InventoryMovementRepository inventoryMovementRepository;
    @Mock
    private PatientRepository patientRepository;
    @Mock
    private MedicalConsultationRepository consultationRepository;

    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        InventoryMapper inventoryMapper = new InventoryMapper();
        inventoryService = new InventoryServiceImpl(
                inventoryItemRepository,
                inventoryMovementRepository,
                patientRepository,
                consultationRepository,
                inventoryMapper
        );
    }

    @Test
    @DisplayName("Debe registrar movimiento de entrada e incrementar stock correctamente")
    void shouldRegisterEntryMovement() {
        InventoryItem item = InventoryItem.builder()
                .id(1L)
                .code("MED-001")
                .name("Paracetamol 500mg")
                .currentStock(new BigDecimal("50.00"))
                .unitOfMeasure(UnitOfMeasure.TABLETA)
                .category(ItemCategory.MEDICAMENTO)
                .build();

        when(inventoryItemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(inventoryMovementRepository.save(any(InventoryMovement.class))).thenAnswer(i -> {
            InventoryMovement m = i.getArgument(0);
            m.setId(10L);
            return m;
        });

        InventoryMovementRequestDto request = InventoryMovementRequestDto.builder()
                .inventoryItemId(1L)
                .movementType(MovementType.ENTRADA_COMPRA)
                .quantity(new BigDecimal("20.00"))
                .unitCostAtMoment(new BigDecimal("0.50"))
                .performedBy("Farmacéutico")
                .build();

        InventoryMovementResponseDto response = inventoryService.registerMovement(request);

        assertThat(response).isNotNull();
        assertThat(response.getStockBefore()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(response.getStockAfter()).isEqualByComparingTo(new BigDecimal("70.00"));

        ArgumentCaptor<InventoryItem> itemCaptor = ArgumentCaptor.forClass(InventoryItem.class);
        verify(inventoryItemRepository).save(itemCaptor.capture());
        assertThat(itemCaptor.getValue().getCurrentStock()).isEqualByComparingTo(new BigDecimal("70.00"));
    }

    @Test
    @DisplayName("Debe rechazar salida de inventario si el stock es insuficiente")
    void shouldRejectExitWhenStockIsInsufficient() {
        InventoryItem item = InventoryItem.builder()
                .id(1L)
                .code("MED-001")
                .name("Paracetamol 500mg")
                .currentStock(new BigDecimal("5.00"))
                .build();

        when(inventoryItemRepository.findById(1L)).thenReturn(Optional.of(item));

        InventoryMovementRequestDto request = InventoryMovementRequestDto.builder()
                .inventoryItemId(1L)
                .movementType(MovementType.SALIDA_CONSULTA)
                .quantity(new BigDecimal("10.00"))
                .build();

        assertThatThrownBy(() -> inventoryService.registerMovement(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Stock insuficiente");
    }
}
