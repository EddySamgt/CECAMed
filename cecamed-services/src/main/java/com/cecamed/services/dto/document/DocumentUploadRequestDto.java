package com.cecamed.services.dto.document;

import com.cecamed.core.model.patient.enums.DocumentType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentUploadRequestDto {

    @NotNull(message = "El ID del paciente es obligatorio")
    private Long patientId;

    private Long consultationId;

    @NotNull(message = "El tipo de documento es obligatorio")
    private DocumentType documentType;

    private String description;
}
