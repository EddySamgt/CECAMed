package com.cecamed.services.service;

import com.cecamed.services.dto.patient.MedicalRecordDto;

public interface MedicalRecordService {

    MedicalRecordDto getRecordByPatientId(Long patientId);

    MedicalRecordDto getRecordByRecordNumber(String recordNumber);

    MedicalRecordDto updateMedicalRecord(Long patientId, MedicalRecordDto dto);
}
