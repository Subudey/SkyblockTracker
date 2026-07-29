package ru.skyblock.tracker.Services;

import javafx.application.Platform;
import javafx.geometry.VPos;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CoflImageService {
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    private static final ExecutorService pool = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });
    private static final Map<String, Image> cache = new ConcurrentHashMap<>();

    private static final Image PLACEHOLDER_IMAGE = createPlaceholderImage();

    public static void setIcon(String itemId, ImageView targetView) {
        if (itemId == null || itemId.isEmpty() || itemId.equalsIgnoreCase("UNKNOWN")) {
            targetView.setImage(PLACEHOLDER_IMAGE);
            return;
        }

        if (cache.containsKey(itemId)) {
            targetView.setImage(cache.get(itemId));
            return;
        }

        targetView.setImage(PLACEHOLDER_IMAGE);

        pool.submit(() -> {
            try {
                String directUrl = "https://sky.coflnet.com/static/icon/" + itemId;

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(directUrl))
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .GET()
                        .build();

                HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

                if (response.statusCode() == 200) {
                    Image img = new Image(response.body(), 32, 32, true, true);

                    if (!img.isError()) {
                        cache.put(itemId, img);
                        Platform.runLater(() -> targetView.setImage(img));
                        return;
                    }
                }
            } catch (Exception e) {
            }

            cache.put(itemId, PLACEHOLDER_IMAGE);
            Platform.runLater(() -> targetView.setImage(PLACEHOLDER_IMAGE));
        });
    }

    private static Image createPlaceholderImage() {
        Canvas canvas = new Canvas(32, 32);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        gc.setFill(Color.web("#2b2b36"));
        gc.fillRoundRect(1, 1, 30, 30, 6, 6);

        gc.setStroke(Color.web("#3f3f4e"));
        gc.setLineWidth(1.5);
        gc.strokeRoundRect(1, 1, 30, 30, 6, 6);

        gc.setFill(Color.web("#888899"));
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText("?", 16, 16);

        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);

        return canvas.snapshot(params, null);
    }

    public static void shutdown() {
        pool.shutdown();
    }
}
