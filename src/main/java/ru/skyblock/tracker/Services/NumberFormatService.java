package ru.skyblock.tracker.Services;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

public class NumberFormatService {

    private static final DecimalFormat INT_FORMAT;
    private static final DecimalFormat FLOAT_FORMAT;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator('.');

        INT_FORMAT = new DecimalFormat("#,###", symbols);
        FLOAT_FORMAT = new DecimalFormat("#,##0.0", symbols);
    }

    public static double parseBudget(String text) {
        if (text == null || text.trim().isEmpty()) return -1;

        try {
            String cleanStr = text.trim().toLowerCase().replaceAll(",", ".").replaceAll(" ", "");
            double multiplier = 1;

            if (cleanStr.endsWith("k")) {
                multiplier = 1_000;
                cleanStr = cleanStr.substring(0, cleanStr.length() - 1);
            } else if (cleanStr.endsWith("m")) {
                multiplier = 1_000_000;
                cleanStr = cleanStr.substring(0, cleanStr.length() - 1);
            } else if (cleanStr.endsWith("b")) {
                multiplier = 1_000_000_000;
                cleanStr = cleanStr.substring(0, cleanStr.length() - 1);
            }

            return Double.parseDouble(cleanStr) * multiplier;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static String formatInt(double value) {
        return INT_FORMAT.format(value);
    }

    public static String formatFloat(double value) {
        return FLOAT_FORMAT.format(value);
    }
}
