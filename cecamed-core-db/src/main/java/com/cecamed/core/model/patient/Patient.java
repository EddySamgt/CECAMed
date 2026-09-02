package com.cecamed.core.model.patient;

import com.cecamed.core.audit.AuditableEntity;
import com.cecamed.core.model.appointment.Appointment;
import com.cecamed.core.model.patient.enums.BloodType;
import com.cecamed.core.model.patient.enums.Gender;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(
    name = "patients",
    indexes = {
        @Index(name = "idx_patient_dni", columnList = "identification_number", unique = true),
        @Index(name = "idx_patient_names", columnList = "last_name, first_name"),
        @Index(name = "idx_patient_phone", columnList = "phone"),
        @Index(name = "idx_patient_active", columnList = "active")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"medicalRecord", "consultations", "documents", "appointments"})
public class Patient extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 100, message = "El apellido no puede exceder 100 caracteres")
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @NotBlank(message = "La identificación/DNI es obligatoria")
    @Size(max = 50, message = "La identificación no puede exceder 50 caracteres")
    @Column(name = "identification_number", nullable = false, unique = true, length = 50)
    private String identificationNumber;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento debe ser en el pasado")
    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @NotNull(message = "El sexo/género es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 20)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(name = "blood_type", length = 20)
    @Builder.Default
    private BloodType bloodType = BloodType.DESCONOCIDO;

    @Size(max = 25, message = "El teléfono no puede exceder 25 caracteres")
    @Column(name = "phone", length = 25)
    private String phone;

    @Email(message = "El correo electrónico debe ser válido")
    @Size(max = 100, message = "El correo no puede exceder 100 caracteres")
    @Column(name = "email", length = 100)
    private String email;

    @Size(max = 255, message = "La dirección no puede exceder 255 caracteres")
    @Column(name = "address", length = 255)
    private String address;

    // Contacto de Emergencia
    @Size(max = 100, message = "El nombre de contacto de emergencia no puede exceder 100 caracteres")
    @Column(name = "emergency_contact_name", length = 100)
    private String emergencyContactName;

    @Size(max = 25, message = "El teléfono de emergencia no puede exceder 25 caracteres")
    @Column(name = "emergency_contact_phone", length = 25)
    private String emergencyContactPhone;

    @Size(max = 50, message = "El parentesco de emergencia no puede exceder 50 caracteres")
    @Column(name = "emergency_contact_relationship", length = 50)
    private String emergencyContactRelationship;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    // Relaciones
    @OneToOne(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private MedicalRecord medicalRecord;

    @Builder.Default
    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("consultationDateTime DESC")
    private List<MedicalConsultation> consultations = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("createdAt DESC")
    private List<PatientDocument> documents = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("startTime DESC")
    private List<Appointment> appointments = new ArrayList<>();

    // Métodos utilitarios
    public String getFullName() {
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    }

    public Integer getAge() {
        if (birthDate == null) {
            return null;
        }
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    public void addConsultation(MedicalConsultation consultation) {
        consultations.add(consultation);
        consultation.setPatient(this);
    }

    public void removeConsultation(MedicalConsultation consultation) {
        consultations.remove(consultation);
        consultation.setPatient(null);
    }

    public void addDocument(PatientDocument document) {
        documents.add(document);
        document.setPatient(this);
    }

    public void removeDocument(PatientDocument document) {
        documents.remove(document);
        document.setPatient(null);
    }

    public void setMedicalRecord(MedicalRecord record) {
        this.medicalRecord = record;
        if (record != null) {
            record.setPatient(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Patient patient)) return false;
        return id != null && Objects.equals(id, patient.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
