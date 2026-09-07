package com.cecamed.services.service.impl;

import com.cecamed.core.model.appointment.DoctorSchedule;
import com.cecamed.core.model.appointment.ScheduleBlock;
import com.cecamed.core.repository.DoctorScheduleRepository;
import com.cecamed.core.repository.ScheduleBlockRepository;
import com.cecamed.services.dto.appointment.DoctorScheduleDto;
import com.cecamed.services.dto.appointment.ScheduleBlockRequestDto;
import com.cecamed.services.dto.appointment.ScheduleBlockResponseDto;
import com.cecamed.services.exception.BusinessRuleException;
import com.cecamed.services.exception.ResourceNotFoundException;
import com.cecamed.services.mapper.AppointmentMapper;
import com.cecamed.services.service.DoctorScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DoctorScheduleServiceImpl implements DoctorScheduleService {

    private final DoctorScheduleRepository doctorScheduleRepository;
    private final ScheduleBlockRepository scheduleBlockRepository;
    private final AppointmentMapper appointmentMapper;

    @Override
    @Transactional
    public DoctorScheduleDto saveSchedule(DoctorScheduleDto dto) {
        log.info("Guardando horario de atención para el día: {}", dto.getDayOfWeek());

        if (dto.getStartTime().isAfter(dto.getEndTime())) {
            throw new BusinessRuleException("La hora de inicio no puede ser posterior a la hora de fin");
        }

        DoctorSchedule schedule = doctorScheduleRepository.findByDayOfWeekAndIsActiveTrue(dto.getDayOfWeek())
                .orElseGet(() -> appointmentMapper.toEntity(dto));

        schedule.setStartTime(dto.getStartTime());
        schedule.setEndTime(dto.getEndTime());
        schedule.setSlotDurationMinutes(dto.getSlotDurationMinutes());
        schedule.setIsActive(dto.getIsActive());

        DoctorSchedule saved = doctorScheduleRepository.save(schedule);
        return appointmentMapper.toDto(saved);
    }

    @Override
    public DoctorScheduleDto getScheduleByDay(DayOfWeek dayOfWeek) {
        return doctorScheduleRepository.findByDayOfWeekAndIsActiveTrue(dayOfWeek)
                .map(appointmentMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("No existe horario configurado para: " + dayOfWeek));
    }

    @Override
    public List<DoctorScheduleDto> getAllActiveSchedules() {
        return doctorScheduleRepository.findAllByIsActiveTrueOrderByDayOfWeekAscStartTimeAsc().stream()
                .map(appointmentMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public ScheduleBlockResponseDto createScheduleBlock(ScheduleBlockRequestDto dto) {
        log.info("Creando bloqueo de agenda desde {} hasta {}", dto.getStartDateTime(), dto.getEndDateTime());

        if (dto.getStartDateTime().isAfter(dto.getEndDateTime())) {
            throw new BusinessRuleException("La fecha de inicio del bloqueo no puede ser posterior a la fecha de fin");
        }

        ScheduleBlock block = appointmentMapper.toEntity(dto);
        ScheduleBlock saved = scheduleBlockRepository.save(block);
        log.info("Bloqueo de agenda creado con ID: {}", saved.getId());

        return appointmentMapper.toResponseDto(saved);
    }

    @Override
    public List<ScheduleBlockResponseDto> getBlocksBetween(LocalDateTime start, LocalDateTime end) {
        return scheduleBlockRepository.findAllByStartDateTimeGreaterThanEqualAndEndDateTimeLessThanEqualOrderByStartDateTimeAsc(start, end).stream()
                .map(appointmentMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional
    public void deleteScheduleBlock(Long blockId) {
        if (!scheduleBlockRepository.existsById(blockId)) {
            throw new ResourceNotFoundException("Bloqueo de agenda", "id", blockId);
        }
        scheduleBlockRepository.deleteById(blockId);
        log.info("Bloqueo de agenda ID {} eliminado", blockId);
    }
}
