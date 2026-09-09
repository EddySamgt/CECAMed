package com.cecamed.ui.controller;

import com.cecamed.ui.component.NotificationService;
import com.cecamed.ui.navigation.NavigationService;
import com.cecamed.ui.navigation.ViewType;
import com.cecamed.ui.session.AuthenticationService;
import com.cecamed.ui.session.UserSession;
import com.cecamed.ui.theme.ThemeManager;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ResourceBundle;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginController implements Initializable {

    private final AuthenticationService authenticationService;
    private final UserSession userSession;
    private final NavigationService navigationService;
    private final ThemeManager themeManager;
    private final NotificationService notificationService;

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordVisibleField;
    @FXML private Button togglePasswordButton;
    @FXML private FontIcon togglePasswordIcon;
    @FXML private Button loginButton;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Label errorLabel;
    @FXML private Button toggleThemeButton;
    @FXML private FontIcon toggleThemeIcon;

    private boolean isPasswordVisible = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        errorLabel.setVisible(false);
        progressIndicator.setVisible(false);

        // Bidirectional sync for password visibility toggle
        passwordVisibleField.textProperty().bindBidirectional(passwordField.textProperty());

        usernameField.setOnKeyPressed(this::handleKeyPressed);
        passwordField.setOnKeyPressed(this::handleKeyPressed);
        passwordVisibleField.setOnKeyPressed(this::handleKeyPressed);

        updateThemeIcon();
    }

    private void handleKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            handleLogin(null);
        }
    }

    @FXML
    public void handleTogglePassword(ActionEvent event) {
        isPasswordVisible = !isPasswordVisible;
        if (isPasswordVisible) {
            passwordVisibleField.setVisible(true);
            passwordVisibleField.setManaged(true);
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            togglePasswordIcon.setIconLiteral("feather-eye-off");
        } else {
            passwordVisibleField.setVisible(false);
            passwordVisibleField.setManaged(false);
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            togglePasswordIcon.setIconLiteral("feather-eye");
        }
    }

    @FXML
    public void handleLogin(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username == null || username.isBlank()) {
            showError("Por favor ingrese su usuario o correo");
            usernameField.requestFocus();
            return;
        }

        if (password == null || password.isBlank()) {
            showError("Por favor ingrese su contraseña");
            passwordField.requestFocus();
            return;
        }

        setLoading(true);
        errorLabel.setVisible(false);

        Task<Boolean> loginTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                Thread.sleep(300);
                return authenticationService.authenticate(username, password);
            }
        };

        loginTask.setOnSucceeded(e -> {
            setLoading(false);
            if (loginTask.getValue()) {
                notificationService.showSuccess("Bienvenido a CECAMed",
                        "Sesión iniciada como " + userSession.getFullName() + " (" + userSession.getRole().getDisplayName() + ")");
                navigationService.navigateRoot(ViewType.MAIN_LAYOUT);
            } else {
                showError("Credenciales incorrectas. Verifique su usuario y contraseña.");
            }
        });

        loginTask.setOnFailed(e -> {
            setLoading(false);
            showError("Error de conexión al autenticar: " + loginTask.getException().getMessage());
        });

        new Thread(loginTask).start();
    }

    @FXML
    public void handleQuickFillMedico(ActionEvent event) {
        usernameField.setText("medico");
        passwordField.setText("medico123");
        handleLogin(null);
    }

    @FXML
    public void handleQuickFillRecepcion(ActionEvent event) {
        usernameField.setText("recepcion");
        passwordField.setText("recepcion123");
        handleLogin(null);
    }

    @FXML
    public void handleToggleTheme(ActionEvent event) {
        themeManager.toggleTheme();
        updateThemeIcon();
    }

    private void updateThemeIcon() {
        if (toggleThemeIcon != null) {
            toggleThemeIcon.setIconLiteral(themeManager.isDarkMode() ? "feather-sun" : "feather-moon");
        }
    }

    private void setLoading(boolean loading) {
        progressIndicator.setVisible(loading);
        loginButton.setDisable(loading);
        usernameField.setDisable(loading);
        passwordField.setDisable(loading);
        passwordVisibleField.setDisable(loading);
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}
