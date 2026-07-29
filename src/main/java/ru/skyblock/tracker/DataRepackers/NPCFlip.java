package ru.skyblock.tracker.DataRepackers;

import com.google.gson.JsonObject;

public record NPCFlip(
        String itemName,
        String itemId,
        String npcName,
        double buyOrderPrice,
        double sellOrderPrice,
        double profitPerUnit,
        double percentProfit,
        double volume,
        double hourlyProfit
) implements FlipItem {

    public static NPCFlip fromJson(JsonObject root) {
        String itemName = root.has("itemName") && !root.get("itemName").isJsonNull()
                ? root.get("itemName").getAsString() : "UNKNOWN";
        String itemId = root.has("itemId") && !root.get("itemId").isJsonNull()
                ? root.get("itemId").getAsString() : "UNKNOWN";
        String npcName = root.has("npcName") && !root.get("npcName").isJsonNull()
                ? root.get("npcName").getAsString() : "UNKNOWN";

        double buyPrice = root.has("npcBuyPrice") ? root.get("npcBuyPrice").getAsDouble() : 0;
        double sellPrice = root.has("sellPrice") ? root.get("sellPrice").getAsDouble() : 0;
        double profitPerUnit = root.has("profit") ? root.get("profit").getAsDouble() : (sellPrice - buyPrice);

        double percentProfit = root.has("profitMargin") ? root.get("profitMargin").getAsDouble() * 100.0 : 0;
        double volume = root.has("volume") ? root.get("volume").getAsDouble() : 0;

        double calculatedHourlyProfit = profitPerUnit * volume;

        return new NPCFlip(
                itemName,
                itemId,
                npcName,
                buyPrice,
                sellPrice,
                profitPerUnit,
                percentProfit,
                volume,
                calculatedHourlyProfit
        );
    }
}