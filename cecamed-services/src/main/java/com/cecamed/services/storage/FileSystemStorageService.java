package com.cecamed.services.storage;

import com.cecamed.services.config.StorageProperties;
import com.cecamed.services.dto.document.StoredFileMetadata;
import com.cecamed.services.exception.FileStorageException;
import com.cecamed.services.exception.InvalidFileTypeException;
import com.cecamed.services.exception.ResourceNotFoundException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
public class FileSystemStorageService implements FileStorageService {

    private final Path rootLocation;
    private final StorageProperties properties;

    public FileSystemStorageService(StorageProperties properties) {
        this.properties = properties;
        this.rootLocation = Paths.get(properties.getUploadDir()).toAbsolutePath().normalize();
    }

    @Override
    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(rootLocation);
            log.info("Directorio de almacenamiento inicializado en: {}", rootLocation);
        } catch (IOException e) {
            throw new FileStorageException("No se pudo inicializar el directorio raíz de almacenamiento", e);
        }
    }

    @Override
    public StoredFileMetadata store(MultipartFile file, String subDirectory) {
        if (file == null || file.isEmpty()) {
            throw new FileStorageException("No se puede almacenar un archivo vacío");
        }

        if (file.getSize() > properties.getMaxFileSizeBytes()) {
            throw new FileStorageException(String.format(
                "El archivo excede el tamaño máximo permitido de %d MB",
                properties.getMaxFileSizeBytes() / (1024 * 1024)
            ));
        }

        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        try {
            return store(file.getInputStream(), originalFilename, file.getContentType(), file.getSize(), subDirectory);
        } catch (IOException e) {
            throw new FileStorageException("Error al leer los datos del archivo subido: " + originalFilename, e);
        }
    }

    @Override
    public StoredFileMetadata store(InputStream inputStream, String originalFilename, String contentType, long size, String subDirectory) {
        String cleanedFilename = StringUtils.cleanPath(originalFilename);

        if (cleanedFilename.contains("..")) {
            throw new FileStorageException("Nombre de archivo inválido por posible Directory Traversal: " + cleanedFilename);
        }

        String extension = getFileExtension(cleanedFilename).toLowerCase();
        validateFileType(extension, contentType);

        String savedFileName = UUID.randomUUID() + extension;

        Path targetDir = rootLocation;
        if (subDirectory != null && !subDirectory.trim().isEmpty()) {
            String sanitizedSubDir = subDirectory.replace("\\", "/").replaceAll("^/+", "");
            targetDir = rootLocation.resolve(sanitizedSubDir).normalize();
            if (!targetDir.startsWith(rootLocation)) {
                throw new FileStorageException("Intento de navegación de directorios no permitido: " + subDirectory);
            }
        }

        try {
            Files.createDirectories(targetDir);
            Path destinationFile = targetDir.resolve(savedFileName).normalize();

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            try (DigestInputStream dis = new DigestInputStream(inputStream, md)) {
                Files.copy(dis, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }

            String checksum = HexFormat.of().formatHex(md.digest());

            // Ruta relativa normalizada con '/' para portabilidad entre SO
            String relativePath = rootLocation.relativize(destinationFile).toString().replace("\\", "/");

            log.info("Archivo almacenado exitosamente: {} (Tamaño: {} bytes, SHA-256: {})",
                    relativePath, size, checksum);

            return StoredFileMetadata.builder()
                    .savedFileName(savedFileName)
                    .originalFileName(cleanedFilename)
                    .relativePath(relativePath)
                    .fileType(determineMimeType(extension, contentType))
                    .fileSizeBytes(Files.size(destinationFile))
                    .checksumSha256(checksum)
                    .build();

        } catch (NoSuchAlgorithmException e) {
            throw new FileStorageException("Algoritmo de checksum SHA-256 no disponible", e);
        } catch (IOException e) {
            throw new FileStorageException("Fallo al escribir el archivo en el sistema de almacenamiento", e);
        }
    }

    @Override
    public Resource loadAsResource(String relativePath) {
        try {
            Path file = rootLocation.resolve(relativePath).normalize();
            if (!file.startsWith(rootLocation)) {
                throw new FileStorageException("Acceso no autorizado al archivo fuera del directorio raíz: " + relativePath);
            }
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("No se encontró o no se pudo leer el archivo: " + relativePath);
            }
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("Ruta de archivo malformada: " + relativePath);
        }
    }

    @Override
    public boolean delete(String relativePath) {
        try {
            Path file = rootLocation.resolve(relativePath).normalize();
            if (!file.startsWith(rootLocation)) {
                throw new FileStorageException("Acceso no autorizado al archivo: " + relativePath);
            }
            boolean deleted = Files.deleteIfExists(file);
            if (deleted) {
                log.info("Archivo eliminado físicamente: {}", relativePath);
            }
            return deleted;
        } catch (IOException e) {
            throw new FileStorageException("Error al eliminar el archivo físico: " + relativePath, e);
        }
    }

    @Override
    public Path getRootLocation() {
        return rootLocation;
    }

    private void validateFileType(String extension, String contentType) {
        boolean extensionAllowed = properties.getAllowedExtensions().stream()
                .anyMatch(ext -> ext.equalsIgnoreCase(extension));

        if (!extensionAllowed) {
            throw new InvalidFileTypeException(String.format(
                    "Extensión de archivo '%s' no permitida. Formatos soportados: %s",
                    extension, properties.getAllowedExtensions()
            ));
        }

        if (contentType != null && !contentType.isBlank() && !contentType.equalsIgnoreCase("application/octet-stream")) {
            boolean mimeAllowed = properties.getAllowedMimeTypes().stream()
                    .anyMatch(mime -> mime.equalsIgnoreCase(contentType));
            if (!mimeAllowed) {
                log.warn("MIME type {} no coincide explícitamente con los soportados, pero la extensión {} es válida",
                        contentType, extension);
            }
        }
    }

    private String determineMimeType(String extension, String providedContentType) {
        if (providedContentType != null && !providedContentType.isBlank() && !providedContentType.equalsIgnoreCase("application/octet-stream")) {
            return providedContentType;
        }
        return switch (extension.toLowerCase()) {
            case ".pdf" -> "application/pdf";
            case ".png" -> "image/png";
            case ".jpg", ".jpeg" -> "image/jpeg";
            default -> "application/octet-stream";
        };
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex);
    }
}
