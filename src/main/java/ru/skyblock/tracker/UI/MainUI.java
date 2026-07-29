package ru.skyblock.tracker.UI;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import ru.skyblock.tracker.DataRepackers.FlipItem;
import ru.skyblock.tracker.DataRepackers.SortOption;
import ru.skyblock.tracker.Services.CoflImageService;
import ru.skyblock.tracker.DataReceiver;
import ru.skyblock.tracker.Services.NumberFormatService;
import ru.skyblock.tracker.Services.ThemeService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainUI extends Application {
    private enum Mode {
        BAZAAR, NPC, FORGE, FAVORITES
    }

    private Mode currentMode = Mode.BAZAAR;

    private final DataReceiver receiver = new DataReceiver();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private FlowPane cardsContainer;
    private TextField searchline;
    private TextField budgetField;
    private ComboBox<SortOption> sortComboBox;
    private Label ghostLabel;
    private Label statusLabel;

    private Button navBazaar;
    private Button navNpc;
    private Button navCraft;
    private Button navFavorites;

    private final List<FlipItem> currentFlips = new ArrayList<>();

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("🚀 Hypixel SkyBlock - Bazaar Tracker");

        VBox sidebar = createSidebar();
        HBox topBar = createTopBar();
        ScrollPane scrollPane = createCardsScrollPane();

        VBox mainContent = new VBox(topBar, scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        BorderPane root = new BorderPane();
        root.setLeft(sidebar);
        root.setCenter(mainContent);

        Scene scene = new Scene(root, 1055, 720);

        boolean isDark = ThemeService.isDarkMode();
        setTheme(scene, isDark ? "/dark.css" : "/light.css");

        primaryStage.setScene(scene);
        primaryStage.show();

        startBackgroundUpdates();
    }

    private void setTheme(Scene scene, String cssPath) {
        scene.getStylesheets().clear();
        if (getClass().getResource(cssPath) != null) {
            scene.getStylesheets().add(getClass().getResource(cssPath).toExternalForm());
        }
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox(10);
        sidebar.setPrefWidth(220);
        sidebar.setPadding(new Insets(20, 15, 20, 15));
        sidebar.getStyleClass().add("sidebar");

        Label logoLabel = new Label("📈 SkyBlock Tracker");
        logoLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        logoLabel.getStyleClass().add("sidebar-logo");
        logoLabel.setPadding(new Insets(0, 0, 15, 5));

        navBazaar = createNavButton("💰 Заработок на базаре", true);
        navNpc = createNavButton("🏪 Заработок на NPC", false);
        navCraft = createNavButton("⚒ Заработок на кузнице", false);
        navFavorites = createNavButton("⭐ Избранные лоты", false);

        navBazaar.setOnAction(e -> switchTab(Mode.BAZAAR, navBazaar));
        navNpc.setOnAction(e -> switchTab(Mode.NPC, navNpc));
        navCraft.setOnAction(e -> switchTab(Mode.FORGE, navCraft));
        navFavorites.setOnAction(e -> switchTab(Mode.FAVORITES, navFavorites));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        ToggleButton themeToggle = new ToggleButton();

        boolean isDark = ThemeService.isDarkMode();
        themeToggle.setSelected(isDark);
        themeToggle.setText(isDark ? "🌙 Тёмная тема" : "☀ Светлая тема");
        themeToggle.setMaxWidth(Double.MAX_VALUE);
        themeToggle.getStyleClass().add("theme-toggle");

        themeToggle.setOnAction(e -> {
            Scene scene = themeToggle.getScene();
            boolean selected = themeToggle.isSelected();

            if (selected) {
                themeToggle.setText("🌙 Тёмная тема");
                setTheme(scene, "/dark.css");
            } else {
                themeToggle.setText("☀ Светлая тема");
                setTheme(scene, "/light.css");
            }

            ThemeService.setDarkMode(selected);
        });

        sidebar.getChildren().addAll(logoLabel, navBazaar, navNpc, navCraft, navFavorites, spacer, themeToggle);
        return sidebar;
    }

    private Button createNavButton(String text, boolean isActive) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        setButtonStyle(btn, isActive);
        return btn;
    }

    private final Map<Mode, List<FlipItem>> dataCache = new java.util.concurrent.ConcurrentHashMap<>();

    private void switchTab(Mode mode, Button activeBtn) {
        this.currentMode = mode;
        updateTabStyles(activeBtn);

        if (mode == Mode.FAVORITES) {
            updateFavoritesFromCache();
        } else if (dataCache.containsKey(mode)) {
            currentFlips.clear();
            currentFlips.addAll(dataCache.get(mode));
            filterAndSortCards();
        }

        fetchDataForCurrentMode();
    }

    private void updateFavoritesFromCache() {
        List<FlipItem> allFlips = new ArrayList<>();
        dataCache.values().forEach(allFlips::addAll);

        currentFlips.clear();
        currentFlips.addAll(allFlips.stream()
                .filter(item -> FavouritesManager.isFavorite(item.itemId()))
                .toList());

        filterAndSortCards();
    }

    private void updateTabStyles(Button activeBtn) {
        setButtonStyle(navBazaar, activeBtn == navBazaar);
        setButtonStyle(navNpc, activeBtn == navNpc);
        setButtonStyle(navCraft, activeBtn == navCraft);
        setButtonStyle(navFavorites, activeBtn == navFavorites);
    }

    private void setButtonStyle(Button btn, boolean isActive) {
        btn.getStyleClass().removeAll("nav-button", "nav-button-active");
        if (isActive) {
            btn.getStyleClass().add("nav-button-active");
        } else {
            btn.getStyleClass().add("nav-button");
        }
    }

    private HBox createTopBar() {
        StackPane searchContainer = createSearchContainer();

        budgetField = new TextField();
        budgetField.setPromptText("Макс. бюджет...");
        budgetField.setPrefWidth(150);
        budgetField.setFont(Font.font("Arial", 13));
        budgetField.getStyleClass().add("input-field");
        budgetField.textProperty().addListener((obs, oldVal, newVal) -> filterAndSortCards());

        sortComboBox = new ComboBox<>();
        sortComboBox.getItems().setAll(SortOption.values());
        sortComboBox.setValue(SortOption.HOURLY_PROFIT);
        sortComboBox.setPrefWidth(200);
        sortComboBox.getStyleClass().add("combo-box");
        sortComboBox.setOnAction(e -> filterAndSortCards());

        statusLabel = new Label("🔄 Инициализация...");
        statusLabel.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        statusLabel.getStyleClass().add("card-detail-label");

        HBox topBar = new HBox(12, searchContainer, budgetField, sortComboBox, statusLabel);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(12, 20, 12, 20));
        topBar.getStyleClass().add("top-bar");
        return topBar;
    }

    private ScrollPane createCardsScrollPane() {
        cardsContainer = new FlowPane();
        cardsContainer.setHgap(15);
        cardsContainer.setVgap(15);
        cardsContainer.setPadding(new Insets(20));
        cardsContainer.setAlignment(Pos.CENTER);
        cardsContainer.setPrefWrapLength(900);

        ScrollPane scrollPane = new ScrollPane(cardsContainer);
        scrollPane.setFitToWidth(true);
        return scrollPane;
    }

    private StackPane createSearchContainer() {
        searchline = new TextField();
        searchline.setPromptText("Поиск по названию...");
        searchline.setPrefWidth(200);
        searchline.setFont(Font.font("Arial", 13));
        searchline.setStyle("-fx-background-color: transparent; -fx-padding: 6 10;");

        searchline.textProperty().addListener((obs, oldText, newText) -> filterAndSortCards());

        ghostLabel = new Label();
        ghostLabel.setFont(Font.font("Arial", 13));
        ghostLabel.getStyleClass().add("card-detail-label");
        ghostLabel.setMouseTransparent(true);

        TabCompleter.attach(searchline, ghostLabel, () -> currentFlips);

        StackPane container = new StackPane(ghostLabel, searchline);
        container.setPrefWidth(200);
        container.setAlignment(Pos.CENTER_LEFT);
        container.getStyleClass().add("search-container");
        StackPane.setMargin(ghostLabel, new Insets(0, 0, 0, 10));

        return container;
    }

    private void filterAndSortCards() {
        String query = searchline.getText().toLowerCase().trim();
        double maxBudget = NumberFormatService.parseBudget(budgetField.getText());
        SortOption selectedSort = sortComboBox.getValue();

        List<FlipItem> filteredAndSorted = currentFlips.stream()
                .filter(flip -> query.isEmpty() || flip.itemName().toLowerCase().contains(query))
                .filter(flip -> maxBudget <= 0 || flip.buyOrderPrice() <= maxBudget)
                .filter(flip -> currentMode != Mode.FAVORITES || FavouritesManager.isFavorite(flip.itemId()))
                .sorted(selectedSort != null ? selectedSort.getComparator() : SortOption.HOURLY_PROFIT.getComparator())
                .toList();

        cardsContainer.getChildren().clear();

        if (filteredAndSorted.isEmpty()) {
            Label emptyLabel = new Label(currentMode == Mode.FAVORITES
                    ? "⭐ У вас пока нет избранных лотов"
                    : "🔍 Ничего не найдено");
            emptyLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
            emptyLabel.setStyle("-fx-text-fill: #888899; -fx-padding: 50 0 0 0;");
            cardsContainer.getChildren().add(emptyLabel);
            return;
        }

        List<FlipCard> newCards = new ArrayList<>();
        for (FlipItem flip : filteredAndSorted) {
            newCards.add(new FlipCard(flip, this::filterAndSortCards));
        }

        cardsContainer.getChildren().setAll(newCards);
    }

    private final java.util.concurrent.atomic.AtomicLong requestId = new java.util.concurrent.atomic.AtomicLong(0);

    private void fetchDataForCurrentMode() {
        long currentRequestId = requestId.incrementAndGet();

        CompletableFuture.runAsync(() -> {
            try {
                if (currentMode == Mode.FAVORITES) {
                    var bazaarFuture = CompletableFuture.supplyAsync(() -> {
                        try {
                            return receiver.getBazarFlips(100);
                        } catch (Exception e) {
                            return new ArrayList<FlipItem>();
                        }
                    });

                    var npcfuture = CompletableFuture.supplyAsync(() -> {
                        try {
                            return receiver.getNpcFlips(100);
                        } catch (Exception e) {
                            return new ArrayList<FlipItem>();
                        }
                    });

                    var forgeFuture = CompletableFuture.supplyAsync(() -> {
                        try {
                            return receiver.getForgeFlips(100);
                        } catch (Exception e) {
                            return new ArrayList<FlipItem>();
                        }
                    });

                    CompletableFuture.allOf(bazaarFuture, npcfuture, forgeFuture).join();

                    List<FlipItem> bazaar = bazaarFuture.join();
                    List<FlipItem> npc = npcfuture.join();
                    List<FlipItem> forge = forgeFuture.join();

                    if (bazaar != null) dataCache.put(Mode.BAZAAR, bazaar);
                    if (npc != null) dataCache.put(Mode.NPC, npc);
                    if (forge != null) dataCache.put(Mode.FORGE, forge);

                    Platform.runLater(() -> {
                        if (currentRequestId == requestId.get()) {
                            updateFavoritesFromCache();
                            statusLabel.setText("✅ Данные обновлены");
                        }
                    });
                } else {
                    List<FlipItem> fetchedData = switch (currentMode) {
                        case BAZAAR -> receiver.getBazarFlips(100);
                        case NPC -> receiver.getNpcFlips(100);
                        case FORGE -> receiver.getForgeFlips(100);
                        default -> List.of();
                    };

                    if (fetchedData != null && !fetchedData.isEmpty()) {
                        dataCache.put(currentMode, fetchedData);

                        Platform.runLater(() -> {
                            if (currentRequestId == requestId.get()) {
                                currentFlips.clear();
                                currentFlips.addAll(fetchedData);
                                filterAndSortCards();
                                statusLabel.setText("✅ Данные обновлены");
                            }
                        });
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void startBackgroundUpdates() {
        scheduler.scheduleAtFixedRate(() -> Platform.runLater(this::fetchDataForCurrentMode), 0, 25, TimeUnit.SECONDS);
    }

    @Override
    public void stop() {
        scheduler.shutdownNow();
        CoflImageService.shutdown();
        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}