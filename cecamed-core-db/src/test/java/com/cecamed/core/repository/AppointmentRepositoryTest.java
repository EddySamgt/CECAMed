package com.cecamed.core.repository;

import com.cecamed.core.model.appointment.Appointment;
import com.cecamed.core.model.appointment.enums.AppointmentStatus;
import com.cecamed.core.model.appointment.enums.GoogleSyncStatus;
import com.cecamed.core.model.patient.Patient;
import com.cecamed.core.model.patient.enums.Gender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("AppointmentRepository - Pruebas de detección de solapamientos y sincronización")
class AppointmentRepositoryTest {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PatientRepository patientRepository;

    private Patient samplePatient;

    @BeforeEach
    void setUp() {
        samplePatient = patientRepository.save(
                Patient.builder()
                        .firstName("Laura")
                        .lastName("Martínez")
                        .identificationNumber("0101-1992-54321")
                        .birthDate(LocalDate.of(1992, 3, 10))
                        .gender(Gender.FEMENINO)
                        .build()
        );
    }

    @Test
    @DisplayName("Debe detectar citas solapadas en el mismo horario")
    void shouldDetectOverlappingAppointments() {
        LocalDateTime start = LocalDateTime.of(2026, 9, 10, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 9, 10, 9, 30);

        Appointment app1 = Appointment.builder()
                .patient(samplePatient)
                .startTime(start)
                .endTime(end)
                .reasonForVisit("Consulta general")
                .status(AppointmentStatus.PROGRAMADA)
                .build();
        appointmentRepository.save(app1);

        // Caso 1: Solapamiento exacto (09:00 - 09:30)
        boolean overlapExact = appointmentRepository.hasOverlappingAppointment(
                LocalDateTime.of(2026, 9, 10, 9, 0),
                LocalDateTime.of(2026, 9, 10, 9, 30),
                null
        );
        assertThat(overlapExact).isTrue();

        // Caso 2: Solapamiento parcial dentro (09:15 - 09:45)
        boolean overlapPartial = appointmentRepository.hasOverlappingAppointment(
                LocalDateTime.of(2026, 9, 10, 9, 15),
                LocalDateTime.of(2026, 9, 10, 9, 45),
                null
        );
        assertThat(overlapPartial).isTrue();

        // Caso 3: Horario adyacente posterior (09:30 - 10:00) -> NO debe solaparse
        boolean noOverlapAdjacent = appointmentRepository.hasOverlappingAppointment(
                LocalDateTime.of(2026, 9, 10, 9, 30),
                LocalDateTime.of(2026, 9, 10, 10, 0),
                null
        );
        assertThat(noOverlapAdjacent).isFalse();

        // Caso 4: Excluyendo el ID de la misma cita (para casos de edición)
        boolean excludeSelf = appointmentRepository.hasOverlappingAppointment(
                start, end, app1.getId()
        );
        assertThat(excludeSelf).isFalse();
    }

    @Test
    @DisplayName("Debe encontrar citas pendientes de sincronización con Google Calendar")
    void shouldFindAppointmentsPendingGoogleSync() {
        Appointment app = Appointment.builder()
                .patient(samplePatient)
                .startTime(LocalDateTime.of(2026, 9, 11, 10, 0))
                .endTime(LocalDateTime.of(2026, 9, 11, 10, 30))
                .reasonForVisit("Revisión de exámenes")
                .status(AppointmentStatus.PROGRAMADA)
                .googleSyncStatus(GoogleSyncStatus.PENDING)
                .build();
        appointmentRepository.save(app);

        List<Appointment> pending = appointmentRepository.findAllByGoogleSyncStatus(GoogleSyncStatus.PENDING);
        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).getReasonForVisit()).isEqualTo("Revisión de exámenes");
    }
}
