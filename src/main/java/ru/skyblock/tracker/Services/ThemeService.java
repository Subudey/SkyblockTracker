package ru.skyblock.tracker.Services;

import java.util.prefs.Preferences;

public class ThemeService {
    private static final Preferences prefs = Preferences.userNodeForPackage(ThemeService.class);
    private static final String DARK_MODE_KEY = "dark_mode_enabled";

    public static void setDarkMode(boolean enabled) {
        prefs.putBoolean(DARK_MODE_KEY, enabled);
    }

    public static boolean isDarkMode() {
        return prefs.getBoolean(DARK_MODE_KEY, true);
    }
}
