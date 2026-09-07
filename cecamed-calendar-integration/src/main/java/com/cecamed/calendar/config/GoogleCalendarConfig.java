package com.cecamed.calendar.config;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collections;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class GoogleCalendarConfig {

    private final GoogleCalendarProperties properties;

    @Bean
    public JsonFactory googleJsonFactory() {
        return GsonFactory.getDefaultInstance();
    }

    @Bean
    public HttpTransport googleHttpTransport() {
        try {
            return GoogleNetHttpTransport.newTrustedTransport();
        } catch (Exception e) {
            log.error("Error al inicializar transporte seguro HTTP de Google: {}", e.getMessage(), e);
            throw new RuntimeException("No se pudo inicializar HttpTransport para Google APIs", e);
        }
    }

    @Bean
    public Calendar googleCalendarService(HttpTransport transport, JsonFactory jsonFactory) {
        if (!properties.isEnabled()) {
            log.info("Integración con Google Calendar deshabilitada por configuración (cecamed.google.calendar.enabled=false)");
            return null;
        }

        try {
            InputStream credentialsStream = loadCredentialsStream(properties.getCredentialsPath());
            if (credentialsStream == null) {
                log.warn("Archivo de credenciales de Google Calendar no encontrado en '{}'. El servicio operará en modo simulado/deshabilitado.",
                        properties.getCredentialsPath());
                return null;
            }

            GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream)
                    .createScoped(Collections.singletonList(CalendarScopes.CALENDAR));

            log.info("Google Calendar API v3 inicializado exitosamente para la aplicación '{}'", properties.getApplicationName());

            return new Calendar.Builder(transport, jsonFactory, new HttpCredentialsAdapter(credentials))
                    .setApplicationName(properties.getApplicationName())
                    .build();

        } catch (Exception e) {
            log.error("No se pudo autenticar con Google Calendar API: {}. Modo offline/deshabilitado activado.", e.getMessage());
            return null;
        }
    }

    private InputStream loadCredentialsStream(String path) {
        try {
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                return new FileInputStream(file);
            }
            ClassPathResource classPathResource = new ClassPathResource(path);
            if (classPathResource.exists()) {
                return classPathResource.getInputStream();
            }
        } catch (Exception e) {
            log.warn("Error al intentar leer credenciales de Google Calendar: {}", e.getMessage());
        }
        return null;
    }
}
