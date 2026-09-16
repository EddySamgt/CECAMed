package com.cecamed.services.storage;

import com.cecamed.services.dto.document.StoredFileMetadata;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Path;

public interface FileStorageService {

    /**
     * Inicializa el almacenamiento creando los directorios raíz si no existen.
     */
    void init();

    /**
     * Almacena un archivo MultipartFile en una subcarpeta indicada.
     * Valida extensión (.pdf, .png, .jpg), tipo MIME, tamaño y previene path traversal.
     */
    StoredFileMetadata store(MultipartFile file, String subDirectory);

    /**
     * Almacena un archivo a partir de su InputStream.
     */
    StoredFileMetadata store(InputStream inputStream, String originalFilename, String contentType, long size, String subDirectory);

    /**
     * Carga el archivo físico como recurso de Spring (Resource).
     */
    Resource loadAsResource(String relativePath);

    /**
     * Elimina el archivo físico del disco si existe.
     */
    boolean delete(String relativePath);

    /**
     * Devuelve la ruta absoluta del directorio raíz de almacenamiento.
     */
    Path getRootLocation();
}
