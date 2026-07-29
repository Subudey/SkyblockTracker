package ru.skyblock.tracker.UI;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

public class FavouritesManager {
    private static final String FILE_PATH = "favorites.txt";
    private static final Set<String> favorites = new HashSet<>();

    static {
        load();
    }

    public static boolean isFavorite(String itemId) {
        return favorites.contains(itemId);
    }

    public static void toggle(String itemId) {
        if (favorites.contains(itemId)) {
            favorites.remove(itemId);
        } else {
            favorites.add(itemId);
        }
        save();
    }

    public static Set<String> getFavorites() {
        return favorites;
    }

    private static void load() {
        try {
            Path path = Paths.get(FILE_PATH);
            if (Files.exists(path)) {
                favorites.addAll(Files.readAllLines(path));
            }
        } catch (IOException e) {
            System.err.println("Ошибка загрузки избранного: " + e.getMessage());
        }
    }

    private static void save() {
        try {
            Files.write(Paths.get(FILE_PATH), favorites);
        } catch (IOException e) {
            System.err.println("Ошибка сохранения избранного: " + e.getMessage());
        }
    }
}
