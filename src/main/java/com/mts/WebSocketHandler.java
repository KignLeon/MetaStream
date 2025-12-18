package com.mts;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * LO6: GUI & Events - WebSocket handles real-time chat events LO7: Exception
 * Handling - Try/catch in message handlers
 */
@WebSocket
public class WebSocketHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketHandler.class);
    private static final Gson gson = new Gson();

    private static final Set<Session> sessions = ConcurrentHashMap.newKeySet();
    private static StreamSession activeStreamSession;

    @OnWebSocketConnect
    public void onConnect(Session session) {
        sessions.add(session);
        LOGGER.info("📡 WebSocket connected: {} (Total: {})", session.getRemoteAddress(), sessions.size());

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
            LOGGER.info("📨 Received WebSocket message: {}", message);

            JsonObject data = gson.fromJson(message, JsonObject.class);
            String type = data.get("type").getAsString();

            switch (type) {
                case "chat":
                    handleChatMessage(data);
                    break;
                case "ping":
                    JsonObject pongMsg = createMessage("system", "pong", "pong");
                    sendToSession(session, pongMsg.toString());
                    break;
                default:
                    LOGGER.warn("Unknown message type: {}", type);
            }

        } catch (Exception e) {
            // LO7: Exception Handling
            LOGGER.error("Error handling WebSocket message: {}", message, e);
        }
    }

    @OnWebSocketError
    public void onError(Session session, Throwable error) {
        LOGGER.error("WebSocket error: {}", error.getMessage(), error);
    }

    private void handleChatMessage(JsonObject data) {
    try {
        String author = data.has("author") ? data.get("author").getAsString() : "Unknown";
        String text = data.has("text") ? data.get("text").getAsString() : "";

            LOGGER.info("[CHAT] {}: {}", author, text);

            if (activeStreamSession != null) {
                ChatMessage msg = new ChatMessage(author, text);
                activeStreamSession.addMessage(msg);
            }

            JsonObject broadcast = new JsonObject();
            broadcast.addProperty("type", "chat");
            broadcast.addProperty("author", author);
            broadcast.addProperty("text", text);
            broadcast.addProperty("timestamp", System.currentTimeMillis());

            broadcastToAll(broadcast.toString());

        } catch (Exception e) {
            LOGGER.error("Error handling chat message", e);
        }
    }

    public static void broadcastToAll(String message) {
        LOGGER.debug("Broadcasting to {} clients", sessions.size());
        sessions.forEach(session -> sendToSession(session, message));
    }

    public static void broadcastStreamStatus(String status, String details) {
        JsonObject msg = createMessage("stream", status, details);
        broadcastToAll(msg.toString());
    }

    public static void broadcastViewerCount(int count) {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "viewers");
        msg.addProperty("count", count);
        broadcastToAll(msg.toString());
    }

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

    public static int getConnectionCount() {
        return sessions.size();
    }

    public static void setActiveStreamSession(StreamSession session) {
        activeStreamSession = session;
        LOGGER.info("Active stream session set: {}", session != null ? session.getSessionId() : "null");
    }
}
