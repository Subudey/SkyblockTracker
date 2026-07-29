package ru.skyblock.tracker.UI;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import ru.skyblock.tracker.DataRepackers.FlipItem;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class TabCompleter {

    private final TextField textField;
    private final Label ghostLabel;
    private final Supplier<List<FlipItem>> dataSupplier;

    private String currentSuggestion = "";

    public TabCompleter(TextField textField, Label ghostLabel, Supplier<List<FlipItem>> dataSupplier) {
        this.textField = textField;
        this.ghostLabel = ghostLabel;
        this.dataSupplier = dataSupplier;
        initListeners();
    }

    public static void attach(TextField textField, Label ghostLabel, Supplier<List<FlipItem>> dataSupplier) {
        new TabCompleter(textField, ghostLabel, dataSupplier);
    }

    private void initListeners() {
        textField.textProperty().addListener((obs, oldVal, newVal) -> updateGhostText(newVal));

        textField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.TAB) {
                if (!currentSuggestion.isEmpty()) {
                    event.consume();
                    textField.setText(currentSuggestion);
                    textField.positionCaret(currentSuggestion.length());
                    clearGhostText();
                }
            }
        });
    }

    private void updateGhostText(String input) {
        String query = input.trim().toLowerCase();

        if (query.isEmpty()) {
            clearGhostText();
            return;
        }

        List<FlipItem> items = dataSupplier.get();

        Optional<String> match = items.stream()
                .map(FlipItem::itemName)
                .filter(name -> name.toLowerCase().startsWith(query))
                .findFirst();

        if (match.isPresent()) {
            String fullMatch = match.get();
            currentSuggestion = fullMatch;

            String ghostPart = fullMatch.substring(input.length());

            ghostLabel.setText(input + ghostPart);
        } else {
            clearGhostText();
        }
    }

    private void clearGhostText() {
        currentSuggestion = "";
        ghostLabel.setText("");
    }
}
