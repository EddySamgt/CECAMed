package com.cecamed.ui.navigation;

public enum ViewType {
    LOGIN("/fxml/login.fxml", "Iniciar Sesión - CECAMed"),
    MAIN_LAYOUT("/fxml/main-layout.fxml", "CECAMed - Sistema de Control y Gestión Médica"),
    DASHBOARD("/fxml/dashboard.fxml", "Panel Principal"),
    PATIENTS("/fxml/patient-list.fxml", "Directorio de Pacientes"),
    MEDICAL_RECORD("/fxml/medical-record.fxml", "Expediente Clínico"),
    APPOINTMENTS("/fxml/appointment-calendar.fxml", "Agenda y Citas Médicas"),
    RECEPTION("/fxml/reception.fxml", "Recepción y Sala de Espera"),
    SETTINGS("/fxml/settings.fxml", "Configuración del Sistema");

    private final String fxmlPath;
    private final String title;

    ViewType(String fxmlPath, String title) {
        this.fxmlPath = fxmlPath;
        this.title = title;
    }

    public String getFxmlPath() {
        return fxmlPath;
    }

    public String getTitle() {
        return title;
    }
}
