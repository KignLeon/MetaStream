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
    private static StreamSession lastSession = null; // Persistence for Summary Page

    public static void main(String[] args) {
        port(8080);
        staticFiles.location("/public");

        // Register WebSocket BEFORE routes
        webSocket("/ws", WebSocketHandler.class);

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
                JsonObject body = gson.fromJson(req.body(), JsonObject.class);
                String username = (body != null && body.has("username")) ? body.get("username").getAsString() : "Streamer";
                
                activeSession = new StreamSession(new User(username));
                System.out.println("🎬 Stream started for: " + username);
                
                return gson.toJson(activeSession);
            } catch (Exception e) {
                res.status(400);
                return gson.toJson("Invalid Request");
            }
        });

        // API: Get Active Session (CRITICAL SYNC)
        get("/api/stream/session", (req, res) -> {
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

        // API: Stop Stream / Reset System
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
            activeSession = null; // Hard reset ghost sessions
            return gson.toJson("Reset");
        });

        // API: Serve the log file for downloading
        get("/download/log", (req, res) -> {
            res.type("text/plain");
            res.header("Content-Disposition", "attachment; filename=stream_log.txt");
            try {
                return Files.readString(Paths.get("stream_log.txt"));
            } catch (Exception e) {
                return "No logs available.";
            }
        });

        init();
        System.out.println("✅ MetaStream Backend Running on http://localhost:8080");
    }

    public static StreamSession getActiveSession() {
        return activeSession;
    }
}