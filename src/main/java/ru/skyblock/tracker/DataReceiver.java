package ru.skyblock.tracker;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ru.skyblock.tracker.DataRepackers.BazarFlip;
import ru.skyblock.tracker.DataRepackers.FlipItem;
import ru.skyblock.tracker.DataRepackers.ForgeFlip;
import ru.skyblock.tracker.DataRepackers.NPCFlip;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DataReceiver {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public List<FlipItem> getBazarFlips(int limit) throws Exception {
        String json = sendGetRequest("https://sky.coflnet.com/api/flip/bazaar/spread");
        JsonElement parsed = JsonParser.parseString(json);
        List<FlipItem> flips = new ArrayList<>();

        if (parsed.isJsonArray()) {
            for (JsonElement element : parsed.getAsJsonArray()) {
                JsonObject root = element.getAsJsonObject();
                if (root.has("isManipulated") && root.get("isManipulated").getAsBoolean()) continue;
                if (!root.has("flip") || root.get("flip").isJsonNull()) continue;

                BazarFlip flip = BazarFlip.fromJson(root);
                if (flip.buyOrderPrice() > 0 && flip.sellOrderPrice() > 0) {
                    flips.add(flip);
                }
            }
        }

        flips.sort(Comparator.comparingDouble(FlipItem::hourlyProfit).reversed());
        return flips.stream().limit(limit).toList();
    }

    public List<FlipItem> getNpcFlips(int limit) throws Exception {
        String json = sendGetRequest("https://sky.coflnet.com/api/flip/npc");
        JsonElement parsed = JsonParser.parseString(json);
        List<FlipItem> flips = new ArrayList<>();

        if (parsed.isJsonArray()) {
            for (JsonElement element : parsed.getAsJsonArray()) {
                JsonObject root = element.getAsJsonObject();

                NPCFlip flip = NPCFlip.fromJson(root);
                if (flip.buyOrderPrice() > 0 && flip.sellOrderPrice() > 0) {
                    flips.add(flip);
                }
            }
        }

        flips.sort(Comparator.comparingDouble(FlipItem::hourlyProfit).reversed());
        return flips.stream().limit(limit).toList();
    }

    public List<FlipItem> getForgeFlips(int limit) throws Exception {
        String json = sendGetRequest("https://sky.coflnet.com/api/flip/forge");
        JsonElement parsed = JsonParser.parseString(json);
        List<FlipItem> flips = new ArrayList<>();

        if (parsed.isJsonArray()) {
            for (JsonElement element : parsed.getAsJsonArray()) {
                JsonObject root = element.getAsJsonObject();
                if (!root.has("craftData") || root.get("craftData").isJsonNull()) continue;

                ForgeFlip flip = ForgeFlip.fromJson(root);
                if (flip.buyOrderPrice() > 0 && flip.sellOrderPrice() > 0) {
                    flips.add(flip);
                }
            }
        }

        flips.sort(Comparator.comparingDouble(FlipItem::hourlyProfit).reversed());
        return flips.stream().limit(limit).toList();
    }

    private String sendGetRequest(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
}