package com.cecamed.ui.theme;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Scene;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Component
public class ThemeManager {

    private final BooleanProperty darkModeProperty = new SimpleBooleanProperty(false);
    private final List<Scene> registeredScenes = new ArrayList<>();

    public void registerScene(Scene scene) {
        if (scene != null && !registeredScenes.contains(scene)) {
            registeredScenes.add(scene);
            applyThemeToScene(scene);
        }
    }

    public void unregisterScene(Scene scene) {
        registeredScenes.remove(scene);
    }

    public void setDarkMode(boolean darkMode) {
        darkModeProperty.set(darkMode);
        applyTheme();
    }

    public void toggleTheme() {
        setDarkMode(!isDarkMode());
    }

    public boolean isDarkMode() {
        return darkModeProperty.get();
    }

    public BooleanProperty darkModeProperty() {
        return darkModeProperty;
    }

    public void applyTheme() {
        if (isDarkMode()) {
            Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        } else {
            Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        }

        for (Scene scene : registeredScenes) {
            applyThemeToScene(scene);
        }
    }

    private void applyThemeToScene(Scene scene) {
        if (scene == null) return;

        scene.getStylesheets().clear();

        addStylesheetIfPresent(scene, "/css/custom.css");

        if (isDarkMode()) {
            addStylesheetIfPresent(scene, "/css/theme-dark.css");
        } else {
            addStylesheetIfPresent(scene, "/css/theme-light.css");
        }
    }

    private void addStylesheetIfPresent(Scene scene, String path) {
        URL resource = getClass().getResource(path);
        if (resource != null) {
            scene.getStylesheets().add(resource.toExternalForm());
        }
    }
}
