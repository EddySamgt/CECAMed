package com.cecamed.core.repository;

import com.cecamed.core.model.patient.Patient;
import com.cecamed.core.model.patient.enums.Gender;
import com.cecamed.core.model.appointment.Appointment;
import com.cecamed.core.model.appointment.ScheduleBlock;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("postgres")
@DataJpaTest
class PostgresMigrationTest {
    @Autowired Flyway flyway;
    @Autowired PatientRepository patients;
    @Autowired AppointmentRepository appointments;
    @Autowired DoctorScheduleRepository schedules;
    @Autowired ScheduleBlockRepository blocks;

    @Test
    void migrationsValidateAndSeedSchedulesOnlyOnce() {
        assertThat(flyway.validateWithResult().validationSuccessful).isTrue();
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(schedules.findAllByIsActiveTrueOrderByDayOfWeekAscStartTimeAsc()).hasSize(6);
        assertThat(schedules.findByDayOfWeekAndIsActiveTrue(DayOfWeek.MONDAY)).isPresent();
    }

    @Test
    void persistsAndSearchesPatientsAndChecksNullableOverlapParameter() {
        Patient patient = patients.saveAndFlush(Patient.builder().firstName("María").lastName("Prueba")
                .identificationNumber("PG-INTEGRATION-TEST").birthDate(LocalDate.of(1990, 1, 1))
                .gender(Gender.FEMENINO).notes("Texto clínico de prueba").build());
        assertThat(patient.getId()).isPositive();
        assertThat(patient.getCreatedAt()).isNotNull();
        assertThat(patients.searchActivePatients("maría")).extracting(Patient::getId).contains(patient.getId());
        LocalDateTime start = LocalDateTime.of(2026, 10, 5, 9, 0);
        Appointment appointment = appointments.saveAndFlush(Appointment.builder().patient(patient)
                .startTime(start).endTime(start.plusMinutes(30)).reasonForVisit("Prueba").build());
        assertThat(appointments.hasOverlappingAppointment(start, start.plusMinutes(15), null)).isTrue();
        assertThat(appointments.hasOverlappingAppointment(start, start.plusMinutes(15), appointment.getId())).isFalse();
        assertThat(appointments.findOverlappingAppointments(start, start.plusMinutes(15), null)).hasSize(1);
        assertThat(appointments.hasOverlappingAppointment(start.plusMinutes(30), start.plusHours(1), null)).isFalse();
    }

    @Test
    void findsBlocksSpanningTheEntireRequestedDay() {
        LocalDateTime day = LocalDate.of(2026, 10, 5).atStartOfDay();
        ScheduleBlock block = blocks.saveAndFlush(ScheduleBlock.builder().title("Vacaciones")
                .startDateTime(day.minusDays(1)).endDateTime(day.plusDays(2)).build());
        assertThat(blocks.findOverlappingBlocks(day, day.plusDays(1)))
                .extracting(ScheduleBlock::getId).contains(block.getId());
    }
    @Test
    void persistsMultiplePatientsWithoutIdentification() {
        Patient first = patients.saveAndFlush(Patient.builder().firstName("Ana").lastName("Sin DNI")
                .birthDate(LocalDate.of(1990, 1, 1)).gender(Gender.FEMENINO).build());
        Patient second = patients.saveAndFlush(Patient.builder().firstName("Luis").lastName("Sin DNI")
                .birthDate(LocalDate.of(1991, 1, 1)).gender(Gender.MASCULINO).build());
        assertThat(first.getId()).isNotEqualTo(second.getId());
        assertThat(first.getIdentificationNumber()).isNull();
        assertThat(second.getIdentificationNumber()).isNull();
        assertThat(patients.searchActivePatients("Sin DNI")).extracting(Patient::getId)
                .contains(first.getId(), second.getId());
    }

    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;

    @Test
    void databaseRejectsDuplicateEvenWhenInactiveAndInsertedWithoutJpa() {
        Patient original = patients.saveAndFlush(Patient.builder().firstName("  María   José ")
                .lastName("LÓPEZ").birthDate(LocalDate.of(1980, 1, 2))
                .gender(Gender.FEMENINO).active(false).build());
        assertThat(patients.existsByNormalizedFirstNameAndNormalizedLastNameAndBirthDateAndIdNot(
                "maria jose", "lopez", original.getBirthDate(), -1L)).isTrue();
        assertThat(patients.existsByNormalizedFirstNameAndNormalizedLastNameAndBirthDateAndIdNot(
                "maria jose", "lopez", original.getBirthDate(), original.getId())).isFalse();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO patients(first_name,last_name,birth_date,gender) VALUES (?,?,?,?)",
                "maria jose", "lopez", original.getBirthDate(), "FEMENINO"))
                .isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
    }

    @Test
    void allowsSameNameWithDifferentBirthDate() {
        for (int day : new int[]{1, 2}) {
            patients.saveAndFlush(Patient.builder().firstName("Ana").lastName("Pérez")
                    .birthDate(LocalDate.of(1980, 1, day)).gender(Gender.FEMENINO).build());
        }
        assertThat(patients.searchActivePatients("Ana")).hasSize(2);
    }

    @Test
    void databaseRejectsUpdateIntoExistingPatient() {
        Patient first = patients.saveAndFlush(Patient.builder().firstName("Ana").lastName("López")
                .birthDate(LocalDate.of(1980, 1, 2)).gender(Gender.FEMENINO).build());
        Patient second = patients.saveAndFlush(Patient.builder().firstName("Luisa").lastName("López")
                .birthDate(first.getBirthDate()).gender(Gender.FEMENINO).build());
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> jdbc.update(
                "UPDATE patients SET first_name = ? WHERE id = ?", " ANA ", second.getId()))
                .isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
    }

}
