package com.cecamed.services.service.impl;

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
import com.cecamed.services.service.PatientDocumentService;
import com.cecamed.services.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PatientDocumentServiceImpl implements PatientDocumentService {

    private final PatientRepository patientRepository;
    private final MedicalConsultationRepository consultationRepository;
    private final PatientDocumentRepository patientDocumentRepository;
    private final FileStorageService fileStorageService;
    private final DocumentMapper documentMapper;

    @Override
    @Transactional
    public PatientDocumentResponseDto uploadDocument(
            Long patientId,
            Long consultationId,
            DocumentType documentType,
            String description,
            MultipartFile file
    ) {
        log.info("Iniciando subida de archivo adjunto para el paciente ID: {}, consulta ID: {}, tipo: {}",
                patientId, consultationId, documentType);

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", "id", patientId));

        MedicalConsultation consultation = null;
        if (consultationId != null) {
            consultation = consultationRepository.findById(consultationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Consulta médica", "id", consultationId));

            if (!Objects.equals(consultation.getPatient().getId(), patientId)) {
                throw new BusinessRuleException("La consulta con ID " + consultationId + " no pertenece al paciente indicado");
            }
        }

        // Subcarpeta organizada por paciente: patients/{patientId}
        String subDirectory = "patients/" + patientId;
        StoredFileMetadata storedFile = fileStorageService.store(file, subDirectory);

        PatientDocument document = PatientDocument.builder()
                .patient(patient)
                .consultation(consultation)
                .fileName(storedFile.getSavedFileName())
                .originalFileName(storedFile.getOriginalFileName())
                .fileType(storedFile.getFileType())
                .documentType(documentType)
                .filePath(storedFile.getRelativePath())
                .fileSizeBytes(storedFile.getFileSizeBytes())
                .checksumSha256(storedFile.getChecksumSha256())
                .description(description)
                .build();

        PatientDocument saved = patientDocumentRepository.save(document);
        log.info("Documento registrado exitosamente en BD con ID: {}, ruta: {}", saved.getId(), saved.getFilePath());

        return documentMapper.toResponseDto(saved);
    }

    @Override
    public PatientDocumentResponseDto getDocumentMetadata(Long documentId) {
        return patientDocumentRepository.findById(documentId)
                .map(documentMapper::toResponseDto)
                .orElseThrow(() -> new ResourceNotFoundException("Documento", "id", documentId));
    }

    @Override
    public Resource loadDocumentFile(Long documentId) {
        PatientDocument document = patientDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento", "id", documentId));

        return fileStorageService.loadAsResource(document.getFilePath());
    }

    @Override
    public List<PatientDocumentResponseDto> getDocumentsByPatient(Long patientId) {
        if (!patientRepository.existsById(patientId)) {
            throw new ResourceNotFoundException("Paciente", "id", patientId);
        }
        return documentMapper.toResponseDtoList(
                patientDocumentRepository.findAllByPatientIdOrderByCreatedAtDesc(patientId)
        );
    }

    @Override
    public List<PatientDocumentResponseDto> getDocumentsByConsultation(Long consultationId) {
        if (!consultationRepository.existsById(consultationId)) {
            throw new ResourceNotFoundException("Consulta médica", "id", consultationId);
        }
        return documentMapper.toResponseDtoList(
                patientDocumentRepository.findAllByConsultationIdOrderByCreatedAtDesc(consultationId)
        );
    }

    @Override
    public List<PatientDocumentResponseDto> getDocumentsByPatientAndType(Long patientId, DocumentType documentType) {
        if (!patientRepository.existsById(patientId)) {
            throw new ResourceNotFoundException("Paciente", "id", patientId);
        }
        return documentMapper.toResponseDtoList(
                patientDocumentRepository.findAllByPatientIdAndDocumentTypeOrderByCreatedAtDesc(patientId, documentType)
        );
    }

    @Override
    @Transactional
    public void deleteDocument(Long documentId) {
        log.info("Eliminando documento con ID: {}", documentId);

        PatientDocument document = patientDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento", "id", documentId));

        // Eliminar del almacenamiento físico
        fileStorageService.delete(document.getFilePath());

        // Eliminar de la base de datos
        patientDocumentRepository.delete(document);
        log.info("Documento con ID {} eliminado satisfactoriamente de BD y almacenamiento físico", documentId);
    }
}
