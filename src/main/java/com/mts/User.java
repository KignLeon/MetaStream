package com.mts;

/**
 * User class represents a streamer or viewer participating in a MetaStream Live
 * session. Stores user identity, communication preferences, and RTMP stream
 * configuration data.
 */
public class User {

    private final String username;
    private String phone;
    private boolean ttsEnabled;

    // ✅ New fields for Instagram or other RTMP-based live streaming
    private String rtmpUrl;
    private String streamKey;

    public User(String username) {
        this.username = username;
    }

    // ---------- Getters and Setters ----------
    public String getUsername() {
        return username;
    }

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

    // --- RTMP Configuration ---
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
                + ", rtmpUrl='" + (rtmpUrl != null ? rtmpUrl : "N/A") + '\''
                + ", streamKey='" + (streamKey != null ? "[HIDDEN]" : "N/A") + '\''
                + '}';
    }
}
