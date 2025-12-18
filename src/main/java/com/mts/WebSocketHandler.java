package com.mts;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
 * DIAGNOSTIC VERSION - Extensive logging for debugging
 * 
 * Learning Outcomes:
 * LO7: Exception Handling - Comprehensive error recovery
 */
@WebSocket
public class WebSocketHandler {
    
    private static final Map<Session, String> sessions = new ConcurrentHashMap<>();
    private static final Gson gson = new Gson();
    
    /**
     * Handle new WebSocket connection
     * LO7: Exception Handling
     */
    @OnWebSocketConnect
    public void onConnect(Session session) {
        try {
            sessions.put(session, "Anonymous");
            System.out.println("📡 WebSocket connected: " + session.getRemoteAddress() + 
                             " (Total: " + sessions.size() + ")");
            System.out.println("🔍 [DEBUG] Session ID: " + session.hashCode());
            System.out.println("🔍 [DEBUG] Protocol: " + session.getUpgradeRequest().getProtocolVersion());
            
        } catch (Exception e) {
            System.err.println("⚠️ Error in onConnect: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Handle incoming WebSocket messages
     * LO7: Exception Handling with null-safe parsing
     */
    @OnWebSocketMessage
    public void onMessage(Session session, String message) {
        System.out.println("📥 [DEBUG] Received message from " + session.getRemoteAddress());
        System.out.println("📥 [DEBUG] Message content: " + message);
        
        try {
            // Parse JSON message
            JsonObject json = gson.fromJson(message, JsonObject.class);
            String type = json.has("type") && !json.get("type").isJsonNull() 
                        ? json.get("type").getAsString() 
                        : "unknown";
            
            System.out.println("🔍 [DEBUG] Message type: " + type);
            
            if ("identify".equals(type)) {
                handleIdentify(session, json);
            } else if ("chat".equals(type)) {
                handleChat(session, json);
            } else {
                System.out.println("⚠️ [DEBUG] Unknown message type: " + type);
            }
            
        } catch (JsonSyntaxException e) {
            System.err.println("❌ JSON parsing error: " + e.getMessage());
            System.err.println("❌ Raw message was: " + message);
            
        } catch (Exception e) {
            System.err.println("❌ Unexpected error in onMessage: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Handle user identification
     */
    private void handleIdentify(Session session, JsonObject json) {
        try {
            String username = "Anonymous";
            
            if (json.has("username") && !json.get("username").isJsonNull()) {
                String rawUsername = json.get("username").getAsString().trim();
                if (!rawUsername.isEmpty() && rawUsername.length() <= 50) {
                    username = rawUsername;
                }
            }
            
            sessions.put(session, username);
            System.out.println("✅ User identified: " + username + " (" + session.getRemoteAddress() + ")");
            
            // Send welcome message
            JsonObject welcome = new JsonObject();
            welcome.addProperty("type", "system");
            welcome.addProperty("text", "Connected to MetaStream Live");
            welcome.addProperty("timestamp", getCurrentTimestamp());
            
            session.getRemote().sendString(gson.toJson(welcome));
            System.out.println("📤 [DEBUG] Sent welcome message to " + username);
            
        } catch (IOException e) {
            System.err.println("❌ Failed to send welcome message: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Error in handleIdentify: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Handle chat message broadcast
     * LO7: Exception Handling
     */
    private void handleChat(Session senderSession, JsonObject json) {
        try {
            // Extract and validate message fields
            String author = sessions.getOrDefault(senderSession, "Anonymous");
            
            String text = "";
            if (json.has("text") && !json.get("text").isJsonNull()) {
                text = json.get("text").getAsString().trim();
            }
            
            // If author is provided in message, use that (for dashboard messages)
            if (json.has("author") && !json.get("author").isJsonNull()) {
                String providedAuthor = json.get("author").getAsString().trim();
                if (!providedAuthor.isEmpty()) {
                    author = providedAuthor;
                }
            }
            
            // Validate message
            if (text.isEmpty()) {
                System.out.println("⚠️ [DEBUG] Empty message from " + author + ", ignoring");
                return;
            }
            
            if (text.length() > 500) {
                text = text.substring(0, 500);
            }
            
            System.out.println("💬 Chat message from " + author + ": " + text);
            
            // Sanitize text (prevent XSS)
            text = escapeHtml(text);
            
            // Create broadcast message
            JsonObject broadcast = new JsonObject();
            broadcast.addProperty("type", "chat");
            broadcast.addProperty("author", author);
            broadcast.addProperty("text", text);
            broadcast.addProperty("timestamp", getCurrentTimestamp());
            
            String broadcastJson = gson.toJson(broadcast);
            System.out.println("📤 [DEBUG] Broadcasting to " + sessions.size() + " clients");
            
            // Broadcast to all connected clients
            int successCount = 0;
            int failureCount = 0;
            
            for (Map.Entry<Session, String> entry : sessions.entrySet()) {
                Session clientSession = entry.getKey();
                String clientName = entry.getValue();
                
                try {
                    if (clientSession.isOpen()) {
                        clientSession.getRemote().sendString(broadcastJson);
                        successCount++;
                        System.out.println("  ✅ Sent to " + clientName);
                    } else {
                        System.out.println("  ⚠️ Session closed for " + clientName);
                        failureCount++;
                    }
                } catch (IOException e) {
                    System.err.println("  ❌ Failed to send to " + clientName + ": " + e.getMessage());
                    failureCount++;
                }
            }
            
            System.out.println("📊 Broadcast complete: " + successCount + " success, " + failureCount + " failed");
            
        } catch (Exception e) {
            System.err.println("❌ Error in handleChat: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Handle WebSocket disconnection
     * LO7: Exception Handling
     */
    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        try {
            String username = sessions.remove(session);
            
            if (username == null) {
                username = "Unknown";
            }
            
            System.out.println("🔌 WebSocket disconnected: " + username + 
                             " (Remaining: " + sessions.size() + ")" +
                             " [Code: " + statusCode + "]");
            
            if (reason != null && !reason.isEmpty()) {
                System.out.println("   Reason: " + reason);
            }
            
        } catch (Exception e) {
            System.err.println("⚠️ Error in onClose: " + e.getMessage());
        }
    }
    
    /**
     * Handle WebSocket errors
     * LO7: Exception Handling - DIAGNOSTIC VERSION
     */
    @OnWebSocketError
    public void onError(Session session, Throwable error) {
        String username = sessions.getOrDefault(session, "Unknown");
        
        System.out.println("🔍 [DEBUG] WebSocket error occurred for " + username);
        System.out.println("🔍 [DEBUG] Error class: " + error.getClass().getName());
        System.out.println("🔍 [DEBUG] Error message: " + error.getMessage());
        
        // Check if this is the harmless opcode 7 error
        if (error instanceof org.eclipse.jetty.websocket.api.ProtocolException) {
            String errorMsg = error.getMessage();
            System.out.println("🔍 [DEBUG] ProtocolException detected");
            
            if (errorMsg != null && (errorMsg.contains("Unknown opcode") || 
                                    errorMsg.contains("opcode: 7") ||
                                    errorMsg.contains("ping") || 
                                    errorMsg.contains("pong"))) {
                // This is harmless - browser sent a close/ping/pong frame
                System.out.println("✅ [DEBUG] Harmless protocol frame ignored (opcode 7 / ping / pong)");
                return; // Don't disconnect or log as error
            }
        }
        
        // For all other errors, log them
        System.err.println("❌ WebSocket error for " + username + ": " + error.getMessage());
        
        // Only print stack trace for non-IO errors (IO errors are common connection issues)
        if (!(error instanceof java.io.IOException)) {
            error.printStackTrace();
        }
    }
    
    /**
     * Get current timestamp in ISO format
     */
    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
    
    /**
     * Escape HTML to prevent XSS attacks
     */
    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#x27;")
                   .replace("/", "&#x2F;");
    }
    
    /**
     * Get count of active sessions
     */
    public static int getActiveSessionCount() {
        return sessions.size();
    }
}