package com.mts;

/**
 * LO1: OOP Principles - Encapsulation via private fields with getters/setters
 * LO3: Classes & Aggregation - User is aggregated into StreamSession
 */
public class User {

    private final String username;
    private String phone;
    private boolean ttsEnabled;
    private String rtmpUrl;
    private String streamKey;

    public User(String username) {
        this.username = username;
    }

    // LO1: Encapsulation - Public getter for private field
    public String getUsername() {
        return username;
    }

    // LO1: Encapsulation - Public setter for private field
    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPhone() {
        return phone;
    }

    public boolean isTtsEnabled() {
        return ttsEnabled;
    }

    public void setPreferences(boolean ttsEnabled) {
        this.ttsEnabled = ttsEnabled;
    }

    public String getRtmpUrl() {
        return rtmpUrl;
    }

    public void setRtmpUrl(String rtmpUrl) {
        this.rtmpUrl = rtmpUrl;
    }

    public String getStreamKey() {
        return streamKey;
    }

    public void setStreamKey(String streamKey) {
        this.streamKey = streamKey;
    }

    @Override
    public String toString() {
        return "User{"
                + "username='" + username + '\''
                + ", phone='" + phone + '\''
                + ", ttsEnabled=" + ttsEnabled
                + '}';
    }
}
