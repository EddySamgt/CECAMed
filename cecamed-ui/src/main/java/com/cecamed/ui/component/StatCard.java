package com.cecamed.ui.component;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

public class StatCard extends HBox {

    private final Label titleLabel = new Label();
    private final Label valueLabel = new Label();
    private final Label subtitleLabel = new Label();
    private final FontIcon icon = new FontIcon();
    private final VBox textContainer = new VBox();
    private final VBox iconWrapper = new VBox();

    public StatCard() {
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(16);
        getStyleClass().addAll("cecamed-card", "stat-card");

        titleLabel.getStyleClass().add("stat-title");
        valueLabel.getStyleClass().add("stat-value");
        subtitleLabel.getStyleClass().add("stat-subtitle");

        textContainer.getChildren().addAll(titleLabel, valueLabel, subtitleLabel);
        textContainer.setSpacing(4);
        HBox.setHgrow(textContainer, Priority.ALWAYS);

        icon.setIconSize(24);
        iconWrapper.setAlignment(Pos.CENTER);
        iconWrapper.getChildren().add(icon);
        iconWrapper.getStyleClass().add("stat-icon-wrapper");

        getChildren().addAll(textContainer, iconWrapper);
    }

    public StatCard(String title, String value, String subtitle, String iconLiteral, String styleModifier) {
        this();
        setData(title, value, subtitle, iconLiteral, styleModifier);
    }

    public void setData(String title, String value, String subtitle, String iconLiteral, String styleModifier) {
        titleLabel.setText(title);
        valueLabel.setText(value);
        subtitleLabel.setText(subtitle);
        icon.setIconLiteral(iconLiteral);
        if (styleModifier != null && !styleModifier.isBlank()) {
            getStyleClass().add(styleModifier);
            iconWrapper.getStyleClass().add(styleModifier + "-icon");
        }
    }

    public void setValue(String value) {
        valueLabel.setText(value);
    }
}
