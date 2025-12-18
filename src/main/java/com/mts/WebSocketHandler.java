package com.mts;

import org.eclipse.jetty.websocket.api.*;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler for real-time chat and stream updates.
 */
@WebSocket
public class WebSocketHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketHandler.class);
    private static final Gson gson = new Gson();

    // Store all connected clients
    private static final Set<Session> sessions = ConcurrentHashMap.newKeySet();

    @OnWebSocketConnect
    public void onConnect(Session session) {
        sessions.add(session);
        LOGGER.info("📡 WebSocket connected: {} (Total: {})", session.getRemoteAddress(), sessions.size());

        // Send welcome message - FIXED: Convert JsonObject to String
        JsonObject welcomeMsg = createMessage("system", "connected", "Connected to MetaStream Live");
        sendToSession(session, welcomeMsg.toString());
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        sessions.remove(session);
        LOGGER.info("❌ WebSocket closed: {} (Total: {})", session.getRemoteAddress(), sessions.size());
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) {
        try {
            JsonObject data = gson.fromJson(message, JsonObject.class);
            String type = data.get("type").getAsString();

            switch (type) {
                case "chat":
                    handleChatMessage(data);
                    break;
                case "ping":
                    // FIXED: Convert JsonObject to String
                    JsonObject pongMsg = createMessage("system", "pong", "pong");
                    sendToSession(session, pongMsg.toString());
                    break;
                default:
                    LOGGER.warn("Unknown message type: {}", type);
            }

        } catch (Exception e) {
            LOGGER.error("Error handling WebSocket message", e);
        }
    }

    @OnWebSocketError
    public void onError(Session session, Throwable error) {
        LOGGER.error("WebSocket error: {}", error.getMessage());
    }

    // ---------- Message Handlers ----------
    private void handleChatMessage(JsonObject data) {
        String author = data.get("author").getAsString();
        String text = data.get("text").getAsString();

        // Create chat message
        ChatMessage msg = new ChatMessage(author, text);

        // Broadcast to all connected clients
        JsonObject broadcast = new JsonObject();
        broadcast.addProperty("type", "chat");
        broadcast.addProperty("author", msg.getAuthor());
        broadcast.addProperty("text", msg.getText());
        broadcast.addProperty("timestamp", msg.getTimestamp().toString());

        broadcastToAll(broadcast.toString());

        LOGGER.info("[CHAT] {}: {}", author, text);
    }

    // ---------- Broadcast Methods ----------
    /**
     * Send message to all connected clients.
     */
    public static void broadcastToAll(String message) {
        sessions.forEach(session -> sendToSession(session, message));
    }

    /**
     * Broadcast stream status update (e.g., "stream started", "stream ended").
     */
    public static void broadcastStreamStatus(String status, String details) {
        JsonObject msg = createMessage("stream", status, details);
        broadcastToAll(msg.toString());
    }

    /**
     * Broadcast viewer count update.
     */
    public static void broadcastViewerCount(int count) {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "viewers");
        msg.addProperty("count", count);
        broadcastToAll(msg.toString());
    }

    // ---------- Helper Methods ----------
    private static void sendToSession(Session session, String message) {
        try {
            if (session != null && session.isOpen()) {
                session.getRemote().sendString(message);
            }
        } catch (Exception e) {
            LOGGER.error("Error sending to session: {}", e.getMessage());
        }
    }

    private static JsonObject createMessage(String type, String event, String data) {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", type);
        msg.addProperty("event", event);
        msg.addProperty("data", data);
        msg.addProperty("timestamp", System.currentTimeMillis());
        return msg;
    }

    /**
     * Get current number of connected clients.
     */
    public static int getConnectionCount() {
        return sessions.size();
    }
}
