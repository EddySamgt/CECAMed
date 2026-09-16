package com.cecamed.services.storage;

import com.cecamed.services.config.StorageProperties;
import com.cecamed.services.dto.document.StoredFileMetadata;
import com.cecamed.services.exception.FileStorageException;
import com.cecamed.services.exception.InvalidFileTypeException;
import com.cecamed.services.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileSystemStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileSystemStorageService storageService;
    private StorageProperties properties;

    @BeforeEach
    void setUp() {
        properties = new StorageProperties();
        properties.setUploadDir(tempDir.toString());
        storageService = new FileSystemStorageService(properties);
        storageService.init();
    }

    @Test
    @DisplayName("Debe almacenar un archivo PDF exitosamente y calcular SHA-256")
    void shouldStorePdfSuccessfully() {
        byte[] content = "%PDF-1.4 test pdf content".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "laboratorio_sangre.pdf",
                "application/pdf",
                content
        );

        StoredFileMetadata metadata = storageService.store(file, "patients/10");

        assertThat(metadata).isNotNull();
        assertThat(metadata.getOriginalFileName()).isEqualTo("laboratorio_sangre.pdf");
        assertThat(metadata.getSavedFileName()).endsWith(".pdf");
        assertThat(metadata.getFileType()).isEqualTo("application/pdf");
        assertThat(metadata.getChecksumSha256()).isNotBlank().hasSize(64);
        assertThat(metadata.getRelativePath()).startsWith("patients/10/");

        Resource resource = storageService.loadAsResource(metadata.getRelativePath());
        assertThat(resource.exists()).isTrue();
        assertThat(resource.isReadable()).isTrue();
    }

    @Test
    @DisplayName("Debe almacenar una imagen PNG exitosamente")
    void shouldStorePngImageSuccessfully() {
        byte[] content = new byte[]{1, 2, 3, 4, 5};
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "radiografia_torax.png",
                "image/png",
                content
        );

        StoredFileMetadata metadata = storageService.store(file, "patients/5/consultations/12");

        assertThat(metadata).isNotNull();
        assertThat(metadata.getOriginalFileName()).isEqualTo("radiografia_torax.png");
        assertThat(metadata.getSavedFileName()).endsWith(".png");
        assertThat(metadata.getFileType()).isEqualTo("image/png");
        assertThat(metadata.getFileSizeBytes()).isEqualTo(5L);

        Resource resource = storageService.loadAsResource(metadata.getRelativePath());
        assertThat(resource.exists()).isTrue();
    }

    @Test
    @DisplayName("Debe almacenar una imagen JPG exitosamente")
    void shouldStoreJpgImageSuccessfully() {
        byte[] content = new byte[]{10, 20, 30};
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "lesion_cutanea.jpg",
                "image/jpeg",
                content
        );

        StoredFileMetadata metadata = storageService.store(file, "patients/3");

        assertThat(metadata).isNotNull();
        assertThat(metadata.getOriginalFileName()).isEqualTo("lesion_cutanea.jpg");
        assertThat(metadata.getSavedFileName()).endsWith(".jpg");
        assertThat(metadata.getFileType()).isEqualTo("image/jpeg");
    }

    @Test
    @DisplayName("Debe rechazar archivos ejecutables o extensiones no permitidas (.exe, .sh)")
    void shouldRejectUnsupportedFileExtension() {
        MockMultipartFile executable = new MockMultipartFile(
                "file",
                "malware.exe",
                "application/octet-stream",
                "echo bad".getBytes()
        );

        assertThatThrownBy(() -> storageService.store(executable, "patients/1"))
                .isInstanceOf(InvalidFileTypeException.class)
                .hasMessageContaining(".exe");
    }

    @Test
    @DisplayName("Debe rechazar archivos vacíos")
    void shouldRejectEmptyFile() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "vacio.pdf",
                "application/pdf",
                new byte[0]
        );

        assertThatThrownBy(() -> storageService.store(emptyFile, "patients/1"))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("vacío");
    }

    @Test
    @DisplayName("Debe prevenir ataques de Directory Traversal en el nombre o subdirectorio")
    void shouldPreventDirectoryTraversal() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "../../etc/passwd.pdf",
                "application/pdf",
                "dummy content".getBytes()
        );

        assertThatThrownBy(() -> storageService.store(file, "patients/1"))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("Directory Traversal");
    }

    @Test
    @DisplayName("Debe eliminar un archivo físico del disco")
    void shouldDeleteFileSuccessfully() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "eliminar.pdf",
                "application/pdf",
                "contenido".getBytes()
        );

        StoredFileMetadata metadata = storageService.store(file, "patients/1");
        Path filePath = tempDir.resolve(metadata.getRelativePath());
        assertThat(Files.exists(filePath)).isTrue();

        boolean deleted = storageService.delete(metadata.getRelativePath());
        assertThat(deleted).isTrue();
        assertThat(Files.exists(filePath)).isFalse();
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException si el archivo no existe al cargar")
    void shouldThrowWhenLoadingNonExistentFile() {
        assertThatThrownBy(() -> storageService.loadAsResource("patients/999/inexistente.pdf"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
