package ru.kantser.pineview.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kantser.pineview.domain.model.ServiceStatus;

public class StatusIndicator extends HBox {
    private static final Logger log = LoggerFactory.getLogger(StatusIndicator.class);

    private final Circle dot;
    private final Label nameLabel;
    private final Label detailLabel;

    public StatusIndicator(String serviceName) {
        log.info("[StatusIndicator] [constructor] - Creating status indicator for service: {}", serviceName);

        setSpacing(10);
        setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        setPadding(new Insets(4, 0, 4, 0));

        dot = new Circle(7);
        dot.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 4, 0.5, 0, 1);");

        nameLabel = new Label(serviceName);
        nameLabel.setStyle("-fx-text-fill: #a6adc8; -fx-font-size: 12px;");

        detailLabel = new Label();
        detailLabel.setStyle("-fx-text-fill: #6c7086; -fx-font-size: 11px;");
        detailLabel.setMinWidth(80);

        getChildren().addAll(dot, nameLabel, detailLabel);
        updateStatus(ServiceStatus.CHECKING, "Initializing...");
    }

    public void updateStatus(ServiceStatus status, String detail) {
        log.info("[StatusIndicator] [updateStatus] - Updating status: {} - {}", status, detail);

        dot.setFill(Color.web(status.getColorHex()));
        detailLabel.setText(detail);
        detailLabel.setStyle("-fx-text-fill: " + status.getColorHex() + "; -fx-font-size: 11px;");
    }
}