package com.cecamed.services.service;

import com.cecamed.core.model.appointment.Appointment;
import com.cecamed.core.model.appointment.DoctorSchedule;
import com.cecamed.core.model.appointment.enums.AppointmentStatus;
import com.cecamed.core.model.patient.Patient;
import com.cecamed.core.repository.AppointmentRepository;
import com.cecamed.core.repository.DoctorScheduleRepository;
import com.cecamed.core.repository.PatientRepository;
import com.cecamed.core.repository.ScheduleBlockRepository;
import com.cecamed.services.dto.appointment.AppointmentRequestDto;
import com.cecamed.services.dto.appointment.AppointmentResponseDto;
import com.cecamed.services.dto.appointment.AvailableSlotDto;
import com.cecamed.services.exception.BusinessRuleException;
import com.cecamed.services.mapper.AppointmentMapper;
import com.cecamed.services.service.impl.AppointmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private DoctorScheduleRepository doctorScheduleRepository;
    @Mock
    private ScheduleBlockRepository scheduleBlockRepository;
    @Mock
    private PatientRepository patientRepository;

    private AppointmentService appointmentService;

    @BeforeEach
    void setUp() {
        AppointmentMapper appointmentMapper = new AppointmentMapper();
        appointmentService = new AppointmentServiceImpl(
                appointmentRepository,
                doctorScheduleRepository,
                scheduleBlockRepository,
                patientRepository,
                appointmentMapper
        );
    }

    @Test
    @DisplayName("Debe agendar cita médica exitosamente sin conflictos")
    void shouldCreateAppointmentSuccessfully() {
        LocalDateTime start = LocalDateTime.of(2026, 10, 12, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 10, 12, 9, 30);

        Patient patient = Patient.builder().id(1L).firstName("Ana").lastName("Suarez").active(true).build();
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(scheduleBlockRepository.isTimeRangeBlocked(start, end)).thenReturn(false);
        when(appointmentRepository.hasOverlappingAppointment(start, end, null)).thenReturn(false);

        Appointment savedAppointment = Appointment.builder()
                .id(50L)
                .patient(patient)
                .startTime(start)
                .endTime(end)
                .status(AppointmentStatus.PROGRAMADA)
                .reasonForVisit("Control general")
                .build();

        when(appointmentRepository.save(any(Appointment.class))).thenReturn(savedAppointment);

        AppointmentRequestDto request = AppointmentRequestDto.builder()
                .patientId(1L)
                .startTime(start)
                .endTime(end)
                .reasonForVisit("Control general")
                .build();

        AppointmentResponseDto response = appointmentService.createAppointment(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(50L);
        assertThat(response.getStatus()).isEqualTo(AppointmentStatus.PROGRAMADA);
    }

    @Test
    @DisplayName("Debe rechazar cita médica si la hora inicio es posterior a la hora fin")
    void shouldRejectInvalidTimeRange() {
        LocalDateTime start = LocalDateTime.of(2026, 10, 12, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 10, 12, 9, 30);

        AppointmentRequestDto request = AppointmentRequestDto.builder()
                .patientId(1L)
                .startTime(start)
                .endTime(end)
                .reasonForVisit("Control")
                .build();

        assertThatThrownBy(() -> appointmentService.createAppointment(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("estrictamente anterior");
    }

    @Test
    @DisplayName("Debe rechazar cita si se solapa con un bloqueo de agenda")
    void shouldRejectWhenBlocked() {
        LocalDateTime start = LocalDateTime.of(2026, 10, 12, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 10, 12, 9, 30);

        Patient patient = Patient.builder().id(1L).active(true).build();
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(scheduleBlockRepository.isTimeRangeBlocked(start, end)).thenReturn(true);

        AppointmentRequestDto request = AppointmentRequestDto.builder()
                .patientId(1L)
                .startTime(start)
                .endTime(end)
                .reasonForVisit("Control")
                .build();

        assertThatThrownBy(() -> appointmentService.createAppointment(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("bloqueo de agenda");
    }

    @Test
    @DisplayName("Debe calcular correctamente turnos disponibles y ocupados")
    void shouldCalculateAvailableSlots() {
        LocalDate monday = LocalDate.of(2026, 10, 12); // Lunes
        DoctorSchedule schedule = DoctorSchedule.builder()
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .slotDurationMinutes(30)
                .isActive(true)
                .build();

        when(doctorScheduleRepository.findByDayOfWeekAndIsActiveTrue(DayOfWeek.MONDAY))
                .thenReturn(Optional.of(schedule));

        // 9:00 - 9:30 ocupado por cita
        LocalDateTime slot1Start = LocalDateTime.of(monday, LocalTime.of(9, 0));
        LocalDateTime slot1End = LocalDateTime.of(monday, LocalTime.of(9, 30));
        when(scheduleBlockRepository.isTimeRangeBlocked(slot1Start, slot1End)).thenReturn(false);
        when(appointmentRepository.hasOverlappingAppointment(slot1Start, slot1End, null)).thenReturn(true);

        // 9:30 - 10:00 libre
        LocalDateTime slot2Start = LocalDateTime.of(monday, LocalTime.of(9, 30));
        LocalDateTime slot2End = LocalDateTime.of(monday, LocalTime.of(10, 0));
        when(scheduleBlockRepository.isTimeRangeBlocked(slot2Start, slot2End)).thenReturn(false);
        when(appointmentRepository.hasOverlappingAppointment(slot2Start, slot2End, null)).thenReturn(false);

        List<AvailableSlotDto> slots = appointmentService.getAvailableSlotsForDate(monday);

        assertThat(slots).hasSize(2);
        assertThat(slots.get(0).isAvailable()).isFalse();
        assertThat(slots.get(0).getReasonIfNotAvailable()).contains("otra cita");
        assertThat(slots.get(1).isAvailable()).isTrue();
    }
}
