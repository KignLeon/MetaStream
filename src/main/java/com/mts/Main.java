package com.mts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import static spark.Spark.awaitInitialization;
import static spark.Spark.before;
import static spark.Spark.get;
import static spark.Spark.init;
import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.staticFiles;
import static spark.Spark.webSocket;

/**
 * LO1: OOP Principles - Modular class design LO6: GUI & Events - HTTP endpoints
 * triggered by frontend events LO7: Exception Handling - Try/catch in all
 * routes
 */
public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private static final Gson gson = new Gson();
    private static StreamSession activeSession = null;
    private static StreamSession lastSession = null;
    private static final FileLogger fileLogger = new FileLogger();

    public static void main(String[] args) {

        // Server configuration
        port(getAssignedPort());
        staticFiles.location("/public");

        // WebSocket for real-time chat (LO6: Event-driven)
        webSocket("/ws", WebSocketHandler.class);

        // CORS
        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            response.header("Access-Control-Allow-Headers", "Content-Type");
        });

        init();

        LOGGER.info("🌐 MetaStream Live Backend starting on port {}", getAssignedPort());

        if (MediaServerClient.isMediaServerHealthy()) {
            LOGGER.info("✅ FFmpeg media server is running");
        } else {
            LOGGER.warn("⚠️ FFmpeg media server not detected");
        }

        // Routes
        get("/", (req, res) -> {
            res.redirect("/index.html");
            return null;
        });

        /**
         * LO7: Exception Handling - Try/catch for API errors LO3: Aggregation -
         * User aggregated into StreamSession
         */
        post("/api/stream/start", (req, res) -> {
            res.type("application/json");

            try {
                if (!MediaServerClient.isMediaServerHealthy()) {
                    res.status(503);
                    return errorResponse("Media server unavailable");
                }

                if (activeSession != null && activeSession.isActive()) {
                    res.status(409);
                    return errorResponse("A stream is already active");
                }

                JsonObject data = gson.fromJson(req.body(), JsonObject.class);
                if (data == null || !data.has("username")) {
                    res.status(400);
                    return errorResponse("Missing username");
                }

                String username = data.get("username").getAsString();
                boolean ttsEnabled = data.has("ttsEnabled") && data.get("ttsEnabled").getAsBoolean();

                // LO1: Encapsulation - Using constructor and setters
                User user = new User(username);
                user.setPreferences(ttsEnabled);

                // LO3: Aggregation - StreamSession contains User
                activeSession = new StreamSession(user);
                activeSession.startSession();

                // Register with WebSocket handler
                WebSocketHandler.setActiveStreamSession(activeSession);
                WebSocketHandler.broadcastStreamStatus("started", username + " went live");

                LOGGER.info("🎬 Stream started by {}", username);

                JsonObject response = new JsonObject();
                response.addProperty("status", "success");
                response.addProperty("sessionId", activeSession.getSessionId());
                response.addProperty("rtmpUrl", activeSession.getRtmpIngestUrl());
                response.addProperty("hlsUrl", activeSession.getHlsPlaybackUrl());
                response.addProperty("message", "Stream session created");

                return gson.toJson(response);

            } catch (JsonSyntaxException e) {
                // LO7: Exception Handling
                LOGGER.error("Invalid JSON", e);
                res.status(400);
                return errorResponse("Invalid JSON format");
            } catch (Exception e) {
                // LO7: Exception Handling
                LOGGER.error("Error starting stream", e);
                res.status(500);
                return errorResponse("Internal server error: " + e.getMessage());
            }
        });

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
         * LO8: Text File I/O - Writes session log to disk
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

                lastSession = activeSession;
                activeSession.stopSession();

                // LO8: Text File I/O - Write log to file
                try {
                    fileLogger.writeLog(activeSession);
                    LOGGER.info("📝 Session log written to file");
                } catch (Exception e) {
                    LOGGER.error("Failed to write log file", e);
                }

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

        get("/api/stream/last", (req, res) -> {
            res.type("application/json");

            if (lastSession == null) {
                res.status(404);
                return errorResponse("No session history");
            }

            JsonObject response = new JsonObject();
            response.addProperty("status", "completed");
            response.addProperty("sessionId", lastSession.getSessionId());
            response.addProperty("username", lastSession.getUser().getUsername());
            response.addProperty("duration", lastSession.getDuration());
            response.addProperty("messages", lastSession.getTotalMessages());
            response.addProperty("peakViewers", lastSession.getPeakViewerCount());
            response.addProperty("notificationsSent", lastSession.getNotificationsSent());

            return gson.toJson(response);
        });

        /**
         * LO2: Arrays - Messages stored in ArrayList LO5: Generic Collections -
         * ArrayList<ChatMessage>
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

                // LO2 & LO5: Add to ArrayList<ChatMessage>
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
         * LO2: Arrays - Returns ArrayList LO5: Generic Collections -
         * ArrayList<ChatMessage>
         */
        get("/api/chat/messages", (req, res) -> {
            res.type("application/json");

            if (activeSession == null) {
                return "[]";
            }

            // LO2 & LO5: Return ArrayList as JSON
            return gson.toJson(activeSession.getMessages());
        });

        /**
         * LO8: Text File I/O - Reads log file
         */
        get("/api/logs", (req, res) -> {
            res.type("text/plain");
            try {
                return fileLogger.readLog();
            } catch (Exception e) {
                LOGGER.error("Error reading log", e);
                return "Error reading log file";
            }
        });

        get("/api/health", (req, res) -> {
            res.type("application/json");

            JsonObject health = new JsonObject();
            health.addProperty("backend", "ok");
            health.addProperty("mediaServer", MediaServerClient.isMediaServerHealthy());
            health.addProperty("activeSession", activeSession != null && activeSession.isActive());
            health.addProperty("websocketConnections", WebSocketHandler.getConnectionCount());

            return gson.toJson(health);
        });

        awaitInitialization();
        LOGGER.info("✅ MetaStream Live Backend ready at http://localhost:{}", getAssignedPort());
        LOGGER.info("🔌 WebSocket endpoint: ws://localhost:{}/ws", getAssignedPort());
    }

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
