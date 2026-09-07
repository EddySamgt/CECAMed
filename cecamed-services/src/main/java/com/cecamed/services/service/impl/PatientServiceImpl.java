package com.cecamed.services.service.impl;

import com.cecamed.core.model.patient.MedicalRecord;
import com.cecamed.core.model.patient.Patient;
import com.cecamed.core.repository.MedicalRecordRepository;
import com.cecamed.core.repository.PatientRepository;
import com.cecamed.services.dto.patient.PatientRequestDto;
import com.cecamed.services.dto.patient.PatientResponseDto;
import com.cecamed.services.exception.BusinessRuleException;
import com.cecamed.services.exception.ResourceNotFoundException;
import com.cecamed.services.mapper.PatientMapper;
import com.cecamed.services.service.PatientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final PatientMapper patientMapper;

    @Override
    @Transactional
    public PatientResponseDto createPatient(PatientRequestDto requestDto) {
        log.info("Creando nuevo paciente con DNI/identificación: {}", requestDto.getIdentificationNumber());

        if (patientRepository.existsByIdentificationNumber(requestDto.getIdentificationNumber())) {
            throw new BusinessRuleException(
                    "Ya existe un paciente registrado con el número de identificación: " + requestDto.getIdentificationNumber()
            );
        }

        Patient patient = patientMapper.toEntity(requestDto);

        // Generación automática del expediente clínico inicial vinculado
        String generatedRecordNumber = generateUniqueRecordNumber();
        MedicalRecord record = MedicalRecord.builder()
                .recordNumber(generatedRecordNumber)
                .build();
        patient.setMedicalRecord(record);

        Patient saved = patientRepository.save(patient);
        log.info("Paciente creado con ID: {} y Expediente: {}", saved.getId(), generatedRecordNumber);

        return patientMapper.toResponseDto(saved);
    }

    @Override
    @Transactional
    public PatientResponseDto updatePatient(Long id, PatientRequestDto requestDto) {
        log.info("Actualizando información del paciente ID: {}", id);

        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", "id", id));

        if (patientRepository.existsByIdentificationNumberAndIdNot(requestDto.getIdentificationNumber(), id)) {
            throw new BusinessRuleException(
                    "El número de identificación " + requestDto.getIdentificationNumber() + " ya está en uso por otro paciente"
            );
        }

        patientMapper.updateEntityFromDto(requestDto, patient);
        Patient updated = patientRepository.save(patient);

        return patientMapper.toResponseDto(updated);
    }

    @Override
    public PatientResponseDto getPatientById(Long id) {
        return patientRepository.findById(id)
                .map(patientMapper::toResponseDto)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", "id", id));
    }

    @Override
    public PatientResponseDto getPatientByIdentification(String identificationNumber) {
        return patientRepository.findByIdentificationNumber(identificationNumber)
                .map(patientMapper::toResponseDto)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", "identificationNumber", identificationNumber));
    }

    @Override
    public List<PatientResponseDto> getAllActivePatients() {
        return patientRepository.findAllByActiveTrueOrderByLastNameAscFirstNameAsc().stream()
                .map(patientMapper::toResponseDto)
                .toList();
    }

    @Override
    public List<PatientResponseDto> searchPatients(String term) {
        if (term == null || term.trim().isEmpty()) {
            return getAllActivePatients();
        }
        return patientRepository.searchActivePatients(term.trim()).stream()
                .map(patientMapper::toResponseDto)
                .toList();
    }

    @Override
    public Page<PatientResponseDto> searchPatientsPaged(String term, Pageable pageable) {
        String query = (term != null) ? term.trim() : "";
        return patientRepository.searchPatientsPaged(query, pageable)
                .map(patientMapper::toResponseDto);
    }

    @Override
    @Transactional
    public void deactivatePatient(Long id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", "id", id));
        patient.setActive(false);
        patientRepository.save(patient);
        log.info("Paciente con ID {} desactivado", id);
    }

    @Override
    @Transactional
    public void activatePatient(Long id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", "id", id));
        patient.setActive(true);
        patientRepository.save(patient);
        log.info("Paciente con ID {} reactivado", id);
    }

    private String generateUniqueRecordNumber() {
        int year = Year.now().getValue();
        String candidate;
        do {
            int randomSeq = ThreadLocalRandom.current().nextInt(10000, 99999);
            candidate = String.format("EXP-%d-%05d", year, randomSeq);
        } while (medicalRecordRepository.existsByRecordNumber(candidate));
        return candidate;
    }
}
