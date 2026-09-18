package com.cecamed.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class Utf8ConfigurationTest {
    @TempDir Path directory;

    @Test
    void importsDotEnvWithSpanishTextAndResolvesPlaceholders() throws Exception {
        String spanish = "áéíóú ÁÉÍÓÚ ñÑ üÜ ¿Cómo está? ¡Atención!";
        Path env = directory.resolve(".env");
        Files.writeString(env, "SPANISH_TEXT=" + spanish + "\n", StandardCharsets.UTF_8);
        Path config = directory.resolve("application.properties");
        Files.writeString(config,
                "spring.config.import=" + env.toUri() + "[.utf8]\n"
                        + "test.resolved=${SPANISH_TEXT}\n", StandardCharsets.UTF_8);

        try (var context = new SpringApplicationBuilder(EmptyConfiguration.class)
                .web(WebApplicationType.NONE)
                .run("--spring.config.location=" + config.toUri(), "--spring.main.banner-mode=off")) {
            assertThat(context.getEnvironment().getProperty("SPANISH_TEXT")).isEqualTo(spanish);
            assertThat(context.getEnvironment().getProperty("test.resolved")).isEqualTo(spanish);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class EmptyConfiguration {
    }
}
