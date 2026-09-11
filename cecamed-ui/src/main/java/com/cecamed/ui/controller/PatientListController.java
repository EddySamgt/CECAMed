package com.cecamed.ui.controller;

import com.cecamed.core.model.patient.enums.BloodType;
import com.cecamed.core.model.patient.enums.Gender;
import com.cecamed.services.dto.patient.PatientResponseDto;
import com.cecamed.services.service.PatientService;
import com.cecamed.ui.component.ConfirmationDialog;
import com.cecamed.ui.component.NotificationService;
import com.cecamed.ui.navigation.NavigationService;
import com.cecamed.ui.navigation.ViewType;
import com.cecamed.ui.session.UserSession;
import com.cecamed.ui.theme.ThemeManager;
import com.cecamed.ui.util.SpringFXMLLoader;
import javafx.animation.PauseTransition;
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
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

@Slf4j
@Component
@RequiredArgsConstructor
public class PatientListController implements Initializable {

    private final PatientService patientService;
    private final NavigationService navigationService;
    private final ThemeManager themeManager;
    private final NotificationService notificationService;
    private final SpringFXMLLoader springFXMLLoader;
    private final UserSession userSession;

    @FXML private TextField searchField;
    @FXML private Button clearSearchButton;
    @FXML private ComboBox<String> statusFilterComboBox;
    @FXML private Label patientCountLabel;
    @FXML private ProgressIndicator loadingIndicator;

    @FXML private TableView<PatientResponseDto> patientsTable;
    @FXML private TableColumn<PatientResponseDto, String> colDni;
    @FXML private TableColumn<PatientResponseDto, String> colName;
    @FXML private TableColumn<PatientResponseDto, String> colAge;
    @FXML private TableColumn<PatientResponseDto, Gender> colGender;
    @FXML private TableColumn<PatientResponseDto, BloodType> colBloodType;
    @FXML private TableColumn<PatientResponseDto, String> colPhone;
    @FXML private TableColumn<PatientResponseDto, String> colEmail;
    @FXML private TableColumn<PatientResponseDto, String> colRecordNumber;
    @FXML private TableColumn<PatientResponseDto, Boolean> colStatus;
    @FXML private TableColumn<PatientResponseDto, Void> colActions;

    private final ObservableList<PatientResponseDto> patientList = FXCollections.observableArrayList();
    private final PauseTransition searchDebounce = new PauseTransition(Duration.millis(350));

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupFilterComboBox();
        setupTableColumns();
        setupSearchDebounce();
        setupTableEvents();
        setupEmptyStatePlaceholder();

