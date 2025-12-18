package com.mts;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Client for communicating with the FFmpeg media server. Handles health checks,
 * stream validation, and HLS endpoint generation.
 */
public class MediaServerClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(MediaServerClient.class);
    private static final String MEDIA_SERVER_URL = "http://localhost:8000";
    private static final Gson gson = new Gson();
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /**
     * Check if the FFmpeg media server is running and healthy.
     *
     * @return true if server is reachable and operational
     */
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
                LOGGER.info("✅ Media server health: {}", status);
                return "ok".equals(status);
            }

            LOGGER.warn("⚠️ Media server returned status code: {}", response.statusCode());
            return false;

        } catch (Exception e) {
            LOGGER.error("❌ Media server health check failed: {}", e.getMessage());
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
            LOGGER.error("❌ Stream status check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Generate the HLS playback URL for the viewer.
     *
     * @param streamKey the stream identifier
     * @return full HLS URL (e.g., http://localhost:8000/live/stream/index.m3u8)
     */
    public static String getHLSUrl(String streamKey) {
        return String.format("%s/live/%s/index.m3u8", MEDIA_SERVER_URL, streamKey);
    }

    /**
     * Validate RTMP URL format.
     *
     * @param rtmpUrl the RTMP URL to validate
     * @return true if format is valid
     */
    public static boolean isValidRTMPUrl(String rtmpUrl) {
        if (rtmpUrl == null || rtmpUrl.trim().isEmpty()) {
            return false;
        }

        // For local FFmpeg server, we expect rtmp://localhost/live/stream
        // Users should use this as their OBS server URL
        String normalized = rtmpUrl.trim().toLowerCase();
        return normalized.startsWith("rtmp://") && normalized.contains("localhost");
    }

    /**
     * Get the recommended RTMP URL for OBS.
     *
     * @return RTMP URL string
     */
    public static String getRecommendedRTMPUrl() {
        return "rtmp://localhost/live/stream";
    }
}
