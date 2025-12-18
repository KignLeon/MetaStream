package com.mts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import static spark.Spark.*;

/**
 * MetaStream Live Backend - Phase 3 with WebSocket support
 */
public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private static final Gson gson = new Gson();
    private static StreamSession activeSession;

    public static void main(String[] args) {

        // ---------- SERVER CONFIGURATION ----------
        // CRITICAL: Set port BEFORE webSocket call
        int serverPort = getAssignedPort();
        port(serverPort);

        // CRITICAL: Configure WebSocket BEFORE any routes
        webSocket("/ws", WebSocketHandler.class);

        // Static files
        staticFiles.location("/public");

        // Enable CORS
        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            response.header("Access-Control-Allow-Headers", "Content-Type");
        });

        // Initialize Spark (this starts the embedded Jetty server)
        init();

        LOGGER.info("🌐 MetaStream Live Backend starting on port {}", serverPort);

        // Check media server health on startup
        if (MediaServerClient.isMediaServerHealthy()) {
            LOGGER.info("✅ FFmpeg media server is running");
        } else {
            LOGGER.warn("⚠️ FFmpeg media server not detected - streams will fail");
            LOGGER.warn("   Start it with: cd media-server && npm start");
        }

        // ---------- ROUTES ----------
        get("/", (req, res) -> {
            res.redirect("/index.html");
            return null;
        });

        // ---------- API ENDPOINTS ----------
        /**
         * POST /api/stream/start
         */
        post("/api/stream/start", (req, res) -> {
            res.type("application/json");

            try {
                if (!MediaServerClient.isMediaServerHealthy()) {
                    res.status(503);
                    return errorResponse("Media server unavailable. Start FFmpeg server first.");
                }

                if (activeSession != null && activeSession.isActive()) {
                    res.status(409);
                    return errorResponse("A stream is already active. Stop it first.");
                }

                JsonObject data = gson.fromJson(req.body(), JsonObject.class);
                if (data == null || !data.has("username")) {
                    res.status(400);
                    return errorResponse("Missing username");
                }

                String username = data.get("username").getAsString();
                boolean ttsEnabled = data.has("ttsEnabled") && data.get("ttsEnabled").getAsBoolean();

                User user = new User(username);
                user.setPreferences(ttsEnabled);

                activeSession = new StreamSession(user);
                activeSession.startSession();

                // Register session with WebSocket handler
                WebSocketHandler.setActiveStreamSession(activeSession);

                // Broadcast stream started
                WebSocketHandler.broadcastStreamStatus("started", username + " went live");

                LOGGER.info("🎬 Stream started by {}", username);

                JsonObject response = new JsonObject();
                response.addProperty("status", "success");
                response.addProperty("sessionId", activeSession.getSessionId());
                response.addProperty("rtmpUrl", activeSession.getRtmpIngestUrl());
                response.addProperty("hlsUrl", activeSession.getHlsPlaybackUrl());
                response.addProperty("message", "Stream session created. Configure OBS and start streaming.");

                return gson.toJson(response);

            } catch (JsonSyntaxException e) {
                LOGGER.error("Invalid JSON", e);
                res.status(400);
                return errorResponse("Invalid JSON format");
            } catch (Exception e) {
                LOGGER.error("Error starting stream", e);
                res.status(500);
                return errorResponse("Internal server error: " + e.getMessage());
            }
        });

        /**
         * GET /api/stream/active
         */
        get("/api/stream/active", (req, res) -> {
            res.type("application/json");

            if (activeSession == null || !activeSession.isActive()) {
                res.status(404);
                return errorResponse("No active stream");
            }

            JsonObject response = new JsonObject();
            response.addProperty("status", "active");
            response.addProperty("sessionId", activeSession.getSessionId());
            response.addProperty("username", activeSession.getUser().getUsername());
            response.addProperty("rtmpUrl", activeSession.getRtmpIngestUrl());
            response.addProperty("hlsUrl", activeSession.getHlsPlaybackUrl());
            response.addProperty("startedAt", activeSession.getStartedAt().toString());
            response.addProperty("duration", activeSession.getDuration());
            response.addProperty("messages", activeSession.getTotalMessages());
            response.addProperty("isLive", activeSession.isStreamingLive());
            response.addProperty("viewers", WebSocketHandler.getConnectionCount());

            return gson.toJson(response);
        });

        /**
         * POST /api/stream/stop
         */
        post("/api/stream/stop", (req, res) -> {
            res.type("application/json");

            if (activeSession == null || !activeSession.isActive()) {
                res.status(400);
                return errorResponse("No active stream to stop");
            }

            try {
                String username = activeSession.getUser().getUsername();
                String duration = activeSession.getDuration();
                int messages = activeSession.getTotalMessages();

                activeSession.stopSession();

                // Broadcast stream ended
                WebSocketHandler.broadcastStreamStatus("ended", username + " ended the stream");

                LOGGER.info("🛑 Stream stopped for {} - Duration: {}, Messages: {}",
                        username, duration, messages);

                JsonObject response = new JsonObject();
                response.addProperty("status", "success");
                response.addProperty("message", "Stream ended successfully");
                response.addProperty("duration", duration);
                response.addProperty("totalMessages", messages);
                response.addProperty("peakViewers", activeSession.getPeakViewerCount());
                response.addProperty("notificationsSent", activeSession.getNotificationsSent());

                return gson.toJson(response);

            } catch (Exception e) {
                LOGGER.error("Error stopping stream", e);
                res.status(500);
                return errorResponse("Failed to stop stream: " + e.getMessage());
            }
        });

        /**
         * POST /api/chat/send Now also broadcasts via WebSocket
         */
        post("/api/chat/send", (req, res) -> {
            res.type("application/json");

            if (activeSession == null || !activeSession.isActive()) {
                res.status(400);
                return errorResponse("No active stream");
            }

            try {
                JsonObject data = gson.fromJson(req.body(), JsonObject.class);
                if (!data.has("author") || !data.has("text")) {
                    res.status(400);
                    return errorResponse("Missing author or text");
                }

                String author = data.get("author").getAsString();
                String text = data.get("text").getAsString();

                ChatMessage message = new ChatMessage(author, text);
                activeSession.addMessage(message);

                // Broadcast via WebSocket
                JsonObject broadcast = new JsonObject();
                broadcast.addProperty("type", "chat");
                broadcast.addProperty("author", author);
                broadcast.addProperty("text", text);
                broadcast.addProperty("timestamp", message.getTimestamp().toString());
                WebSocketHandler.broadcastToAll(broadcast.toString());

                LOGGER.info("[CHAT] {}: {}", author, text);

                JsonObject response = new JsonObject();
                response.addProperty("status", "success");
                response.addProperty("message", "Message sent");

                return gson.toJson(response);

            } catch (JsonSyntaxException e) {
                LOGGER.error("Invalid message format", e);
                res.status(400);
                return errorResponse("Invalid message format");
            }
        });

        /**
         * GET /api/chat/messages
         */
        get("/api/chat/messages", (req, res) -> {
            res.type("application/json");

            if (activeSession == null) {
                return "[]";
            }

            return gson.toJson(activeSession.getMessages());
        });

        /**
         * GET /api/health
         */
        get("/api/health", (req, res) -> {
            res.type("application/json");

            JsonObject health = new JsonObject();
            health.addProperty("backend", "ok");
            health.addProperty("mediaServer", MediaServerClient.isMediaServerHealthy());
            health.addProperty("activeSession", activeSession != null && activeSession.isActive());
            health.addProperty("websocketConnections", WebSocketHandler.getConnectionCount());

            return gson.toJson(health);
        });

        // Wait for server to fully initialize
        awaitInitialization();

        LOGGER.info("✅ MetaStream Live Backend ready at http://localhost:{}", serverPort);
        LOGGER.info("🔌 WebSocket endpoint: ws://localhost:{}/ws", serverPort);
    }

    // ---------- HELPERS ----------
    private static int getAssignedPort() {
        String port = System.getenv("PORT");
        return port != null ? Integer.parseInt(port) : 8080;
    }

    private static String errorResponse(String message) {
        JsonObject error = new JsonObject();
        error.addProperty("status", "error");
        error.addProperty("message", message);
        return gson.toJson(error);
    }
}
