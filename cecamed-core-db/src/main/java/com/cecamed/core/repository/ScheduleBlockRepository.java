package com.cecamed.core.repository;

import com.cecamed.core.model.appointment.ScheduleBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScheduleBlockRepository extends JpaRepository<ScheduleBlock, Long> {

    List<ScheduleBlock> findAllByStartDateTimeGreaterThanEqualAndEndDateTimeLessThanEqualOrderByStartDateTimeAsc(
        LocalDateTime start,
        LocalDateTime end
    );

    /**
     * Busca bloqueos de agenda que se solapen con el intervalo [startTime, endTime).
     */
    @Query("""
        SELECT b FROM ScheduleBlock b
        WHERE b.startDateTime < :endTime
          AND b.endDateTime > :startTime
    """)
    List<ScheduleBlock> findOverlappingBlocks(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    @Query("""
        SELECT COUNT(b) > 0 FROM ScheduleBlock b
        WHERE b.startDateTime < :endTime
          AND b.endDateTime > :startTime
    """)
    boolean isTimeRangeBlocked(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );
}
