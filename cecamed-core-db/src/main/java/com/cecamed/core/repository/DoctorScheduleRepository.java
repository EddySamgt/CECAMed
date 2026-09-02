package com.cecamed.core.repository;

import com.cecamed.core.model.appointment.DoctorSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorScheduleRepository extends JpaRepository<DoctorSchedule, Long> {

    Optional<DoctorSchedule> findByDayOfWeekAndIsActiveTrue(DayOfWeek dayOfWeek);

    List<DoctorSchedule> findAllByIsActiveTrueOrderByDayOfWeekAscStartTimeAsc();
}
