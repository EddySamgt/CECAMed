package com.cecamed.services.mapper;

import com.cecamed.core.model.patient.MedicalConsultation;
import com.cecamed.core.model.patient.VitalSigns;
import com.cecamed.services.dto.consultation.MedicalConsultationRequestDto;
import com.cecamed.services.dto.consultation.MedicalConsultationResponseDto;
import com.cecamed.services.dto.consultation.VitalSignsDto;
import org.springframework.stereotype.Component;

@Component
public class ConsultationMapper {

    private final DocumentMapper documentMapper;

    public ConsultationMapper(DocumentMapper documentMapper) {
        this.documentMapper = documentMapper;
    }

    public MedicalConsultation toEntity(MedicalConsultationRequestDto dto) {
        if (dto == null) return null;

        MedicalConsultation consultation = MedicalConsultation.builder()
                .consultationDateTime(dto.getConsultationDateTime())
                .reason(dto.getReason())
                .symptoms(dto.getSymptoms())
                .physicalExamination(dto.getPhysicalExamination())
                .vitalSigns(toVitalSignsEntity(dto.getVitalSigns()))
                .diagnosis(dto.getDiagnosis())
                .icd10Code(dto.getIcd10Code())
                .treatmentPlan(dto.getTreatmentPlan())
                .privateNotes(dto.getPrivateNotes())
                .build();

        if (consultation.getVitalSigns() != null) {
            consultation.getVitalSigns().calculateAndSetBmi();
        }

        return consultation;
    }

    public void updateEntityFromDto(MedicalConsultationRequestDto dto, MedicalConsultation consultation) {
        if (dto == null || consultation == null) return;

        consultation.setConsultationDateTime(dto.getConsultationDateTime());
        consultation.setReason(dto.getReason());
        consultation.setSymptoms(dto.getSymptoms());
        consultation.setPhysicalExamination(dto.getPhysicalExamination());
        consultation.setVitalSigns(toVitalSignsEntity(dto.getVitalSigns()));
        if (consultation.getVitalSigns() != null) {
            consultation.getVitalSigns().calculateAndSetBmi();
        }
        consultation.setDiagnosis(dto.getDiagnosis());
        consultation.setIcd10Code(dto.getIcd10Code());
        consultation.setTreatmentPlan(dto.getTreatmentPlan());
        consultation.setPrivateNotes(dto.getPrivateNotes());
    }

    public MedicalConsultationResponseDto toResponseDto(MedicalConsultation consultation) {
        if (consultation == null) return null;

        return MedicalConsultationResponseDto.builder()
                .id(consultation.getId())
                .patientId(consultation.getPatient() != null ? consultation.getPatient().getId() : null)
                .patientFullName(consultation.getPatient() != null ? consultation.getPatient().getFullName() : null)
                .patientIdentificationNumber(consultation.getPatient() != null ? consultation.getPatient().getIdentificationNumber() : null)
                .consultationDateTime(consultation.getConsultationDateTime())
                .reason(consultation.getReason())
                .symptoms(consultation.getSymptoms())
                .physicalExamination(consultation.getPhysicalExamination())
                .vitalSigns(toVitalSignsDto(consultation.getVitalSigns()))
                .diagnosis(consultation.getDiagnosis())
                .icd10Code(consultation.getIcd10Code())
                .treatmentPlan(consultation.getTreatmentPlan())
                .privateNotes(consultation.getPrivateNotes())
                .attachedDocuments(documentMapper.toResponseDtoList(consultation.getAttachedDocuments()))
                .createdAt(consultation.getCreatedAt())
                .updatedAt(consultation.getUpdatedAt())
                .build();
    }

    public VitalSigns toVitalSignsEntity(VitalSignsDto dto) {
        if (dto == null) return new VitalSigns();

        VitalSigns vs = VitalSigns.builder()
                .systolicPressure(dto.getSystolicPressure())
                .diastolicPressure(dto.getDiastolicPressure())
                .heartRate(dto.getHeartRate())
                .respiratoryRate(dto.getRespiratoryRate())
                .temperatureCelsius(dto.getTemperatureCelsius())
                .weightKg(dto.getWeightKg())
                .heightCm(dto.getHeightCm())
                .oxygenSaturationPercentage(dto.getOxygenSaturationPercentage())
                .build();

        vs.calculateAndSetBmi();
        return vs;
    }

    public VitalSignsDto toVitalSignsDto(VitalSigns entity) {
        if (entity == null) return null;

        return VitalSignsDto.builder()
                .systolicPressure(entity.getSystolicPressure())
                .diastolicPressure(entity.getDiastolicPressure())
                .heartRate(entity.getHeartRate())
                .respiratoryRate(entity.getRespiratoryRate())
                .temperatureCelsius(entity.getTemperatureCelsius())
                .weightKg(entity.getWeightKg())
                .heightCm(entity.getHeightCm())
                .bmi(entity.getBmi())
                .oxygenSaturationPercentage(entity.getOxygenSaturationPercentage())
                .build();
    }
}
