package com.cecamed.services.mapper;

import com.cecamed.core.model.patient.MedicalRecord;
import com.cecamed.core.model.patient.Patient;
import com.cecamed.services.dto.patient.MedicalRecordDto;
import com.cecamed.services.dto.patient.PatientRequestDto;
import com.cecamed.services.dto.patient.PatientResponseDto;
import org.springframework.stereotype.Component;

@Component
public class PatientMapper {

    public Patient toEntity(PatientRequestDto dto) {
        if (dto == null) return null;

        return Patient.builder()
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .identificationNumber(dto.getIdentificationNumber())
                .birthDate(dto.getBirthDate())
                .gender(dto.getGender())
                .bloodType(dto.getBloodType())
                .phone(dto.getPhone())
                .email(dto.getEmail())
                .address(dto.getAddress())
                .emergencyContactName(dto.getEmergencyContactName())
                .emergencyContactPhone(dto.getEmergencyContactPhone())
                .emergencyContactRelationship(dto.getEmergencyContactRelationship())
                .notes(dto.getNotes())
                .active(true)
                .build();
    }

    public void updateEntityFromDto(PatientRequestDto dto, Patient patient) {
        if (dto == null || patient == null) return;

        patient.setFirstName(dto.getFirstName());
        patient.setLastName(dto.getLastName());
        patient.setIdentificationNumber(dto.getIdentificationNumber());
        patient.setBirthDate(dto.getBirthDate());
        patient.setGender(dto.getGender());
        if (dto.getBloodType() != null) {
            patient.setBloodType(dto.getBloodType());
        }
        patient.setPhone(dto.getPhone());
        patient.setEmail(dto.getEmail());
        patient.setAddress(dto.getAddress());
        patient.setEmergencyContactName(dto.getEmergencyContactName());
        patient.setEmergencyContactPhone(dto.getEmergencyContactPhone());
        patient.setEmergencyContactRelationship(dto.getEmergencyContactRelationship());
        patient.setNotes(dto.getNotes());
    }

    public PatientResponseDto toResponseDto(Patient patient) {
        if (patient == null) return null;

        return PatientResponseDto.builder()
                .id(patient.getId())
                .firstName(patient.getFirstName())
                .lastName(patient.getLastName())
                .fullName(patient.getFullName())
                .identificationNumber(patient.getIdentificationNumber())
                .birthDate(patient.getBirthDate())
                .age(patient.getAge())
                .gender(patient.getGender())
                .bloodType(patient.getBloodType())
                .phone(patient.getPhone())
                .email(patient.getEmail())
                .address(patient.getAddress())
                .emergencyContactName(patient.getEmergencyContactName())
                .emergencyContactPhone(patient.getEmergencyContactPhone())
                .emergencyContactRelationship(patient.getEmergencyContactRelationship())
                .notes(patient.getNotes())
                .active(patient.getActive())
                .medicalRecordNumber(patient.getMedicalRecord() != null ? patient.getMedicalRecord().getRecordNumber() : null)
                .createdAt(patient.getCreatedAt())
                .updatedAt(patient.getUpdatedAt())
                .build();
    }

    public MedicalRecordDto toRecordDto(MedicalRecord record) {
        if (record == null) return null;

        return MedicalRecordDto.builder()
                .id(record.getId())
                .patientId(record.getPatient() != null ? record.getPatient().getId() : null)
                .recordNumber(record.getRecordNumber())
                .allergies(record.getAllergies())
                .pathologicalHistory(record.getPathologicalHistory())
                .nonPathologicalHistory(record.getNonPathologicalHistory())
                .familyHistory(record.getFamilyHistory())
                .surgicalHistory(record.getSurgicalHistory())
                .currentMedications(record.getCurrentMedications())
                .generalObservations(record.getGeneralObservations())
                .createdAt(record.getCreatedAt())
                .updatedAt(record.getUpdatedAt())
                .build();
    }

    public void updateRecordFromDto(MedicalRecordDto dto, MedicalRecord record) {
        if (dto == null || record == null) return;

        record.setAllergies(dto.getAllergies());
        record.setPathologicalHistory(dto.getPathologicalHistory());
        record.setNonPathologicalHistory(dto.getNonPathologicalHistory());
        record.setFamilyHistory(dto.getFamilyHistory());
        record.setSurgicalHistory(dto.getSurgicalHistory());
        record.setCurrentMedications(dto.getCurrentMedications());
        record.setGeneralObservations(dto.getGeneralObservations());
    }
}
