package com.cecamed.ui;

import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = "com.cecamed")
@EntityScan(basePackages = "com.cecamed.core.model")
@EnableJpaRepositories(basePackages = "com.cecamed.core.repository")
public class CecamedApplication {

    public static void main(String[] args) {
        Application.launch(JavaFxApplication.class, args);
    }
}
