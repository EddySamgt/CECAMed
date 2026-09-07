package com.cecamed.services.service.impl;

import com.cecamed.core.model.patient.MedicalConsultation;
import com.cecamed.core.model.patient.Patient;
import com.cecamed.core.repository.MedicalConsultationRepository;
import com.cecamed.core.repository.PatientRepository;
import com.cecamed.services.dto.consultation.MedicalConsultationRequestDto;
import com.cecamed.services.dto.consultation.MedicalConsultationResponseDto;
import com.cecamed.services.exception.ResourceNotFoundException;
import com.cecamed.services.mapper.ConsultationMapper;
import com.cecamed.services.service.MedicalConsultationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MedicalConsultationServiceImpl implements MedicalConsultationService {

    private final MedicalConsultationRepository consultationRepository;
    private final PatientRepository patientRepository;
    private final ConsultationMapper consultationMapper;

    @Override
    @Transactional
    public MedicalConsultationResponseDto createConsultation(MedicalConsultationRequestDto dto) {
        log.info("Registrando nueva consulta médica para el paciente ID: {}", dto.getPatientId());

        Patient patient = patientRepository.findById(dto.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", "id", dto.getPatientId()));

        MedicalConsultation consultation = consultationMapper.toEntity(dto);
        consultation.setPatient(patient);

        MedicalConsultation saved = consultationRepository.save(consultation);
        log.info("Consulta médica creada con ID: {}", saved.getId());

        return consultationMapper.toResponseDto(saved);
    }

    @Override
    public MedicalConsultationResponseDto getConsultationById(Long id) {
        return consultationRepository.findById(id)
                .map(consultationMapper::toResponseDto)
                .orElseThrow(() -> new ResourceNotFoundException("Consulta médica", "id", id));
    }

    @Override
    public List<MedicalConsultationResponseDto> getConsultationsByPatientId(Long patientId) {
        if (!patientRepository.existsById(patientId)) {
            throw new ResourceNotFoundException("Paciente", "id", patientId);
        }

        return consultationRepository.findAllByPatientIdOrderByConsultationDateTimeDesc(patientId).stream()
                .map(consultationMapper::toResponseDto)
                .toList();
    }

    @Override
    public List<MedicalConsultationResponseDto> getConsultationsInDateRange(LocalDateTime start, LocalDateTime end) {
        return consultationRepository.findAllInDateRange(start, end).stream()
                .map(consultationMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional
    public MedicalConsultationResponseDto updateConsultation(Long id, MedicalConsultationRequestDto dto) {
        log.info("Actualizando consulta médica ID: {}", id);

        MedicalConsultation consultation = consultationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Consulta médica", "id", id));

        consultationMapper.updateEntityFromDto(dto, consultation);
        MedicalConsultation updated = consultationRepository.save(consultation);

        return consultationMapper.toResponseDto(updated);
    }
}
