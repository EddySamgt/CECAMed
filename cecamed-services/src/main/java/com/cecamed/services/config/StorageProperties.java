package com.cecamed.services.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "cecamed.storage")
@Getter
@Setter
public class StorageProperties {

    /**
     * Directorio raíz para el almacenamiento local de archivos adjuntos.
     */
    private String uploadDir = "uploads/documents";

    /**
     * Tamaño máximo permitido por archivo en bytes (por defecto 15MB).
     */
    private long maxFileSizeBytes = 15 * 1024 * 1024L;

    /**
     * Extensiones permitidas (.pdf, .png, .jpg, .jpeg).
     */
    private List<String> allowedExtensions = Arrays.asList(".pdf", ".png", ".jpg", ".jpeg");

    /**
     * Tipos MIME permitidos.
     */
    private List<String> allowedMimeTypes = Arrays.asList("application/pdf", "image/png", "image/jpeg");
}
