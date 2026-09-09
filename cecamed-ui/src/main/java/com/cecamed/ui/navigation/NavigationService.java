package com.cecamed.ui.navigation;

import com.cecamed.ui.theme.ThemeManager;
import com.cecamed.ui.util.SpringFXMLLoader;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class NavigationService {

    private final SpringFXMLLoader fxmlLoader;
    private final ThemeManager themeManager;

    @Getter
    @Setter
    private Stage primaryStage;

    @Setter
    private StackPane contentArea;

    @Getter
    private ViewType currentView;

    private final Map<String, Object> navigationParameters = new HashMap<>();

    public NavigationService(SpringFXMLLoader fxmlLoader, ThemeManager themeManager) {
        this.fxmlLoader = fxmlLoader;
        this.themeManager = themeManager;
    }

    public void setParameter(String key, Object value) {
        navigationParameters.put(key, value);
    }

    public Object getParameter(String key) {
        return navigationParameters.get(key);
    }

    public Object removeParameter(String key) {
        return navigationParameters.remove(key);
    }

    public void clearParameters() {
        navigationParameters.clear();
    }

    public void navigateRoot(ViewType viewType) {
        if (primaryStage == null) {
            log.error("Primary stage is null in NavigationService");
            return;
        }

        Platform.runLater(() -> {
            try {
                Parent root = fxmlLoader.load(viewType.getFxmlPath());
                Scene scene = new Scene(root);
                themeManager.registerScene(scene);

                primaryStage.setScene(scene);
                primaryStage.setTitle(viewType.getTitle());
                primaryStage.setMinWidth(1100);
                primaryStage.setMinHeight(700);
                primaryStage.show();
                currentView = viewType;
            } catch (IOException e) {
                log.error("Error cargando vista raíz: {}", viewType, e);
            }
        });
    }

    public void navigateTo(ViewType viewType) {
        if (contentArea == null) {
            log.warn("ContentArea no asignado en NavigationService, redirigiendo a MainLayout...");
            navigateRoot(ViewType.MAIN_LAYOUT);
            return;
        }

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = fxmlLoader.createLoader(viewType.getFxmlPath());
                Node viewNode = loader.load();
                contentArea.getChildren().setAll(viewNode);
                currentView = viewType;
            } catch (IOException e) {
                log.error("Error navegando al módulo interno: {}", viewType, e);
            }
        });
    }
}
