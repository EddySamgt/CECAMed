package com.cecamed.ui.controller;

import com.cecamed.core.model.appointment.enums.BlockType;
import com.cecamed.services.dto.appointment.DoctorScheduleDto;
import com.cecamed.services.dto.appointment.ScheduleBlockRequestDto;
import com.cecamed.services.dto.appointment.ScheduleBlockResponseDto;
import com.cecamed.services.service.DoctorScheduleService;
import com.cecamed.ui.component.ConfirmationDialog;
import com.cecamed.ui.component.NotificationService;
import com.cecamed.ui.session.UserSession;
import com.cecamed.ui.theme.ThemeManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

@Slf4j
@Component
@RequiredArgsConstructor
public class SettingsController implements Initializable {

    private final ThemeManager themeManager;
    private final DoctorScheduleService doctorScheduleService;
    private final NotificationService notificationService;
    private final UserSession userSession;

    @Value("${cecamed.google.calendar.calendar-id:primary}")
    private String googleCalendarId;

    @Value("${cecamed.google.calendar.enabled:false}")
    private boolean googleCalendarEnabled;

    // Tab Apariencia
    @FXML private RadioButton radioLightMode;
    @FXML private RadioButton radioDarkMode;
    @FXML private ToggleGroup themeGroup;

    // Tab Horarios M?dicos
    @FXML private TableView<DoctorScheduleDto> schedulesTable;
    @FXML private TableColumn<DoctorScheduleDto, String> colDayOfWeek;
    @FXML private TableColumn<DoctorScheduleDto, String> colStartTime;
    @FXML private TableColumn<DoctorScheduleDto, String> colEndTime;
    @FXML private TableColumn<DoctorScheduleDto, String> colDuration;
    @FXML private TableColumn<DoctorScheduleDto, Boolean> colActive;

    // Tab Bloqueos de Agenda
    @FXML private TableView<ScheduleBlockResponseDto> blocksTable;
    @FXML private TableColumn<ScheduleBlockResponseDto, String> colBlockTitle;
    @FXML private TableColumn<ScheduleBlockResponseDto, BlockType> colBlockType;
    @FXML private TableColumn<ScheduleBlockResponseDto, String> colBlockStart;
    @FXML private TableColumn<ScheduleBlockResponseDto, String> colBlockEnd;
    @FXML private TableColumn<ScheduleBlockResponseDto, Void> colBlockActions;

    // Formulario Nuevo Bloqueo
    @FXML private TextField blockTitleField;
    @FXML private ComboBox<BlockType> blockTypeComboBox;
    @FXML private DatePicker blockStartDatePicker;
    @FXML private TextField blockStartTimeField;
    @FXML private DatePicker blockEndDatePicker;
    @FXML private TextField blockEndTimeField;
    @FXML private TextField blockNotesField;
    @FXML private Button btnCreateBlock;

    // Tab Google Calendar & Info
    @FXML private Label labelGoogleCalendarId;
    @FXML private Label labelGoogleSyncStatus;
    @FXML private Label labelJavaVersion;
    @FXML private Label labelJavaFxVersion;
    @FXML private Label labelSpringVersion;
    @FXML private Label labelCurrentUser;
    @FXML private Label labelCurrentRole;

    private final ObservableList<DoctorScheduleDto> schedulesList = FXCollections.observableArrayList();
    private final ObservableList<ScheduleBlockResponseDto> blocksList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupThemeTab();
        setupSchedulesTab();
        setupBlocksTab();
        setupSystemInfoTab();

