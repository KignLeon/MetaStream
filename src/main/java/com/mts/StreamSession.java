package com.mts;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StreamSession {
    private String sessionId;
    private String username;
    private String ingestUrl;
    private String playbackUrl;
    private long startTime;
    private long endTime;
    private boolean active;
    private List<ChatMessage> chatMessages;

    public StreamSession(String username, String ingestUrl, String playbackUrl) {
        this.sessionId = UUID.randomUUID().toString();
        this.username = username;
        this.ingestUrl = ingestUrl;
        this.playbackUrl = playbackUrl;
        this.startTime = System.currentTimeMillis();
        this.active = true;
        this.chatMessages = new ArrayList<>();
    }

    public void stop() {
        this.active = false;
        this.endTime = System.currentTimeMillis();
    }

    // --- The Missing Method ---
    public void addChatMessage(ChatMessage message) {
        if (this.chatMessages == null) {
            this.chatMessages = new ArrayList<>();
        }
        this.chatMessages.add(message);
    }

    // Getters
    public String getSessionId() { return sessionId; }
    public String getUsername() { return username; }
    public String getIngestUrl() { return ingestUrl; }
    public String getPlaybackUrl() { return playbackUrl; }
    public boolean isActive() { return active; }
    public List<ChatMessage> getChatMessages() { return chatMessages; }
    
    public String getDuration() {
        long end = (endTime == 0) ? System.currentTimeMillis() : endTime;
        long diff = end - startTime;
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        return String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60);
    }
}