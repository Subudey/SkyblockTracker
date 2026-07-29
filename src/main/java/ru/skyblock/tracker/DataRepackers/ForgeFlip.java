package ru.skyblock.tracker.DataRepackers;

import com.google.gson.JsonObject;

import java.util.Map;

public record ForgeFlip(
        String itemName,
        String itemId,
        double buyOrderPrice,
        double sellOrderPrice,
        double profitPerUnit,
        double percentProfit,
        long durationSeconds,
        double hourlyProfit
) implements FlipItem {
    private static final Map<String, Long> FORGE_TIMES = Map.ofEntries(
            Map.entry("TITANIUM_DRILL_4", 14400L),
            Map.entry("TITANIUM_DRILL_3", 10800L),
            Map.entry("TITANIUM_DRILL_2", 7200L),
            Map.entry("TITANIUM_DRILL_1", 3600L),
            Map.entry("PETRIFIED_FUEL_TANK", 14400L),
            Map.entry("PERFECTLY_CUT_FUEL_TANK", 7200L),
            Map.entry("GEMSTONE_FUEL_TANK", 3600L),
            Map.entry("RUBY_POLISHED_DRILL_ENGINE", 7200L),
            Map.entry("SAPPHIRE_POLISHED_DRILL_ENGINE", 3600L),
            Map.entry("TITANIUM_PLATED_DRILL_ENGINE", 1800L)
    );

    public static ForgeFlip fromJson(JsonObject root) {
        JsonObject craftData = root.has("craftData") && !root.get("craftData").isJsonNull()
                ? root.getAsJsonObject("craftData") : new JsonObject();

        String rawName = craftData.has("itemName") && !craftData.get("itemName").isJsonNull()
                ? craftData.get("itemName").getAsString() : "UNKNOWN";
        String itemName = rawName.replaceAll("§[0-9a-fk-or]", "");

        String itemId = craftData.has("itemId") && !craftData.get("itemId").isJsonNull()
                ? craftData.get("itemId").getAsString() : "UNKNOWN";

        double buyPrice = craftData.has("buyOrderCraftCost") && craftData.get("buyOrderCraftCost").getAsDouble() > 0
                ? craftData.get("buyOrderCraftCost").getAsDouble()
                : (craftData.has("craftCost") ? craftData.get("craftCost").getAsDouble() : 0);

        double sellPrice = craftData.has("sellPrice") ? craftData.get("sellPrice").getAsDouble() : 0;

        double profitPerUnit = (sellPrice * 0.9875) - buyPrice;
        double percentProfit = buyPrice > 0 ? (profitPerUnit / buyPrice) * 100.0 : 0;

        long durationSeconds = FORGE_TIMES.getOrDefault(itemId, 3600L);

        double hours = durationSeconds / 3600.0;
        double hourlyProfit = hours > 0 ? (profitPerUnit / hours) : profitPerUnit;

        return new ForgeFlip(
                itemName,
                itemId,
                buyPrice,
                sellPrice,
                profitPerUnit,
                percentProfit,
                durationSeconds,
                hourlyProfit
        );
    }

    @Override
    public double volume() {
        return 1.0;
    }
}