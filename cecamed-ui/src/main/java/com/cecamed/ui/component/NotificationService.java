package com.cecamed.ui.component;

import javafx.application.Platform;
import javafx.util.Duration;
import org.controlsfx.control.Notifications;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    public void showSuccess(String title, String message) {
        Platform.runLater(() -> Notifications.create()
                .title(title)
                .text(message)
                .hideAfter(Duration.seconds(4))
                .showInformation());
    }

    public void showWarning(String title, String message) {
        Platform.runLater(() -> Notifications.create()
                .title(title)
                .text(message)
                .hideAfter(Duration.seconds(5))
                .showWarning());
    }

    public void showError(String title, String message) {
        Platform.runLater(() -> Notifications.create()
                .title(title)
                .text(message)
                .hideAfter(Duration.seconds(6))
                .showError());
    }

    public void showInfo(String title, String message) {
        Platform.runLater(() -> Notifications.create()
                .title(title)
                .text(message)
                .hideAfter(Duration.seconds(4))
                .show());
    }
}
