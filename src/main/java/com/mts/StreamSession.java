package com.mts;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.UUID;

/**
 * LO1: OOP Principles - Encapsulation
 * LO3: Aggregation - Contains a User object
 */
public class StreamSession {
    private String sessionId;
    private User user; // LO3: Aggregation
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int totalMessages;
    private int peakViewerCount;

    public StreamSession(User user) {
        this.sessionId = UUID.randomUUID().toString();
        this.user = user;
        this.startTime = LocalDateTime.now();
        this.totalMessages = 0;
        this.peakViewerCount = 1;
    }

    public void stopSession() {
        this.endTime = LocalDateTime.now();
    }

    public String getSessionId() { return sessionId; }
    public User getUser() { return user; }
    
    public String getDuration() {
        if (startTime == null) return "00:00:00";
        LocalDateTime end = (endTime != null) ? endTime : LocalDateTime.now();
        Duration d = Duration.between(startTime, end);
        long h = d.toHours();
        long m = d.toMinutesPart();
        long s = d.toSecondsPart();
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    public int getTotalMessages() { return totalMessages; }
    public void incrementMessages() { this.totalMessages++; }
    public int getPeakViewerCount() { return peakViewerCount; }
}