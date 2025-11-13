package com.mts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import static spark.Spark.get;
import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.staticFiles;

/**
 * MetaStream Live — Backend Server Handles serving pages and live streaming API
 * routes.
 */
public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private static final Gson gson = new Gson();

    // Current active session
    private static StreamSession activeSession;

    public static void main(String[] args) {

        // --- SERVER CONFIGURATION ---
        port(getAssignedPort());
        staticFiles.location("/public");
        LOGGER.info("🌐 MetaStream Live running at http://localhost:{}", getAssignedPort());

        // --- ROUTES ---
        get("/", (req, res) -> {
            res.redirect("/index.html");
            return null;
        });

        get("/viewer", (req, res) -> {
            res.redirect("/viewer.html");
            return null;
        });

        // --- API ROUTES ---
        /**
         * POST /startStream Starts a new stream with RTMP configuration.
         * Expected JSON: { "username": "Leon", "ttsEnabled": true, "rtmpUrl":
         * "rtmps://live-upload.instagram.com:443/rtmp/", "streamKey":
         * "IG-STREAM-KEY-HERE" }
         */
        post("/startStream", (req, res) -> {
            res.type("application/json");
            try {
                JsonObject data = gson.fromJson(req.body(), JsonObject.class);
                if (data == null || !data.has("username")) {
                    res.status(400);
                    return "{\"status\":\"error\", \"message\":\"Missing username.\"}";
                }

                // Extract fields
                String username = data.get("username").getAsString();
                boolean ttsEnabled = data.has("ttsEnabled") && data.get("ttsEnabled").getAsBoolean();
                String rtmpUrl = data.has("rtmpUrl") ? data.get("rtmpUrl").getAsString() : null;
                String streamKey = data.has("streamKey") ? data.get("streamKey").getAsString() : null;

                // Create and configure user
                User user = new User(username);
                user.setPreferences(ttsEnabled);
                user.setRtmpUrl(rtmpUrl);
                user.setStreamKey(streamKey);

                // Create and start session
                activeSession = new StreamSession(user);
                activeSession.startSession();

                LOGGER.info("🎬 Stream started by {} with RTMP URL: {}", username, rtmpUrl);
                return gson.toJson(new JsonObjectBuilder()
                        .add("status", "success")
                        .add("message", "Stream started for user: " + username)
                        .add("rtmpUrl", rtmpUrl != null ? rtmpUrl : "N/A")
                        .build());

            } catch (JsonSyntaxException e) {
                LOGGER.error("❌ JSON parsing error", e);
                res.status(400);
                return "{\"status\":\"error\",\"message\":\"Invalid JSON format.\"}";
            }
        });

        /**
         * POST /sendMessage Adds chat messages to active session.
         */
        post("/sendMessage", (req, res) -> {
            res.type("application/json");

            if (activeSession == null) {
                res.status(400);
                return "{\"status\":\"error\",\"message\":\"No active stream.\"}";
            }

            try {
                JsonObject data = gson.fromJson(req.body(), JsonObject.class);
                if (!data.has("author") || !data.has("text")) {
                    res.status(400);
                    return "{\"status\":\"error\",\"message\":\"Missing fields.\"}";
                }

                String author = data.get("author").getAsString();
                String text = data.get("text").getAsString();

                ChatMessage message = new ChatMessage(author, text);
                activeSession.addMessage(message);

                LOGGER.info("[{}]: {}", author, text);
                return "{\"status\":\"success\",\"message\":\"Message sent.\"}";

            } catch (JsonSyntaxException e) {
                LOGGER.error("Error parsing chat message", e);
                res.status(400);
                return "{\"status\":\"error\",\"message\":\"Invalid message format.\"}";
            }
        });

        /**
         * GET /activeStream Returns active session info for dashboard refresh.
         */
        get("/activeStream", (req, res) -> {
            res.type("application/json");

            if (activeSession == null || !activeSession.isActive()) {
                res.status(404);
                return "{\"status\":\"error\",\"message\":\"No active stream.\"}";
            }

            JsonObject response = new JsonObject();
            User u = activeSession.getUser();

            response.addProperty("status", "active");
            response.addProperty("username", u.getUsername());
            response.addProperty("rtmpUrl", u.getRtmpUrl());
            response.addProperty("startedAt", activeSession.getStartedAt().toString());
            response.addProperty("messages", activeSession.getMessages().size());
            return gson.toJson(response);
        });

        /**
         * POST /stopStream Ends the active stream and clears session.
         */
        post("/stopStream", (req, res) -> {
            res.type("application/json");

            if (activeSession == null) {
                res.status(400);
                return "{\"status\":\"error\",\"message\":\"No active stream to stop.\"}";
            }

            try {
                activeSession.stopSession();
                LOGGER.info("🛑 Stream stopped. Total messages: {}", activeSession.getMessages().size());
                activeSession = null;
                return "{\"status\":\"success\",\"message\":\"Stream ended successfully.\"}";
            } catch (Exception e) {
                LOGGER.error("Error stopping stream", e);
                res.status(500);
                return "{\"status\":\"error\",\"message\":\"Unable to stop stream.\"}";
            }
        });
    }

    // --- PORT HANDLER ---
    private static int getAssignedPort() {
        String port = new ProcessBuilder().environment().get("PORT");
        return port != null ? Integer.parseInt(port) : 8080;
    }

    // --- HELPER JSON BUILDER ---
    private static class JsonObjectBuilder {

        private final JsonObject obj = new JsonObject();

        JsonObjectBuilder add(String key, String value) {
            obj.addProperty(key, value);
            return this;
        }

        JsonObject build() {
            return obj;
        }
    }
}
