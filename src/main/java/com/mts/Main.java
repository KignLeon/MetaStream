package com.mts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.staticFiles;

/**
 * MetaStream Live - Backend Server Handles user sessions, chat messages, and
 * notification events.
 */
public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private static final Gson gson = new Gson();

    // Active session
    private static StreamSession activeSession;

    public static void main(String[] args) {

        // --- Server Configuration ---
        port(getAssignedPort());
        staticFiles.location("/public"); // Points to src/main/resources/public
        LOGGER.info("MetaStream Live Server started at http://localhost:{}", getAssignedPort());

        // --- ROUTES ---
        /**
         * Start a new live stream session. Request JSON: { "username": "Leon",
         * "ttsEnabled": true }
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
                return "{\"status\":\"success\", \"message\":\"Stream started for user: " + username + "\"}";

            } catch (JsonSyntaxException e) {
                LOGGER.error("JSON parsing error", e);
                res.status(400);
                return "{\"status\":\"error\",\"message\":\"Invalid JSON format.\"}";
            }
        });

        /**
         * Add a chat message to the active stream. Request JSON: { "author":
         * "Sarah", "text": "Hello everyone!" }
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
                LOGGER.error("Error adding chat message", e);
                res.status(500);
                return "{\"status\":\"error\",\"message\":\"Internal error.\"}";
            }
        });

        /**
         * Stop the current stream session and log details.
         */
        post("/stopStream", (req, res) -> {
            res.type("application/json");

            if (activeSession == null) {
                res.status(400);
                return "{\"status\":\"error\",\"message\":\"No active stream.\"}";
            }

            try {
                activeSession.stopSession();
                LOGGER.info("🛑 Stream stopped. Total messages: {}", activeSession.getMessages().size());
                activeSession = null;
                return "{\"status\":\"success\",\"message\":\"Stream ended.\"}";
            } catch (Exception e) {
                LOGGER.error("Error stopping stream", e);
                res.status(500);
                return "{\"status\":\"error\",\"message\":\"Could not stop stream.\"}";
            }
        });
    }

    /**
     * Gets the port number from environment variable (for deployment), or
     * defaults to 8080 when running locally.
     */
    private static int getAssignedPort() {
        String port = new ProcessBuilder().environment().get("PORT");
        return port != null ? Integer.parseInt(port) : 8080;
    }
}