        loadSchedulesAsync();
        loadBlocksAsync();
    }

    private void setupThemeTab() {
        if (themeManager.isDarkMode()) {
            radioDarkMode.setSelected(true);
        } else {
            radioLightMode.setSelected(true);
        }

        themeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            boolean dark = newVal == radioDarkMode;
            if (themeManager.isDarkMode() != dark) {
                themeManager.setDarkMode(dark);
                notificationService.showInfo("Tema Actualizado", "Se ha aplicado el modo " + (dark ? "Oscuro" : "Claro"));
            }
        });
    }

    private void setupSchedulesTab() {
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");

        colDayOfWeek.setCellValueFactory(data -> new SimpleStringProperty(formatDayOfWeek(data.getValue().getDayOfWeek())));
        colStartTime.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getStartTime() != null ? data.getValue().getStartTime().format(timeFmt) : "--:--"
        ));
        colEndTime.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getEndTime() != null ? data.getValue().getEndTime().format(timeFmt) : "--:--"
        ));
        colDuration.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getSlotDurationMinutes() != null ? data.getValue().getSlotDurationMinutes() + " min" : "--"
        ));

        colActive.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getIsActive()));
        colActive.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean active, boolean empty) {
                super.updateItem(active, empty);
                if (empty || active == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(active ? "Laborable" : "No laborable");
                    badge.getStyleClass().addAll("status-badge", active ? "badge-success" : "badge-secondary");
                    setGraphic(badge);
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                }
            }
        });

        schedulesTable.setItems(schedulesList);
    }

    private void setupBlocksTab() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        colBlockTitle.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitle()));
        colBlockType.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getBlockType()));
        colBlockStart.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getStartDateTime() != null ? data.getValue().getStartDateTime().format(dtf) : "--"
        ));
        colBlockEnd.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getEndDateTime() != null ? data.getValue().getEndDateTime().format(dtf) : "--"
        ));

        colBlockActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnDelete = new Button();
            {
                btnDelete.getStyleClass().add("ghost-button");
                btnDelete.setTooltip(new Tooltip("Eliminar Bloqueo"));
                FontIcon icon = new FontIcon("feather-trash-2");
                icon.setIconSize(14);
                icon.setIconColor(javafx.scene.paint.Color.web("#D32F2F"));
                btnDelete.setGraphic(icon);

                btnDelete.setOnAction(e -> {
                    ScheduleBlockResponseDto item = getTableView().getItems().get(getIndex());
                    handleDeleteBlock(item);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnDelete);
            }
        });

        blocksTable.setItems(blocksList);

        blockTypeComboBox.setItems(FXCollections.observableArrayList(BlockType.values()));
        blockTypeComboBox.setValue(BlockType.PERSONAL);
        blockStartDatePicker.setValue(LocalDate.now());
        blockStartTimeField.setText("12:00");
        blockEndDatePicker.setValue(LocalDate.now());
        blockEndTimeField.setText("13:00");
    }

    private void setupSystemInfoTab() {
        labelGoogleCalendarId.setText(googleCalendarId != null ? googleCalendarId : "No configurado");
        labelGoogleSyncStatus.setText(googleCalendarEnabled ? "Habilitado (Activo)" : "Deshabilitado (Simulaci?n en local)");

        labelJavaVersion.setText(System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")");
        labelJavaFxVersion.setText("21.0.4 + AtlantaFX 2.0.1");
        labelSpringVersion.setText("Spring Boot 3.3.4");

        if (userSession.isAuthenticated()) {
            labelCurrentUser.setText(userSession.getFullName() + " (" + userSession.getUsername() + ")");
            labelCurrentRole.setText(userSession.getRole().getDisplayName());
        } else {
            labelCurrentUser.setText("Usuario por defecto");
            labelCurrentRole.setText("M?dico");
        }
    }

    private void loadSchedulesAsync() {
        Task<List<DoctorScheduleDto>> task = new Task<>() {
            @Override
            protected List<DoctorScheduleDto> call() {
                return doctorScheduleService.getAllActiveSchedules();
            }
        };

        task.setOnSucceeded(e -> schedulesList.setAll(task.getValue()));
        task.setOnFailed(e -> log.error("Error al consultar horarios de doctor", task.getException()));
        new Thread(task).start();
    }

    private void loadBlocksAsync() {
        LocalDate today = LocalDate.now();
        LocalDateTime startRange = today.minusMonths(1).atStartOfDay();
        LocalDateTime endRange = today.plusMonths(6).atTime(LocalTime.MAX);

        Task<List<ScheduleBlockResponseDto>> task = new Task<>() {
            @Override
            protected List<ScheduleBlockResponseDto> call() {
                return doctorScheduleService.getBlocksBetween(startRange, endRange);
            }
        };

        task.setOnSucceeded(e -> blocksList.setAll(task.getValue()));
        task.setOnFailed(e -> log.error("Error al consultar bloqueos de agenda", task.getException()));
        new Thread(task).start();
    }

    @FXML
    public void handleCreateBlock(ActionEvent event) {
        String title = blockTitleField.getText();
        if (title == null || title.isBlank()) {
            notificationService.showWarning("Campo Requerido", "Ingrese un t?tulo descriptivo para el bloqueo");
            blockTitleField.requestFocus();
            return;
        }

        LocalDate startDate = blockStartDatePicker.getValue();
        LocalDate endDate = blockEndDatePicker.getValue();
        if (startDate == null || endDate == null) {
            notificationService.showWarning("Fechas Requeridas", "Seleccione fecha de inicio y fin");
            return;
        }

        LocalTime startTime = parseTime(blockStartTimeField.getText(), LocalTime.of(8, 0));
        LocalTime endTime = parseTime(blockEndTimeField.getText(), LocalTime.of(17, 0));

        LocalDateTime start = LocalDateTime.of(startDate, startTime);
        LocalDateTime end = LocalDateTime.of(endDate, endTime);

        if (!start.isBefore(end)) {
            notificationService.showWarning("Rango Inv?lido", "La fecha y hora de inicio debe ser anterior a la de fin");
            return;
        }

        ScheduleBlockRequestDto dto = ScheduleBlockRequestDto.builder()
                .title(title.trim())
                .blockType(blockTypeComboBox.getValue())
                .startDateTime(start)
                .endDateTime(end)
                .reason(blockNotesField.getText() != null ? blockNotesField.getText().trim() : null)
                .build();

        btnCreateBlock.setDisable(true);

        Task<ScheduleBlockResponseDto> task = new Task<>() {
            @Override
            protected ScheduleBlockResponseDto call() {
                return doctorScheduleService.createScheduleBlock(dto);
            }
        };

        task.setOnSucceeded(e -> {
            btnCreateBlock.setDisable(false);
            notificationService.showSuccess("Bloqueo Creado", "El bloqueo de agenda fue registrado con ?xito");
            blockTitleField.clear();
            blockNotesField.clear();
            loadBlocksAsync();
        });

        task.setOnFailed(e -> {
            btnCreateBlock.setDisable(false);
            log.error("Error al crear bloqueo", task.getException());
            notificationService.showError("Error", "No se pudo crear el bloqueo: " + task.getException().getMessage());
        });

        new Thread(task).start();
    }

    private void handleDeleteBlock(ScheduleBlockResponseDto block) {
        boolean confirmed = ConfirmationDialog.confirm(
                "Eliminar Bloqueo",
                "?Desea eliminar este bloqueo de agenda?",
                "Bloqueo: " + block.getTitle() + " (" + block.getBlockType() + ")"
        );
        if (!confirmed) return;

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                doctorScheduleService.deleteScheduleBlock(block.getId());
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            notificationService.showSuccess("Bloqueo Eliminado", "El bloqueo fue removido correctamente");
            loadBlocksAsync();
        });

        task.setOnFailed(e -> {
            log.error("Error al eliminar bloqueo", task.getException());
            notificationService.showError("Error", "No fue posible eliminar el bloqueo");
        });

        new Thread(task).start();
    }

    private LocalTime parseTime(String text, LocalTime fallback) {
        if (text == null || text.isBlank()) return fallback;
        try {
            return LocalTime.parse(text.trim(), DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception e) {
            return fallback;
        }
    }

    private String formatDayOfWeek(DayOfWeek day) {
        if (day == null) return "--";
        return switch (day) {
            case MONDAY -> "Lunes";
            case TUESDAY -> "Martes";
            case WEDNESDAY -> "Mi?rcoles";
            case THURSDAY -> "Jueves";
            case FRIDAY -> "Viernes";
            case SATURDAY -> "S?bado";
            case SUNDAY -> "Domingo";
        };
    }
}
