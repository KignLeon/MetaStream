package com.mts;

import java.time.Duration;
import java.time.LocalDateTime;
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
    private boolean isActive;

    public StreamSession(User user) {
        this.sessionId = UUID.randomUUID().toString();
        this.user = user;
        this.totalMessages = 0;
        this.peakViewerCount = 0;
        this.isActive = false;
    }

    /**
     * Start the session (called from Main.java)
     */
    public void startSession() {
        this.startTime = LocalDateTime.now();
        this.isActive = true;
    }

    /**
     * Stop the session
     */
    public void stopSession() {
        this.endTime = LocalDateTime.now();
        this.isActive = false;
    }

    // Getters
    public String getSessionId() { 
        return sessionId; 
    }
    
    public User getUser() { 
        return user; 
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    /**
     * Get formatted duration string
     */
    public String getDuration() {
        if (startTime == null) return "00:00:00";
        LocalDateTime end = (endTime != null) ? endTime : LocalDateTime.now();
        Duration d = Duration.between(startTime, end);
        long h = d.toHours();
        long m = d.toMinutesPart();
        long s = d.toSecondsPart();
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    public int getTotalMessages() { 
        return totalMessages; 
    }
    
    public void incrementMessages() { 
        this.totalMessages++; 
    }
    
    public int getPeakViewerCount() { 
        return peakViewerCount; 
    }
    
    public void setPeakViewerCount(int count) {
        if (count > this.peakViewerCount) {
            this.peakViewerCount = count;
        }
    }
}