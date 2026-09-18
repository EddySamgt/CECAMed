package com.cecamed.ui.component;

import com.cecamed.core.model.appointment.enums.AppointmentStatus;
import com.cecamed.core.model.appointment.enums.GoogleSyncStatus;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.javafx.FontIcon;

public class StatusBadge extends HBox {

    private final FontIcon icon = new FontIcon();
    private final Label label = new Label();

    public StatusBadge() {
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(6);
        getStyleClass().add("status-badge");
        getChildren().addAll(icon, label);
    }

    public static StatusBadge ofAppointment(AppointmentStatus status) {
        StatusBadge badge = new StatusBadge();
        badge.setAppointmentStatus(status);
        return badge;
    }

    public static StatusBadge ofGoogleSync(GoogleSyncStatus status) {
        StatusBadge badge = new StatusBadge();
        badge.setGoogleSyncStatus(status);
        return badge;
    }

    public void setAppointmentStatus(AppointmentStatus status) {
        getStyleClass().removeAll("badge-success", "badge-warning", "badge-danger", "badge-info", "badge-secondary");
        if (status == null) {
            label.setText("N/D");
            return;
        }

        switch (status) {
            case CONFIRMADA -> {
                label.setText("Confirmada");
                icon.setIconLiteral("fth-check-circle");
                getStyleClass().add("badge-success");
            }
            case EN_SALA -> {
                label.setText("En Sala");
                icon.setIconLiteral("fth-clock");
                getStyleClass().add("badge-warning");
            }
            case ATENDIDA -> {
                label.setText("Atendida");
                icon.setIconLiteral("fth-check");
                getStyleClass().add("badge-success");
            }
            case PROGRAMADA -> {
                label.setText("Programada");
                icon.setIconLiteral("fth-calendar");
                getStyleClass().add("badge-info");
            }
            case REPROGRAMADA -> {
                label.setText("Reprogramada");
                icon.setIconLiteral("fth-refresh-cw");
                getStyleClass().add("badge-info");
            }
            case NO_ASISTIO -> {
                label.setText("No Asistió");
                icon.setIconLiteral("fth-user-x");
                getStyleClass().add("badge-danger");
            }
            case CANCELADA -> {
                label.setText("Cancelada");
                icon.setIconLiteral("fth-x-circle");
                getStyleClass().add("badge-danger");
            }
        }
    }

    public void setGoogleSyncStatus(GoogleSyncStatus status) {
        getStyleClass().removeAll("badge-success", "badge-warning", "badge-danger", "badge-info", "badge-secondary");
        if (status == null) {
            label.setText("Sin Sync");
            return;
        }

        switch (status) {
            case SYNCED -> {
                label.setText("Sincronizado");
                icon.setIconLiteral("fth-cloud");
                getStyleClass().add("badge-success");
            }
            case PENDING -> {
                label.setText("Pendiente");
                icon.setIconLiteral("fth-loader");
                getStyleClass().add("badge-warning");
            }
            case FAILED -> {
                label.setText("Fallo Sync");
                icon.setIconLiteral("fth-alert-triangle");
                getStyleClass().add("badge-danger");
            }
            case NOT_APPLICABLE -> {
                label.setText("Desactivado");
                icon.setIconLiteral("fth-cloud-off");
                getStyleClass().add("badge-secondary");
            }
        }
    }
}
