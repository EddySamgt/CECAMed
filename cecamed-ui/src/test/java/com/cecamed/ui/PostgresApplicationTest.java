package com.cecamed.ui;

import com.cecamed.services.service.PatientService;
import com.cecamed.services.storage.FileStorageService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("postgres")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.datasource.url=${TEST_POSTGRES_URL}",
        "spring.datasource.username=${TEST_POSTGRES_USERNAME}",
        "spring.datasource.password=${TEST_POSTGRES_PASSWORD:}"
})
class PostgresApplicationTest {
    @TempDir static Path uploads;
    @Autowired ApplicationContext context;

    @DynamicPropertySource
    static void storageDirectory(DynamicPropertyRegistry registry) {
        registry.add("cecamed.storage.upload-dir", () -> uploads.toString());
    }

    @Test
    void wiresApplicationServicesWithProductionMigrations() {
        assertThat(context.getBean(PatientService.class)).isNotNull();
        assertThat(context.getBean(FileStorageService.class)).isNotNull();
    }
}
