package com.cecamed.ui.controller;

import com.cecamed.core.model.appointment.enums.AppointmentStatus;
import com.cecamed.core.model.patient.enums.BloodType;
import com.cecamed.core.model.patient.enums.Gender;
import com.cecamed.services.dto.appointment.AppointmentResponseDto;
import com.cecamed.services.dto.consultation.MedicalConsultationRequestDto;
import com.cecamed.services.dto.consultation.MedicalConsultationResponseDto;
import com.cecamed.services.dto.consultation.VitalSignsDto;
import com.cecamed.services.dto.patient.MedicalRecordDto;
import com.cecamed.services.dto.patient.PatientResponseDto;
import com.cecamed.services.service.AppointmentService;
import com.cecamed.services.service.MedicalConsultationService;
import com.cecamed.services.service.MedicalRecordService;
import com.cecamed.services.service.PatientService;
import com.cecamed.ui.component.NotificationService;
import com.cecamed.ui.component.StatusBadge;
import com.cecamed.ui.navigation.NavigationService;
import com.cecamed.ui.navigation.ViewType;
import com.cecamed.ui.session.UserSession;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
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
public class MedicalRecordController implements Initializable {

    private final PatientService patientService;
    private final MedicalRecordService medicalRecordService;
    private final MedicalConsultationService consultationService;
    private final AppointmentService appointmentService;
    private final NavigationService navigationService;
    private final NotificationService notificationService;
    private final UserSession userSession;

    // Header Patient Info
    @FXML private Label patientNameLabel;
    @FXML private Label patientDniLabel;
    @FXML private Label patientAgeLabel;
    @FXML private Label patientRecordNumberLabel;
    @FXML private Label bloodTypeBadge;
    @FXML private Label allergiesSummaryLabel;
    @FXML private ComboBox<PatientResponseDto> patientSelectorComboBox;

    // TabPane
    @FXML private TabPane recordTabPane;
    @FXML private Tab tabNewConsultation;

    // Tab 1: Antecedentes
    @FXML private TextArea allergiesArea;
    @FXML private TextArea currentMedicationsArea;
    @FXML private TextArea pathologicalHistoryArea;
    @FXML private TextArea nonPathologicalHistoryArea;
    @FXML private TextArea familyHistoryArea;
    @FXML private TextArea surgicalHistoryArea;
    @FXML private TextArea generalObservationsArea;
    @FXML private Button saveRecordButton;

    // Tab 2: Historial Consultas
    @FXML private TableView<MedicalConsultationResponseDto> consultationsTable;
    @FXML private TableColumn<MedicalConsultationResponseDto, String> colConsultationDate;
    @FXML private TableColumn<MedicalConsultationResponseDto, String> colConsultationReason;
    @FXML private TableColumn<MedicalConsultationResponseDto, String> colConsultationDiagnosis;
    @FXML private TableColumn<MedicalConsultationResponseDto, String> colConsultationIcd10;

    // Detalle de consulta seleccionada
    @FXML private VBox consultationDetailPane;
    @FXML private Label detailDateLabel;
    @FXML private Label detailReasonLabel;
    @FXML private Label detailDiagnosisLabel;
    @FXML private Label detailTreatmentPlanLabel;
    @FXML private Label detailBloodPressureLabel;
    @FXML private Label detailHeartRateLabel;
    @FXML private Label detailTemperatureLabel;
    @FXML private Label detailRespiratoryRateLabel;
    @FXML private Label detailWeightLabel;
    @FXML private Label detailHeightLabel;
    @FXML private Label detailBmiLabel;
    @FXML private Label detailSpo2Label;
    @FXML private Label detailPhysicalExamLabel;

    // Tab 3: Nueva Consulta
    @FXML private DatePicker newConsultationDatePicker;
    @FXML private TextField newConsultationReasonField;
    @FXML private TextArea newConsultationSymptomsArea;
    @FXML private TextArea newConsultationPhysicalExamArea;
    @FXML private TextField vitalSystolicField;
    @FXML private TextField vitalDiastolicField;
    @FXML private TextField vitalHeartRateField;
    @FXML private TextField vitalRespiratoryRateField;
    @FXML private TextField vitalTempField;
    @FXML private TextField vitalWeightField;
    @FXML private TextField vitalHeightField;
    @FXML private TextField vitalSpo2Field;
    @FXML private TextArea newConsultationDiagnosisArea;
    @FXML private TextField newConsultationIcd10Field;
    @FXML private TextArea newConsultationTreatmentArea;
    @FXML private TextArea newConsultationPrivateNotesArea;
    @FXML private Button saveConsultationButton;

