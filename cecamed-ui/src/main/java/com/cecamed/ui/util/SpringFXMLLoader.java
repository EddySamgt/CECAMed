package com.cecamed.ui.util;

import javafx.fxml.FXMLLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;

@Component
@RequiredArgsConstructor
public class SpringFXMLLoader {

    private final ApplicationContext context;

    public <T> T load(String fxmlPath) throws IOException {
        FXMLLoader loader = createLoader(fxmlPath);
        return loader.load();
    }

    public FXMLLoader createLoader(String fxmlPath) {
        URL resource = getClass().getResource(fxmlPath);
        if (resource == null) {
            throw new IllegalArgumentException("No se encontró el recurso FXML: " + fxmlPath);
        }
        FXMLLoader loader = new FXMLLoader(resource);
        loader.setControllerFactory(context::getBean);
        return loader;
    }
}
