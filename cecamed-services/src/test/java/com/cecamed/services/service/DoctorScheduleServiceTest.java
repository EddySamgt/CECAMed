package com.cecamed.services.service;

import com.cecamed.core.repository.DoctorScheduleRepository;
import com.cecamed.core.repository.ScheduleBlockRepository;
import com.cecamed.services.dto.appointment.DoctorScheduleDto;
import com.cecamed.services.dto.appointment.ScheduleBlockRequestDto;
import com.cecamed.services.exception.BusinessRuleException;
import com.cecamed.services.mapper.AppointmentMapper;
import com.cecamed.services.service.impl.DoctorScheduleServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class DoctorScheduleServiceTest {
    private final DoctorScheduleRepository schedules = mock(DoctorScheduleRepository.class);
    private final ScheduleBlockRepository blocks = mock(ScheduleBlockRepository.class);
    private final DoctorScheduleService service = new DoctorScheduleServiceImpl(schedules, blocks, new AppointmentMapper());

    @Test
    void rejectsEmptyIntervalsAndInvalidSlotDuration() {
        var dto = DoctorScheduleDto.builder().startTime(LocalTime.NOON).endTime(LocalTime.NOON).build();
        assertThatThrownBy(() -> service.saveSchedule(dto)).isInstanceOf(BusinessRuleException.class);
        dto.setEndTime(LocalTime.of(13, 0));
        dto.setSlotDurationMinutes(0);
        assertThatThrownBy(() -> service.saveSchedule(dto)).isInstanceOf(BusinessRuleException.class);
        LocalDateTime time = LocalDateTime.of(2026, 10, 5, 9, 0);
        assertThatThrownBy(() -> service.createScheduleBlock(ScheduleBlockRequestDto.builder()
                .startDateTime(time).endDateTime(time).build())).isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(schedules, blocks);
    }

    @Test
    void requestsOverlappingBlocksIncludingMultiDayBlocks() {
        LocalDateTime start = LocalDateTime.of(2026, 10, 5, 0, 0);
        when(blocks.findOverlappingBlocks(start, start.plusDays(1))).thenReturn(List.of());
        service.getBlocksBetween(start, start.plusDays(1));
        verify(blocks).findOverlappingBlocks(start, start.plusDays(1));
    }
}
