package com.cecamed.core.model.patient;

import com.cecamed.core.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Objects;

/**
 * Expediente clínico base y antecedentes médicos del paciente.
 */
@Entity
@Table(
    name = "medical_records",
    indexes = {
        @Index(name = "idx_record_number", columnList = "record_number", unique = true),
        @Index(name = "idx_record_patient_id", columnList = "patient_id", unique = true)
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "patient")
public class MedicalRecord extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "El paciente asociado es obligatorio")
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false, unique = true)
    private Patient patient;

    @NotBlank(message = "El número de expediente es obligatorio")
    @Size(max = 50, message = "El número de expediente no puede exceder 50 caracteres")
    @Column(name = "record_number", nullable = false, unique = true, length = 50)
    private String recordNumber;

    @Column(name = "allergies", columnDefinition = "TEXT")
    private String allergies; // Alergias conocidas (medicamentos, alimentos, etc.)

    @Column(name = "pathological_history", columnDefinition = "TEXT")
    private String pathologicalHistory; // Antecedentes patológicos (enfermedades crónicas)

    @Column(name = "non_pathological_history", columnDefinition = "TEXT")
    private String nonPathologicalHistory; // No patológicos (hábitos, tabaquismo, alcohol, etc.)

    @Column(name = "family_history", columnDefinition = "TEXT")
    private String familyHistory; // Antecedentes heredofamiliares

    @Column(name = "surgical_history", columnDefinition = "TEXT")
    private String surgicalHistory; // Cirugías y hospitalizaciones previas

    @Column(name = "current_medications", columnDefinition = "TEXT")
    private String currentMedications; // Medicación habitual o de uso continuo

    @Column(name = "general_observations", columnDefinition = "TEXT")
    private String generalObservations;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MedicalRecord that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
