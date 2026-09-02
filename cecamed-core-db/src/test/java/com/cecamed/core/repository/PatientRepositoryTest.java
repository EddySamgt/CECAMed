package com.cecamed.core.repository;

import com.cecamed.core.model.patient.Patient;
import com.cecamed.core.model.patient.enums.BloodType;
import com.cecamed.core.model.patient.enums.Gender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("PatientRepository - Pruebas de persistencia y consultas")
class PatientRepositoryTest {

    @Autowired
    private PatientRepository patientRepository;

    private Patient createSamplePatient(String firstName, String lastName, String dni, String phone) {
        return Patient.builder()
                .firstName(firstName)
                .lastName(lastName)
                .identificationNumber(dni)
                .birthDate(LocalDate.of(1990, 5, 15))
                .gender(Gender.MASCULINO)
                .bloodType(BloodType.O_POSITIVO)
                .phone(phone)
                .email(firstName.toLowerCase() + "@example.com")
                .emergencyContactName("María Gómez")
                .emergencyContactPhone("555-9999")
                .emergencyContactRelationship("Esposa")
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Debe guardar y recuperar un paciente por ID")
    void shouldSaveAndFindById() {
        Patient patient = createSamplePatient("Juan", "Pérez", "0101-1990-12345", "555-1234");
        Patient saved = patientRepository.save(patient);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();

        Optional<Patient> found = patientRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getFullName()).isEqualTo("Juan Pérez");
        assertThat(found.get().getIdentificationNumber()).isEqualTo("0101-1990-12345");
    }

    @Test
    @DisplayName("Debe buscar por número de identificación (DNI)")
    void shouldFindByIdentificationNumber() {
        Patient patient = createSamplePatient("Carlos", "Santana", "0801-1985-00112", "555-4321");
        patientRepository.save(patient);

        Optional<Patient> found = patientRepository.findByIdentificationNumber("0801-1985-00112");
        assertThat(found).isPresent();
        assertThat(found.get().getLastName()).isEqualTo("Santana");

        boolean exists = patientRepository.existsByIdentificationNumber("0801-1985-00112");
        assertThat(exists).isTrue();

        boolean notExists = patientRepository.existsByIdentificationNumber("9999-9999-99999");
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Debe buscar pacientes activos por término de búsqueda (nombre, apellido, DNI, teléfono)")
    void shouldSearchActivePatients() {
        Patient p1 = createSamplePatient("Mario", "Bros", "1111", "555-0001");
        Patient p2 = createSamplePatient("Luigi", "Bros", "2222", "555-0002");
        Patient p3 = createSamplePatient("Peach", "Toadstool", "3333", "555-0003");
        p3.setActive(false);

        patientRepository.saveAll(List.of(p1, p2, p3));

        List<Patient> brosResults = patientRepository.searchActivePatients("Bros");
        assertThat(brosResults).hasSize(2);

        List<Patient> phoneResults = patientRepository.searchActivePatients("0002");
        assertThat(phoneResults).hasSize(1);
        assertThat(phoneResults.get(0).getFirstName()).isEqualTo("Luigi");

        // El paciente inactivo no debe aparecer en searchActivePatients
        List<Patient> inactiveResults = patientRepository.searchActivePatients("Peach");
        assertThat(inactiveResults).isEmpty();
    }

    @Test
    @DisplayName("Debe paginar la búsqueda de pacientes")
    void shouldSearchPatientsPaged() {
        for (int i = 1; i <= 15; i++) {
            Patient p = createSamplePatient("Paciente" + i, "Test", "DNI-" + i, "555-" + i);
            patientRepository.save(p);
        }

        Page<Patient> page = patientRepository.searchPatientsPaged("Paciente", PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(15);
        assertThat(page.getContent()).hasSize(10);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }
}
