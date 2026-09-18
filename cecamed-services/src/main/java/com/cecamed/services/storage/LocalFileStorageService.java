package com.cecamed.services.storage;

import com.cecamed.services.config.StorageProperties;
import com.cecamed.services.dto.document.StoredFileMetadata;
import com.cecamed.services.exception.FileStorageException;
import com.cecamed.services.exception.InvalidFileTypeException;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

@Service
public class LocalFileStorageService implements FileStorageService {
    private final StorageProperties properties;
    private final Path root;

    public LocalFileStorageService(StorageProperties properties) {
        this.properties = properties;
        try {
            root = Files.createDirectories(Path.of(properties.getUploadDir()).toAbsolutePath().normalize())
                    .toRealPath();
        } catch (IOException e) {
            throw new FileStorageException("No se pudo preparar el directorio de adjuntos", e);
        }
    }

    @Override
    public StoredFileMetadata store(MultipartFile file, String subDirectory) {
        if (file == null || file.isEmpty() || file.getSize() > properties.getMaxFileSizeBytes()) {
            throw new FileStorageException("El archivo está vacío o supera el tamaño máximo permitido");
        }
        String original = file.getOriginalFilename();
        if (original == null || original.isBlank() || original.length() > 255
                || original.contains("/") || original.contains("\\")) {
            throw new FileStorageException("Nombre de archivo inválido");
        }
        int dot = original.lastIndexOf('.');
        String extension = dot < 0 ? "" : original.substring(dot).toLowerCase(Locale.ROOT);
        if (!properties.getAllowedExtensions().contains(extension)
                || !properties.getAllowedMimeTypes().contains(file.getContentType())) {
            throw new InvalidFileTypeException("Tipo de archivo no permitido");
        }
        Path directory = resolve(subDirectory);
        Path destination = directory.resolve(UUID.randomUUID() + extension);
        boolean created = false;
        try {
            Files.createDirectories(directory);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            long size = 0;
            try (var input = file.getInputStream();
                 var output = Files.newOutputStream(destination, java.nio.file.StandardOpenOption.CREATE_NEW)) {
                created = true;
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    size += count;
                    if (size > properties.getMaxFileSizeBytes()) {
                        throw new FileStorageException("El archivo supera el tamaño máximo permitido");
                    }
                    digest.update(buffer, 0, count);
                    output.write(buffer, 0, count);
                }
            }
            if (size == 0) {
                throw new FileStorageException("El archivo está vacío");
            }
            return StoredFileMetadata.builder()
                    .savedFileName(destination.getFileName().toString())
                    .originalFileName(original)
                    .relativePath(root.relativize(destination).toString().replace('\\', '/'))
                    .fileType(file.getContentType()).fileSizeBytes(size)
                    .checksumSha256(HexFormat.of().formatHex(digest.digest())).build();
        } catch (IOException | NoSuchAlgorithmException | RuntimeException e) {
            if (created) {
                try {
                    Files.deleteIfExists(destination);
                } catch (IOException cleanupError) {
                    e.addSuppressed(cleanupError);
                }
            }
            throw new FileStorageException("No se pudo guardar el adjunto", e);
        }
    }

    @Override
    public Resource loadAsResource(String relativePath) {
        Path path = resolve(relativePath);
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new FileStorageException("No se encontró el archivo adjunto");
        }
        return new FileSystemResource(path);
    }

    @Override
    public void delete(String relativePath) {
        try {
            Files.deleteIfExists(resolve(relativePath));
        } catch (IOException e) {
            throw new FileStorageException("No se pudo eliminar el adjunto", e);
        }
    }

    private Path resolve(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new FileStorageException("Ruta de adjunto inválida");
        }
        Path relative = Path.of(relativePath);
        Path path = root.resolve(relative).normalize();
        if (relative.isAbsolute() || !path.startsWith(root) || path.equals(root)) {
            throw new FileStorageException("La ruta debe estar dentro del directorio de adjuntos");
        }
        for (Path current = path; !current.equals(root); current = current.getParent()) {
            if (Files.exists(current, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(current)) {
                throw new FileStorageException("No se permiten enlaces simbólicos en las rutas de adjuntos");
            }
        }
        return path;
    }
}
