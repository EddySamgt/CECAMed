package com.cecamed.core.repository;

import com.cecamed.core.model.patient.MedicalConsultation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MedicalConsultationRepository extends JpaRepository<MedicalConsultation, Long> {

    List<MedicalConsultation> findAllByPatientIdOrderByConsultationDateTimeDesc(Long patientId);

    @Query("""
        SELECT c FROM MedicalConsultation c
        WHERE c.consultationDateTime BETWEEN :startDate AND :endDate
        ORDER BY c.consultationDateTime DESC
    """)
    List<MedicalConsultation> findAllInDateRange(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    long countByPatientId(Long patientId);
}
