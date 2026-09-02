package com.cecamed.core.repository;

import com.cecamed.core.model.appointment.Appointment;
import com.cecamed.core.model.appointment.enums.AppointmentStatus;
import com.cecamed.core.model.appointment.enums.GoogleSyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findAllByPatientIdOrderByStartTimeDesc(Long patientId);

    List<Appointment> findAllByStartTimeBetweenOrderByStartTimeAsc(LocalDateTime start, LocalDateTime end);

    List<Appointment> findAllByStatusAndStartTimeBetweenOrderByStartTimeAsc(
        AppointmentStatus status,
        LocalDateTime start,
        LocalDateTime end
    );

    Optional<Appointment> findByGoogleEventId(String googleEventId);

    List<Appointment> findAllByGoogleSyncStatus(GoogleSyncStatus googleSyncStatus);

    /**
     * Busca citas que se solapan con el intervalo [start, end) y no están canceladas.
     * Si excludeId no es nulo, excluye dicha cita (para casos de edición/reprogramación).
     */
    @Query("""
        SELECT a FROM Appointment a
        WHERE a.status <> 'CANCELADA'
          AND a.startTime < :endTime
          AND a.endTime > :startTime
          AND (:excludeId IS NULL OR a.id <> :excludeId)
    """)
    List<Appointment> findOverlappingAppointments(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime,
        @Param("excludeId") Long excludeId
    );

    @Query("""
        SELECT COUNT(a) > 0 FROM Appointment a
        WHERE a.status <> 'CANCELADA'
          AND a.startTime < :endTime
          AND a.endTime > :startTime
          AND (:excludeId IS NULL OR a.id <> :excludeId)
    """)
    boolean hasOverlappingAppointment(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime,
        @Param("excludeId") Long excludeId
    );
}
