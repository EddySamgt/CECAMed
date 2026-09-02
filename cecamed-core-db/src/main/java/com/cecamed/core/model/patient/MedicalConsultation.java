package com.cecamed.core.model.patient;

import com.cecamed.core.audit.AuditableEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Registro de una consulta médica / nota de evolución de un paciente.
 */
@Entity
@Table(
    name = "medical_consultations",
    indexes = {
        @Index(name = "idx_consultation_patient_id", columnList = "patient_id"),
        @Index(name = "idx_consultation_date_time", columnList = "consultation_date_time")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"patient", "attachedDocuments"})
public class MedicalConsultation extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "El paciente es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @NotNull(message = "La fecha y hora de la consulta es obligatoria")
    @Column(name = "consultation_date_time", nullable = false)
    private LocalDateTime consultationDateTime;

    @NotBlank(message = "El motivo de la consulta es obligatorio")
    @Size(max = 255, message = "El motivo no puede exceder 255 caracteres")
    @Column(name = "reason", nullable = false, length = 255)
    private String reason;

    @Column(name = "symptoms", columnDefinition = "TEXT")
    private String symptoms;

    @Column(name = "physical_examination", columnDefinition = "TEXT")
    private String physicalExamination;

    @Embedded
    @Builder.Default
    private VitalSigns vitalSigns = new VitalSigns();

    @NotBlank(message = "El diagnóstico es obligatorio")
    @Column(name = "diagnosis", nullable = false, columnDefinition = "TEXT")
    private String diagnosis;

    @Size(max = 20, message = "El código CIE-10 no puede exceder 20 caracteres")
    @Column(name = "icd10_code", length = 20)
    private String icd10Code;

    @Column(name = "treatment_plan", columnDefinition = "TEXT")
    private String treatmentPlan;

    @Column(name = "private_notes", columnDefinition = "TEXT")
    private String privateNotes;

    @Builder.Default
    @OneToMany(mappedBy = "consultation", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @OrderBy("createdAt DESC")
    private List<PatientDocument> attachedDocuments = new ArrayList<>();

    public void addDocument(PatientDocument document) {
        attachedDocuments.add(document);
        document.setConsultation(this);
        if (this.patient != null && document.getPatient() == null) {
            document.setPatient(this.patient);
        }
    }

    public void removeDocument(PatientDocument document) {
        attachedDocuments.remove(document);
        document.setConsultation(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MedicalConsultation that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
