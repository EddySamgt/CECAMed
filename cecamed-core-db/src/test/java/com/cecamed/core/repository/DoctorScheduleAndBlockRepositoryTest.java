package com.cecamed.core.repository;

import com.cecamed.core.model.appointment.DoctorSchedule;
import com.cecamed.core.model.appointment.ScheduleBlock;
import com.cecamed.core.model.appointment.enums.BlockType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("DoctorSchedule & ScheduleBlock Repositories - Pruebas de jornadas y bloqueos")
class DoctorScheduleAndBlockRepositoryTest {

    @Autowired
    private DoctorScheduleRepository doctorScheduleRepository;

    @Autowired
    private ScheduleBlockRepository scheduleBlockRepository;

    @Test
    @DisplayName("Debe consultar horario médico activo por día de la semana")
    void shouldFindScheduleByDayOfWeek() {
        DoctorSchedule mondaySchedule = DoctorSchedule.builder()
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(17, 0))
                .slotDurationMinutes(30)
                .isActive(true)
                .build();
        doctorScheduleRepository.save(mondaySchedule);

        Optional<DoctorSchedule> found = doctorScheduleRepository.findByDayOfWeekAndIsActiveTrue(DayOfWeek.MONDAY);
        assertThat(found).isPresent();
        assertThat(found.get().getSlotDurationMinutes()).isEqualTo(30);

        Optional<DoctorSchedule> sunday = doctorScheduleRepository.findByDayOfWeekAndIsActiveTrue(DayOfWeek.SUNDAY);
        assertThat(sunday).isEmpty();
    }

    @Test
    @DisplayName("Debe verificar si un rango de tiempo está bloqueado por un ScheduleBlock")
    void shouldDetectBlockedTimeRange() {
        LocalDateTime blockStart = LocalDateTime.of(2026, 9, 15, 8, 0);
        LocalDateTime blockEnd = LocalDateTime.of(2026, 9, 15, 18, 0);

        ScheduleBlock holiday = ScheduleBlock.builder()
                .title("Feriado Nacional - Día de la Independencia")
                .startDateTime(blockStart)
                .endDateTime(blockEnd)
                .blockType(BlockType.FERIADO)
                .allDay(true)
                .build();
        scheduleBlockRepository.save(holiday);

        boolean blockedDuringDay = scheduleBlockRepository.isTimeRangeBlocked(
                LocalDateTime.of(2026, 9, 15, 10, 0),
                LocalDateTime.of(2026, 9, 15, 10, 30)
        );
        assertThat(blockedDuringDay).isTrue();

        boolean unblockedNextDay = scheduleBlockRepository.isTimeRangeBlocked(
                LocalDateTime.of(2026, 9, 16, 10, 0),
                LocalDateTime.of(2026, 9, 16, 10, 30)
        );
        assertThat(unblockedNextDay).isFalse();
    }
}
