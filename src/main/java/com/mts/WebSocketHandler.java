package com.mts;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@WebSocket
public class WebSocketHandler {

    private static final Map<Session, String> sessions = new ConcurrentHashMap<>();
    private static final Gson gson = new Gson();
    private static StreamSession activeStreamSession;

    /**
     * Sets the active stream session to allow chat logging.
     * Called by Main.java when a stream starts.
     */
    public static void setActiveStreamSession(StreamSession session) {
        activeStreamSession = session;
    }

    /**
     * Returns the number of currently connected WebSocket clients.
     * Called by Main.java for stats.
     */
    public static int getConnectionCount() {
        return sessions.size();
    }

    /**
     * Broadcasts a raw JSON string to all connected clients.
     * Called by Main.java.
     */
    public static void broadcastToAll(String message) {
        sessions.keySet().stream().filter(Session::isOpen).forEach(session -> {
            try {
                session.getRemote().sendString(message);
            } catch (IOException e) {
                System.err.println("❌ Error broadcasting to session: " + e.getMessage());
            }
        });
    }

    /**
     * Broadcasts stream status updates (e.g. duration) to clients.
     * Called by Main.java's timer loop.
     */
    public static void broadcastStreamStatus(String status, String time) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "stream_status");
        json.addProperty("status", status);
        json.addProperty("time", time);
        broadcastToAll(json.toString());
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        sessions.put(session, "Anonymous");
        System.out.println("📡 WebSocket connected: " + session.getRemoteAddress().getAddress() + " (Total: " + sessions.size() + ")");
        
        try {
            JsonObject welcomeMsg = createMessage("system", "connected", "Connected to MetaStream Live");
            sendToSession(session, welcomeMsg.toString());
        } catch (Exception e) {
            System.err.println("❌ Error sending welcome message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        sessions.remove(session);
        System.out.println("❌ WebSocket disconnected: " + statusCode + " - " + reason);
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) {
        // 1. Force Print immediately to prove we reached here
        System.out.println("📨 [DEBUG] Raw payload received: " + message);
        
        try {
            // Validate JSON string
            if (message == null || message.trim().isEmpty()) {
                System.err.println("⚠️ Received empty message");
                return;
            }

            // 2. Use instance method fromJson (Safest way to parse)
            JsonObject json = gson.fromJson(message, JsonObject.class);
            
            if (json == null) {
                System.err.println("⚠️ Parsed JSON is null");
                return;
            }

            // Check for required fields safely
            if (!json.has("type")) {
                 System.err.println("⚠️ Message missing 'type' field: " + message);
                 return;
            }

            String type = json.get("type").getAsString();

            switch (type) {
                case "chat":
                    handleChatMessage(session, json);
                    break;
                case "ping":
                    JsonObject pongMsg = createMessage("system", "pong", "pong");
                    sendToSession(session, pongMsg.toString());
                    break;
                default:
                    System.out.println("⚠️ Unknown message type: " + type);
            }
        } catch (Throwable t) { 
            // 3. Catch Throwable to trap Errors (like NoSuchMethodError) that usually crash threads silently
            System.err.println("🔥 CRITICAL ERROR in onMessage: " + t.getClass().getName());
            t.printStackTrace();
        }
    }

    private void handleChatMessage(Session session, JsonObject json) {
        try {
            String text = json.has("text") ? json.get("text").getAsString() : "";
            String author = json.has("author") ? json.get("author").getAsString() : "Anonymous";
            
            // Update session username if provided
            sessions.put(session, author);

            System.out.println("[CHAT] " + author + ": " + text);

            // Log chat to active session if available
            if (activeStreamSession != null) {
                // This line caused the error because StreamSession was missing the method
                activeStreamSession.addChatMessage(new ChatMessage(author, text));
            } else {
                System.out.println("⚠️ No active stream session to log chat to.");
            }

            // Broadcast to all clients
            broadcastMessage("chat", "message", json);
            
        } catch (Exception e) {
            System.err.println("❌ Error processing chat message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void broadcastMessage(String type, String event, JsonObject data) {
        // Ensure timestamp exists
        if (!data.has("timestamp")) {
            data.addProperty("timestamp", System.currentTimeMillis());
        }
        
        String payload = data.toString();
        // Use the new public method to avoid duplication logic
        broadcastToAll(payload);
    }

    private void sendToSession(Session session, String message) {
        try {
            session.getRemote().sendString(message);
        } catch (IOException e) {
            System.err.println("❌ Error sending to session: " + e.getMessage());
        }
    }

    private JsonObject createMessage(String type, String event, String data) {
        JsonObject json = new JsonObject();
        json.addProperty("type", type);
        json.addProperty("event", event);
        json.addProperty("data", data);
        json.addProperty("timestamp", System.currentTimeMillis());
        return json;
    }
}