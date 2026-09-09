package com.cecamed.ui.controller;

import com.cecamed.core.model.appointment.enums.AppointmentStatus;
import com.cecamed.services.dto.appointment.AppointmentResponseDto;
import com.cecamed.services.dto.patient.PatientResponseDto;
import com.cecamed.services.service.AppointmentService;
import com.cecamed.services.service.PatientService;
import com.cecamed.ui.component.StatCard;
import com.cecamed.ui.component.StatusBadge;
import com.cecamed.ui.navigation.NavigationService;
import com.cecamed.ui.navigation.ViewType;
import com.cecamed.ui.session.UserSession;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

@Slf4j
@Component
@RequiredArgsConstructor
public class DashboardController implements Initializable {

    private final PatientService patientService;
    private final AppointmentService appointmentService;
    private final NavigationService navigationService;
    private final UserSession userSession;

    @FXML private Label welcomeLabel;
    @FXML private HBox statCardsContainer;

    @FXML private TableView<AppointmentResponseDto> upcomingAppointmentsTable;
    @FXML private TableColumn<AppointmentResponseDto, String> colTime;
    @FXML private TableColumn<AppointmentResponseDto, String> colPatient;
    @FXML private TableColumn<AppointmentResponseDto, String> colReason;
    @FXML private TableColumn<AppointmentResponseDto, AppointmentStatus> colStatus;

    private final ObservableList<AppointmentResponseDto> appointmentsList = FXCollections.observableArrayList();

    private StatCard cardAppointmentsToday;
    private StatCard cardPatientsActive;
    private StatCard cardWaitingRoom;
    private StatCard cardCompletedToday;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupWelcomeText();
        setupStatCards();
        setupTable();
        loadDashboardDataAsync();
    }

    private void setupWelcomeText() {
        String name = userSession.isAuthenticated() ? userSession.getFullName() : "Dr. Carlos Morales";
        welcomeLabel.setText("¡Bienvenido, " + name + "!");
    }

    private void setupStatCards() {
        cardAppointmentsToday = new StatCard("Citas de Hoy", "0", "Programadas para el día", "feather-calendar", "primary");
        cardWaitingRoom = new StatCard("En Sala de Espera", "0", "Pacientes aguardando atención", "feather-clock", "warning");
        cardCompletedToday = new StatCard("Atendidos Hoy", "0", "Consultas finalizadas", "feather-check-circle", "success");
        cardPatientsActive = new StatCard("Pacientes Activos", "0", "Total en base de datos", "feather-users", "accent");

        statCardsContainer.getChildren().addAll(cardAppointmentsToday, cardWaitingRoom, cardCompletedToday, cardPatientsActive);
    }

    private void setupTable() {
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        colTime.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getStartTime() != null ? data.getValue().getStartTime().format(timeFormatter) : "--:--"
        ));

        colPatient.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getPatientFullName() != null ? data.getValue().getPatientFullName() : "Paciente no asignado"
        ));

        colReason.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getReasonForVisit() != null ? data.getValue().getReasonForVisit() : "-"
        ));

        colStatus.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getStatus()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(AppointmentStatus status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                } else {
                    setGraphic(StatusBadge.ofAppointment(status));
                }
            }
        });

        upcomingAppointmentsTable.setItems(appointmentsList);
    }

    private void loadDashboardDataAsync() {
        Task<DashboardData> task = new Task<>() {
            @Override
            protected DashboardData call() {
                LocalDate today = LocalDate.now();
                LocalDateTime startOfDay = today.atStartOfDay();
                LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

                List<AppointmentResponseDto> todayAppointments = appointmentService.getAppointmentsByDateRange(startOfDay, endOfDay);
                List<PatientResponseDto> activePatients = patientService.getAllActivePatients();

                long inWaitingRoom = todayAppointments.stream()
                        .filter(a -> a.getStatus() == AppointmentStatus.EN_SALA)
                        .count();

                long completed = todayAppointments.stream()
                        .filter(a -> a.getStatus() == AppointmentStatus.ATENDIDA)
                        .count();

                return new DashboardData(todayAppointments, activePatients.size(), inWaitingRoom, completed);
            }
        };

        task.setOnSucceeded(e -> {
            DashboardData data = task.getValue();
            cardAppointmentsToday.setValue(String.valueOf(data.todayAppointments.size()));
            cardPatientsActive.setValue(String.valueOf(data.activePatientsCount));
            cardWaitingRoom.setValue(String.valueOf(data.waitingCount));
            cardCompletedToday.setValue(String.valueOf(data.completedCount));

            appointmentsList.setAll(data.todayAppointments);
        });

        task.setOnFailed(e -> log.error("Error al cargar métricas del Dashboard", task.getException()));

        new Thread(task).start();
    }

    @FXML
    public void handleQuickNewPatient(ActionEvent event) {
        navigationService.navigateTo(ViewType.PATIENTS);
    }

    @FXML
    public void handleQuickNewAppointment(ActionEvent event) {
        navigationService.navigateTo(ViewType.APPOINTMENTS);
    }

    @FXML
    public void handleQuickViewAgenda(ActionEvent event) {
        navigationService.navigateTo(ViewType.APPOINTMENTS);
    }

    @FXML
    public void handleQuickWaitingRoom(ActionEvent event) {
        navigationService.navigateTo(ViewType.RECEPTION);
    }

    private record DashboardData(List<AppointmentResponseDto> todayAppointments, int activePatientsCount, long waitingCount, long completedCount) {}
}
