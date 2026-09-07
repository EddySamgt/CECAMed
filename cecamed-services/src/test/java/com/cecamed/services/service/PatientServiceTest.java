package com.cecamed.services.service;

import com.cecamed.core.model.patient.Patient;
import com.cecamed.core.model.patient.enums.BloodType;
import com.cecamed.core.model.patient.enums.Gender;
import com.cecamed.core.repository.MedicalRecordRepository;
import com.cecamed.core.repository.PatientRepository;
import com.cecamed.services.dto.patient.PatientRequestDto;
import com.cecamed.services.dto.patient.PatientResponseDto;
import com.cecamed.services.exception.BusinessRuleException;
import com.cecamed.services.exception.ResourceNotFoundException;
import com.cecamed.services.mapper.PatientMapper;
import com.cecamed.services.service.impl.PatientServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock
    private PatientRepository patientRepository;
    @Mock
    private MedicalRecordRepository medicalRecordRepository;

    private PatientService patientService;

    @BeforeEach
    void setUp() {
        PatientMapper patientMapper = new PatientMapper();
        patientService = new PatientServiceImpl(patientRepository, medicalRecordRepository, patientMapper);
    }

    @Test
    @DisplayName("Debe crear paciente y generar su expediente clínico automáticamente")
    void shouldCreatePatientWithMedicalRecord() {
        PatientRequestDto request = PatientRequestDto.builder()
                .firstName("Maria")
                .lastName("Lopez")
                .identificationNumber("DNI-12345678")
                .birthDate(LocalDate.of(1990, 5, 20))
                .gender(Gender.FEMENINO)
                .bloodType(BloodType.O_POSITIVO)
                .phone("555-1234")
                .email("maria@example.com")
                .build();

        when(patientRepository.existsByIdentificationNumber("DNI-12345678")).thenReturn(false);
        when(medicalRecordRepository.existsByRecordNumber(any())).thenReturn(false);

        Patient savedMock = Patient.builder()
                .id(1L)
                .firstName("Maria")
                .lastName("Lopez")
                .identificationNumber("DNI-12345678")
                .birthDate(LocalDate.of(1990, 5, 20))
                .gender(Gender.FEMENINO)
                .bloodType(BloodType.O_POSITIVO)
                .active(true)
                .build();

        when(patientRepository.save(any(Patient.class))).thenReturn(savedMock);

        PatientResponseDto response = patientService.createPatient(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getFullName()).isEqualTo("Maria Lopez");

        ArgumentCaptor<Patient> captor = ArgumentCaptor.forClass(Patient.class);
        verify(patientRepository).save(captor.capture());
        Patient captured = captor.getValue();
        assertThat(captured.getMedicalRecord()).isNotNull();
        assertThat(captured.getMedicalRecord().getRecordNumber()).startsWith("EXP-");
    }

    @Test
    @DisplayName("Debe rechazar la creación de paciente con DNI duplicado")
    void shouldRejectDuplicateIdentification() {
        PatientRequestDto request = PatientRequestDto.builder()
                .identificationNumber("DNI-99999999")
                .build();

        when(patientRepository.existsByIdentificationNumber("DNI-99999999")).thenReturn(true);

        assertThatThrownBy(() -> patientService.createPatient(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Ya existe un paciente");
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException si el paciente no existe")
    void shouldThrowIfPatientNotFound() {
        when(patientRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> patientService.getPatientById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
