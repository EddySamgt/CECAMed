package com.cecamed.ui;

import com.cecamed.ui.event.StageReadyEvent;
import com.cecamed.ui.navigation.NavigationService;
import com.cecamed.ui.navigation.ViewType;
import com.cecamed.ui.theme.ThemeManager;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StageInitializer implements ApplicationListener<StageReadyEvent> {

    private final NavigationService navigationService;
    private final ThemeManager themeManager;

    @Override
    public void onApplicationEvent(StageReadyEvent event) {
        Stage stage = event.getStage();
        log.info("Inicializando Stage principal de CECAMed...");
        themeManager.applyTheme();
        navigationService.setPrimaryStage(stage);
        navigationService.navigateRoot(ViewType.LOGIN);
    }
}
