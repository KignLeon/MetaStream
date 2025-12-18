package com.mts;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Client for communicating with the FFmpeg media server.
 */
public class MediaServerClient {

    private static final String MEDIA_SERVER_URL = "http://localhost:8000";
    private static final Gson gson = new Gson();
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /**
     * Check if the FFmpeg media server is running and healthy. FIXED: Renamed
     * from isMediaServerHealthy() to checkHealth() to match Main.java
     *
     * @return true if server is reachable and operational
     */
    public static boolean checkHealth() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(MEDIA_SERVER_URL + "/health"))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonObject health = gson.fromJson(response.body(), JsonObject.class);
                String status = health.get("status").getAsString();
                System.out.println("✅ Media server health: " + status);
                return "ok".equals(status);
            }

            System.out.println("⚠️ Media server returned status code: " + response.statusCode());
            return false;

        } catch (Exception e) {
            System.err.println("❌ Media server health check failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if a specific stream is currently active.
     *
     * @param streamKey the stream identifier (e.g., "stream")
     * @return true if HLS files exist for this stream
     */
    public static boolean isStreamActive(String streamKey) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(MEDIA_SERVER_URL + "/health"))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonObject health = gson.fromJson(response.body(), JsonObject.class);
                return health.has("streaming") && health.get("streaming").getAsBoolean();
            }

            return false;

        } catch (Exception e) {
            System.err.println("❌ Stream status check failed: " + e.getMessage());
            return false;
        }
    }
}
