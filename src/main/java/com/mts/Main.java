package com.mts;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import static spark.Spark.*;

public class Main {

    // Port configurations
    private static final int PORT = 8080;

    // Dependencies
    private static final Gson gson = new Gson();

    // State
    public static StreamSession activeSession = null;

    public static void main(String[] args) {
        port(PORT);
        staticFiles.location("/public");

        // Initialize WebSocket before routes
        webSocket("/ws", WebSocketHandler.class);

        init();

        // Check for media server availability on startup
        boolean mediaServerReady = MediaServerClient.checkHealth();
        if (mediaServerReady) {
            System.out.println("✅ FFmpeg media server is running");
        } else {
            System.err.println("⚠️ FFmpeg media server NOT detected on port 8000");
        }

        // Enable CORS
        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            response.header("Access-Control-Allow-Headers", "Content-Type");
        });

        // --- API Routes ---
        // Health check
        get("/api/health", (req, res) -> {
            res.type("application/json");
            JsonObject status = new JsonObject();
            status.addProperty("backend", "ok");
            status.addProperty("mediaServer", MediaServerClient.checkHealth());
            status.addProperty("activeSession", activeSession != null && activeSession.isActive());
            status.addProperty("websocketConnections", WebSocketHandler.getConnectionCount());
            return status;
        }, gson::toJson);

        // Start a new stream session
        post("/api/stream/start", (req, res) -> {
            res.type("application/json");

            // Check if media server is alive
            if (!MediaServerClient.checkHealth()) {
                res.status(503);
                return new ErrorResponse("Media server unavailable. Start FFmpeg server first.");
            }

            // Check if session already exists
            if (activeSession != null && activeSession.isActive()) {
                res.status(409);
                return new ErrorResponse("A stream is already active. Stop it first.");
            }

            try {
                User user = gson.fromJson(req.body(), User.class);
                if (user == null || user.getUsername() == null || user.getUsername().isEmpty()) {
                    res.status(400);
                    return new ErrorResponse("Username is required");
                }

                // Construct URLs
                String ingestUrl = "rtmp://localhost/live/stream";
                String playbackUrl = "http://localhost:8000/live/stream/index.m3u8";

                // Initialize session - Fixed constructor usage
                activeSession = new StreamSession(user.getUsername(), ingestUrl, playbackUrl);

                // Fixed: method 'startSession()' doesn't exist, session starts in constructor.
                // If needed, we can log it here.
                System.out.println("🎬 Stream started for " + activeSession.getUsername());

                // Register with WebSocket handler for logging
                WebSocketHandler.setActiveStreamSession(activeSession);

                // Notify clients
                WebSocketHandler.broadcastStreamStatus("live", "00:00:00");

                return activeSession;

            } catch (Exception e) {
                e.printStackTrace();
                res.status(500);
                return new ErrorResponse("Failed to start stream: " + e.getMessage());
            }
        }, gson::toJson);

        // Get active stream status
        get("/api/stream/active", (req, res) -> {
            res.type("application/json");
            if (activeSession != null && activeSession.isActive()) {
                JsonObject json = new JsonObject();
                json.addProperty("status", "active");
                json.addProperty("sessionId", activeSession.getSessionId());

                // Fixed: getUser() -> getUsername()
                json.addProperty("username", activeSession.getUsername());

                // Fixed: getRtmpIngestUrl() -> getIngestUrl()
                json.addProperty("rtmpUrl", activeSession.getIngestUrl());

                // Fixed: getHlsPlaybackUrl() -> getPlaybackUrl()
                json.addProperty("hlsUrl", activeSession.getPlaybackUrl());

                // Fixed: getStartedAt() doesn't exist, removing or would need getter for startTime
                // json.addProperty("startedAt", activeSession.getStartTime()); 
                json.addProperty("duration", activeSession.getDuration());

                // Fixed: getTotalMessages() -> getChatMessages().size()
                json.addProperty("messages", activeSession.getChatMessages().size());

                // Fixed: isStreamingLive() -> isActive() (already checked above, but valid for property)
                json.addProperty("isLive", activeSession.isActive());
                json.addProperty("viewers", WebSocketHandler.getConnectionCount());

                return json;
            } else {
                res.status(404);
                return new ErrorResponse("No active stream");
            }
        });

        // Stop stream
        post("/api/stream/stop", (req, res) -> {
            res.type("application/json");
            if (activeSession != null && activeSession.isActive()) {
                activeSession.stop(); // Fixed: stopSession() -> stop()

                // Log to file
                FileLogger.logSession(activeSession);

                // Notify clients
                WebSocketHandler.broadcastStreamStatus("offline", activeSession.getDuration());

                System.out.println("🛑 Stream ended for " + activeSession.getUsername());

                return new SuccessResponse("Stream stopped", activeSession);
            }
            res.status(404);
            return new ErrorResponse("No active stream to stop");
        }, gson::toJson);

        // Add chat message (via HTTP fallback if WS fails)
        post("/api/chat", (req, res) -> {
            res.type("application/json");
            if (activeSession == null || !activeSession.isActive()) {
                res.status(400);
                return new ErrorResponse("No active stream");
            }

            try {
                ChatMessage msg = gson.fromJson(req.body(), ChatMessage.class);
                // Fixed: addMessage -> addChatMessage
                activeSession.addChatMessage(msg);

                // Broadcast via WebSocket
                JsonObject chatJson = new JsonObject();
                chatJson.addProperty("type", "chat");
                chatJson.addProperty("author", msg.getAuthor());
                chatJson.addProperty("text", msg.getText());
                WebSocketHandler.broadcastToAll(chatJson.toString()); // Fixed: broadcastToAll expects String

                return new SuccessResponse("Message sent", null);
            } catch (Exception e) {
                res.status(400);
                return new ErrorResponse("Invalid message format");
            }
        }, gson::toJson);

        // Get chat history
        get("/api/chat/history", (req, res) -> {
            res.type("application/json");
            if (activeSession != null) {
                // Fixed: getMessages() -> getChatMessages()
                return activeSession.getChatMessages();
            }
            return new java.util.ArrayList<>();
        }, gson::toJson);

        System.out.println("✅ MetaStream Live Backend ready at http://localhost:" + PORT);
        System.out.println("🔌 WebSocket endpoint: ws://localhost:" + PORT + "/ws");

        awaitInitialization();
    }

    // Helper classes for JSON responses
    static class ErrorResponse {

        String error;

        public ErrorResponse(String error) {
            this.error = error;
        }
    }

    static class SuccessResponse {

        String message;
        Object data;

        public SuccessResponse(String message, Object data) {
            this.message = message;
            this.data = data;
        }
    }
}
