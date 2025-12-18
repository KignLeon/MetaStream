package com.mts;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

/**
 * WebSocket Handler for Real-Time Chat
 * 
 * Learning Outcomes:
 * LO6: GUI & Events (Real-time bidirectional communication)
 * LO7: Exception Handling (Null-safe parsing, error recovery)
 */
@WebSocket
public class WebSocketHandler {
    
    // Thread-safe session storage
    private static final Map<Session, String> sessions = new ConcurrentHashMap<>();
    private static final Gson gson = new Gson();
    private static final FileLogger fileLogger = new FileLogger();
    
    /**
     * Handle new WebSocket connection
     * LO6: Event-driven programming
     */
    @OnWebSocketConnect
    public void onConnect(Session session) {
        try {
            String address = session.getRemoteAddress().getAddress().toString();
            System.out.println("📡 WebSocket connected: " + address + " (Total: " + (sessions.size() + 1) + ")");
            
            // Register session with default username
            sessions.put(session, "Anonymous");
            
            // Send welcome message
            JsonObject welcome = new JsonObject();
            welcome.addProperty("type", "system");
            welcome.addProperty("event", "connected");
            welcome.addProperty("message", "Connected to MetaStream Live");
            welcome.addProperty("timestamp", System.currentTimeMillis());
            
            sendToSession(session, gson.toJson(welcome));
            
            // Notify about active stream
            StreamSession activeSession = Main.getActiveSession();
            if (activeSession != null) {
                JsonObject streamInfo = new JsonObject();
                streamInfo.addProperty("type", "system");
                streamInfo.addProperty("event", "stream_active");
                streamInfo.addProperty("streamer", activeSession.getUser().getUsername());
                streamInfo.addProperty("sessionId", activeSession.getSessionId());
                
                sendToSession(session, gson.toJson(streamInfo));
            }
            
        } catch (Exception e) {
            // LO7: Exception Handling - Prevent connection failure from crashing server
            System.err.println("⚠️ Error in onConnect: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Handle WebSocket disconnection
     * LO6: Event-driven programming
     */
    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        try {
            String username = sessions.getOrDefault(session, "Unknown");
            sessions.remove(session);
            
            System.out.println("🔌 WebSocket disconnected: " + username + 
                             " (Remaining: " + sessions.size() + ")" +
                             " [Code: " + statusCode + "]");
            
        } catch (Exception e) {
            // LO7: Exception Handling
            System.err.println("⚠️ Error in onClose: " + e.getMessage());
        }
    }
    
    /**
     * Handle WebSocket errors
     * LO7: Exception Handling
     */
    @OnWebSocketError
    public void onError(Session session, Throwable error) {
        String username = sessions.getOrDefault(session, "Unknown");
        
        // Ignore common protocol errors (ping/pong frames, opcode issues)
        if (error instanceof org.eclipse.jetty.websocket.api.ProtocolException) {
            String errorMsg = error.getMessage();
            if (errorMsg != null && (errorMsg.contains("Unknown opcode") || 
                                    errorMsg.contains("opcode: 7") ||
                                    errorMsg.contains("ping") || 
                                    errorMsg.contains("pong"))) {
                // These are harmless ping/pong frame errors - don't disconnect
                System.out.println("⚠️ WebSocket protocol frame ignored for " + username);
                return; // Don't propagate the error
            }
        }
        
        // Log genuine errors (but don't print full stack trace for common IO issues)
        System.err.println("❌ WebSocket error for " + username + ": " + error.getMessage());
        
        if (!(error instanceof java.io.IOException)) {
            error.printStackTrace();
        }
    }
    
    /**
     * Handle incoming WebSocket messages
     * LO6: Event-driven programming
     * LO7: Exception Handling - Comprehensive null-safety
     */
    @OnWebSocketMessage
    public void onMessage(Session session, String message) {
        // LO7: Exception Handling - Comprehensive null checks
        if (session == null || !session.isOpen()) {
            System.err.println("⚠️ Received message from closed session");
            return;
        }
        
        if (message == null || message.trim().isEmpty()) {
            System.err.println("⚠️ Received null or empty message");
            return;
        }
        
        try {
            // Parse JSON message
            JsonObject json;
            try {
                json = gson.fromJson(message, JsonObject.class);
            } catch (JsonSyntaxException e) {
                // LO7: Exception Handling - Invalid JSON
                System.err.println("⚠️ Invalid JSON received: " + message);
                sendError(session, "Invalid JSON format");
                return;
            }
            
            // Validate JSON structure
            if (json == null) {
                System.err.println("⚠️ Null JSON object after parsing");
                sendError(session, "Invalid message format");
                return;
            }
            
            // Check for message type
            if (!json.has("type")) {
                System.err.println("⚠️ Message missing 'type' field");
                sendError(session, "Message must have a 'type' field");
                return;
            }
            
            String messageType = json.get("type").getAsString();
            
            // Route based on message type
            switch (messageType) {
                case "chat":
                    handleChatMessage(session, json);
                    break;
                    
                case "ping":
                    handlePingMessage(session, json);
                    break;
                    
                case "identify":
                    handleIdentifyMessage(session, json);
                    break;
                    
                default:
                    System.err.println("⚠️ Unknown message type: " + messageType);
                    sendError(session, "Unknown message type: " + messageType);
            }
            
        } catch (Throwable t) {
            // LO7: Exception Handling - Catch ALL errors to prevent thread death
            System.err.println("❌ Critical error processing WebSocket message: " + t.getMessage());
            t.printStackTrace();
            
            try {
                sendError(session, "Internal server error processing message");
            } catch (Exception e) {
                System.err.println("❌ Failed to send error response: " + e.getMessage());
            }
        }
    }
    
    /**
     * Handle chat message
     * LO7: Null-safe chat message processing
     * LO8: File I/O - Log chat messages
     */
    private void handleChatMessage(Session session, JsonObject json) {
        try {
            // LO7: Null-safe field extraction
            String author = "Anonymous";
            if (json.has("author") && !json.get("author").isJsonNull()) {
                author = json.get("author").getAsString().trim();
            }
            
            if (author.isEmpty()) {
                author = "Anonymous";
            }
            
            String text = "";
            if (json.has("text") && !json.get("text").isJsonNull()) {
                text = json.get("text").getAsString().trim();
            }
            
            // Validate message has content
            if (text.isEmpty()) {
                sendError(session, "Message text cannot be empty");
                return;
            }
            
            // Sanitize text (basic XSS prevention)
            text = sanitizeText(text);
            
            // Limit message length
            if (text.length() > 500) {
                text = text.substring(0, 500);
            }
            
            // Update session username if provided
            if (!author.equals("Anonymous")) {
                sessions.put(session, author);
            }
            
            // Update active session message counter
            StreamSession activeSession = Main.getActiveSession();
            if (activeSession != null) {
                activeSession.incrementMessages();
            }
            
            // LO8: File I/O - Log chat message
            try {
                fileLogger.logChat(author, text);
            } catch (Exception e) {
                // LO7: Don't fail message delivery if logging fails
                System.err.println("⚠️ Failed to log chat message: " + e.getMessage());
            }
            
            // Log to console
            System.out.println("[CHAT] " + author + ": " + text);
            
            // Broadcast to all connected clients
            JsonObject broadcast = new JsonObject();
            broadcast.addProperty("type", "chat");
            broadcast.addProperty("author", author);
            broadcast.addProperty("text", text);
            broadcast.addProperty("timestamp", System.currentTimeMillis());
            
            broadcastMessage(gson.toJson(broadcast));
            
        } catch (Exception e) {
            // LO7: Exception Handling
            System.err.println("❌ Error handling chat message: " + e.getMessage());
            e.printStackTrace();
            sendError(session, "Failed to process chat message");
        }
    }
    
    /**
     * Handle ping message (keepalive)
     */
    private void handlePingMessage(Session session, JsonObject json) {
        try {
            JsonObject pong = new JsonObject();
            pong.addProperty("type", "pong");
            pong.addProperty("timestamp", System.currentTimeMillis());
            
            sendToSession(session, gson.toJson(pong));
            
        } catch (Exception e) {
            System.err.println("⚠️ Error handling ping: " + e.getMessage());
        }
    }
    
    /**
     * Handle user identification
     */
    private void handleIdentifyMessage(Session session, JsonObject json) {
        try {
            if (json.has("username") && !json.get("username").isJsonNull()) {
                String username = json.get("username").getAsString().trim();
                
                if (!username.isEmpty()) {
                    sessions.put(session, username);
                    System.out.println("👤 User identified: " + username);
                    
                    JsonObject ack = new JsonObject();
                    ack.addProperty("type", "identified");
                    ack.addProperty("username", username);
                    
                    sendToSession(session, gson.toJson(ack));
                }
            }
            
        } catch (Exception e) {
            System.err.println("⚠️ Error handling identify: " + e.getMessage());
        }
    }
    
    /**
     * Sanitize text to prevent XSS
     */
    private String sanitizeText(String text) {
        if (text == null) return "";
        
        return text
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
            .replace("/", "&#x2F;");
    }
    
    /**
     * Send error message to specific session
     */
    private void sendError(Session session, String errorMessage) {
        try {
            if (session != null && session.isOpen()) {
                JsonObject error = new JsonObject();
                error.addProperty("type", "error");
                error.addProperty("message", errorMessage);
                error.addProperty("timestamp", System.currentTimeMillis());
                
                session.getRemote().sendString(gson.toJson(error));
            }
        } catch (Exception e) {
            System.err.println("⚠️ Failed to send error message: " + e.getMessage());
        }
    }
    
    /**
     * Send message to specific session
     */
    private void sendToSession(Session session, String message) {
        try {
            if (session != null && session.isOpen()) {
                session.getRemote().sendString(message);
            }
        } catch (Exception e) {
            // LO7: Exception Handling
            System.err.println("⚠️ Failed to send message to session: " + e.getMessage());
        }
    }
    
    /**
     * Broadcast message to all connected sessions
     */
    private void broadcastMessage(String message) {
        // Create a snapshot of sessions to avoid ConcurrentModificationException
        sessions.keySet().stream()
            .filter(session -> session != null && session.isOpen())
            .forEach(session -> {
                try {
                    session.getRemote().sendString(message);
                } catch (Exception e) {
                    // LO7: Exception Handling - Don't let one failure stop others
                    System.err.println("⚠️ Failed to broadcast to session: " + e.getMessage());
                }
            });
    }
    
    /**
     * Get current connection count
     */
    public static int getConnectionCount() {
        return sessions.size();
    }
}