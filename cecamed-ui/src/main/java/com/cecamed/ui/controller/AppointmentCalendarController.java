package com.cecamed.ui.controller;

import com.cecamed.core.model.appointment.enums.AppointmentStatus;
import com.cecamed.core.model.appointment.enums.GoogleSyncStatus;
import com.cecamed.services.dto.appointment.AppointmentResponseDto;
import com.cecamed.services.service.AppointmentService;
import com.cecamed.ui.component.ConfirmationDialog;
import com.cecamed.ui.component.NotificationService;
import com.cecamed.ui.component.StatusBadge;
import com.cecamed.ui.navigation.NavigationService;
import com.cecamed.ui.navigation.ViewType;
import com.cecamed.ui.session.UserSession;
import com.cecamed.ui.theme.ThemeManager;
import com.cecamed.ui.util.SpringFXMLLoader;
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
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentCalendarController implements Initializable {

    private final AppointmentService appointmentService;
    private final NavigationService navigationService;
    private final ThemeManager themeManager;
    private final NotificationService notificationService;
    private final SpringFXMLLoader springFXMLLoader;
    private final UserSession userSession;

    public enum CalendarViewMode { DAY, WEEK, MONTH }

    @FXML private Label currentPeriodLabel;
    @FXML private DatePicker datePickerJump;
    @FXML private ToggleGroup viewModeGroup;
    @FXML private ToggleButton btnModeDay;
    @FXML private ToggleButton btnModeWeek;
    @FXML private ToggleButton btnModeMonth;
    @FXML private ComboBox<String> statusFilterComboBox;
    @FXML private Label appointmentCountLabel;
    @FXML private ProgressIndicator loadingIndicator;

    @FXML private TableView<AppointmentResponseDto> appointmentsTable;
    @FXML private TableColumn<AppointmentResponseDto, String> colDate;
    @FXML private TableColumn<AppointmentResponseDto, String> colTime;
    @FXML private TableColumn<AppointmentResponseDto, String> colPatient;
    @FXML private TableColumn<AppointmentResponseDto, String> colPhone;
    @FXML private TableColumn<AppointmentResponseDto, String> colReason;
    @FXML private TableColumn<AppointmentResponseDto, AppointmentStatus> colStatus;
    @FXML private TableColumn<AppointmentResponseDto, GoogleSyncStatus> colGoogleSync;
    @FXML private TableColumn<AppointmentResponseDto, Void> colActions;

    private LocalDate currentDate = LocalDate.now();
    private CalendarViewMode currentMode = CalendarViewMode.DAY;

    private final ObservableList<AppointmentResponseDto> appointmentsList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupFilterComboBox();
        setupViewModeToggle();
        setupTableColumns();
        setupDatePicker();
        setupEmptyPlaceholder();

        loadAppointmentsAsync();
    }

    private void setupFilterComboBox() {
        statusFilterComboBox.setItems(FXCollections.observableArrayList(
                "Todos los Estados", "PROGRAMADA", "CONFIRMADA", "EN_SALA", "ATENDIDA", "CANCELADA", "REPROGRAMADA", "NO_ASISTIO"
        ));
        statusFilterComboBox.setValue("Todos los Estados");
        statusFilterComboBox.setOnAction(e -> loadAppointmentsAsync());
    }

    private void setupViewModeToggle() {
        btnModeDay.setSelected(true);
        viewModeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == btnModeDay) {
                currentMode = CalendarViewMode.DAY;
            } else if (newVal == btnModeWeek) {
                currentMode = CalendarViewMode.WEEK;
            } else if (newVal == btnModeMonth) {
                currentMode = CalendarViewMode.MONTH;
            }
            updatePeriodLabel();
            loadAppointmentsAsync();
        });
    }

    private void setupDatePicker() {
        datePickerJump.setValue(currentDate);
        datePickerJump.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                currentDate = newVal;
                updatePeriodLabel();
                loadAppointmentsAsync();
            }
        });
    }

    private void setupTableColumns() {
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");

        colDate.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getStartTime() != null ? data.getValue().getStartTime().format(dateFmt) : "-"
        ));

        colTime.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getStartTime() != null ? data.getValue().getStartTime().format(timeFmt) + " - " + data.getValue().getEndTime().format(timeFmt) : "-"
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

        colGoogleSync.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getGoogleSyncStatus()));
        colGoogleSync.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(GoogleSyncStatus sync, boolean empty) {
                super.updateItem(sync, empty);
                if (empty || sync == null) {
                    setGraphic(null);
                } else {
                    setGraphic(StatusBadge.ofGoogleSync(sync));
                }
            }
        });

        setupActionsColumn();
        appointmentsTable.setItems(appointmentsList);
    }

    private void setupActionsColumn() {
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnRecord = new Button();
            private final Button btnReschedule = new Button();
            private final Button btnComplete = new Button();
            private final Button btnCancel = new Button();
            private final HBox container = new HBox(4, btnRecord, btnReschedule, btnComplete, btnCancel);

            {
                container.setAlignment(Pos.CENTER);

                btnRecord.getStyleClass().add("ghost-button");
                btnRecord.setTooltip(new Tooltip("Ver Expediente Cl?nico"));
                FontIcon iconRec = new FontIcon("feather-clipboard");
                iconRec.setIconSize(14);
                btnRecord.setGraphic(iconRec);

                btnReschedule.getStyleClass().add("ghost-button");
                btnReschedule.setTooltip(new Tooltip("Reprogramar Cita"));
                FontIcon iconRes = new FontIcon("feather-refresh-cw");
                iconRes.setIconSize(14);
                btnReschedule.setGraphic(iconRes);

                btnComplete.getStyleClass().add("ghost-button");
                btnComplete.setTooltip(new Tooltip("Marcar como Atendida"));
                FontIcon iconComp = new FontIcon("feather-check-circle");
                iconComp.setIconSize(14);
                iconComp.setIconColor(javafx.scene.paint.Color.web("#16A34A"));
                btnComplete.setGraphic(iconComp);

                btnCancel.getStyleClass().add("ghost-button");
                btnCancel.setTooltip(new Tooltip("Cancelar Cita"));
                FontIcon iconCan = new FontIcon("feather-x-circle");
                iconCan.setIconSize(14);
                iconCan.setIconColor(javafx.scene.paint.Color.web("#D32F2F"));
                btnCancel.setGraphic(iconCan);

                btnRecord.setOnAction(e -> {
                    AppointmentResponseDto item = getTableView().getItems().get(getIndex());
                    openMedicalRecord(item.getPatientId());
                });

                btnReschedule.setOnAction(e -> {
                    AppointmentResponseDto item = getTableView().getItems().get(getIndex());
                    openRescheduleDialog(item);
                });

                btnComplete.setOnAction(e -> {
                    AppointmentResponseDto item = getTableView().getItems().get(getIndex());
                    handleCompleteAppointment(item);
                });

                btnCancel.setOnAction(e -> {
                    AppointmentResponseDto item = getTableView().getItems().get(getIndex());
                    handleCancelAppointment(item);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    AppointmentResponseDto app = getTableView().getItems().get(getIndex());
                    boolean isFinalized = app.getStatus() == AppointmentStatus.ATENDIDA || app.getStatus() == AppointmentStatus.CANCELADA;
                    btnReschedule.setDisable(isFinalized);
                    btnComplete.setDisable(isFinalized);
                    btnCancel.setDisable(isFinalized);
                    setGraphic(container);
                }
            }
        });
    }

    private void setupEmptyPlaceholder() {
        VBox placeholder = new VBox(8);
        placeholder.setAlignment(Pos.CENTER);
        FontIcon icon = new FontIcon("feather-calendar");
        icon.setIconSize(36);
        icon.setStyle("-fx-icon-color: #94A3B8;");
        Label label = new Label("No hay citas m?dicas programadas para el per?odo seleccionado");
        label.setStyle("-fx-text-fill: #64748B; -fx-font-size: 13px;");
        placeholder.getChildren().addAll(icon, label);
        appointmentsTable.setPlaceholder(placeholder);
    }

    private void updatePeriodLabel() {
        DateTimeFormatter fullDateFmt = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM yyyy");
        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy");
        DateTimeFormatter dayMonthFmt = DateTimeFormatter.ofPattern("d 'de' MMMM");

        switch (currentMode) {
            case DAY -> currentPeriodLabel.setText(capitalize(currentDate.format(fullDateFmt)));
            case WEEK -> {
                LocalDate startWeek = currentDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                LocalDate endWeek = currentDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
                currentPeriodLabel.setText("Semana del " + startWeek.format(dayMonthFmt) + " al " + endWeek.format(dayMonthFmt) + " de " + endWeek.getYear());
            }
            case MONTH -> currentPeriodLabel.setText(capitalize(currentDate.format(monthFmt)));
        }
    }

    public void loadAppointmentsAsync() {
        updatePeriodLabel();
        loadingIndicator.setVisible(true);

        LocalDateTime startRange;
        LocalDateTime endRange;

        switch (currentMode) {
            case DAY -> {
                startRange = currentDate.atStartOfDay();
                endRange = currentDate.atTime(LocalTime.MAX);
            }
            case WEEK -> {
                LocalDate startWeek = currentDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                LocalDate endWeek = currentDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
                startRange = startWeek.atStartOfDay();
                endRange = endWeek.atTime(LocalTime.MAX);
            }
            case MONTH -> {
                LocalDate startMonth = currentDate.withDayOfMonth(1);
                LocalDate endMonth = currentDate.withDayOfMonth(currentDate.lengthOfMonth());
                startRange = startMonth.atStartOfDay();
                endRange = endMonth.atTime(LocalTime.MAX);
            }
            default -> {
                startRange = currentDate.atStartOfDay();
                endRange = currentDate.atTime(LocalTime.MAX);
            }
        }

        String filter = statusFilterComboBox.getValue();

        Task<List<AppointmentResponseDto>> task = new Task<>() {
            @Override
            protected List<AppointmentResponseDto> call() {
                List<AppointmentResponseDto> list = appointmentService.getAppointmentsByDateRange(startRange, endRange);
                if (filter != null && !filter.equalsIgnoreCase("Todos los Estados")) {
                    return list.stream()
                            .filter(a -> a.getStatus().name().equalsIgnoreCase(filter))
                            .toList();
                }
                return list;
            }
        };

        task.setOnSucceeded(e -> {
            loadingIndicator.setVisible(false);
            List<AppointmentResponseDto> result = task.getValue();
            appointmentsList.setAll(result);
            appointmentCountLabel.setText("Total: " + result.size() + " cita" + (result.size() == 1 ? "" : "s"));
        });

        task.setOnFailed(e -> {
            loadingIndicator.setVisible(false);
            log.error("Error al cargar citas de agenda", task.getException());
            notificationService.showError("Error", "No fue posible cargar las citas de la agenda");
        });

        new Thread(task).start();
    }

    @FXML
    public void handlePrevious(ActionEvent event) {
        switch (currentMode) {
            case DAY -> currentDate = currentDate.minusDays(1);
            case WEEK -> currentDate = currentDate.minusWeeks(1);
            case MONTH -> currentDate = currentDate.minusMonths(1);
        }
        datePickerJump.setValue(currentDate);
        loadAppointmentsAsync();
    }

    @FXML
    public void handleToday(ActionEvent event) {
        currentDate = LocalDate.now();
        datePickerJump.setValue(currentDate);
        loadAppointmentsAsync();
    }

    @FXML
    public void handleNext(ActionEvent event) {
        switch (currentMode) {
            case DAY -> currentDate = currentDate.plusDays(1);
            case WEEK -> currentDate = currentDate.plusWeeks(1);
            case MONTH -> currentDate = currentDate.plusMonths(1);
        }
        datePickerJump.setValue(currentDate);
        loadAppointmentsAsync();
    }

    @FXML
    public void handleNewAppointment(ActionEvent event) {
        try {
            FXMLLoader loader = springFXMLLoader.createLoader("/fxml/appointment-dialog.fxml");
            Parent root = loader.load();

            AppointmentDialogController dialogCtrl = loader.getController();

            Stage stage = new Stage();
            stage.setTitle("Agendar Cita M?dica - CECAMed");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(appointmentsTable.getScene().getWindow());

            Scene scene = new Scene(root);
            themeManager.registerScene(scene);
            stage.setScene(scene);

            dialogCtrl.setDialogStage(stage);
            dialogCtrl.initCreateMode(currentDate, null);

            stage.showAndWait();

            if (dialogCtrl.isSavedSuccessfully()) {
                loadAppointmentsAsync();
            }
        } catch (IOException ex) {
            log.error("Error al abrir di?logo de citas", ex);
            notificationService.showError("Error", "No fue posible abrir el di?logo de nueva cita");
        }
    }

    private void openRescheduleDialog(AppointmentResponseDto appointment) {
        try {
            FXMLLoader loader = springFXMLLoader.createLoader("/fxml/appointment-dialog.fxml");
            Parent root = loader.load();

            AppointmentDialogController dialogCtrl = loader.getController();

            Stage stage = new Stage();
            stage.setTitle("Reprogramar Cita - CECAMed");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(appointmentsTable.getScene().getWindow());

            Scene scene = new Scene(root);
            themeManager.registerScene(scene);
            stage.setScene(scene);

            dialogCtrl.setDialogStage(stage);
            dialogCtrl.initRescheduleMode(appointment);

            stage.showAndWait();

            if (dialogCtrl.isSavedSuccessfully()) {
                loadAppointmentsAsync();
            }
        } catch (IOException ex) {
            log.error("Error al abrir di?logo de reprogramaci?n", ex);
            notificationService.showError("Error", "No se pudo abrir el di?logo para reprogramar la cita");
        }
    }

    private void handleCompleteAppointment(AppointmentResponseDto appointment) {
        boolean confirmed = ConfirmationDialog.confirm(
                "Finalizar Atenci?n M?dica",
                "?Desea marcar como ATENDIDA la cita?",
                "Paciente: " + appointment.getPatientFullName() + "\nHora: " + appointment.getStartTime().toLocalTime()
        );

        if (!confirmed) return;

        Task<AppointmentResponseDto> task = new Task<>() {
            @Override
            protected AppointmentResponseDto call() {
                return appointmentService.completeAppointment(appointment.getId());
            }
        };

        task.setOnSucceeded(e -> {
            notificationService.showSuccess("Cita Atendida", "La cita fue marcada como ATENDIDA exitosamente");
            loadAppointmentsAsync();
        });

        task.setOnFailed(e -> {
            log.error("Error al completar cita", task.getException());
            notificationService.showError("Error", "No se pudo marcar la cita como atendida");
        });

        new Thread(task).start();
    }

    private void handleCancelAppointment(AppointmentResponseDto appointment) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Cancelar Cita M?dica");
        dialog.setHeaderText("Cancelaci?n de cita para " + appointment.getPatientFullName());
        dialog.setContentText("Motivo de la cancelaci?n *:");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty() || result.get().isBlank()) {
            return;
        }

        String reason = result.get().trim();

        Task<AppointmentResponseDto> task = new Task<>() {
            @Override
            protected AppointmentResponseDto call() {
                return appointmentService.cancelAppointment(appointment.getId(), reason);
            }
        };

        task.setOnSucceeded(e -> {
            notificationService.showSuccess("Cita Cancelada", "La cita ha sido cancelada correctamente");
            loadAppointmentsAsync();
        });

        task.setOnFailed(e -> {
            log.error("Error al cancelar cita", task.getException());
            notificationService.showError("Error", "No fue posible cancelar la cita: " + task.getException().getMessage());
        });

        new Thread(task).start();
    }

    private void openMedicalRecord(Long patientId) {
        if (userSession.isRecepcion()) {
            notificationService.showWarning("Acceso Restringido",
                    "El perfil de Recepci?n no tiene permisos para acceder al expediente cl?nico detallado");
            return;
        }
        navigationService.setParameter("patientId", patientId);
        navigationService.navigateTo(ViewType.MEDICAL_RECORD);
    }

    @FXML
    public void handleRefresh(ActionEvent event) {
        loadAppointmentsAsync();
    }

    private String capitalize(String text) {
        if (text == null || text.isBlank()) return "";
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }
}
