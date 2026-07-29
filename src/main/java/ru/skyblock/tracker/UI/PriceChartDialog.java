package ru.skyblock.tracker.UI;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Stage;
import ru.skyblock.tracker.DataRepackers.FlipItem;
import ru.skyblock.tracker.Services.PriceHistoryService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PriceChartDialog {

    private static String activeRange = "day";

    public static void show(FlipItem flip, Stage ownerStage) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(ownerStage);
        dialog.setTitle("📊 История цен: " + flip.itemName());

        Label title = new Label(flip.itemName());
        title.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        title.getStyleClass().add("dialog-title");

        Label profitInfo = new Label(String.format("Текущий профит: %,.0f / час (%.1f%%)", flip.hourlyProfit(), flip.percentProfit()));
        profitInfo.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        profitInfo.getStyleClass().add("card-profit");

        VBox headerBox = new VBox(4, title, profitInfo);
        headerBox.setAlignment(Pos.CENTER);

        Button btnHour = createRangeButton("За час");
        Button btnDay = createRangeButton("За день");
        Button btnWeek = createRangeButton("За неделю");

        HBox rangeBox = new HBox(10, btnHour, btnDay, btnWeek);
        rangeBox.setAlignment(Pos.CENTER);

        ProgressIndicator loadingIndicator = new ProgressIndicator();
        loadingIndicator.setMaxSize(30, 30);

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setAnimated(false);

        NumberAxis yAxis = new NumberAxis();
        yAxis.setForceZeroInRange(false);

        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setAnimated(false);
        lineChart.setCreateSymbols(false);
        lineChart.setLegendVisible(true);
        VBox.setVgrow(lineChart, Priority.ALWAYS);

        Popup tooltipPopup = new Popup();
        Label tooltipLabel = new Label();
        tooltipLabel.getStyleClass().add("chart-tooltip");
        tooltipPopup.getContent().add(tooltipLabel);

        VBox root = new VBox(12, headerBox, rangeBox, loadingIndicator, lineChart);
        root.setPadding(new Insets(15));
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("dialog-root");

        Scene scene = new Scene(root, 800, 540);

        if (ownerStage.getScene() != null) {
            scene.getStylesheets().addAll(ownerStage.getScene().getStylesheets());
        }

        List<PriceHistoryService.PricePoint> currentHistory = new ArrayList<>();

        Runnable hideTooltip = () -> {
            if (tooltipPopup.isShowing()) tooltipPopup.hide();
        };

        Runnable loadData = () -> {
            lineChart.setVisible(false);
            loadingIndicator.setVisible(true);
            hideTooltip.run();

            updateButtonStyles(btnHour, btnDay, btnWeek);

            CompletableFuture.supplyAsync(() -> PriceHistoryService.getRealHistory(flip.itemId(), activeRange))
                    .thenAccept(history -> Platform.runLater(() -> {
                        lineChart.getData().clear();
                        currentHistory.clear();
                        currentHistory.addAll(history);

                        XYChart.Series<String, Number> sellSeries = new XYChart.Series<>();
                        sellSeries.setName("Sell offer");

                        XYChart.Series<String, Number> buySeries = new XYChart.Series<>();
                        buySeries.setName("Buy order");

                        for (PriceHistoryService.PricePoint point : history) {
                            sellSeries.getData().add(new XYChart.Data<>(point.timestamp(), point.sellPrice()));
                            buySeries.getData().add(new XYChart.Data<>(point.timestamp(), point.buyPrice()));
                        }

                        lineChart.getData().addAll(sellSeries, buySeries);

                        loadingIndicator.setVisible(false);
                        lineChart.setVisible(true);
                    }));
        };

        lineChart.addEventHandler(MouseEvent.MOUSE_MOVED, event -> {
            if (currentHistory.isEmpty() || !lineChart.isVisible()) return;

            Node plotBackground = lineChart.lookup(".chart-plot-background");
            if (plotBackground == null) return;

            Point2D mouseInPlot = plotBackground.sceneToLocal(event.getSceneX(), event.getSceneY());
            double plotWidth = plotBackground.getLayoutBounds().getWidth();
            double plotHeight = plotBackground.getLayoutBounds().getHeight();

            if (mouseInPlot.getX() < 0 || mouseInPlot.getY() < 0 ||
                    mouseInPlot.getX() > plotWidth || mouseInPlot.getY() > plotHeight) {
                hideTooltip.run();
                return;
            }

            double percentX = mouseInPlot.getX() / plotWidth;
            int index = (int) Math.round(percentX * (currentHistory.size() - 1));
            index = Math.max(0, Math.min(currentHistory.size() - 1, index));

            PriceHistoryService.PricePoint pointData = currentHistory.get(index);

            tooltipLabel.setText(String.format(
                    "⏰ Время: %s\n📈 Sell offer: %,.0f\n📉 Buy order: %,.0f",
                    pointData.timestamp(),
                    pointData.sellPrice(),
                    pointData.buyPrice()
            ));

            if (!tooltipPopup.isShowing()) {
                tooltipPopup.show(scene.getWindow(), event.getScreenX() + 14, event.getScreenY() + 14);
            } else {
                tooltipPopup.setX(event.getScreenX() + 14);
                tooltipPopup.setY(event.getScreenY() + 14);
            }
        });

        lineChart.setOnMouseExited(e -> hideTooltip.run());

        btnHour.setOnAction(e -> {
            activeRange = "hour";
            loadData.run();
        });
        btnDay.setOnAction(e -> {
            activeRange = "day";
            loadData.run();
        });
        btnWeek.setOnAction(e -> {
            activeRange = "week";
            loadData.run();
        });

        dialog.setScene(scene);
        loadData.run();

        dialog.showAndWait();
    }

    private static Button createRangeButton(String text) {
        Button btn = new Button(text);
        btn.getStyleClass().add("theme-toggle");
        return btn;
    }

    private static void updateButtonStyles(Button btnHour, Button btnDay, Button btnWeek) {
        setButtonStyle(btnHour, "hour".equals(activeRange));
        setButtonStyle(btnDay, "day".equals(activeRange));
        setButtonStyle(btnWeek, "week".equals(activeRange));
    }

    private static void setButtonStyle(Button btn, boolean isActive) {
        if (isActive) {
            btn.setStyle("-fx-background-color: #0969da; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 6 14; -fx-background-radius: 6;");
        } else {
            btn.setStyle("-fx-background-color: #2b2b36; -fx-text-fill: #aaaaaa; -fx-cursor: hand; -fx-padding: 6 14; -fx-background-radius: 6;");
        }
    }
}