package com.cecamed.services.service;

import com.cecamed.services.dto.consultation.MedicalConsultationRequestDto;
import com.cecamed.services.dto.consultation.MedicalConsultationResponseDto;

import java.time.LocalDateTime;
import java.util.List;

public interface MedicalConsultationService {

    MedicalConsultationResponseDto createConsultation(MedicalConsultationRequestDto dto);

    MedicalConsultationResponseDto getConsultationById(Long id);

    List<MedicalConsultationResponseDto> getConsultationsByPatientId(Long patientId);

    List<MedicalConsultationResponseDto> getConsultationsInDateRange(LocalDateTime start, LocalDateTime end);

    MedicalConsultationResponseDto updateConsultation(Long id, MedicalConsultationRequestDto dto);
}
