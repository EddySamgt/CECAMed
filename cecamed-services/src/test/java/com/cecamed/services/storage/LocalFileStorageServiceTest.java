package com.cecamed.services.storage;

import com.cecamed.services.config.StorageProperties;
import com.cecamed.services.exception.FileStorageException;
import com.cecamed.services.exception.InvalidFileTypeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalFileStorageServiceTest {
    @TempDir Path directory;
    private LocalFileStorageService storage;
    private StorageProperties properties;

    @BeforeEach
    void setUp() {
        properties = new StorageProperties();
        properties.setUploadDir(directory.resolve("uploads").toString());
        storage = new LocalFileStorageService(properties);
    }

    @Test
    void storesReadsAndDeletesWithoutOverwritingSameOriginalName() throws Exception {
        byte[] bytes = "%PDF-1.4 test".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        var file = new MockMultipartFile("file", "test.pdf", "application/pdf", bytes);
        var first = storage.store(file, "patients/1");
        var second = storage.store(file, "patients/1");
        assertThat(first.getRelativePath()).isNotEqualTo(second.getRelativePath());
        assertThat(first.getFileSizeBytes()).isEqualTo(bytes.length);
        assertThat(first.getChecksumSha256()).isEqualTo(
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
        assertThat(storage.loadAsResource(first.getRelativePath()).getContentAsByteArray()).isEqualTo(bytes);
        storage.delete(first.getRelativePath());
        assertThatThrownBy(() -> storage.loadAsResource(first.getRelativePath())).isInstanceOf(FileStorageException.class);
        assertThat(storage.loadAsResource(second.getRelativePath()).exists()).isTrue();
    }

    @Test
    void rejectsUnsupportedEmptyAndOversizedFiles() {
        assertThatThrownBy(() -> storage.store(new MockMultipartFile("file", "test.exe",
                "application/octet-stream", new byte[]{1}), "patients/1"))
                .isInstanceOf(InvalidFileTypeException.class);
        assertThatThrownBy(() -> storage.store(new MockMultipartFile("file", new byte[0]), "patients/1"))
                .isInstanceOf(FileStorageException.class);
        properties.setMaxFileSizeBytes(1);
        assertThatThrownBy(() -> storage.store(new MockMultipartFile("file", "test.pdf",
                "application/pdf", new byte[]{1, 2}), "patients/1"))
                .isInstanceOf(FileStorageException.class);
    }

    @Test
    void rejectsTraversalAndSymlinksForReadsWritesAndDeletes() throws Exception {
        Path outside = Files.writeString(directory.resolve("outside.pdf"), "keep");
        assertThatThrownBy(() -> storage.loadAsResource("../outside.pdf")).isInstanceOf(FileStorageException.class);
        assertThatThrownBy(() -> storage.delete(outside.toString())).isInstanceOf(FileStorageException.class);
        Files.createSymbolicLink(directory.resolve("uploads/link"), directory);
        assertThatThrownBy(() -> storage.delete("link/outside.pdf")).isInstanceOf(FileStorageException.class);
        var file = new MockMultipartFile("file", "test.pdf", "application/pdf", new byte[]{1});
        assertThatThrownBy(() -> storage.store(file, "../escape")).isInstanceOf(FileStorageException.class);
        assertThatThrownBy(() -> storage.store(file, "link")).isInstanceOf(FileStorageException.class);
        assertThat(Files.readString(outside)).isEqualTo("keep");
    }
}
