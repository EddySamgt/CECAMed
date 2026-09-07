package com.cecamed.services.service;

import com.cecamed.services.dto.appointment.DoctorScheduleDto;
import com.cecamed.services.dto.appointment.ScheduleBlockRequestDto;
import com.cecamed.services.dto.appointment.ScheduleBlockResponseDto;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;

public interface DoctorScheduleService {

    DoctorScheduleDto saveSchedule(DoctorScheduleDto dto);

    DoctorScheduleDto getScheduleByDay(DayOfWeek dayOfWeek);

    List<DoctorScheduleDto> getAllActiveSchedules();

    ScheduleBlockResponseDto createScheduleBlock(ScheduleBlockRequestDto dto);

    List<ScheduleBlockResponseDto> getBlocksBetween(LocalDateTime start, LocalDateTime end);

    void deleteScheduleBlock(Long blockId);
}
