package ru.skyblock.tracker.UI;

import javafx.animation.PauseTransition;
import javafx.scene.control.Label;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.Popup;
import javafx.util.Duration;

public class CopyWindowUI {

    public static void attachCopyToLabel(Label label, String textToCopy) {
        Popup customTooltip = new Popup();
        Label tooltipLabel = new Label("📋 Скопировать");
        tooltipLabel.getStyleClass().add("custom-tooltip");
        customTooltip.getContent().add(tooltipLabel);

        label.setOnMouseMoved(e -> {
            if (!customTooltip.isShowing() && label.getScene() != null && label.getScene().getWindow() != null) {
                customTooltip.show(label.getScene().getWindow());
            }
            customTooltip.setX(e.getScreenX() + 12);
            customTooltip.setY(e.getScreenY() + 15);
        });

        label.setOnMouseExited(e -> {
            customTooltip.hide();
            resetTooltipStyle(tooltipLabel);
        });

        label.setOnMouseClicked(e -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(textToCopy);
            Clipboard.getSystemClipboard().setContent(content);

            tooltipLabel.setText("✅ Скопировано!");
            tooltipLabel.getStyleClass().removeAll("custom-tooltip");
            tooltipLabel.getStyleClass().add("custom-tooltip-copied");

            PauseTransition pause = new PauseTransition(Duration.seconds(1.5));
            pause.setOnFinished(ev -> {
                if (label.isHover()) {
                    resetTooltipStyle(tooltipLabel);
                }
            });
            pause.play();
        });
    }

    private static void resetTooltipStyle(Label tooltipLabel) {
        tooltipLabel.setText("📋 Скопировать");
        tooltipLabel.getStyleClass().removeAll("custom-tooltip-copied");
        if (!tooltipLabel.getStyleClass().contains("custom-tooltip")) {
            tooltipLabel.getStyleClass().add("custom-tooltip");
        }
    }
}
