package com.cecamed.services.dto.patient;

import com.cecamed.core.model.patient.enums.BloodType;
import com.cecamed.core.model.patient.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientResponseDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String fullName;
    private String identificationNumber;
    private LocalDate birthDate;
    private Integer age;
    private Gender gender;
    private BloodType bloodType;
    private String phone;
    private String email;
    private String address;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelationship;
    private String notes;
    private Boolean active;
    private String medicalRecordNumber;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
