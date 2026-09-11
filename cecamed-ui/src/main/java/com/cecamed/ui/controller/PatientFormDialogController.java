package com.cecamed.ui.controller;

import com.cecamed.core.model.patient.enums.BloodType;
import com.cecamed.core.model.patient.enums.Gender;
import com.cecamed.services.dto.patient.PatientRequestDto;
import com.cecamed.services.dto.patient.PatientResponseDto;
import com.cecamed.services.exception.BusinessRuleException;
import com.cecamed.services.service.PatientService;
import com.cecamed.ui.component.NotificationService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
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
import java.util.ResourceBundle;

@Slf4j
@Component
@RequiredArgsConstructor
public class PatientFormDialogController implements Initializable {

    private final PatientService patientService;
    private final NotificationService notificationService;

    @FXML private Label dialogTitleLabel;
    @FXML private TextField dniField;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private DatePicker birthDatePicker;
    @FXML private ComboBox<Gender> genderComboBox;
    @FXML private ComboBox<BloodType> bloodTypeComboBox;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private TextArea addressArea;
    @FXML private TextField emergencyNameField;
    @FXML private TextField emergencyPhoneField;
    @FXML private TextField emergencyRelationshipField;
    @FXML private TextArea notesArea;

    @FXML private Label errorLabel;
    @FXML private Button saveButton;
    @FXML private ProgressIndicator progressIndicator;

    private Stage dialogStage;
    private Long existingPatientId = null;
    private boolean savedSuccessfully = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        genderComboBox.setItems(FXCollections.observableArrayList(Gender.values()));
        bloodTypeComboBox.setItems(FXCollections.observableArrayList(BloodType.values()));

