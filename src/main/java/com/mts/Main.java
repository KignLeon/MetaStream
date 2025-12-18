package com.mts;

import static spark.Spark.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class Main {
    private static final Gson gson = new Gson();
    private static StreamSession activeSession = null;
    private static StreamSession lastSession = null; // DEMO FIX: Stats persistence

    public static void main(String[] args) {
        port(8080);
        staticFiles.location("/public");

        // Register WebSocket BEFORE routes
        webSocket("/ws", WebSocketHandler.class);

        // API: Health Check
        get("/api/health", (req, res) -> {
            res.type("application/json");
            Map<String, Object> health = new HashMap<>();
            health.put("status", "ok");
            health.put("sessionActive", activeSession != null);
            return gson.toJson(health);
        });

        // API: Start Stream
        post("/api/stream/start", (req, res) -> {
            res.type("application/json");
            if (activeSession != null) {
                res.status(409);
                JsonObject err = new JsonObject();
                err.addProperty("error", "Stream already active.");
                return gson.toJson(err);
            }

            try {
                String bodyStr = req.body();
                if (bodyStr == null || bodyStr.isEmpty()) {
                    res.status(400);
                    return gson.toJson("Missing JSON body");
                }
                
                JsonObject body = gson.fromJson(bodyStr, JsonObject.class);
                String username = body.has("username") ? body.get("username").getAsString() : "Streamer";
                
                activeSession = new StreamSession(new User(username));
                System.out.println("🎬 Stream started for: " + username);
                
                return gson.toJson(activeSession);
            } catch (Exception e) {
                res.status(400);
                return gson.toJson("Invalid Request Format");
            }
        });

        // API: Get Active Session (CRITICAL SYNC)
        get("/api/stream/active", (req, res) -> {
            res.type("application/json");
            if (activeSession == null) {
                res.status(404);
                return gson.toJson("No active session");
            }
            return gson.toJson(activeSession);
        });

        // API: Get Last Session (Summary Page fix)
        get("/api/stream/last", (req, res) -> {
            res.type("application/json");
            if (lastSession == null) {
                res.status(404);
                return gson.toJson("No history");
            }
            return gson.toJson(lastSession);
        });

        // API: Stop Stream
        post("/api/stream/stop", (req, res) -> {
            res.type("application/json");
            if (activeSession != null) {
                activeSession.stopSession();
                lastSession = activeSession; // SAVE DATA BEFORE CLEARING
                
                FileLogger logger = new FileLogger();
                logger.logChat("SYSTEM", "Stream Ended. Duration: " + lastSession.getDuration());
                
                activeSession = null;
                System.out.println("🛑 Stream stopped.");
                return gson.toJson("Stopped");
            }
            activeSession = null; // Clean up ghost session
            return gson.toJson("Reset");
        });

        // Serve Log File
        get("/stream_log.txt", (req, res) -> {
            res.type("text/plain");
            try {
                return Files.readString(Paths.get("stream_log.txt"));
            } catch (Exception e) {
                return "No logs yet.";
            }
        });

        init();
        System.out.println("✅ MetaStream Backend Running on http://localhost:8080");
    }

    public static StreamSession getActiveSession() {
        return activeSession;
    }
}