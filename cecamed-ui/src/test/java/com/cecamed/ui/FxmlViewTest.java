package com.cecamed.ui;

import com.cecamed.ui.component.NotificationService;
import com.cecamed.ui.controller.LoginController;
import com.cecamed.ui.navigation.NavigationService;
import com.cecamed.ui.session.AuthenticationService;
import com.cecamed.ui.session.UserSession;
import com.cecamed.ui.theme.ThemeManager;
import com.cecamed.ui.util.SpringFXMLLoader;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.ApplicationContext;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("javafx")
class FxmlViewTest {
    @BeforeAll
    static void startJavaFx() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(() -> {
            Platform.setImplicitExit(false);
            started.countDown();
        });
        assertTrue(started.await(15, TimeUnit.SECONDS), "JavaFX did not start");
    }

    @AfterAll
    static void stopJavaFx() {
        Platform.exit();
    }

    @ParameterizedTest
    @ValueSource(strings = {"login", "main-layout", "dashboard", "patient-list",
            "patient-form-dialog", "medical-record", "appointment-calendar",
            "appointment-dialog", "reception", "settings"})
    void loadsView(String view) throws Exception {
        FutureTask<Void> load = new FutureTask<>(() -> {
            ThemeManager theme = new ThemeManager();
            ApplicationContext context = mock(ApplicationContext.class);
            when(context.getBean(any(Class.class))).thenAnswer(invocation -> {
                Class<?> type = invocation.getArgument(0);
                if (type == LoginController.class) {
                    return new LoginController(mock(AuthenticationService.class), mock(UserSession.class),
                            mock(NavigationService.class), theme, mock(NotificationService.class));
                }
                // Other views load without invoking business services or modifying the database.
                return mock(type);
            });
            var loader = new SpringFXMLLoader(context).createLoader("/fxml/" + view + ".fxml");
            Parent root = loader.load();
            assertNotNull(root);
            if (view.equals("medical-record")) {
                var tabs = (javafx.scene.control.TabPane) loader.getNamespace().get("recordTabPane");
                assertEquals("Antecedentes patológicos", tabs.getTabs().get(0).getText());
                assertEquals("Antecedentes no patológicos", tabs.getTabs().get(1).getText());
                for (String field : new String[]{"familyHistoryArea", "surgicalHistoryArea", "pathologicalHistoryArea",
                        "traumaticHistoryArea", "allergiesArea", "gynecologicalObstetricHistoryArea"}) {
                    assertNotNull(((javafx.scene.control.ScrollPane) tabs.getTabs().get(0).getContent()).getContent().lookup("#" + field));
                }
                for (String field : new String[]{"nonPathologicalHistoryArea", "waterGlassesPerDayField", "mealsPerDayField"}) {
                    assertNotNull(((javafx.scene.control.ScrollPane) tabs.getTabs().get(1).getContent()).getContent().lookup("#" + field));
                }
            }
            if (view.equals("patient-form-dialog")) {
                assertFalse(loader.getNamespace().containsKey("dniField"));
                TextField lastName = (TextField) loader.getNamespace().get("lastNameField");
                assertEquals("ej. Pérez Gómez", lastName.getPromptText());
                lastName.setText("Muñoz Agüero");
                assertEquals("Muñoz Agüero", lastName.getText());
            }
            if (view.equals("login")) {
                Stage stage = new Stage();
                try {
                    Scene scene = new Scene(root);
                    theme.registerScene(scene);
                    stage.setScene(scene);
                    stage.show();
                    root.applyCss();
                    root.layout();
                    assertTrue(stage.isShowing());
                    assertEquals("Iniciar Sesión", ((Button) root.lookup("#loginButton")).getText());
                    assertEquals("••••••••", ((TextField) root.lookup("#passwordField")).getPromptText());
                    assertTrue(root.lookupAll(".form-field-label").stream()
                            .anyMatch(node -> node instanceof Label label && label.getText().equals("Contraseña")));
                    TextField username = (TextField) root.lookup("#usernameField");
                    String spanish = "áéíóú ÁÉÍÓÚ ñÑ üÜ ¿Cómo está? ¡Atención!";
                    username.setText(spanish);
                    assertEquals(spanish, username.getText());
                    Button toggleTheme = (Button) root.lookup("#toggleThemeButton");
                    assertNotNull(toggleTheme);
                    toggleTheme.fire();
                    assertTrue(theme.isDarkMode());
                } finally {
                    stage.close();
                }
            }
            return null;
        });
        Platform.runLater(load);
        load.get(20, TimeUnit.SECONDS);
    }
}
