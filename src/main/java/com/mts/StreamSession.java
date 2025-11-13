package com.mts;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * StreamSession represents a single live streaming session. Tracks the active
 * user, messages, and stream configuration (RTMP info).
 */
public class StreamSession {

    private final User user;
    private final List<ChatMessage> messages;

    // ✅ New fields
    private boolean active;
    private final LocalDateTime startedAt;
    private LocalDateTime endedAt;

    public StreamSession(User user) {
        this.user = user;
        this.messages = new ArrayList<>();
        this.active = false;
        this.startedAt = LocalDateTime.now();
    }

    // ---------- Session Controls ----------
    public void startSession() {
        this.active = true;
        System.out.println("🎬 Stream started for " + user.getUsername());
        System.out.println("🔗 RTMP URL: " + user.getRtmpUrl());
        System.out.println("🔑 Stream Key: [HIDDEN]");
    }

    public void stopSession() {
        this.active = false;
        this.endedAt = LocalDateTime.now();
        System.out.println("🛑 Stream ended for " + user.getUsername());
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    // ---------- Messaging ----------
    public void addMessage(ChatMessage msg) {
        messages.add(msg);
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    // ---------- User Access ----------
    public User getUser() {
        return user;
    }

    // ---------- Utility ----------
    @Override
    public String toString() {
        return "StreamSession{"
                + "user=" + user.getUsername()
                + ", active=" + active
                + ", messages=" + messages.size()
                + ", startedAt=" + startedAt
                + ", endedAt=" + endedAt
                + '}';
    }
}