        genderComboBox.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Gender item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatGender(item));
            }
        });
        genderComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Gender item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatGender(item));
            }
        });

        bloodTypeComboBox.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(BloodType item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatBloodType(item));
            }
        });
        bloodTypeComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(BloodType item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatBloodType(item));
            }
        });

        errorLabel.setVisible(false);
        progressIndicator.setVisible(false);
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public boolean isSavedSuccessfully() {
        return savedSuccessfully;
    }

    public void initCreateMode() {
        this.existingPatientId = null;
        dialogTitleLabel.setText("Registrar Nuevo Paciente");
        saveButton.setText("Guardar Paciente");
        clearForm();
        bloodTypeComboBox.setValue(BloodType.DESCONOCIDO);
    }

    public void initEditMode(PatientResponseDto patient) {
        this.existingPatientId = patient.getId();
        dialogTitleLabel.setText("Editar Paciente: " + patient.getFullName());
        saveButton.setText("Actualizar Datos");

        dniField.setText(patient.getIdentificationNumber());
        firstNameField.setText(patient.getFirstName());
        lastNameField.setText(patient.getLastName());
        birthDatePicker.setValue(patient.getBirthDate());
        genderComboBox.setValue(patient.getGender());
        bloodTypeComboBox.setValue(patient.getBloodType() != null ? patient.getBloodType() : BloodType.DESCONOCIDO);
        phoneField.setText(patient.getPhone());
        emailField.setText(patient.getEmail());
        addressArea.setText(patient.getAddress());
        emergencyNameField.setText(patient.getEmergencyContactName());
        emergencyPhoneField.setText(patient.getEmergencyContactPhone());
        emergencyRelationshipField.setText(patient.getEmergencyContactRelationship());
        notesArea.setText(patient.getNotes());
    }

    @FXML
    public void handleSave(ActionEvent event) {
        if (!validateInputs()) {
            return;
        }

        PatientRequestDto requestDto = PatientRequestDto.builder()
                .identificationNumber(dniField.getText().trim())
                .firstName(firstNameField.getText().trim())
                .lastName(lastNameField.getText().trim())
                .birthDate(birthDatePicker.getValue())
                .gender(genderComboBox.getValue())
                .bloodType(bloodTypeComboBox.getValue())
                .phone(phoneField.getText() != null && !phoneField.getText().isBlank() ? phoneField.getText().trim() : null)
                .email(emailField.getText() != null && !emailField.getText().isBlank() ? emailField.getText().trim() : null)
                .address(addressArea.getText() != null && !addressArea.getText().isBlank() ? addressArea.getText().trim() : null)
                .emergencyContactName(emergencyNameField.getText() != null && !emergencyNameField.getText().isBlank() ? emergencyNameField.getText().trim() : null)
                .emergencyContactPhone(emergencyPhoneField.getText() != null && !emergencyPhoneField.getText().isBlank() ? emergencyPhoneField.getText().trim() : null)
                .emergencyContactRelationship(emergencyRelationshipField.getText() != null && !emergencyRelationshipField.getText().isBlank() ? emergencyRelationshipField.getText().trim() : null)
                .notes(notesArea.getText() != null && !notesArea.getText().isBlank() ? notesArea.getText().trim() : null)
                .build();

        setLoading(true);
        errorLabel.setVisible(false);

        Task<PatientResponseDto> saveTask = new Task<>() {
            @Override
            protected PatientResponseDto call() {
                if (existingPatientId == null) {
                    return patientService.createPatient(requestDto);
                } else {
                    return patientService.updatePatient(existingPatientId, requestDto);
                }
            }
        };

        saveTask.setOnSucceeded(e -> {
            setLoading(false);
            savedSuccessfully = true;
            PatientResponseDto saved = saveTask.getValue();
            if (existingPatientId == null) {
                notificationService.showSuccess("Paciente Registrado",
                        "El paciente " + saved.getFullName() + " fue registrado exitosamente con el expediente " + saved.getMedicalRecordNumber());
            } else {
                notificationService.showSuccess("Paciente Actualizado",
                        "Los datos de " + saved.getFullName() + " fueron actualizados correctamente");
            }
            if (dialogStage != null) {
                dialogStage.close();
            }
        });

        saveTask.setOnFailed(e -> {
            setLoading(false);
            Throwable ex = saveTask.getException();
            String errorMsg = ex.getMessage();
            if (ex instanceof BusinessRuleException) {
                showError(errorMsg);
            } else {
                log.error("Error al guardar paciente", ex);
                showError("No fue posible guardar el paciente: " + (errorMsg != null ? errorMsg : "Error interno"));
            }
        });

        new Thread(saveTask).start();
    }

    @FXML
    public void handleCancel(ActionEvent event) {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    private boolean validateInputs() {
        if (dniField.getText() == null || dniField.getText().isBlank()) {
            showError("La identificacion o DNI es obligatoria");
            dniField.requestFocus();
            return false;
        }
        if (firstNameField.getText() == null || firstNameField.getText().isBlank()) {
            showError("El nombre del paciente es obligatorio");
            firstNameField.requestFocus();
            return false;
        }
        if (lastNameField.getText() == null || lastNameField.getText().isBlank()) {
            showError("El apellido del paciente es obligatorio");
            lastNameField.requestFocus();
            return false;
        }
        if (birthDatePicker.getValue() == null) {
            showError("La fecha de nacimiento es obligatoria");
            birthDatePicker.requestFocus();
            return false;
        }
        if (birthDatePicker.getValue().isAfter(LocalDate.now())) {
            showError("La fecha de nacimiento debe ser una fecha pasada");
            birthDatePicker.requestFocus();
            return false;
        }
        if (genderComboBox.getValue() == null) {
            showError("El genero/sexo es obligatorio");
            genderComboBox.requestFocus();
            return false;
        }
        if (emailField.getText() != null && !emailField.getText().isBlank()) {
            String email = emailField.getText().trim();
            if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                showError("El formato del correo electronico es invalido");
                emailField.requestFocus();
                return false;
            }
        }
        return true;
    }

    private void clearForm() {
        dniField.clear();
        firstNameField.clear();
        lastNameField.clear();
        birthDatePicker.setValue(null);
        genderComboBox.setValue(null);
        bloodTypeComboBox.setValue(BloodType.DESCONOCIDO);
        phoneField.clear();
        emailField.clear();
        addressArea.clear();
        emergencyNameField.clear();
        emergencyPhoneField.clear();
        emergencyRelationshipField.clear();
        notesArea.clear();
        errorLabel.setVisible(false);
    }

    private void setLoading(boolean loading) {
        progressIndicator.setVisible(loading);
        saveButton.setDisable(loading);
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }

    private String formatGender(Gender gender) {
        return switch (gender) {
            case MASCULINO -> "Masculino";
            case FEMENINO -> "Femenino";
            case OTRO -> "Otro";
        };
    }

    private String formatBloodType(BloodType bt) {
        return switch (bt) {
            case A_POSITIVO -> "A+ (A Positivo)";
            case A_NEGATIVO -> "A- (A Negativo)";
            case B_POSITIVO -> "B+ (B Positivo)";
            case B_NEGATIVO -> "B- (B Negativo)";
            case AB_POSITIVO -> "AB+ (AB Positivo)";
            case AB_NEGATIVO -> "AB- (AB Negativo)";
            case O_POSITIVO -> "O+ (O Positivo)";
            case O_NEGATIVO -> "O- (O Negativo)";
            case DESCONOCIDO -> "Desconocido / Sin tipificar";
        };
    }
}
