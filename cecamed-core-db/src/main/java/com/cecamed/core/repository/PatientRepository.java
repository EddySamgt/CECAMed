package com.cecamed.core.repository;

import com.cecamed.core.model.patient.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long>, JpaSpecificationExecutor<Patient> {

    Optional<Patient> findByIdentificationNumber(String identificationNumber);

    boolean existsByIdentificationNumber(String identificationNumber);

    boolean existsByIdentificationNumberAndIdNot(String identificationNumber, Long id);

    List<Patient> findAllByActiveTrueOrderByLastNameAscFirstNameAsc();

    @Query("""
        SELECT p FROM Patient p
        WHERE p.active = true
          AND (
            LOWER(p.firstName) LIKE LOWER(CONCAT('%', :term, '%'))
            OR LOWER(p.lastName) LIKE LOWER(CONCAT('%', :term, '%'))
            OR LOWER(p.identificationNumber) LIKE LOWER(CONCAT('%', :term, '%'))
            OR LOWER(p.phone) LIKE LOWER(CONCAT('%', :term, '%'))
          )
        ORDER BY p.lastName ASC, p.firstName ASC
    """)
    List<Patient> searchActivePatients(@Param("term") String term);

    @Query("""
        SELECT p FROM Patient p
        WHERE (
            LOWER(p.firstName) LIKE LOWER(CONCAT('%', :term, '%'))
            OR LOWER(p.lastName) LIKE LOWER(CONCAT('%', :term, '%'))
            OR LOWER(p.identificationNumber) LIKE LOWER(CONCAT('%', :term, '%'))
            OR LOWER(p.phone) LIKE LOWER(CONCAT('%', :term, '%'))
          )
    """)
    Page<Patient> searchPatientsPaged(@Param("term") String term, Pageable pageable);
}
