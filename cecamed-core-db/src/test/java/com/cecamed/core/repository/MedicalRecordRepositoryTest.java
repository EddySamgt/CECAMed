package com.cecamed.core.repository;

import com.cecamed.core.model.patient.MedicalRecord;
import com.cecamed.core.model.patient.Patient;
import com.cecamed.core.model.patient.enums.Gender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("MedicalRecordRepository - Pruebas de persistencia del expediente clínico")
class MedicalRecordRepositoryTest {

    @Autowired
    private MedicalRecordRepository medicalRecordRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Test
    @DisplayName("Debe guardar expediente asociado a paciente y buscar por número de expediente")
    void shouldSaveAndFindByRecordNumber() {
        Patient patient = Patient.builder()
                .firstName("Ana")
                .lastName("García")
                .identificationNumber("0501-1995-12345")
                .birthDate(LocalDate.of(1995, 8, 20))
                .gender(Gender.FEMENINO)
                .build();
        Patient savedPatient = patientRepository.save(patient);

        MedicalRecord record = MedicalRecord.builder()
                .patient(savedPatient)
                .recordNumber("EXP-2026-0001")
                .allergies("Penicilina, Sulfas")
                .pathologicalHistory("Hipertensión arterial controlada")
                .surgicalHistory("Apendicectomía en 2015")
                .currentMedications("Losartán 50mg cada 24h")
                .build();

        MedicalRecord savedRecord = medicalRecordRepository.save(record);
        assertThat(savedRecord.getId()).isNotNull();

        Optional<MedicalRecord> foundByNumber = medicalRecordRepository.findByRecordNumber("EXP-2026-0001");
        assertThat(foundByNumber).isPresent();
        assertThat(foundByNumber.get().getAllergies()).contains("Penicilina");

        Optional<MedicalRecord> foundByPatient = medicalRecordRepository.findByPatientId(savedPatient.getId());
        assertThat(foundByPatient).isPresent();
        assertThat(foundByPatient.get().getRecordNumber()).isEqualTo("EXP-2026-0001");
    }
}