    // Tab 4: Historial de Citas
    @FXML private TableView<AppointmentResponseDto> patientAppointmentsTable;
    @FXML private TableColumn<AppointmentResponseDto, String> colAppDate;
    @FXML private TableColumn<AppointmentResponseDto, String> colAppTime;
    @FXML private TableColumn<AppointmentResponseDto, String> colAppReason;
    @FXML private TableColumn<AppointmentResponseDto, AppointmentStatus> colAppStatus;

    private Long currentPatientId = null;
    private PatientResponseDto currentPatient = null;
    private MedicalRecordDto currentMedicalRecord = null;

    private final ObservableList<MedicalConsultationResponseDto> consultationsList = FXCollections.observableArrayList();
    private final ObservableList<AppointmentResponseDto> appointmentsList = FXCollections.observableArrayList();
    private final ObservableList<PatientResponseDto> patientOptions = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupConsultationTable();
        setupAppointmentsTable();
        setupPatientSelector();

        if (userSession.isRecepcion()) {
            tabNewConsultation.setDisable(true);
            saveRecordButton.setDisable(true);
        }

        Object passedPatientId = navigationService.getParameter("patientId");
        if (passedPatientId instanceof Long pId) {
            loadPatientRecord(pId);
        } else {
            loadDefaultFirstPatient();
        }
    }

    private void setupPatientSelector() {
        patientSelectorComboBox.setItems(patientOptions);
        patientSelectorComboBox.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(PatientResponseDto item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getFullName() + " (" + item.getIdentificationNumber() + ")");
            }
        });
        patientSelectorComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(PatientResponseDto item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getFullName() + " (" + item.getIdentificationNumber() + ")");
            }
        });

        patientSelectorComboBox.setOnAction(e -> {
            PatientResponseDto selected = patientSelectorComboBox.getValue();
            if (selected != null && (currentPatientId == null || !currentPatientId.equals(selected.getId()))) {
                loadPatientRecord(selected.getId());
            }
        });
    }

    private void setupConsultationTable() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        colConsultationDate.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getConsultationDateTime() != null ? data.getValue().getConsultationDateTime().format(dtf) : "-"
        ));
        colConsultationReason.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getReason()));
        colConsultationDiagnosis.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDiagnosis()));
        colConsultationIcd10.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getIcd10Code() != null ? data.getValue().getIcd10Code() : "-"
        ));

        consultationsTable.setItems(consultationsList);
        consultationsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            displayConsultationDetail(newVal);
        });
    }

    private void setupAppointmentsTable() {
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");

        colAppDate.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getStartTime() != null ? data.getValue().getStartTime().format(dateFmt) : "-"
        ));
        colAppTime.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getStartTime() != null ? data.getValue().getStartTime().format(timeFmt) : "-"
        ));
        colAppReason.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getReasonForVisit()));

        colAppStatus.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getStatus()));
        colAppStatus.setCellFactory(col -> new TableCell<>() {
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

        patientAppointmentsTable.setItems(appointmentsList);
    }

    public void loadPatientRecord(Long patientId) {
        this.currentPatientId = patientId;

        Task<PatientRecordData> task = new Task<>() {
            @Override
            protected PatientRecordData call() {
                PatientResponseDto patient = patientService.getPatientById(patientId);
                MedicalRecordDto record = medicalRecordService.getRecordByPatientId(patientId);
                List<MedicalConsultationResponseDto> consultations = consultationService.getConsultationsByPatientId(patientId);
                List<AppointmentResponseDto> appointments = appointmentService.getAppointmentsByPatient(patientId);
                List<PatientResponseDto> allPatients = patientService.getAllActivePatients();

                return new PatientRecordData(patient, record, consultations, appointments, allPatients);
            }
        };

        task.setOnSucceeded(e -> {
            PatientRecordData data = task.getValue();
            currentPatient = data.patient;
            currentMedicalRecord = data.record;

            patientOptions.setAll(data.allPatients);
            patientSelectorComboBox.setValue(data.patient);

            renderPatientHeader(data.patient, data.record);
            renderMedicalRecordTab(data.record);

            consultationsList.setAll(data.consultations);
            if (!data.consultations.isEmpty()) {
                consultationsTable.getSelectionModel().selectFirst();
            } else {
                displayConsultationDetail(null);
            }

            appointmentsList.setAll(data.appointments);

            initNewConsultationFormDefaults();
        });

        task.setOnFailed(e -> {
            log.error("Error al cargar expediente del paciente ID: {}", patientId, task.getException());
            notificationService.showError("Error", "No se pudo cargar el expediente clinico del paciente");
        });

        new Thread(task).start();
    }

    private void loadDefaultFirstPatient() {
        Task<List<PatientResponseDto>> task = new Task<>() {
            @Override
            protected List<PatientResponseDto> call() {
                return patientService.getAllActivePatients();
            }
        };

        task.setOnSucceeded(e -> {
            List<PatientResponseDto> list = task.getValue();
            patientOptions.setAll(list);
            if (!list.isEmpty()) {
                loadPatientRecord(list.get(0).getId());
            } else {
                patientNameLabel.setText("No hay pacientes registrados");
            }
        });

        new Thread(task).start();
    }

    private void renderPatientHeader(PatientResponseDto patient, MedicalRecordDto record) {
        patientNameLabel.setText(patient.getFullName());
        patientDniLabel.setText("DNI: " + patient.getIdentificationNumber());
        patientAgeLabel.setText("Edad: " + (patient.getAge() != null ? patient.getAge() + " anios" : "--") +
                " ? Sexo: " + (patient.getGender() == Gender.MASCULINO ? "Masculino" : patient.getGender() == Gender.FEMENINO ? "Femenino" : "Otro"));
        patientRecordNumberLabel.setText("Expediente: " + (patient.getMedicalRecordNumber() != null ? patient.getMedicalRecordNumber() : "--"));

        bloodTypeBadge.setText("Grupo: " + (patient.getBloodType() != null ? patient.getBloodType().name().replace("_", " ") : "N/D"));

        String allergies = record != null && record.getAllergies() != null && !record.getAllergies().isBlank()
                ? record.getAllergies() : "Ninguna reportada";
        allergiesSummaryLabel.setText("Alergias: " + allergies);
    }

    private void renderMedicalRecordTab(MedicalRecordDto record) {
        if (record == null) {
            clearRecordFields();
            return;
        }
        allergiesArea.setText(record.getAllergies());
        currentMedicationsArea.setText(record.getCurrentMedications());
        pathologicalHistoryArea.setText(record.getPathologicalHistory());
        nonPathologicalHistoryArea.setText(record.getNonPathologicalHistory());
        familyHistoryArea.setText(record.getFamilyHistory());
        surgicalHistoryArea.setText(record.getSurgicalHistory());
        generalObservationsArea.setText(record.getGeneralObservations());
    }

    private void clearRecordFields() {
        allergiesArea.clear();
        currentMedicationsArea.clear();
        pathologicalHistoryArea.clear();
        nonPathologicalHistoryArea.clear();
        familyHistoryArea.clear();
        surgicalHistoryArea.clear();
        generalObservationsArea.clear();
    }

    private void displayConsultationDetail(MedicalConsultationResponseDto c) {
        if (c == null) {
            consultationDetailPane.setVisible(false);
            return;
        }

        consultationDetailPane.setVisible(true);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        detailDateLabel.setText(c.getConsultationDateTime() != null ? c.getConsultationDateTime().format(dtf) : "-");
        detailReasonLabel.setText(c.getReason() != null ? c.getReason() : "-");
        detailDiagnosisLabel.setText((c.getDiagnosis() != null ? c.getDiagnosis() : "-") + (c.getIcd10Code() != null ? " (CIE-10: " + c.getIcd10Code() + ")" : ""));
        detailTreatmentPlanLabel.setText(c.getTreatmentPlan() != null ? c.getTreatmentPlan() : "Sin plan registrado");
        detailPhysicalExamLabel.setText(c.getPhysicalExamination() != null ? c.getPhysicalExamination() : "-");

        VitalSignsDto v = c.getVitalSigns();
        if (v != null) {
            detailBloodPressureLabel.setText((v.getSystolicPressure() != null ? v.getSystolicPressure() : "--") + "/" +
                    (v.getDiastolicPressure() != null ? v.getDiastolicPressure() : "--") + " mmHg");
            detailHeartRateLabel.setText((v.getHeartRate() != null ? v.getHeartRate() : "--") + " bpm");
            detailTemperatureLabel.setText((v.getTemperatureCelsius() != null ? v.getTemperatureCelsius() : "--") + " C");
            detailRespiratoryRateLabel.setText((v.getRespiratoryRate() != null ? v.getRespiratoryRate() : "--") + " rpm");
            detailWeightLabel.setText((v.getWeightKg() != null ? v.getWeightKg() : "--") + " kg");
            detailHeightLabel.setText((v.getHeightCm() != null ? v.getHeightCm() : "--") + " cm");
            detailBmiLabel.setText((v.getBmi() != null ? v.getBmi() : "--") + " kg/m2");
            detailSpo2Label.setText((v.getOxygenSaturationPercentage() != null ? v.getOxygenSaturationPercentage() : "--") + " %");
        } else {
            detailBloodPressureLabel.setText("--/--");
            detailHeartRateLabel.setText("--");
            detailTemperatureLabel.setText("--");
            detailRespiratoryRateLabel.setText("--");
            detailWeightLabel.setText("--");
            detailHeightLabel.setText("--");
            detailBmiLabel.setText("--");
            detailSpo2Label.setText("--");
        }
    }

    private void initNewConsultationFormDefaults() {
        newConsultationDatePicker.setValue(LocalDate.now());
        newConsultationReasonField.clear();
        newConsultationSymptomsArea.clear();
        newConsultationPhysicalExamArea.clear();
        vitalSystolicField.clear();
        vitalDiastolicField.clear();
        vitalHeartRateField.clear();
        vitalRespiratoryRateField.clear();
        vitalTempField.clear();
        vitalWeightField.clear();
        vitalHeightField.clear();
        vitalSpo2Field.clear();
        newConsultationDiagnosisArea.clear();
        newConsultationIcd10Field.clear();
        newConsultationTreatmentArea.clear();
        newConsultationPrivateNotesArea.clear();
    }

    @FXML
    public void handleSaveMedicalRecord(ActionEvent event) {
        if (currentPatientId == null) {
            notificationService.showWarning("Advertencia", "No hay paciente seleccionado");
            return;
        }

        MedicalRecordDto dto = MedicalRecordDto.builder()
                .patientId(currentPatientId)
                .recordNumber(currentPatient.getMedicalRecordNumber())
                .allergies(allergiesArea.getText() != null ? allergiesArea.getText().trim() : null)
                .currentMedications(currentMedicationsArea.getText() != null ? currentMedicationsArea.getText().trim() : null)
                .pathologicalHistory(pathologicalHistoryArea.getText() != null ? pathologicalHistoryArea.getText().trim() : null)
                .nonPathologicalHistory(nonPathologicalHistoryArea.getText() != null ? nonPathologicalHistoryArea.getText().trim() : null)
                .familyHistory(familyHistoryArea.getText() != null ? familyHistoryArea.getText().trim() : null)
                .surgicalHistory(surgicalHistoryArea.getText() != null ? surgicalHistoryArea.getText().trim() : null)
                .generalObservations(generalObservationsArea.getText() != null ? generalObservationsArea.getText().trim() : null)
                .build();

        saveRecordButton.setDisable(true);

        Task<MedicalRecordDto> task = new Task<>() {
            @Override
            protected MedicalRecordDto call() {
                return medicalRecordService.updateMedicalRecord(currentPatientId, dto);
            }
        };

        task.setOnSucceeded(e -> {
            saveRecordButton.setDisable(false);
            currentMedicalRecord = task.getValue();
            notificationService.showSuccess("Expediente Actualizado", "Los antecedentes clinicos fueron guardados satisfactoriamente");
            allergiesSummaryLabel.setText("Alergias: " + (currentMedicalRecord.getAllergies() != null && !currentMedicalRecord.getAllergies().isBlank()
                    ? currentMedicalRecord.getAllergies() : "Ninguna reportada"));
        });

        task.setOnFailed(e -> {
            saveRecordButton.setDisable(false);
            log.error("Error al guardar antecedentes", task.getException());
            notificationService.showError("Error", "No fue posible actualizar los antecedentes del expediente");
        });

        new Thread(task).start();
    }

    @FXML
    public void handleSaveNewConsultation(ActionEvent event) {
        if (currentPatientId == null) {
            notificationService.showWarning("Advertencia", "No hay paciente seleccionado");
            return;
        }

        String reason = newConsultationReasonField.getText();
        if (reason == null || reason.isBlank()) {
            notificationService.showWarning("Campo Obligatorio", "El motivo de consulta es requerido");
            newConsultationReasonField.requestFocus();
            return;
        }

        String diagnosis = newConsultationDiagnosisArea.getText();
        if (diagnosis == null || diagnosis.isBlank()) {
            notificationService.showWarning("Campo Obligatorio", "El diagnostico clinico es requerido");
            newConsultationDiagnosisArea.requestFocus();
            return;
        }

        LocalDate date = newConsultationDatePicker.getValue() != null ? newConsultationDatePicker.getValue() : LocalDate.now();
        LocalDateTime consultationTime = LocalDateTime.of(date, LocalTime.now());

        VitalSignsDto vitalSigns = VitalSignsDto.builder()
                .systolicPressure(parseInteger(vitalSystolicField.getText()))
                .diastolicPressure(parseInteger(vitalDiastolicField.getText()))
                .heartRate(parseInteger(vitalHeartRateField.getText()))
                .respiratoryRate(parseInteger(vitalRespiratoryRateField.getText()))
                .temperatureCelsius(parseBigDecimal(vitalTempField.getText()))
                .weightKg(parseBigDecimal(vitalWeightField.getText()))
                .heightCm(parseBigDecimal(vitalHeightField.getText()))
                .oxygenSaturationPercentage(parseInteger(vitalSpo2Field.getText()))
                .build();

        MedicalConsultationRequestDto requestDto = MedicalConsultationRequestDto.builder()
                .patientId(currentPatientId)
                .consultationDateTime(consultationTime)
                .reason(reason.trim())
                .symptoms(newConsultationSymptomsArea.getText() != null ? newConsultationSymptomsArea.getText().trim() : null)
                .physicalExamination(newConsultationPhysicalExamArea.getText() != null ? newConsultationPhysicalExamArea.getText().trim() : null)
                .vitalSigns(vitalSigns)
                .diagnosis(diagnosis.trim())
                .icd10Code(newConsultationIcd10Field.getText() != null ? newConsultationIcd10Field.getText().trim() : null)
                .treatmentPlan(newConsultationTreatmentArea.getText() != null ? newConsultationTreatmentArea.getText().trim() : null)
                .privateNotes(newConsultationPrivateNotesArea.getText() != null ? newConsultationPrivateNotesArea.getText().trim() : null)
                .build();

        saveConsultationButton.setDisable(true);

        Task<MedicalConsultationResponseDto> task = new Task<>() {
            @Override
            protected MedicalConsultationResponseDto call() {
                return consultationService.createConsultation(requestDto);
            }
        };

        task.setOnSucceeded(e -> {
            saveConsultationButton.setDisable(false);
            MedicalConsultationResponseDto created = task.getValue();
            notificationService.showSuccess("Consulta Guardada", "La consulta medica fue registrada exitosamente");
            consultationsList.add(0, created);
            consultationsTable.getSelectionModel().select(created);
            initNewConsultationFormDefaults();
            recordTabPane.getSelectionModel().select(1); // Cambiar a pesta?a de Historial
        });

        task.setOnFailed(e -> {
            saveConsultationButton.setDisable(false);
            log.error("Error al registrar consulta", task.getException());
            notificationService.showError("Error", "No se pudo guardar la consulta medica");
        });

        new Thread(task).start();
    }

    @FXML
    public void handleBackToPatients(ActionEvent event) {
        navigationService.navigateTo(ViewType.PATIENTS);
    }

    private Integer parseInteger(String val) {
        if (val == null || val.isBlank()) return null;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal parseBigDecimal(String val) {
        if (val == null || val.isBlank()) return null;
        try {
            return new BigDecimal(val.trim().replace(",", "."));
        } catch (Exception e) {
            return null;
        }
    }

    private record PatientRecordData(
            PatientResponseDto patient,
            MedicalRecordDto record,
            List<MedicalConsultationResponseDto> consultations,
            List<AppointmentResponseDto> appointments,
            List<PatientResponseDto> allPatients
    ) {}
}
