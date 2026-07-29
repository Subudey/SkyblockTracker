package ru.skyblock.tracker.UI;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import ru.skyblock.tracker.DataRepackers.BazarFlip;
import ru.skyblock.tracker.DataRepackers.FlipItem;
import ru.skyblock.tracker.DataRepackers.ForgeFlip;
import ru.skyblock.tracker.Services.CoflImageService;

public class FlipCard extends VBox {
    private final FlipItem flip;
    private final Runnable onFavoriteToggle;

    public FlipCard(FlipItem flip, Runnable onFavoriteToggle) {
        super(8);
        this.flip = flip;
        this.onFavoriteToggle = onFavoriteToggle;

        initStyle();
        buildUI();
    }

    public FlipCard(FlipItem flip) {
        this(flip, null);
    }

    private void initStyle() {
        setPadding(new Insets(15));
        setPrefWidth(250);
        getStyleClass().add("flip-card");
    }

    private void buildUI() {
        ImageView iconView = new ImageView();
        iconView.setFitWidth(32);
        iconView.setFitHeight(32);

        CoflImageService.setIcon(flip.itemId(), iconView);

        Button favBtn = new Button();
        updateFavButtonState(favBtn);

        favBtn.setOnAction(e -> {
            FavouritesManager.toggle(flip.itemId());
            updateFavButtonState(favBtn);
            if (onFavoriteToggle != null) {
                onFavoriteToggle.run();
            }
        });

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        HBox topHeader = new HBox(iconView, topSpacer, favBtn);
        topHeader.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(flip.itemName());
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 15));
        titleLabel.getStyleClass().add("card-title");
        titleLabel.setWrapText(true);
        titleLabel.setAlignment(Pos.CENTER);
        titleLabel.setPadding(new Insets(2, 6, 2, 6));

        CopyWindowUI.attachCopyToLabel(titleLabel, flip.itemName());

        Label hourlyProfitLabel = new Label(String.format("%,.0f / час", flip.hourlyProfit()));
        hourlyProfitLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        hourlyProfitLabel.getStyleClass().add("card-profit");

        Region spacer = new Region();
        spacer.setPrefHeight(4);

        Label unitProfitLabel = createFieldLabel("Профит/шт: ", String.format("%,.1f (%.1f%%)", flip.profitPerUnit(), flip.percentProfit()));
        Label buyLabel = createFieldLabel("Покупка: ", String.format("%,.1f", flip.buyOrderPrice()));
        Label sellLabel = createFieldLabel("Продажа: ", String.format("%,.1f", flip.sellOrderPrice()));

        VBox detailsBox = new VBox(4, unitProfitLabel, buyLabel, sellLabel);

        if (flip instanceof ForgeFlip forgeFlip) {
            long totalSeconds = forgeFlip.durationSeconds();
            long hours = totalSeconds / 3600;
            long minutes = (totalSeconds % 3600) / 60;

            String durationStr = hours > 0
                    ? (minutes > 0 ? String.format("%dч %dмин", hours, minutes) : String.format("%dч", hours))
                    : String.format("%dмин", minutes);

            Label durationLabel = createFieldLabel("Время крафта: ", durationStr);
            detailsBox.getChildren().add(durationLabel);
        } else {
            Label volumeLabel = createFieldLabel("Объём: ", String.format("~%.1f шт/час", flip.volume()));
            detailsBox.getChildren().add(volumeLabel);
        }

        getChildren().addAll(
                topHeader,
                titleLabel,
                hourlyProfitLabel,
                spacer,
                detailsBox
        );

        if (flip instanceof BazarFlip) {
            this.setCursor(javafx.scene.Cursor.HAND);
            this.setOnMouseClicked(e -> {
                boolean clickedTitle = titleLabel.equals(e.getTarget()) || titleLabel.getChildrenUnmodifiable().contains(e.getTarget());
                boolean clickedButton = e.getTarget() instanceof Button;

                if (!clickedTitle && !clickedButton) {
                    Stage stage = (Stage) this.getScene().getWindow();
                    PriceChartDialog.show(flip, stage);
                }
            });
        } else {
            this.setCursor(javafx.scene.Cursor.DEFAULT);
        }
    }

    private void updateFavButtonState(Button favBtn) {
        boolean isFav = FavouritesManager.isFavorite(flip.itemId());
        favBtn.setText(isFav ? "★" : "☆");
        favBtn.setStyle("-fx-background-color: transparent; -fx-font-size: 16px; -fx-cursor: hand; -fx-padding: 0; -fx-text-fill: " + (isFav ? "#FFD700;" : "#555566;"));
    }

    private Label createFieldLabel(String title, String value) {
        Label label = new Label(title + value);
        label.setFont(Font.font("Arial", 12));
        label.getStyleClass().add("card-detail-label");
        return label;
    }
}