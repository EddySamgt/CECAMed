package com.cecamed.services.mapper;

import com.cecamed.core.model.patient.MedicalRecord;
import com.cecamed.services.dto.patient.MedicalRecordDto;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PatientMapperTest {
    @Test
    void roundTripsAndClearsHistoryFields() {
        var mapper = new PatientMapper();
        var record = new MedicalRecord();
        var dto = MedicalRecordDto.builder()
                .gynecologicalObstetricHistory("Dos partos")
                .nonPathologicalHistory("Camina diariamente")
                .waterGlassesPerDay(0).mealsPerDay(3).build();
        mapper.updateRecordFromDto(dto, record);
        assertThat(mapper.toRecordDto(record)).usingRecursiveComparison().isEqualTo(dto);
        var empty = new MedicalRecordDto();
        mapper.updateRecordFromDto(empty, record);
        assertThat(mapper.toRecordDto(record)).usingRecursiveComparison().isEqualTo(empty);
    }
}
