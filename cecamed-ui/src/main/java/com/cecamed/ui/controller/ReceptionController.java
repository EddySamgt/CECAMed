package com.cecamed.ui.controller;

import com.cecamed.core.model.appointment.enums.AppointmentStatus;
import com.cecamed.services.dto.appointment.AppointmentResponseDto;
import com.cecamed.services.service.AppointmentService;
import com.cecamed.ui.component.ConfirmationDialog;
import com.cecamed.ui.component.NotificationService;
import com.cecamed.ui.component.StatCard;
import com.cecamed.ui.component.StatusBadge;
import com.cecamed.ui.navigation.NavigationService;
import com.cecamed.ui.navigation.ViewType;
import com.cecamed.ui.theme.ThemeManager;
import com.cecamed.ui.util.SpringFXMLLoader;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReceptionController implements Initializable {

    private final AppointmentService appointmentService;
    private final NavigationService navigationService;
    private final ThemeManager themeManager;
    private final NotificationService notificationService;
    private final SpringFXMLLoader springFXMLLoader;

    // Stat Cards Metrics
    @FXML private HBox statCardsContainer;
    private StatCard cardTodayAppointments;
    private StatCard cardWaitingRoom;
    private StatCard cardAttendedToday;
    private StatCard cardPendingToday;

    // Search & Filter
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilterComboBox;
    @FXML private Label appointmentsCountLabel;
    @FXML private Label waitingCountLabel;
    @FXML private ProgressIndicator loadingIndicator;

    // Tabla Citas del D?a
    @FXML private TableView<AppointmentResponseDto> todayAppointmentsTable;
    @FXML private TableColumn<AppointmentResponseDto, String> colTime;
    @FXML private TableColumn<AppointmentResponseDto, String> colPatient;
    @FXML private TableColumn<AppointmentResponseDto, String> colPhone;
    @FXML private TableColumn<AppointmentResponseDto, String> colReason;
    @FXML private TableColumn<AppointmentResponseDto, AppointmentStatus> colStatus;
    @FXML private TableColumn<AppointmentResponseDto, Void> colActions;

    // Sala de Espera Table
    @FXML private TableView<WaitingPatientItem> waitingRoomTable;
    @FXML private TableColumn<WaitingPatientItem, String> colWaitPatient;
    @FXML private TableColumn<WaitingPatientItem, String> colWaitAppTime;
    @FXML private TableColumn<WaitingPatientItem, String> colWaitArrivalTime;
    @FXML private TableColumn<WaitingPatientItem, String> colWaitElapsedTime;
    @FXML private TableColumn<WaitingPatientItem, Void> colWaitActions;

    private final ObservableList<AppointmentResponseDto> allTodayAppointments = FXCollections.observableArrayList();
    private final ObservableList<WaitingPatientItem> waitingPatientsList = FXCollections.observableArrayList();

    // Registro de horas de llegada por ID de cita
    private static final Map<Long, LocalTime> ARRIVAL_TIMES = new ConcurrentHashMap<>();

    private Timeline waitingTimerTimeline;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupStatCards();
        setupFilterComboBox();
        setupTodayAppointmentsTable();
        setupWaitingRoomTable();
        setupRealtimeTimer();

        loadTodayAppointmentsAsync();
    }

    private void setupStatCards() {
        cardTodayAppointments = new StatCard("Citas de Hoy", "0", "Total agendadas", "feather-calendar", "primary");
        cardWaitingRoom = new StatCard("En Sala de Espera", "0", "Pacientes aguardando", "feather-clock", "warning");
        cardAttendedToday = new StatCard("Atendidos Hoy", "0", "Consultas finalizadas", "feather-check-circle", "success");
        cardPendingToday = new StatCard("Pendientes", "0", "Por ingresar", "feather-user-check", "accent");

        statCardsContainer.getChildren().addAll(cardTodayAppointments, cardWaitingRoom, cardAttendedToday, cardPendingToday);
    }

    private void setupFilterComboBox() {
        statusFilterComboBox.setItems(FXCollections.observableArrayList(
                "Todas", "PROGRAMADA", "CONFIRMADA", "EN_SALA", "ATENDIDA", "NO_ASISTIO", "CANCELADA"
        ));
        statusFilterComboBox.setValue("Todas");
        statusFilterComboBox.setOnAction(e -> filterAppointmentsTable());

        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterAppointmentsTable());
    }

    private void setupTodayAppointmentsTable() {
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");

        colTime.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getStartTime() != null ? data.getValue().getStartTime().format(timeFmt) : "--:--"
        ));

        colPatient.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getPatientFullName() != null ? data.getValue().getPatientFullName() : "Paciente no asignado"
        ));

        colPhone.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getPatientPhone() != null && !data.getValue().getPatientPhone().isBlank() ? data.getValue().getPatientPhone() : "-"
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

        setupTodayActionsColumn();
        todayAppointmentsTable.setItems(allTodayAppointments);
    }

    private void setupTodayActionsColumn() {
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnConfirm = new Button();
            private final Button btnCheckIn = new Button();
            private final Button btnConsult = new Button();
            private final Button btnNoShow = new Button();
            private final Button btnReschedule = new Button();
            private final HBox container = new HBox(4, btnConfirm, btnCheckIn, btnConsult, btnNoShow, btnReschedule);

            {
                container.setAlignment(Pos.CENTER);

                btnConfirm.getStyleClass().add("ghost-button");
                btnConfirm.setTooltip(new Tooltip("Confirmar Asistencia"));
                FontIcon iconConf = new FontIcon("feather-thumbs-up");
                iconConf.setIconSize(13);
                btnConfirm.setGraphic(iconConf);

                btnCheckIn.getStyleClass().addAll("accent-button");
                btnCheckIn.setStyle("-fx-padding: 3 8; -fx-font-size: 11px;");
                btnCheckIn.setText("Check-in");
                FontIcon iconCheck = new FontIcon("feather-user-check");
                iconCheck.setIconSize(12);
                iconCheck.setIconColor(javafx.scene.paint.Color.WHITE);
                btnCheckIn.setGraphic(iconCheck);

                btnConsult.getStyleClass().add("primary-button");
                btnConsult.setStyle("-fx-padding: 3 8; -fx-font-size: 11px;");
                btnConsult.setText("A Consulta");
                FontIcon iconDoc = new FontIcon("feather-arrow-right-circle");
                iconDoc.setIconSize(12);
                iconDoc.setIconColor(javafx.scene.paint.Color.WHITE);
                btnConsult.setGraphic(iconDoc);

                btnNoShow.getStyleClass().add("ghost-button");
                btnNoShow.setTooltip(new Tooltip("Marcar No Asisti?"));
                FontIcon iconNo = new FontIcon("feather-user-x");
                iconNo.setIconSize(13);
                iconNo.setIconColor(javafx.scene.paint.Color.web("#D32F2F"));
                btnNoShow.setGraphic(iconNo);

                btnReschedule.getStyleClass().add("ghost-button");
                btnReschedule.setTooltip(new Tooltip("Reprogramar Cita"));
                FontIcon iconRes = new FontIcon("feather-calendar");
                iconRes.setIconSize(13);
                btnReschedule.setGraphic(iconRes);

                btnConfirm.setOnAction(e -> {
                    AppointmentResponseDto item = getTableView().getItems().get(getIndex());
                    handleUpdateStatus(item, AppointmentStatus.CONFIRMADA, "Cita confirmada");
                });

                btnCheckIn.setOnAction(e -> {
                    AppointmentResponseDto item = getTableView().getItems().get(getIndex());
                    handleCheckIn(item);
                });

                btnConsult.setOnAction(e -> {
                    AppointmentResponseDto item = getTableView().getItems().get(getIndex());
                    handleUpdateStatus(item, AppointmentStatus.ATENDIDA, "Paciente ingresado a consulta con el m?dico");
                });

                btnNoShow.setOnAction(e -> {
                    AppointmentResponseDto item = getTableView().getItems().get(getIndex());
                    handleNoShow(item);
                });

                btnReschedule.setOnAction(e -> {
                    AppointmentResponseDto item = getTableView().getItems().get(getIndex());
                    openRescheduleDialog(item);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    AppointmentResponseDto app = getTableView().getItems().get(getIndex());
                    AppointmentStatus status = app.getStatus();

                    boolean isDone = status == AppointmentStatus.ATENDIDA || status == AppointmentStatus.CANCELADA || status == AppointmentStatus.NO_ASISTIO;
                    btnConfirm.setVisible(status == AppointmentStatus.PROGRAMADA);
                    btnConfirm.setManaged(status == AppointmentStatus.PROGRAMADA);

                    btnCheckIn.setVisible(status == AppointmentStatus.PROGRAMADA || status == AppointmentStatus.CONFIRMADA);
                    btnCheckIn.setManaged(status == AppointmentStatus.PROGRAMADA || status == AppointmentStatus.CONFIRMADA);

                    btnConsult.setVisible(status == AppointmentStatus.EN_SALA);
                    btnConsult.setManaged(status == AppointmentStatus.EN_SALA);

                    btnNoShow.setVisible(!isDone);
                    btnNoShow.setManaged(!isDone);

                    btnReschedule.setVisible(!isDone);
                    btnReschedule.setManaged(!isDone);

                    setGraphic(container);
                }
            }
        });
    }

    private void setupWaitingRoomTable() {
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");

        colWaitPatient.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().appointment().getPatientFullName()));
        colWaitAppTime.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().appointment().getStartTime() != null ? data.getValue().appointment().getStartTime().format(timeFmt) : "--:--"
        ));
        colWaitArrivalTime.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().arrivalTime().format(timeFmt)
        ));
        colWaitElapsedTime.setCellValueFactory(data -> data.getValue().elapsedTimeProperty());

        colWaitActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEnter = new Button("Pasar a Consulta");
            {
                btnEnter.getStyleClass().add("primary-button");
                btnEnter.setStyle("-fx-padding: 4 10; -fx-font-size: 11px;");
                FontIcon icon = new FontIcon("feather-check");
                icon.setIconColor(javafx.scene.paint.Color.WHITE);
                icon.setIconSize(13);
                btnEnter.setGraphic(icon);

                btnEnter.setOnAction(e -> {
                    WaitingPatientItem item = getTableView().getItems().get(getIndex());
                    handleUpdateStatus(item.appointment(), AppointmentStatus.ATENDIDA,
                            "Paciente " + item.appointment().getPatientFullName() + " pas? al consultorio m?dico");
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btnEnter);
                }
            }
        });

        waitingRoomTable.setItems(waitingPatientsList);
    }

    private void setupRealtimeTimer() {
        // Actualiza el tiempo de espera transcurrido en vivo cada segundo sin bloquear la UI
        waitingTimerTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            LocalTime now = LocalTime.now();
            for (WaitingPatientItem item : waitingPatientsList) {
                java.time.Duration diff = java.time.Duration.between(item.arrivalTime(), now);
                long minutes = Math.max(0, diff.toMinutes());
                long seconds = Math.max(0, diff.toSecondsPart());
                item.setElapsedTime(String.format("%d min %02d s", minutes, seconds));
            }
        }));
        waitingTimerTimeline.setCycleCount(Animation.INDEFINITE);
        waitingTimerTimeline.play();
    }

    public void loadTodayAppointmentsAsync() {
        loadingIndicator.setVisible(true);

        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        Task<List<AppointmentResponseDto>> task = new Task<>() {
            @Override
            protected List<AppointmentResponseDto> call() {
                return appointmentService.getAppointmentsByDateRange(startOfDay, endOfDay);
            }
        };

        task.setOnSucceeded(e -> {
            loadingIndicator.setVisible(false);
            List<AppointmentResponseDto> list = task.getValue();
            allTodayAppointments.setAll(list);
            filterAppointmentsTable();
            refreshWaitingRoomList(list);
            updateStatCardsMetrics(list);
        });

        task.setOnFailed(e -> {
            loadingIndicator.setVisible(false);
            log.error("Error al cargar citas de recepci?n", task.getException());
            notificationService.showError("Error", "No fue posible cargar las citas del d?a");
        });

        new Thread(task).start();
    }

    private void filterAppointmentsTable() {
        String filter = statusFilterComboBox.getValue();
        String term = searchField.getText() != null ? searchField.getText().trim().toLowerCase() : "";

        List<AppointmentResponseDto> filtered = allTodayAppointments.stream()
                .filter(a -> {
                    if (filter != null && !filter.equalsIgnoreCase("Todas")) {
                        if (!a.getStatus().name().equalsIgnoreCase(filter)) return false;
                    }
                    if (!term.isBlank()) {
                        String patient = a.getPatientFullName() != null ? a.getPatientFullName().toLowerCase() : "";
                        String dni = a.getPatientIdentificationNumber() != null ? a.getPatientIdentificationNumber().toLowerCase() : "";
                        return patient.contains(term) || dni.contains(term);
                    }
                    return true;
                })
                .toList();

        todayAppointmentsTable.setItems(FXCollections.observableArrayList(filtered));
        appointmentsCountLabel.setText("Citas listadas: " + filtered.size());
    }

    private void refreshWaitingRoomList(List<AppointmentResponseDto> list) {
        LocalTime now = LocalTime.now();
        waitingPatientsList.clear();

        for (AppointmentResponseDto app : list) {
            if (app.getStatus() == AppointmentStatus.EN_SALA) {
                LocalTime arrivalTime = ARRIVAL_TIMES.computeIfAbsent(app.getId(), k -> {
                    // Si ya estaba en sala al iniciar, tomamos la hora de cita o la hora actual
                    if (app.getStartTime() != null && app.getStartTime().toLocalTime().isBefore(now)) {
                        return app.getStartTime().toLocalTime();
                    }
                    return now;
                });

                java.time.Duration diff = java.time.Duration.between(arrivalTime, now);
                String elapsed = String.format("%d min %02d s", Math.max(0, diff.toMinutes()), Math.max(0, diff.toSecondsPart()));

                waitingPatientsList.add(new WaitingPatientItem(app, arrivalTime, elapsed));
            } else {
                ARRIVAL_TIMES.remove(app.getId());
            }
        }

        waitingCountLabel.setText("Pacientes en espera: " + waitingPatientsList.size());
    }

    private void updateStatCardsMetrics(List<AppointmentResponseDto> list) {
        long total = list.size();
        long inWaiting = list.stream().filter(a -> a.getStatus() == AppointmentStatus.EN_SALA).count();
        long completed = list.stream().filter(a -> a.getStatus() == AppointmentStatus.ATENDIDA).count();
        long pending = list.stream().filter(a -> a.getStatus() == AppointmentStatus.PROGRAMADA || a.getStatus() == AppointmentStatus.CONFIRMADA).count();

        cardTodayAppointments.setValue(String.valueOf(total));
        cardWaitingRoom.setValue(String.valueOf(inWaiting));
        cardAttendedToday.setValue(String.valueOf(completed));
        cardPendingToday.setValue(String.valueOf(pending));
    }

    private void handleCheckIn(AppointmentResponseDto app) {
        ARRIVAL_TIMES.put(app.getId(), LocalTime.now());
        handleUpdateStatus(app, AppointmentStatus.EN_SALA, "Check-in completado: " + app.getPatientFullName() + " pas? a sala de espera");
    }

    private void handleNoShow(AppointmentResponseDto app) {
        boolean confirmed = ConfirmationDialog.confirm(
                "Marcar Inasistencia",
                "?Desea registrar al paciente como NO ASISTI??",
                "Paciente: " + app.getPatientFullName() + " - Cita: " + app.getStartTime().toLocalTime()
        );
        if (!confirmed) return;

        handleUpdateStatus(app, AppointmentStatus.NO_ASISTIO, "Cita marcada como No Asisti?");
    }

    private void handleUpdateStatus(AppointmentResponseDto app, AppointmentStatus newStatus, String successMsg) {
        loadingIndicator.setVisible(true);

        Task<AppointmentResponseDto> task = new Task<>() {
            @Override
            protected AppointmentResponseDto call() {
                return appointmentService.updateAppointmentStatus(app.getId(), newStatus);
            }
        };

        task.setOnSucceeded(e -> {
            loadingIndicator.setVisible(false);
            notificationService.showSuccess("Recepci?n", successMsg);
            loadTodayAppointmentsAsync();
        });

        task.setOnFailed(e -> {
            loadingIndicator.setVisible(false);
            log.error("Error al actualizar estado en recepci?n", task.getException());
            notificationService.showError("Error", "No fue posible actualizar el estado de la cita");
        });

        new Thread(task).start();
    }

    private void openRescheduleDialog(AppointmentResponseDto appointment) {
        try {
            FXMLLoader loader = springFXMLLoader.createLoader("/fxml/appointment-dialog.fxml");
            Parent root = loader.load();

            AppointmentDialogController dialogCtrl = loader.getController();

            Stage stage = new Stage();
            stage.setTitle("Reprogramar Cita - CECAMed");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(todayAppointmentsTable.getScene().getWindow());

            Scene scene = new Scene(root);
            themeManager.registerScene(scene);
            stage.setScene(scene);

            dialogCtrl.setDialogStage(stage);
            dialogCtrl.initRescheduleMode(appointment);

            stage.showAndWait();

            if (dialogCtrl.isSavedSuccessfully()) {
                loadTodayAppointmentsAsync();
            }
        } catch (IOException ex) {
            log.error("Error al abrir di?logo de reprogramaci?n", ex);
            notificationService.showError("Error", "No se pudo abrir el di?logo de reprogramaci?n");
        }
    }

    @FXML
    public void handleRefresh(ActionEvent event) {
        loadTodayAppointmentsAsync();
    }

    @FXML
    public void handleNewAppointment(ActionEvent event) {
        try {
            FXMLLoader loader = springFXMLLoader.createLoader("/fxml/appointment-dialog.fxml");
            Parent root = loader.load();

            AppointmentDialogController dialogCtrl = loader.getController();

            Stage stage = new Stage();
            stage.setTitle("Agendar Cita R?pida - CECAMed");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(todayAppointmentsTable.getScene().getWindow());

            Scene scene = new Scene(root);
            themeManager.registerScene(scene);
            stage.setScene(scene);

            dialogCtrl.setDialogStage(stage);
            dialogCtrl.initCreateMode(LocalDate.now(), null);

            stage.showAndWait();

            if (dialogCtrl.isSavedSuccessfully()) {
                loadTodayAppointmentsAsync();
            }
        } catch (IOException ex) {
            log.error("Error al abrir di?logo de citas", ex);
            notificationService.showError("Error", "No fue posible abrir el di?logo de cita");
        }
    }

    // Modelo reactivo para el monitor de la sala de espera
    public static class WaitingPatientItem {
        private final AppointmentResponseDto appointment;
        private final LocalTime arrivalTime;
        private final SimpleStringProperty elapsedTimeProperty;

        public WaitingPatientItem(AppointmentResponseDto appointment, LocalTime arrivalTime, String initialElapsed) {
            this.appointment = appointment;
            this.arrivalTime = arrivalTime;
            this.elapsedTimeProperty = new SimpleStringProperty(initialElapsed);
        }

        public AppointmentResponseDto appointment() { return appointment; }
        public LocalTime arrivalTime() { return arrivalTime; }
        public SimpleStringProperty elapsedTimeProperty() { return elapsedTimeProperty; }
        public void setElapsedTime(String time) { elapsedTimeProperty.set(time); }
    }
}
