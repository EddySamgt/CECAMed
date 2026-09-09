package com.cecamed.ui.controller;

import com.cecamed.calendar.service.AppointmentCalendarSyncService;
import com.cecamed.core.model.appointment.enums.GoogleSyncStatus;
import com.cecamed.ui.component.StatusBadge;
import com.cecamed.ui.navigation.NavigationService;
import com.cecamed.ui.navigation.ViewType;
import com.cecamed.ui.session.AuthenticationService;
import com.cecamed.ui.session.UserSession;
import com.cecamed.ui.theme.ThemeManager;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

@Slf4j
@Component
@RequiredArgsConstructor
public class MainLayoutController implements Initializable {

    private final NavigationService navigationService;
    private final ThemeManager themeManager;
    private final UserSession userSession;
    private final AuthenticationService authenticationService;

    @Autowired(required = false)
    private AppointmentCalendarSyncService calendarSyncService;

    @FXML private StackPane contentArea;
    @FXML private Label userFullNameLabel;
    @FXML private Label userRoleLabel;
    @FXML private Label clockLabel;
    @FXML private FontIcon toggleThemeIcon;
    @FXML private HBox syncBadgeContainer;

    @FXML private Button btnDashboard;
    @FXML private Button btnPatients;
    @FXML private Button btnMedicalRecord;
    @FXML private Button btnAppointments;
    @FXML private Button btnReception;
    @FXML private Button btnSettings;

    private final List<Button> navButtons = new ArrayList<>();
    private Timeline clockTimeline;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        navigationService.setContentArea(contentArea);

        navButtons.addAll(List.of(btnDashboard, btnPatients, btnMedicalRecord, btnAppointments, btnReception, btnSettings));

        setupUserData();
        setupClock();
        setupGoogleSyncIndicator();
        updateThemeIcon();
        applyRoleRestrictions();

        // Default to Dashboard
        handleNavDashboard(null);
    }

    private void setupUserData() {
        if (userSession.isAuthenticated()) {
            userFullNameLabel.setText(userSession.getFullName());
            userRoleLabel.setText(userSession.getRole().getDisplayName());
        } else {
            userFullNameLabel.setText("Dr. Carlos Eduardo Morales");
            userRoleLabel.setText("Médico Especialista");
        }
    }

    private void setupClock() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM yyyy • HH:mm:ss");
        clockTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            clockLabel.setText(LocalDateTime.now().format(formatter));
        }));
        clockTimeline.setCycleCount(Animation.INDEFINITE);
        clockTimeline.play();
        clockLabel.setText(LocalDateTime.now().format(formatter));
    }

    private void setupGoogleSyncIndicator() {
        StatusBadge syncBadge = StatusBadge.ofGoogleSync(GoogleSyncStatus.SYNCED);
        syncBadgeContainer.getChildren().clear();
        syncBadgeContainer.getChildren().add(syncBadge);
    }

    private void applyRoleRestrictions() {
        if (userSession.isRecepcion()) {
            btnMedicalRecord.setVisible(false);
            btnMedicalRecord.setManaged(false);
        }
    }

    private void setActiveButton(Button activeButton) {
        for (Button btn : navButtons) {
            btn.getStyleClass().remove("active");
        }
        if (activeButton != null && !activeButton.getStyleClass().contains("active")) {
            activeButton.getStyleClass().add("active");
        }
    }

    @FXML
    public void handleNavDashboard(ActionEvent event) {
        setActiveButton(btnDashboard);
        navigationService.navigateTo(ViewType.DASHBOARD);
    }

    @FXML
    public void handleNavPatients(ActionEvent event) {
        setActiveButton(btnPatients);
        navigationService.navigateTo(ViewType.PATIENTS);
    }

    @FXML
    public void handleNavMedicalRecord(ActionEvent event) {
        setActiveButton(btnMedicalRecord);
        navigationService.navigateTo(ViewType.MEDICAL_RECORD);
    }

    @FXML
    public void handleNavAppointments(ActionEvent event) {
        setActiveButton(btnAppointments);
        navigationService.navigateTo(ViewType.APPOINTMENTS);
    }

    @FXML
    public void handleNavReception(ActionEvent event) {
        setActiveButton(btnReception);
        navigationService.navigateTo(ViewType.RECEPTION);
    }

    @FXML
    public void handleNavSettings(ActionEvent event) {
        setActiveButton(btnSettings);
        navigationService.navigateTo(ViewType.SETTINGS);
    }

    @FXML
    public void handleToggleTheme(ActionEvent event) {
        themeManager.toggleTheme();
        updateThemeIcon();
    }

    @FXML
    public void handleLogout(ActionEvent event) {
        if (clockTimeline != null) {
            clockTimeline.stop();
        }
        authenticationService.logout();
        navigationService.navigateRoot(ViewType.LOGIN);
    }

    private void updateThemeIcon() {
        if (toggleThemeIcon != null) {
            toggleThemeIcon.setIconLiteral(themeManager.isDarkMode() ? "feather-sun" : "feather-moon");
        }
    }
}
