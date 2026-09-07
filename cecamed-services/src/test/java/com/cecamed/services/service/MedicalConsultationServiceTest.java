package com.cecamed.services.service;

import com.cecamed.core.model.patient.MedicalConsultation;
import com.cecamed.core.model.patient.Patient;
import com.cecamed.core.repository.MedicalConsultationRepository;
import com.cecamed.core.repository.PatientRepository;
import com.cecamed.services.dto.consultation.MedicalConsultationRequestDto;
import com.cecamed.services.dto.consultation.MedicalConsultationResponseDto;
import com.cecamed.services.dto.consultation.VitalSignsDto;
import com.cecamed.services.mapper.ConsultationMapper;
import com.cecamed.services.mapper.DocumentMapper;
import com.cecamed.services.service.impl.MedicalConsultationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicalConsultationServiceTest {

    @Mock
    private MedicalConsultationRepository consultationRepository;
    @Mock
    private PatientRepository patientRepository;

    private MedicalConsultationService consultationService;

    @BeforeEach
    void setUp() {
        DocumentMapper documentMapper = new DocumentMapper();
        ConsultationMapper consultationMapper = new ConsultationMapper(documentMapper);
        consultationService = new MedicalConsultationServiceImpl(
                consultationRepository,
                patientRepository,
                consultationMapper
        );
    }

    @Test
    @DisplayName("Debe registrar consulta médica y calcular el IMC automáticamente a partir de peso y talla")
    void shouldCreateConsultationAndCalculateBmi() {
        Patient patient = Patient.builder().id(1L).firstName("Lucia").lastName("Mendez").build();
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));

        VitalSignsDto signs = VitalSignsDto.builder()
                .weightKg(new BigDecimal("70.00"))
                .heightCm(new BigDecimal("175.00"))
                .systolicPressure(120)
                .diastolicPressure(80)
                .build();

        MedicalConsultationRequestDto request = MedicalConsultationRequestDto.builder()
                .patientId(1L)
                .consultationDateTime(LocalDateTime.now())
                .reason("Chequeo preventivo")
                .diagnosis("Paciente clínicamente sano")
                .vitalSigns(signs)
                .build();

        when(consultationRepository.save(any(MedicalConsultation.class))).thenAnswer(i -> {
            MedicalConsultation mc = i.getArgument(0);
            mc.setId(25L);
            return mc;
        });

        MedicalConsultationResponseDto response = consultationService.createConsultation(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(25L);
        assertThat(response.getVitalSigns()).isNotNull();
        // IMC = 70 / (1.75^2) = 70 / 3.0625 = 22.86
        assertThat(response.getVitalSigns().getBmi()).isNotNull();
        assertThat(response.getVitalSigns().getBmi()).isEqualByComparingTo(new BigDecimal("22.86"));
    }
}
