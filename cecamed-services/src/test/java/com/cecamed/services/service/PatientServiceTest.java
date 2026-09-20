package com.cecamed.services.service;

import com.cecamed.core.model.patient.Patient;
import com.cecamed.core.model.patient.enums.Gender;
import com.cecamed.core.repository.MedicalRecordRepository;
import com.cecamed.core.repository.PatientRepository;
import com.cecamed.services.dto.patient.PatientRequestDto;
import com.cecamed.services.exception.BusinessRuleException;
import com.cecamed.services.exception.ResourceNotFoundException;
import com.cecamed.services.mapper.PatientMapper;
import com.cecamed.services.service.impl.PatientServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {
    @Mock PatientRepository patients;
    @Mock MedicalRecordRepository records;
    private PatientService service;
    private final LocalDate birthday = LocalDate.of(1990, 5, 20);

    @BeforeEach
    void setUp() {
        service = new PatientServiceImpl(patients, records, new PatientMapper());
    }

    private PatientRequestDto request() {
        return PatientRequestDto.builder().firstName("  María   José ").lastName("LÓPEZ")
                .birthDate(birthday).gender(Gender.FEMENINO).build();
    }

    @Test
    void createsPatientAndRecordWithoutIdentification() {
        try (var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(request())).isEmpty();
        }
        when(patients.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var response = service.createPatient(request());
        assertThat(response.getIdentificationNumber()).isNull();
        assertThat(response.getMedicalRecordNumber()).startsWith("EXP-");
        verify(patients).existsByNormalizedFirstNameAndNormalizedLastNameAndBirthDateAndIdNot(
                "maria jose", "lopez", birthday, -1L);
    }

    @Test
    void rejectsDuplicateBeforeCreatingRecord() {
        when(patients.existsByNormalizedFirstNameAndNormalizedLastNameAndBirthDateAndIdNot(
                "maria jose", "lopez", birthday, -1L)).thenReturn(true);
        assertThatThrownBy(() -> service.createPatient(request())).isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("inactivos");
        verify(patients, never()).saveAndFlush(any());
        verifyNoInteractions(records);
    }

    @Test
    void allowsEditingSamePatientAndPreservesHistoricalIdentification() {
        var existing = Patient.builder().id(1L).identificationNumber("legacy").build();
        when(patients.findById(1L)).thenReturn(Optional.of(existing));
        when(patients.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        assertThat(service.updatePatient(1L, request()).getIdentificationNumber()).isEqualTo("legacy");
        verify(patients).existsByNormalizedFirstNameAndNormalizedLastNameAndBirthDateAndIdNot(
                "maria jose", "lopez", birthday, 1L);
    }

    @Test
    void rejectsEditingIntoAnotherPatientBeforeChangingEntity() {
        var existing = Patient.builder().id(1L).firstName("Ana").build();
        when(patients.findById(1L)).thenReturn(Optional.of(existing));
        when(patients.existsByNormalizedFirstNameAndNormalizedLastNameAndBirthDateAndIdNot(
                "maria jose", "lopez", birthday, 1L)).thenReturn(true);
        assertThatThrownBy(() -> service.updatePatient(1L, request())).isInstanceOf(BusinessRuleException.class);
        assertThat(existing.getFirstName()).isEqualTo("Ana");
        verify(patients, never()).saveAndFlush(any());
    }

    @Test
    void concurrentDuplicateReportsBusinessError() {
        var violation = new org.hibernate.exception.ConstraintViolationException("duplicate",
                new java.sql.SQLException("duplicate", "23505"), "uk_patient_identity");
        when(patients.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate", violation));
        assertThatThrownBy(() -> service.createPatient(request())).isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Ya existe un paciente");
    }

    @Test
    void doesNotMaskOtherDatabaseErrors() {
        var error = new DataIntegrityViolationException("other constraint");
        when(patients.saveAndFlush(any())).thenThrow(error);
        assertThatThrownBy(() -> service.createPatient(request())).isSameAs(error);
    }

    @Test
    void missingPatientFails() {
        assertThatThrownBy(() -> service.getPatientById(999L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
