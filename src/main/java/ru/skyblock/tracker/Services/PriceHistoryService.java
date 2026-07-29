package ru.skyblock.tracker.Services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PriceHistoryService {
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public record PricePoint(String timestamp, double buyPrice, double sellPrice) {
    }

    public static List<PricePoint> getRealHistory(String itemId, String range) {
        List<PricePoint> points = new ArrayList<>();

        try {
            String url = "https://sky.coflnet.com/api/bazaar/" + itemId + "/history/" + range;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "SkyBlockTrackerApp/1.0")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String json = response.body();

                Pattern objPattern = Pattern.compile("\\{[^\\}]*\\}");
                Matcher objMatcher = objPattern.matcher(json);

                List<PricePoint> rawPoints = new ArrayList<>();

                DateTimeFormatter timeFormatter = "week".equals(range)
                        ? DateTimeFormatter.ofPattern("dd.MM HH:mm")
                        : DateTimeFormatter.ofPattern("HH:mm");

                while (objMatcher.find()) {
                    String block = objMatcher.group();

                    double buy = parseDoubleField(block, "buy");
                    double sell = parseDoubleField(block, "sell");
                    String timeStr = parseStringField(block, "timestamp");

                    if (!timeStr.isEmpty()) {
                        String formattedTime = timeStr;
                        try {
                            String cleanTime = timeStr.split("\\.")[0].replace("Z", "");

                            LocalDateTime utcDateTime = LocalDateTime.parse(cleanTime);
                            ZonedDateTime utcZone = utcDateTime.atZone(ZoneOffset.UTC);
                            ZonedDateTime localZone = utcZone.withZoneSameInstant(ZoneId.systemDefault());

                            formattedTime = localZone.format(timeFormatter);
                        } catch (Exception e) {
                            System.err.println("Ошибка конвертации времени: " + timeStr);
                        }

                        if (buy > 0 && sell > 0) {
                            rawPoints.add(new PricePoint(formattedTime, buy, sell));
                        }
                    }
                }

                java.util.Collections.reverse(rawPoints);

                if (!rawPoints.isEmpty()) {
                    int step = Math.max(1, rawPoints.size() / 15);
                    for (int i = 0; i < rawPoints.size(); i += step) {
                        points.add(rawPoints.get(i));
                    }
                    PricePoint last = rawPoints.get(rawPoints.size() - 1);
                    if (points.isEmpty() || !points.get(points.size() - 1).equals(last)) {
                        points.add(last);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка при получении истории цен (" + range + ") для " + itemId + ": " + e.getMessage());
        }

        return points;
    }

    private static double parseDoubleField(String jsonBlock, String fieldName) {
        Pattern p = Pattern.compile("\"" + fieldName + "\"\\s*:\\s*([\\d\\.]+)");
        Matcher m = p.matcher(jsonBlock);
        return m.find() ? Double.parseDouble(m.group(1)) : 0.0;
    }

    private static String parseStringField(String jsonBlock, String fieldName) {
        Pattern p = Pattern.compile("\"" + fieldName + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher m = p.matcher(jsonBlock);
        return m.find() ? m.group(1) : "";
    }
}
