package com.mts;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import static spark.Spark.*;

public class Main {
    private static final int PORT = 8080;
    private static final Gson gson = new Gson();
    
    // Public static to allow access from WebSocketHandler
    public static StreamSession activeSession = null;

    public static void main(String[] args) {
        port(PORT);
        staticFiles.location("/public");
        
        // Initialize WebSocket BEFORE routes/init
        webSocket("/ws", WebSocketHandler.class);
        
        init();

        // Health Check Log
        if (MediaServerClient.checkHealth()) {
            System.out.println("✅ FFmpeg media server is running");
        } else {
            System.err.println("⚠️ FFmpeg media server NOT detected on port 8000");
        }

        // CORS Headers
        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            response.header("Access-Control-Allow-Headers", "Content-Type");
        });

        // 1. Health Endpoint
        get("/api/health", (req, res) -> {
            res.type("application/json");
            JsonObject status = new JsonObject();
            status.addProperty("backend", "ok");
            status.addProperty("mediaServer", MediaServerClient.checkHealth());
            status.addProperty("activeSession", activeSession != null && activeSession.isActive());
            status.addProperty("websocketConnections", WebSocketHandler.getConnectionCount());
            return status;
        }, gson::toJson);

        // 2. Start Stream Endpoint
        post("/api/stream/start", (req, res) -> {
            res.type("application/json");
            
            if (!MediaServerClient.checkHealth()) {
                res.status(503);
                return new ErrorResponse("Media server unavailable. Start FFmpeg server first.");
            }

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

                // FIXED: Manual construction using Strings
                String ingestUrl = "rtmp://localhost/live/stream";
                String playbackUrl = "http://localhost:8000/live/stream/index.m3u8";

                // Correct constructor call matching StreamSession.java
                activeSession = new StreamSession(user.getUsername(), ingestUrl, playbackUrl);
                
                System.out.println("🎬 Stream started for " + activeSession.getUsername());

                // Set session for logging and notify clients
                WebSocketHandler.setActiveStreamSession(activeSession);
                WebSocketHandler.broadcastStreamStatus("live", "00:00:00");

                return activeSession;

            } catch (Exception e) {
                e.printStackTrace();
                res.status(500);
                return new ErrorResponse("Failed to start stream: " + e.getMessage());
            }
        }, gson::toJson);

        // 3. Active Stream Status
        get("/api/stream/active", (req, res) -> {
            res.type("application/json");
            if (activeSession != null && activeSession.isActive()) {
                JsonObject json = new JsonObject();
                json.addProperty("status", "active");
                json.addProperty("sessionId", activeSession.getSessionId());
                json.addProperty("username", activeSession.getUsername());
                json.addProperty("rtmpUrl", activeSession.getIngestUrl());
                json.addProperty("hlsUrl", activeSession.getPlaybackUrl());
                json.addProperty("duration", activeSession.getDuration());
                json.addProperty("messages", activeSession.getChatMessages().size());
                json.addProperty("isLive", activeSession.isActive()); 
                json.addProperty("viewers", WebSocketHandler.getConnectionCount());
                return json;
            } else {
                res.status(404);
                return new ErrorResponse("No active stream");
            }
        });

        // 4. Stop Stream
        post("/api/stream/stop", (req, res) -> {
            res.type("application/json");
            if (activeSession != null && activeSession.isActive()) {
                activeSession.stop();
                FileLogger.logSession(activeSession);
                WebSocketHandler.broadcastStreamStatus("offline", activeSession.getDuration());
                System.out.println("🛑 Stream ended for " + activeSession.getUsername());
                return new SuccessResponse("Stream stopped", activeSession);
            }
            res.status(404);
            return new ErrorResponse("No active stream to stop");
        }, gson::toJson);
        
        // 5. Chat Fallback
        post("/api/chat", (req, res) -> {
            res.type("application/json");
            if (activeSession == null || !activeSession.isActive()) {
                res.status(400);
                return new ErrorResponse("No active stream");
            }
            try {
                ChatMessage msg = gson.fromJson(req.body(), ChatMessage.class);
                activeSession.addChatMessage(msg);
                
                JsonObject chatJson = new JsonObject();
                chatJson.addProperty("type", "chat");
                chatJson.addProperty("author", msg.getAuthor());
                chatJson.addProperty("text", msg.getText());
                WebSocketHandler.broadcastToAll(chatJson.toString());
                
                return new SuccessResponse("Message sent", null);
            } catch (Exception e) {
                res.status(400);
                return new ErrorResponse("Invalid message format");
            }
        }, gson::toJson);
        
        // 6. Chat History
        get("/api/chat/history", (req, res) -> {
            res.type("application/json");
            if (activeSession != null) {
                return activeSession.getChatMessages();
            }
            return new java.util.ArrayList<>();
        }, gson::toJson);

        System.out.println("✅ MetaStream Live Backend ready at http://localhost:" + PORT);
        System.out.println("🔌 WebSocket endpoint: ws://localhost:" + PORT + "/ws");
        
        awaitInitialization();
    }
    
    static class ErrorResponse {
        String error;
        public ErrorResponse(String error) { this.error = error; }
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