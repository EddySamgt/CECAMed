package com.cecamed.services.storage;

import com.cecamed.services.config.StorageProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class StorageConfigurationTest {
    @TempDir
    Path directory;

    @Test
    void componentScanProvidesOneStorageService() {
        var properties = new StorageProperties();
        properties.setUploadDir(directory.toString());

        try (var context = new AnnotationConfigApplicationContext()) {
            context.registerBean(StorageProperties.class, () -> properties);
            context.scan("com.cecamed.services.storage");
            context.refresh();

            assertThat(context.getBeansOfType(FileStorageService.class)).hasSize(1);
            assertThat(context.getBean(FileStorageService.class)).isInstanceOf(LocalFileStorageService.class);
        }
    }
}
