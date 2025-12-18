package com.mts;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import static spark.Spark.awaitInitialization;
import static spark.Spark.before;
import static spark.Spark.get;
import static spark.Spark.init;
import static spark.Spark.notFound;
import static spark.Spark.options;
import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.staticFiles;
import static spark.Spark.webSocket;

/**
 * Main Backend for MetaStream Live
 * Handles stream session management, WebSocket chat, and file logging
 * 
 * Learning Outcomes:
 * LO1: OOP Principles (Encapsulation in User, StreamSession)
 * LO3: Aggregation (StreamSession contains User)
 * LO6: GUI & Events (HTTP endpoints, WebSocket)
 * LO7: Exception Handling (try-catch blocks)
 * LO8: File I/O (Log file operations)
 */
public class Main {
    private static final Gson gson = new Gson();
    
    // CRITICAL: Explicitly initialize to null to prevent ghost sessions
    private static StreamSession activeSession = null;
    private static StreamSession lastSession = null; // For summary page

    public static void main(String[] args) {
        // ================================================================
        // FORCE RESET: Clear any ghost session state on startup
        // ================================================================
        activeSession = null;
        lastSession = null;
        System.out.println("🔄 System initialized - all session state cleared");
        
        // ================================================================
        // Server Configuration
        // ================================================================
        port(8080);
        staticFiles.location("/public");
        
        // ================================================================
        // WebSocket Registration (CRITICAL: Must be BEFORE any routes)
        // ================================================================
        webSocket("/ws", WebSocketHandler.class);
        
        // ================================================================
        // CORS Configuration (Must come AFTER WebSocket)
        // ================================================================
        options("/*", (req, res) -> {
            String accessControlRequestHeaders = req.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                res.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }
            
            String accessControlRequestMethod = req.headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                res.header("Access-Control-Allow-Methods", accessControlRequestMethod);
            }
            
            return "OK";
        });
        
        before((req, res) -> {
            res.header("Access-Control-Allow-Origin", "*");
            res.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            res.header("Access-Control-Allow-Headers", "Content-Type, Authorization");
        });
        
        // ================================================================
        // API: Health Check
        // ================================================================
        get("/api/health", (req, res) -> {
            res.type("application/json");
            
            JsonObject health = new JsonObject();
            health.addProperty("backend", "ok");
            health.addProperty("activeSession", activeSession != null);
            health.addProperty("mediaServer", MediaServerClient.isMediaServerHealthy());
            health.addProperty("timestamp", System.currentTimeMillis());
            
            return gson.toJson(health);
        });
        
        // ================================================================
        // API: Start Stream Session
        // ================================================================
        post("/api/stream/start", (req, res) -> {
            res.type("application/json");
            
            try {
                // LO7: Exception Handling - Validate session state
                if (activeSession != null) {
                    res.status(409); // Conflict
                    JsonObject error = new JsonObject();
                    error.addProperty("error", "A stream is already active. Stop it first.");
                    error.addProperty("activeUser", activeSession.getUser().getUsername());
                    return gson.toJson(error);
                }
                
                // Parse request body
                JsonObject body;
                try {
                    body = gson.fromJson(req.body(), JsonObject.class);
                } catch (JsonSyntaxException e) {
                    res.status(400); // Bad Request
                    JsonObject error = new JsonObject();
                    error.addProperty("error", "Invalid JSON format");
                    return gson.toJson(error);
                }
                
                // Validate username field
                if (body == null || !body.has("username") || body.get("username").getAsString().trim().isEmpty()) {
                    res.status(400);
                    JsonObject error = new JsonObject();
                    error.addProperty("error", "Username is required");
                    return gson.toJson(error);
                }
                
                String username = body.get("username").getAsString().trim();
                boolean ttsEnabled = body.has("ttsEnabled") ? body.get("ttsEnabled").getAsBoolean() : false;
                
                // Verify media server is running
                if (!MediaServerClient.isMediaServerHealthy()) {
                    res.status(503); // Service Unavailable
                    JsonObject error = new JsonObject();
                    error.addProperty("error", "Media server is not running. Start it first.");
                    return gson.toJson(error);
                }
                
                // LO1/LO3: Create User and StreamSession (Aggregation)
                User user = new User(username);
                activeSession = new StreamSession(user);
                activeSession.startSession();
                
                // LO4: Polymorphism - Use NotificationService interface
                if (ttsEnabled) {
                    NotificationService tts = new TTSNotifier();
                    tts.sendNotification("Stream started by " + username);
                }
                
                NotificationService sms = new SMSNotifier();
                sms.sendNotification("🎬 Stream session created for " + username);
                
                System.out.println("🎬 Stream started for: " + username);
                System.out.println("📡 RTMP Ingest: " + MediaServerClient.getRecommendedRTMPUrl());
                System.out.println("📺 HLS Playback: " + MediaServerClient.getHLSUrl("stream"));
                
                // Return session data
                JsonObject response = new JsonObject();
                response.addProperty("status", "started");
                response.addProperty("sessionId", activeSession.getSessionId());
                response.addProperty("username", username);
                response.addProperty("rtmpUrl", MediaServerClient.getRecommendedRTMPUrl());
                response.addProperty("hlsUrl", MediaServerClient.getHLSUrl("stream"));
                response.addProperty("startTime", activeSession.getStartTime().toString());
                
                res.status(200);
                return gson.toJson(response);
                
            } catch (Exception e) {
                // LO7: Exception Handling
                res.status(500);
                JsonObject error = new JsonObject();
                error.addProperty("error", "Internal server error: " + e.getMessage());
                System.err.println("❌ Error starting stream: " + e.getMessage());
                e.printStackTrace();
                return gson.toJson(error);
            }
        });
        
        // ================================================================
        // API: Get Active Session (CRITICAL - Must match frontend)
        // ================================================================
        get("/api/stream/session", (req, res) -> {
            res.type("application/json");
            
            if (activeSession == null) {
                res.status(404);
                JsonObject error = new JsonObject();
                error.addProperty("error", "No active stream session");
                return gson.toJson(error);
            }
            
            // Return complete session data
            JsonObject response = new JsonObject();
            response.addProperty("status", "active");
            response.addProperty("sessionId", activeSession.getSessionId());
            
            // User object (LO3: Aggregation)
            JsonObject userObj = new JsonObject();
            userObj.addProperty("username", activeSession.getUser().getUsername());
            response.add("user", userObj);
            
            response.addProperty("duration", activeSession.getDuration());
            response.addProperty("totalMessages", activeSession.getTotalMessages());
            response.addProperty("peakViewerCount", activeSession.getPeakViewerCount());
            response.addProperty("hlsUrl", MediaServerClient.getHLSUrl("stream"));
            response.addProperty("rtmpUrl", MediaServerClient.getRecommendedRTMPUrl());
            
            return gson.toJson(response);
        });
        
        // ================================================================
        // API: Get Last Session (For Summary Page)
        // ================================================================
        get("/api/stream/last", (req, res) -> {
            res.type("application/json");
            
            if (lastSession == null) {
                res.status(404);
                JsonObject error = new JsonObject();
                error.addProperty("error", "No session history available");
                return gson.toJson(error);
            }
            
            JsonObject response = new JsonObject();
            response.addProperty("status", "completed");
            response.addProperty("sessionId", lastSession.getSessionId());
            
            JsonObject userObj = new JsonObject();
            userObj.addProperty("username", lastSession.getUser().getUsername());
            response.add("user", userObj);
            
            response.addProperty("duration", lastSession.getDuration());
            response.addProperty("totalMessages", lastSession.getTotalMessages());
            response.addProperty("peakViewerCount", lastSession.getPeakViewerCount());
            
            return gson.toJson(response);
        });
        
        // ================================================================
        // API: Stop Stream Session
        // ================================================================
        post("/api/stream/stop", (req, res) -> {
            res.type("application/json");
            
            try {
                if (activeSession != null) {
                    // Stop the session
                    activeSession.stopSession();
                    
                    // LO8: File I/O - Write session log
                    try {
                        FileLogger logger = new FileLogger();
                        logger.writeLog(activeSession);
                        System.out.println("✅ Stream session logged to stream_log.txt");
                    } catch (Exception e) {
                        // LO7: Exception Handling
                        System.err.println("⚠️ Failed to write log: " + e.getMessage());
                    }
                    
                    System.out.println("🛑 Stream ended for " + activeSession.getUser().getUsername());
                    System.out.println("   Duration: " + activeSession.getDuration());
                    System.out.println("   Messages: " + activeSession.getTotalMessages());
                    
                    // Save for summary page BEFORE clearing
                    lastSession = activeSession;
                    activeSession = null;
                    
                    JsonObject response = new JsonObject();
                    response.addProperty("status", "stopped");
                    response.addProperty("message", "Stream session ended successfully");
                    
                    return gson.toJson(response);
                } else {
                    // No active session - perform force reset anyway
                    activeSession = null;
                    lastSession = null;
                    
                    JsonObject response = new JsonObject();
                    response.addProperty("status", "reset");
                    response.addProperty("message", "System reset - no active session was found");
                    
                    return gson.toJson(response);
                }
                
            } catch (Exception e) {
                // LO7: Exception Handling - Always clear session on error
                res.status(500);
                activeSession = null; // Force clear to break ghost session loop
                
                JsonObject error = new JsonObject();
                error.addProperty("error", "Error stopping stream: " + e.getMessage());
                error.addProperty("systemReset", true);
                
                System.err.println("❌ Error stopping stream: " + e.getMessage());
                return gson.toJson(error);
            }
        });
        
        // ================================================================
        // API: Force Reset (Emergency Recovery)
        // ================================================================
        post("/api/stream/reset", (req, res) -> {
            res.type("application/json");
            
            System.out.println("🔧 Emergency reset triggered");
            activeSession = null;
            lastSession = null;
            
            JsonObject response = new JsonObject();
            response.addProperty("status", "reset");
            response.addProperty("message", "System state cleared");
            
            return gson.toJson(response);
        });
        
        // ================================================================
        // File Serving: Stream Log Download
        // ================================================================
        get("/api/stream/log", (req, res) -> {
            res.type("text/plain");
            res.header("Content-Disposition", "attachment; filename=\"metastream-log.txt\"");
            
            try {
                File logFile = new File("stream_log.txt");
                if (logFile.exists()) {
                    return Files.readString(Paths.get("stream_log.txt"));
                } else {
                    return "No log file found. Streams will create this file automatically.";
                }
            } catch (Exception e) {
                // LO7: Exception Handling
                System.err.println("⚠️ Error reading log file: " + e.getMessage());
                return "Error reading log file: " + e.getMessage();
            }
        });
        
        // Also serve at root level for direct access
        get("/stream_log.txt", (req, res) -> {
            res.type("text/plain");
            
            try {
                File logFile = new File("stream_log.txt");
                if (logFile.exists()) {
                    return Files.readString(Paths.get("stream_log.txt"));
                } else {
                    res.status(404);
                    return "Log file not found";
                }
            } catch (Exception e) {
                res.status(500);
                return "Error reading log file";
            }
        });
        
        // ================================================================
        // 404 Handler
        // ================================================================
        notFound((req, res) -> {
            res.type("application/json");
            JsonObject error = new JsonObject();
            error.addProperty("error", "Endpoint not found: " + req.pathInfo());
            return gson.toJson(error);
        });
        
        // ================================================================
        // Server Initialization
        // ================================================================
        init();
        
        // Wait for server to fully start
        awaitInitialization();
        
        System.out.println("✅ MetaStream Live Backend ready at http://localhost:8080");
        System.out.println("🔌 WebSocket endpoint: ws://localhost:8080/ws");
        System.out.println("📊 Health check: http://localhost:8080/api/health");
        
        // Verify media server
        if (MediaServerClient.isMediaServerHealthy()) {
            System.out.println("✅ FFmpeg media server is running");
        } else {
            System.out.println("⚠️  FFmpeg media server not detected - start it before streaming");
        }
    }
    
    /**
     * Accessor for WebSocketHandler to get active session
     * @return Current active StreamSession or null
     */
    public static StreamSession getActiveSession() {
        return activeSession;
    }
}