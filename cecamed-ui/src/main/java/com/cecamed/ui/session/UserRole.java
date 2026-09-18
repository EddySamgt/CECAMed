package com.cecamed.ui.session;

public enum UserRole {
    MEDICO("Médico Especialista", "fth-user-check"),
    RECEPCION("Recepción Clínica", "fth-clipboard");

    private final String displayName;
    private final String iconLiteral;

    UserRole(String displayName, String iconLiteral) {
        this.displayName = displayName;
        this.iconLiteral = iconLiteral;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIconLiteral() {
        return iconLiteral;
    }
}
