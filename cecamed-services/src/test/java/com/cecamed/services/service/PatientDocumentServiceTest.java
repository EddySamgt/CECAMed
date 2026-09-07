package com.cecamed.services.service;

import com.cecamed.core.model.patient.MedicalConsultation;
import com.cecamed.core.model.patient.Patient;
import com.cecamed.core.model.patient.PatientDocument;
import com.cecamed.core.model.patient.enums.DocumentType;
import com.cecamed.core.repository.MedicalConsultationRepository;
import com.cecamed.core.repository.PatientDocumentRepository;
import com.cecamed.core.repository.PatientRepository;
import com.cecamed.services.dto.document.PatientDocumentResponseDto;
import com.cecamed.services.dto.document.StoredFileMetadata;
import com.cecamed.services.exception.BusinessRuleException;
import com.cecamed.services.exception.ResourceNotFoundException;
import com.cecamed.services.mapper.DocumentMapper;
import com.cecamed.services.service.impl.PatientDocumentServiceImpl;
import com.cecamed.services.storage.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientDocumentServiceTest {

    @Mock
    private PatientRepository patientRepository;
    @Mock
    private MedicalConsultationRepository consultationRepository;
    @Mock
    private PatientDocumentRepository patientDocumentRepository;
    @Mock
    private FileStorageService fileStorageService;

    private PatientDocumentService patientDocumentService;

    @BeforeEach
    void setUp() {
        DocumentMapper documentMapper = new DocumentMapper();
        patientDocumentService = new PatientDocumentServiceImpl(
                patientRepository,
                consultationRepository,
                patientDocumentRepository,
                fileStorageService,
                documentMapper
        );
    }

    @Test
    @DisplayName("Debe subir documento y asociarlo al paciente correctamente")
    void shouldUploadDocumentSuccessfully() {
        Patient patient = Patient.builder().id(1L).firstName("Carlos").lastName("Gomez").build();
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "hemograma.pdf",
                "application/pdf",
                "pdf content".getBytes()
        );

        StoredFileMetadata storedMeta = StoredFileMetadata.builder()
                .savedFileName("uuid-123.pdf")
                .originalFileName("hemograma.pdf")
                .relativePath("patients/1/uuid-123.pdf")
                .fileType("application/pdf")
                .fileSizeBytes(100L)
                .checksumSha256("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
                .build();

        when(fileStorageService.store(eq(file), eq("patients/1"))).thenReturn(storedMeta);

        PatientDocument savedDoc = PatientDocument.builder()
                .id(100L)
                .patient(patient)
                .fileName("uuid-123.pdf")
                .originalFileName("hemograma.pdf")
                .fileType("application/pdf")
                .documentType(DocumentType.ESTUDIO_LABORATORIO)
                .filePath("patients/1/uuid-123.pdf")
                .fileSizeBytes(100L)
                .checksumSha256("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
                .description("Resultados de laboratorio anual")
                .build();

        when(patientDocumentRepository.save(any(PatientDocument.class))).thenReturn(savedDoc);

        PatientDocumentResponseDto response = patientDocumentService.uploadDocument(
                1L,
                null,
                DocumentType.ESTUDIO_LABORATORIO,
                "Resultados de laboratorio anual",
                file
        );

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getPatientId()).isEqualTo(1L);
        assertThat(response.getOriginalFileName()).isEqualTo("hemograma.pdf");
        assertThat(response.getDocumentType()).isEqualTo(DocumentType.ESTUDIO_LABORATORIO);

        ArgumentCaptor<PatientDocument> captor = ArgumentCaptor.forClass(PatientDocument.class);
        verify(patientDocumentRepository).save(captor.capture());
        assertThat(captor.getValue().getFilePath()).isEqualTo("patients/1/uuid-123.pdf");
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException si el paciente no existe al subir documento")
    void shouldThrowIfPatientNotFound() {
        when(patientRepository.findById(99L)).thenReturn(Optional.empty());
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "data".getBytes());

        assertThatThrownBy(() -> patientDocumentService.uploadDocument(
                99L, null, DocumentType.OTRO, "Desc", file
        )).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Debe lanzar BusinessRuleException si la consulta no pertenece al paciente")
    void shouldThrowIfConsultationBelongsToAnotherPatient() {
        Patient patient1 = Patient.builder().id(1L).build();
        Patient patient2 = Patient.builder().id(2L).build();
        MedicalConsultation consultation = MedicalConsultation.builder().id(50L).patient(patient2).build();

        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient1));
        when(consultationRepository.findById(50L)).thenReturn(Optional.of(consultation));

        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "data".getBytes());

        assertThatThrownBy(() -> patientDocumentService.uploadDocument(
                1L, 50L, DocumentType.RECETA_MEDICA, "Receta", file
        )).isInstanceOf(BusinessRuleException.class)
          .hasMessageContaining("no pertenece al paciente");
    }

    @Test
    @DisplayName("Debe cargar recurso físico del documento existente")
    void shouldLoadDocumentFile() {
        PatientDocument doc = PatientDocument.builder()
                .id(10L)
                .filePath("patients/1/doc.pdf")
                .build();
        when(patientDocumentRepository.findById(10L)).thenReturn(Optional.of(doc));
        when(fileStorageService.loadAsResource("patients/1/doc.pdf"))
                .thenReturn(new ByteArrayResource("dummy content".getBytes()));

        Resource resource = patientDocumentService.loadDocumentFile(10L);
        assertThat(resource).isNotNull();
    }

    @Test
    @DisplayName("Debe eliminar tanto el archivo físico como el registro en BD")
    void shouldDeleteDocumentAndPhysicalFile() {
        PatientDocument doc = PatientDocument.builder()
                .id(10L)
                .filePath("patients/1/doc.pdf")
                .build();
        when(patientDocumentRepository.findById(10L)).thenReturn(Optional.of(doc));

        patientDocumentService.deleteDocument(10L);

        verify(fileStorageService).delete("patients/1/doc.pdf");
        verify(patientDocumentRepository).delete(doc);
    }
}
