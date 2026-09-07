package com.cecamed.services.service.impl;

import com.cecamed.core.model.patient.MedicalRecord;
import com.cecamed.core.model.patient.Patient;
import com.cecamed.core.repository.MedicalRecordRepository;
import com.cecamed.core.repository.PatientRepository;
import com.cecamed.services.dto.patient.MedicalRecordDto;
import com.cecamed.services.exception.ResourceNotFoundException;
import com.cecamed.services.mapper.PatientMapper;
import com.cecamed.services.service.MedicalRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MedicalRecordServiceImpl implements MedicalRecordService {

    private final MedicalRecordRepository medicalRecordRepository;
    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;

    @Override
    public MedicalRecordDto getRecordByPatientId(Long patientId) {
        return medicalRecordRepository.findByPatientId(patientId)
                .map(patientMapper::toRecordDto)
                .orElseThrow(() -> new ResourceNotFoundException("Expediente clínico no encontrado para el paciente con ID: " + patientId));
    }

    @Override
    public MedicalRecordDto getRecordByRecordNumber(String recordNumber) {
        return medicalRecordRepository.findByRecordNumber(recordNumber)
                .map(patientMapper::toRecordDto)
                .orElseThrow(() -> new ResourceNotFoundException("Expediente clínico", "recordNumber", recordNumber));
    }

    @Override
    @Transactional
    public MedicalRecordDto updateMedicalRecord(Long patientId, MedicalRecordDto dto) {
        log.info("Actualizando antecedentes del expediente para el paciente ID: {}", patientId);

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", "id", patientId));

        MedicalRecord record = medicalRecordRepository.findByPatientId(patientId)
                .orElseGet(() -> {
                    MedicalRecord newRecord = MedicalRecord.builder()
                            .recordNumber(dto.getRecordNumber() != null ? dto.getRecordNumber() : "EXP-" + patientId)
                            .patient(patient)
                            .build();
                    patient.setMedicalRecord(newRecord);
                    return newRecord;
                });

        patientMapper.updateRecordFromDto(dto, record);
        MedicalRecord saved = medicalRecordRepository.save(record);

        return patientMapper.toRecordDto(saved);
    }
}