        loadPatientsAsync();
    }

    private void setupFilterComboBox() {
        statusFilterComboBox.setItems(FXCollections.observableArrayList("Solo Activos", "Todos los Pacientes", "Inactivos"));
        statusFilterComboBox.setValue("Solo Activos");
        statusFilterComboBox.setOnAction(e -> loadPatientsAsync());
    }

    private void setupTableColumns() {
        DateTimeFormatter birthFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        colDni.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getIdentificationNumber()));
        colName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFullName()));
        
        colAge.setCellValueFactory(data -> {
            PatientResponseDto p = data.getValue();
            String ageStr = p.getAge() != null ? p.getAge() + " anios" : "--";
            String birthStr = p.getBirthDate() != null ? " (" + p.getBirthDate().format(birthFormatter) + ")" : "";
            return new SimpleStringProperty(ageStr + birthStr);
        });

        colGender.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getGender()));
        colGender.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Gender gender, boolean empty) {
                super.updateItem(gender, empty);
                if (empty || gender == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(formatGender(gender));
                }
            }
        });

        colBloodType.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getBloodType()));
        colBloodType.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BloodType bloodType, boolean empty) {
                super.updateItem(bloodType, empty);
                if (empty || bloodType == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(formatBloodType(bloodType));
                    badge.getStyleClass().addAll("status-badge", "badge-info");
                    setGraphic(badge);
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                }
            }
        });

        colPhone.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getPhone() != null && !data.getValue().getPhone().isBlank() ? data.getValue().getPhone() : "-"
        ));

        colEmail.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getEmail() != null && !data.getValue().getEmail().isBlank() ? data.getValue().getEmail() : "-"
        ));

        colRecordNumber.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getMedicalRecordNumber() != null ? data.getValue().getMedicalRecordNumber() : "-"
        ));

        colStatus.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getActive()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean active, boolean empty) {
                super.updateItem(active, empty);
                if (empty || active == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(active ? "Activo" : "Inactivo");
                    badge.getStyleClass().addAll("status-badge", active ? "badge-success" : "badge-secondary");
                    setGraphic(badge);
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                }
            }
        });

        setupActionsColumn();
        patientsTable.setItems(patientList);
    }

    private void setupActionsColumn() {
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnRecord = new Button();
            private final Button btnEdit = new Button();
            private final Button btnToggleStatus = new Button();
            private final HBox container = new HBox(6, btnRecord, btnEdit, btnToggleStatus);

            {
                container.setAlignment(Pos.CENTER);

                btnRecord.getStyleClass().addAll("ghost-button");
                btnRecord.setTooltip(new Tooltip("Ver Expediente Clinico"));
                FontIcon iconRecord = new FontIcon("feather-clipboard");
                iconRecord.setIconSize(15);
                btnRecord.setGraphic(iconRecord);

                btnEdit.getStyleClass().addAll("ghost-button");
                btnEdit.setTooltip(new Tooltip("Editar Datos del Paciente"));
                FontIcon iconEdit = new FontIcon("feather-edit-2");
                iconEdit.setIconSize(15);
                btnEdit.setGraphic(iconEdit);

                btnToggleStatus.getStyleClass().addAll("ghost-button");
                FontIcon iconStatus = new FontIcon("feather-power");
                iconStatus.setIconSize(15);
                btnToggleStatus.setGraphic(iconStatus);

                btnRecord.setOnAction(e -> {
                    PatientResponseDto patient = getTableView().getItems().get(getIndex());
                    openMedicalRecord(patient);
                });

                btnEdit.setOnAction(e -> {
                    PatientResponseDto patient = getTableView().getItems().get(getIndex());
                    openEditDialog(patient);
                });

                btnToggleStatus.setOnAction(e -> {
                    PatientResponseDto patient = getTableView().getItems().get(getIndex());
                    handleTogglePatientStatus(patient);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    PatientResponseDto patient = getTableView().getItems().get(getIndex());
                    if (Boolean.TRUE.equals(patient.getActive())) {
                        btnToggleStatus.setTooltip(new Tooltip("Desactivar Paciente"));
                        ((FontIcon) btnToggleStatus.getGraphic()).setIconLiteral("feather-user-x");
                        btnToggleStatus.setStyle("-fx-text-fill: #D32F2F;");
                    } else {
                        btnToggleStatus.setTooltip(new Tooltip("Reactivar Paciente"));
                        ((FontIcon) btnToggleStatus.getGraphic()).setIconLiteral("feather-user-check");
                        btnToggleStatus.setStyle("-fx-text-fill: #16A34A;");
                    }
                    setGraphic(container);
                }
            }
        });
    }

    private void setupSearchDebounce() {
        searchDebounce.setOnFinished(e -> loadPatientsAsync());
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            clearSearchButton.setVisible(newVal != null && !newVal.isBlank());
            searchDebounce.playFromStart();
        });
    }

    private void setupTableEvents() {
        patientsTable.setRowFactory(tv -> {
            TableRow<PatientResponseDto> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openMedicalRecord(row.getItem());
                }
            });
            return row;
        });
    }

    private void setupEmptyStatePlaceholder() {
        VBox placeholder = new VBox(10);
        placeholder.setAlignment(Pos.CENTER);
        FontIcon icon = new FontIcon("feather-users");
        icon.setIconSize(36);
        icon.setStyle("-fx-icon-color: #94A3B8;");
        Label label = new Label("No se encontraron pacientes registrados o que coincidan con la busqueda");
        label.setStyle("-fx-text-fill: #64748B; -fx-font-size: 13px;");
        placeholder.getChildren().addAll(icon, label);
        patientsTable.setPlaceholder(placeholder);
    }

    public void loadPatientsAsync() {
        String searchTerm = searchField.getText() != null ? searchField.getText().trim() : "";
        String filter = statusFilterComboBox.getValue();

        loadingIndicator.setVisible(true);

        Task<List<PatientResponseDto>> task = new Task<>() {
            @Override
            protected List<PatientResponseDto> call() {
                List<PatientResponseDto> results;
                if (!searchTerm.isBlank()) {
                    results = patientService.searchPatients(searchTerm);
                } else {
                    results = patientService.getAllActivePatients();
                }

                if ("Inactivos".equalsIgnoreCase(filter)) {
                    return results.stream().filter(p -> Boolean.FALSE.equals(p.getActive())).toList();
                } else if ("Solo Activos".equalsIgnoreCase(filter)) {
                    return results.stream().filter(p -> Boolean.TRUE.equals(p.getActive())).toList();
                }
                return results;
            }
        };

        task.setOnSucceeded(e -> {
            loadingIndicator.setVisible(false);
            List<PatientResponseDto> list = task.getValue();
            patientList.setAll(list);
            patientCountLabel.setText("Mostrando " + list.size() + " paciente" + (list.size() == 1 ? "" : "s"));
        });

        task.setOnFailed(e -> {
            loadingIndicator.setVisible(false);
            log.error("Error al consultar pacientes", task.getException());
            notificationService.showError("Error de Consulta", "No fue posible cargar el listado de pacientes");
        });

        new Thread(task).start();
    }

    @FXML
    public void handleNewPatient(ActionEvent event) {
        try {
            FXMLLoader loader = springFXMLLoader.createLoader("/fxml/patient-form-dialog.fxml");
            Parent root = loader.load();

            PatientFormDialogController formController = loader.getController();

            Stage stage = new Stage();
            stage.setTitle("Registrar Paciente - CECAMed");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(patientsTable.getScene().getWindow());

            Scene scene = new Scene(root);
            themeManager.registerScene(scene);
            stage.setScene(scene);

            formController.setDialogStage(stage);
            formController.initCreateMode();

            stage.showAndWait();

            if (formController.isSavedSuccessfully()) {
                loadPatientsAsync();
            }
        } catch (IOException ex) {
            log.error("Error al abrir dialogo de paciente", ex);
            notificationService.showError("Error de Interfaz", "No se pudo abrir el formulario de paciente: " + ex.getMessage());
        }
    }

    private void openEditDialog(PatientResponseDto patient) {
        try {
            FXMLLoader loader = springFXMLLoader.createLoader("/fxml/patient-form-dialog.fxml");
            Parent root = loader.load();

            PatientFormDialogController formController = loader.getController();

            Stage stage = new Stage();
            stage.setTitle("Editar Paciente - CECAMed");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(patientsTable.getScene().getWindow());

            Scene scene = new Scene(root);
            themeManager.registerScene(scene);
            stage.setScene(scene);

            formController.setDialogStage(stage);
            formController.initEditMode(patient);

            stage.showAndWait();

            if (formController.isSavedSuccessfully()) {
                loadPatientsAsync();
            }
        } catch (IOException ex) {
            log.error("Error al abrir formulario de edicion", ex);
            notificationService.showError("Error de Interfaz", "No se pudo abrir el formulario de edicion");
        }
    }

    private void openMedicalRecord(PatientResponseDto patient) {
        if (userSession.isRecepcion()) {
            notificationService.showWarning("Acceso Restringido",
                    "El perfil de Recepcion no tiene permisos para consultar el expediente clinico detallado");
            return;
        }
        navigationService.setParameter("patientId", patient.getId());
        navigationService.navigateTo(ViewType.MEDICAL_RECORD);
    }

    private void handleTogglePatientStatus(PatientResponseDto patient) {
        boolean currentlyActive = Boolean.TRUE.equals(patient.getActive());
        String actionName = currentlyActive ? "desactivar" : "reactivar";

        boolean confirmed = ConfirmationDialog.confirm(
                "Confirmar Operacion",
                "Desea " + actionName + " al paciente?",
                "Paciente: " + patient.getFullName() + " (" + patient.getIdentificationNumber() + ")"
        );

        if (!confirmed) return;

        Task<Void> toggleTask = new Task<>() {
            @Override
            protected Void call() {
                if (currentlyActive) {
                    patientService.deactivatePatient(patient.getId());
                } else {
                    patientService.activatePatient(patient.getId());
                }
                return null;
            }
        };

        toggleTask.setOnSucceeded(e -> {
            notificationService.showSuccess("Estado Actualizado",
                    "El paciente " + patient.getFullName() + " fue " + (currentlyActive ? "desactivado" : "reactivado") + " con exito");
            loadPatientsAsync();
        });

        toggleTask.setOnFailed(e -> {
            log.error("Error al cambiar estado del paciente", toggleTask.getException());
            notificationService.showError("Error", "No fue posible cambiar el estado del paciente");
        });

        new Thread(toggleTask).start();
    }

    @FXML
    public void handleClearSearch(ActionEvent event) {
        searchField.clear();
        clearSearchButton.setVisible(false);
    }

    @FXML
    public void handleRefresh(ActionEvent event) {
        loadPatientsAsync();
    }

    private String formatGender(Gender g) {
        return switch (g) {
            case MASCULINO -> "M";
            case FEMENINO -> "F";
            case OTRO -> "Otro";
        };
    }

    private String formatBloodType(BloodType bt) {
        if (bt == null) return "-";
        return switch (bt) {
            case A_POSITIVO -> "A+";
            case A_NEGATIVO -> "A-";
            case B_POSITIVO -> "B+";
            case B_NEGATIVO -> "B-";
            case AB_POSITIVO -> "AB+";
            case AB_NEGATIVO -> "AB-";
            case O_POSITIVO -> "O+";
            case O_NEGATIVO -> "O-";
            case DESCONOCIDO -> "N/D";
        };
    }
}
