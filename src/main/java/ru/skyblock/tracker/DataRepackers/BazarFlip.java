package ru.skyblock.tracker.DataRepackers;

import com.google.gson.JsonObject;

public record BazarFlip(
        String itemName,
        String itemId,
        double buyOrderPrice,
        double sellOrderPrice,
        double profitPerUnit,
        double percentProfit,
        double volume,
        double hourlyProfit
) implements FlipItem {
    public static BazarFlip fromJson(JsonObject root) {
        String itemName = root.has("itemName") && !root.get("itemName").isJsonNull()
                ? root.get("itemName").getAsString() : "UNKNOWN";

        JsonObject flipObj = root.getAsJsonObject("flip");

        String itemId = flipObj.has("itemTag") && !flipObj.get("itemTag").isJsonNull()
                ? flipObj.get("itemTag").getAsString() : "UNKNOWN";

        double buyPrice = flipObj.has("sellPrice") ? flipObj.get("sellPrice").getAsDouble() : 0;
        double sellPrice = flipObj.has("buyPrice") ? flipObj.get("buyPrice").getAsDouble() : 0;

        double hourlyProfit = flipObj.has("profitPerHour") ? flipObj.get("profitPerHour").getAsDouble() : 0;

        double rawVolume = flipObj.has("volume") ? flipObj.get("volume").getAsDouble() : 0;
        double volumePerHour = rawVolume / 168.0;

        double profitPerUnit = (sellPrice * 0.9875) - buyPrice;
        double percentProfit = buyPrice > 0 ? (profitPerUnit / buyPrice) * 100.0 : 0;

        return new BazarFlip(
                itemName,
                itemId,
                buyPrice,
                sellPrice,
                profitPerUnit,
                percentProfit,
                volumePerHour,
                hourlyProfit
        );
    }
}