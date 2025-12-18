package com.mts;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * LO1: OOP Principles - Encapsulation with private fields and public methods
 * LO2: Arrays - ArrayList<ChatMessage> for chat history LO3: Aggregation -
 * Contains User object and List of ChatMessage objects LO5: Generic Collections
 * - ArrayList<ChatMessage> with type parameter
 */
public class StreamSession {

    private final String sessionId;
    private final User user; // LO3: Aggregation
    private final List<ChatMessage> messages; // LO2 & LO5: ArrayList<ChatMessage>

    private boolean active;
    private final LocalDateTime startedAt;
    private LocalDateTime endedAt;

    private String rtmpIngestUrl;
    private String hlsPlaybackUrl;
    private String streamKey;

    private int peakViewerCount;
    private int totalMessages;
    private int notificationsSent;

    public StreamSession(User user) {
        this.sessionId = UUID.randomUUID().toString();
        this.user = user;
        this.messages = new ArrayList<>(); // LO2 & LO5: Generic ArrayList
        this.active = false;
        this.startedAt = LocalDateTime.now();
        this.peakViewerCount = 0;
        this.totalMessages = 0;
        this.notificationsSent = 0;
    }

    // ---------- Session Lifecycle ----------
    public void startSession() {
        if (active) {
            throw new IllegalStateException("Session already active");
        }

        this.active = true;
        this.streamKey = "stream";
        this.rtmpIngestUrl = MediaServerClient.getRecommendedRTMPUrl();
        this.hlsPlaybackUrl = MediaServerClient.getHLSUrl(streamKey);

        System.out.println("🎬 Stream started for " + user.getUsername());
        System.out.println("📡 RTMP Ingest: " + rtmpIngestUrl);
        System.out.println("📺 HLS Playback: " + hlsPlaybackUrl);
    }

    public void stopSession() {
        if (!active) {
            throw new IllegalStateException("Session not active");
        }

        this.active = false;
        this.endedAt = LocalDateTime.now();
        System.out.println("🛑 Stream ended for " + user.getUsername());
    }

    public boolean isActive() {
        return active;
    }

    // ---------- Messaging (LO2 & LO5) ----------
    /**
     * LO2: Arrays - Adds to ArrayList LO5: Generic Collections - Type-safe
     * addition
     */
    public void addMessage(ChatMessage msg) {
        messages.add(msg);
        totalMessages++;
    }

    /**
     * LO2: Arrays - Returns ArrayList LO5: Generic Collections - Returns
     * List<ChatMessage>
     */
    public List<ChatMessage> getMessages() {
        return new ArrayList<>(messages); // Return defensive copy
    }

    // ---------- Analytics ----------
    public void updateViewerCount(int count) {
        if (count > peakViewerCount) {
            peakViewerCount = count;
        }
    }

    public void incrementNotifications() {
        notificationsSent++;
    }

    public String getDuration() {
        LocalDateTime end = endedAt != null ? endedAt : LocalDateTime.now();
        Duration duration = Duration.between(startedAt, end);

        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        return String.format("%dh %dm %ds", hours, minutes, seconds);
    }

    // ---------- Getters (LO1: Encapsulation) ----------
    public String getSessionId() {
        return sessionId;
    }

    public User getUser() {
        return user; // LO3: Aggregation
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public String getRtmpIngestUrl() {
        return rtmpIngestUrl;
    }

    public String getHlsPlaybackUrl() {
        return hlsPlaybackUrl;
    }

    public String getStreamKey() {
        return streamKey;
    }

    public int getPeakViewerCount() {
        return peakViewerCount;
    }

    public int getTotalMessages() {
        return totalMessages;
    }

    public int getNotificationsSent() {
        return notificationsSent;
    }

    public boolean isStreamingLive() {
        return active && MediaServerClient.isStreamActive(streamKey);
    }

    @Override
    public String toString() {
        return "StreamSession{"
                + "sessionId='" + sessionId + '\''
                + ", user=" + user.getUsername()
                + ", active=" + active
                + ", messages=" + totalMessages
                + ", duration=" + getDuration()
                + '}';
    }
}
