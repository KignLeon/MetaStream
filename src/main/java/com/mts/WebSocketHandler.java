package com.mts;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@WebSocket
public class WebSocketHandler {
    private static final Map<Session, String> sessions = new ConcurrentHashMap<>();
    private static final Gson gson = new Gson();
    private static final FileLogger fileLogger = new FileLogger();

    @OnWebSocketConnect
    public void onConnect(Session session) {
        System.out.println("📡 WebSocket Connected: " + session.getRemoteAddress().getAddress());
        sessions.put(session, "Anonymous");
        try {
            JsonObject welcome = new JsonObject();
            welcome.addProperty("type", "system");
            welcome.addProperty("data", "Connected to MetaStream Live");
            session.getRemote().sendString(gson.toJson(welcome));
        } catch (Exception e) {}
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        sessions.remove(session);
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) {
        try {
            if (message == null || message.trim().isEmpty()) return;
            JsonObject json = gson.fromJson(message, JsonObject.class);
            
            if (json != null && json.has("type") && "chat".equals(json.get("type").getAsString())) {
                String author = json.has("author") ? json.get("author").getAsString() : "Viewer";
                String text = json.has("text") ? json.get("text").getAsString() : "";

                if (!text.isEmpty()) {
                    // Update the active session counter
                    StreamSession active = Main.getActiveSession();
                    if (active != null) active.incrementMessages();

                    // LO8: Log to File
                    fileLogger.logChat(author, text);
                    
                    // Broadcast back to all clients
                    broadcast(message);
                }
            }
        } catch (Throwable t) {
            System.err.println("⚠️ WS Processing error: " + t.getMessage());
        }
    }

    private void broadcast(String msg) {
        sessions.keySet().stream().filter(Session::isOpen).forEach(s -> {
            try { s.getRemote().sendString(msg); } catch (Exception e) {}
        });
    }
}