package com.cecamed.core.model.patient;

import com.cecamed.core.audit.AuditableEntity;
import com.cecamed.core.model.patient.enums.DocumentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
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
 * Representa un archivo adjunto (PDF, PNG, JPG, JPEG) asociado al expediente del paciente
 * y opcionalmente vinculado a una consulta específica.
 */
@Entity
@Table(
    name = "patient_documents",
    indexes = {
        @Index(name = "idx_doc_patient_id", columnList = "patient_id"),
        @Index(name = "idx_doc_consultation_id", columnList = "consultation_id"),
        @Index(name = "idx_doc_type", columnList = "document_type")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"patient", "consultation"})
public class PatientDocument extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "El paciente es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consultation_id")
    private MedicalConsultation consultation;

    @NotBlank(message = "El nombre físico del archivo es obligatorio")
    @Size(max = 255)
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @NotBlank(message = "El nombre original del archivo es obligatorio")
    @Size(max = 255)
    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @NotBlank(message = "El tipo MIME del archivo es obligatorio")
    @Size(max = 100)
    @Column(name = "file_type", nullable = false, length = 100)
    private String fileType; // ej. application/pdf, image/jpeg, image/png

    @NotNull(message = "La categoría del documento es obligatoria")
    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 50)
    private DocumentType documentType;

    @NotBlank(message = "La ruta relativa del archivo es obligatoria")
    @Size(max = 500)
    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath; // Ruta relativa en el sistema de archivos

    @NotNull(message = "El tamaño del archivo es obligatorio")
    @Min(value = 1, message = "El tamaño del archivo debe ser mayor a 0 bytes")
    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;

    @Size(max = 64)
    @Column(name = "checksum_sha256", length = 64)
    private String checksumSha256;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PatientDocument that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
