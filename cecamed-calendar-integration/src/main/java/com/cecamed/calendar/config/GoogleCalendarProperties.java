package com.cecamed.calendar.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;

@Configuration
@ConfigurationProperties(prefix = "cecamed.google.calendar")
@Getter
@Setter
public class GoogleCalendarProperties {

    /**
     * Habilita o deshabilita la sincronización en vivo con Google Calendar API.
     */
    private boolean enabled = false;

    /**
     * Nombre de la aplicación cliente para el encabezado HTTP de Google API.
     */
    private String applicationName = "CECAMed-Clinical-System";

    /**
     * Identificador del calendario de Google (por defecto "primary" del usuario autenticado).
     */
    private String calendarId = "primary";

    /**
     * Ruta al archivo JSON de credenciales (Service Account o OAuth2 Client Secrets).
     */
    private String credentialsPath = "credentials.json";

    /**
     * Directorio para almacenar tokens OAuth2 de acceso/refresco si se usa OAuth2.
     */
    private String tokensDirectoryPath = "tokens";

    /**
     * Zona horaria para la interpretación de fechas y eventos (por defecto zona del sistema).
     */
    private String timeZone = ZoneId.systemDefault().getId();

    /**
     * Si debe sincronizar automáticamente cada vez que se crea o modifica una cita.
     */
    private boolean syncOnAppointmentChange = true;
}
