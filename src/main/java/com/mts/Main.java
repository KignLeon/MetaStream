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
 * MetaStream Live — Local + Cloud Backend Server Handles serving static web
 * pages and API routes for chat and session control.
 */
public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private static final Gson gson = new Gson();

    // Active stream session
    private static StreamSession activeSession;

    public static void main(String[] args) {

        // --- SERVER CONFIG ---
        port(getAssignedPort());
        staticFiles.location("/public"); // Serves files from src/main/resources/public
        LOGGER.info("🌐 MetaStream Live running at http://localhost:{}", getAssignedPort());

        // --- ROUTES ---
        // Root route: serve index.html directly
        get("/", (req, res) -> {
            res.redirect("/index.html");
            return null;
        });

        // Serve viewer.html manually (optional, demonstration)
        get("/viewer", (req, res) -> {
            res.redirect("/viewer.html");
            return null;
        });

        // --- API ROUTES ---
        /**
         * POST /startStream Starts a new live session. Example JSON: {
         * "username": "Leon", "ttsEnabled": true }
         */
        post("/startStream", (req, res) -> {
            res.type("application/json");
            try {
                JsonObject data = gson.fromJson(req.body(), JsonObject.class);
                if (data == null || !data.has("username")) {
                    res.status(400);
                    return "{\"status\":\"error\", \"message\":\"Missing username.\"}";
                }

                String username = data.get("username").getAsString();
                boolean ttsEnabled = data.has("ttsEnabled") && data.get("ttsEnabled").getAsBoolean();

                User user = new User(username);
                user.setPreferences(ttsEnabled);
                activeSession = new StreamSession(user);
                activeSession.startSession();

                LOGGER.info("🎬 Stream started by user: {}", username);
                return gson.toJson(new JsonObjectBuilder()
                        .add("status", "success")
                        .add("message", "Stream started for user: " + username)
                        .build());

            } catch (JsonSyntaxException e) {
                LOGGER.error("JSON parsing error", e);
                res.status(400);
                return "{\"status\":\"error\",\"message\":\"Invalid JSON format.\"}";
            }
        });

        /**
         * POST /sendMessage Adds a chat message to the current stream. Example
         * JSON: { "author": "Sarah", "text": "Hello everyone!" }
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
                return "{\"status\":\"success\",\"message\":\"Message received.\"}";

            } catch (JsonSyntaxException e) {
                LOGGER.error("Error parsing chat message", e);
                res.status(400);
                return "{\"status\":\"error\",\"message\":\"Invalid message format.\"}";
            }
        });

        /**
         * POST /stopStream Ends the current live stream session.
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

    /**
     * Gets the port number from the environment (for deployment), or defaults
     * to 8080 for local testing.
     */
    private static int getAssignedPort() {
        String port = new ProcessBuilder().environment().get("PORT");
        return port != null ? Integer.parseInt(port) : 8080;
    }

    // --- HELPER CLASS ---
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
