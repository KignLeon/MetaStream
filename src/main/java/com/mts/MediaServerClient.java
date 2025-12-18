package com.mts;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * LO7: Exception Handling - Try/catch for HTTP requests
 */
public class MediaServerClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(MediaServerClient.class);
    private static final String MEDIA_SERVER_URL = "http://localhost:8000";
    private static final Gson gson = new Gson();
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public static boolean isMediaServerHealthy() {
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
                return "ok".equals(status);
            }
            return false;

        } catch (Exception e) {
            // LO7: Exception Handling
            LOGGER.debug("Media server not reachable: {}", e.getMessage());
            return false;
        }
    }

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
            return false;
        }
    }

    public static String getHLSUrl(String streamKey) {
        return String.format("%s/live/%s/index.m3u8", MEDIA_SERVER_URL, streamKey);
    }

    public static String getRecommendedRTMPUrl() {
        return "rtmp://localhost/live/stream";
    }
}
