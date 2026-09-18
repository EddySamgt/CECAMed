package com.cecamed.services.storage;

import com.cecamed.services.dto.document.StoredFileMetadata;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    StoredFileMetadata store(MultipartFile file, String subDirectory);
    Resource loadAsResource(String relativePath);
    void delete(String relativePath);
}
