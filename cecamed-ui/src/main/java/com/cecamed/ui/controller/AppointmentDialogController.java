package com.cecamed.ui.controller;

import com.cecamed.services.dto.appointment.AppointmentRequestDto;
import com.cecamed.services.dto.appointment.AppointmentRescheduleDto;
import com.cecamed.services.dto.appointment.AppointmentResponseDto;
import com.cecamed.services.dto.appointment.AvailableSlotDto;
import com.cecamed.services.dto.patient.PatientResponseDto;
import com.cecamed.services.exception.BusinessRuleException;
import com.cecamed.services.service.AppointmentService;
import com.cecamed.services.service.PatientService;
import com.cecamed.ui.component.NotificationService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentDialogController implements Initializable {

    private final AppointmentService appointmentService;
    private final PatientService patientService;
    private final NotificationService notificationService;

    @FXML private Label dialogTitleLabel;
    @FXML private Label dialogSubtitleLabel;

    @FXML private ComboBox<PatientResponseDto> patientComboBox;
    @FXML private DatePicker appointmentDatePicker;
    @FXML private ComboBox<AvailableSlotDto> slotComboBox;
    @FXML private TextField reasonField;
    @FXML private TextArea notesArea;

    @FXML private Label rescheduleReasonLabel;
    @FXML private TextField rescheduleReasonField;

    @FXML private Label errorLabel;
    @FXML private Button saveButton;
    @FXML private ProgressIndicator progressIndicator;

    private Stage dialogStage;
    private boolean savedSuccessfully = false;
    private Long reschedulingAppointmentId = null;

    private final ObservableList<PatientResponseDto> patientList = FXCollections.observableArrayList();
    private final ObservableList<AvailableSlotDto> availableSlots = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupPatientComboBox();
        setupSlotComboBox();
        setupDatePicker();

        errorLabel.setVisible(false);
        progressIndicator.setVisible(false);
        rescheduleReasonLabel.setVisible(false);
        rescheduleReasonLabel.setManaged(false);
        rescheduleReasonField.setVisible(false);
        rescheduleReasonField.setManaged(false);
    }

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    public boolean isSavedSuccessfully() {
        return savedSuccessfully;
    }

    private void setupPatientComboBox() {
        patientComboBox.setItems(patientList);
        patientComboBox.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(PatientResponseDto item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getFullName() + " (" + item.getIdentificationNumber() + ")");
            }
        });
        patientComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(PatientResponseDto item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getFullName() + " (" + item.getIdentificationNumber() + ")");
            }
        });
    }

    private void setupSlotComboBox() {
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        slotComboBox.setItems(availableSlots);
        slotComboBox.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(AvailableSlotDto slot, boolean empty) {
                super.updateItem(slot, empty);
                if (empty || slot == null) {
                    setText(null);
                    setDisable(false);
                } else {
                    String timeRange = slot.getStartTime().format(timeFormatter) + " - " + slot.getEndTime().format(timeFormatter);
                    if (slot.isAvailable()) {
                        setText(timeRange + " (Disponible)");
                        setDisable(false);
                        setStyle("-fx-text-fill: #16A34A;");
                    } else {
                        setText(timeRange + " (Ocupado: " + (slot.getReasonIfNotAvailable() != null ? slot.getReasonIfNotAvailable() : "No disponible") + ")");
                        setDisable(true);
                        setStyle("-fx-text-fill: #94A3B8;");
                    }
                }
            }
        });

        slotComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(AvailableSlotDto slot, boolean empty) {
                super.updateItem(slot, empty);
                if (empty || slot == null) {
                    setText(null);
                } else {
                    String timeRange = slot.getStartTime().format(timeFormatter) + " - " + slot.getEndTime().format(timeFormatter);
                    setText(timeRange + (slot.isAvailable() ? " [Disponible]" : " [No Disponible]"));
                }
            }
        });
    }

    private void setupDatePicker() {
        appointmentDatePicker.setValue(LocalDate.now());
        appointmentDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadAvailableSlots(newVal);
            }
        });
    }

    public void initCreateMode(LocalDate preselectedDate, Long preselectedPatientId) {
        this.reschedulingAppointmentId = null;
        dialogTitleLabel.setText("Agendar Nueva Cita M?dica");
        dialogSubtitleLabel.setText("Seleccione el paciente, fecha y horario disponible");
        saveButton.setText("Agendar Cita");

        rescheduleReasonLabel.setVisible(false);
        rescheduleReasonLabel.setManaged(false);
        rescheduleReasonField.setVisible(false);
        rescheduleReasonField.setManaged(false);
        patientComboBox.setDisable(false);

        loadPatients(preselectedPatientId);

        LocalDate dateToUse = preselectedDate != null ? preselectedDate : LocalDate.now();
        appointmentDatePicker.setValue(dateToUse);
        loadAvailableSlots(dateToUse);
    }

    public void initRescheduleMode(AppointmentResponseDto appointment) {
        this.reschedulingAppointmentId = appointment.getId();
        dialogTitleLabel.setText("Reprogramar Cita M?dica");
        dialogSubtitleLabel.setText("Seleccione el nuevo d?a y horario para el paciente " + appointment.getPatientFullName());
        saveButton.setText("Confirmar Reprogramaci?n");

        rescheduleReasonLabel.setVisible(true);
        rescheduleReasonLabel.setManaged(true);
        rescheduleReasonField.setVisible(true);
        rescheduleReasonField.setManaged(true);

        patientComboBox.setDisable(true);
        loadPatients(appointment.getPatientId());

        reasonField.setText(appointment.getReasonForVisit());
        notesArea.setText(appointment.getNotes());

        LocalDate appointmentDate = appointment.getStartTime().toLocalDate();
        appointmentDatePicker.setValue(appointmentDate);
        loadAvailableSlots(appointmentDate);
    }

    private void loadPatients(Long selectPatientId) {
        Task<List<PatientResponseDto>> task = new Task<>() {
            @Override
            protected List<PatientResponseDto> call() {
                return patientService.getAllActivePatients();
            }
        };

        task.setOnSucceeded(e -> {
            patientList.setAll(task.getValue());
            if (selectPatientId != null) {
                patientList.stream()
                        .filter(p -> p.getId().equals(selectPatientId))
                        .findFirst()
                        .ifPresent(patientComboBox::setValue);
            } else if (!patientList.isEmpty()) {
                patientComboBox.setValue(patientList.get(0));
            }
        });

        new Thread(task).start();
    }

    private void loadAvailableSlots(LocalDate date) {
        progressIndicator.setVisible(true);

        Task<List<AvailableSlotDto>> task = new Task<>() {
            @Override
            protected List<AvailableSlotDto> call() {
                return appointmentService.getAvailableSlotsForDate(date);
            }
        };

        task.setOnSucceeded(e -> {
            progressIndicator.setVisible(false);
            List<AvailableSlotDto> slots = task.getValue();
            availableSlots.setAll(slots);

            // Seleccionar primer slot disponible si existe
            slots.stream()
                    .filter(AvailableSlotDto::isAvailable)
                    .findFirst()
                    .ifPresent(slotComboBox::setValue);
        });

        task.setOnFailed(e -> {
            progressIndicator.setVisible(false);
            log.error("Error al consultar horarios disponibles", task.getException());
            showError("No fue posible cargar los horarios para la fecha seleccionada");
        });

        new Thread(task).start();
    }

    @FXML
    public void handleSave(ActionEvent event) {
        if (!validateInputs()) {
            return;
        }

        PatientResponseDto selectedPatient = patientComboBox.getValue();
        AvailableSlotDto selectedSlot = slotComboBox.getValue();
        String reason = reasonField.getText().trim();
        String notes = notesArea.getText() != null ? notesArea.getText().trim() : null;

        setLoading(true);
        errorLabel.setVisible(false);

        if (reschedulingAppointmentId == null) {
            // Modo Creaci?n
            AppointmentRequestDto requestDto = AppointmentRequestDto.builder()
                    .patientId(selectedPatient.getId())
                    .startTime(selectedSlot.getStartTime())
                    .endTime(selectedSlot.getEndTime())
                    .reasonForVisit(reason)
                    .notes(notes)
                    .build();

            Task<AppointmentResponseDto> task = new Task<>() {
                @Override
                protected AppointmentResponseDto call() {
                    return appointmentService.createAppointment(requestDto);
                }
            };

            task.setOnSucceeded(e -> {
                setLoading(false);
                savedSuccessfully = true;
                AppointmentResponseDto created = task.getValue();
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                notificationService.showSuccess("Cita Agendada",
                        "Cita confirmada para " + created.getPatientFullName() + " el " + created.getStartTime().format(dtf));
                if (dialogStage != null) dialogStage.close();
            });

            task.setOnFailed(e -> {
                setLoading(false);
                handleTaskFailure(task.getException());
            });

            new Thread(task).start();

        } else {
            // Modo Reprogramaci?n
            String rescheduleReason = rescheduleReasonField.getText() != null ? rescheduleReasonField.getText().trim() : "";
            AppointmentRescheduleDto rescheduleDto = AppointmentRescheduleDto.builder()
                    .newStartTime(selectedSlot.getStartTime())
                    .newEndTime(selectedSlot.getEndTime())
                    .reason(rescheduleReason)
                    .build();

            Task<AppointmentResponseDto> task = new Task<>() {
                @Override
                protected AppointmentResponseDto call() {
                    return appointmentService.rescheduleAppointment(reschedulingAppointmentId, rescheduleDto);
                }
            };

            task.setOnSucceeded(e -> {
                setLoading(false);
                savedSuccessfully = true;
                AppointmentResponseDto updated = task.getValue();
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                notificationService.showSuccess("Cita Reprogramada",
                        "La cita fue reprogramada con ?xito para el " + updated.getStartTime().format(dtf));
                if (dialogStage != null) dialogStage.close();
            });

            task.setOnFailed(e -> {
                setLoading(false);
                handleTaskFailure(task.getException());
            });

            new Thread(task).start();
        }
    }

    private void handleTaskFailure(Throwable ex) {
        log.error("Error al procesar cita medica", ex);
        if (ex instanceof BusinessRuleException) {
            showError(ex.getMessage());
        } else {
            showError("No fue posible agendar la cita: " + (ex.getMessage() != null ? ex.getMessage() : "Error interno"));
        }
    }

    @FXML
    public void handleCancel(ActionEvent event) {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    private boolean validateInputs() {
        if (patientComboBox.getValue() == null) {
            showError("Debe seleccionar un paciente para la cita");
            patientComboBox.requestFocus();
            return false;
        }
        if (appointmentDatePicker.getValue() == null) {
            showError("Debe seleccionar una fecha para la cita");
            appointmentDatePicker.requestFocus();
            return false;
        }
        if (slotComboBox.getValue() == null) {
            showError("Debe seleccionar un horario disponible");
            slotComboBox.requestFocus();
            return false;
        }
        if (!slotComboBox.getValue().isAvailable()) {
            showError("El horario seleccionado est? ocupado o bloqueado. Elija otro horario.");
            slotComboBox.requestFocus();
            return false;
        }
        if (reasonField.getText() == null || reasonField.getText().isBlank()) {
            showError("El motivo de la consulta es obligatorio");
            reasonField.requestFocus();
            return false;
        }
        if (reschedulingAppointmentId != null && (rescheduleReasonField.getText() == null || rescheduleReasonField.getText().isBlank())) {
            showError("Indique el motivo por el cual se reprograma la cita");
            rescheduleReasonField.requestFocus();
            return false;
        }
        return true;
    }

    private void setLoading(boolean loading) {
        progressIndicator.setVisible(loading);
        saveButton.setDisable(loading);
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }
}
