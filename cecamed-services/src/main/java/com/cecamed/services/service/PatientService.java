package com.cecamed.services.service;

import com.cecamed.services.dto.patient.PatientRequestDto;
import com.cecamed.services.dto.patient.PatientResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PatientService {

    PatientResponseDto createPatient(PatientRequestDto requestDto);

    PatientResponseDto updatePatient(Long id, PatientRequestDto requestDto);

    PatientResponseDto getPatientById(Long id);

    PatientResponseDto getPatientByIdentification(String identificationNumber);

    List<PatientResponseDto> getAllActivePatients();

    List<PatientResponseDto> searchPatients(String term);

    Page<PatientResponseDto> searchPatientsPaged(String term, Pageable pageable);

    void deactivatePatient(Long id);

    void activatePatient(Long id);
}
