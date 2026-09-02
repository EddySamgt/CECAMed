package com.cecamed.core.repository;

import com.cecamed.core.model.patient.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {

    Optional<MedicalRecord> findByPatientId(Long patientId);

    Optional<MedicalRecord> findByRecordNumber(String recordNumber);

    boolean existsByRecordNumber(String recordNumber);

    boolean existsByRecordNumberAndIdNot(String recordNumber, Long id);
}
