package com.mts;

public class User {

    private final String username;
    private String phone;
    private boolean ttsEnabled;

    public User(String username) {
        this.username = username;
    }

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

    @Override
    public String toString() {
        return "User{"
                + "username='" + username + '\''
                + ", phone='" + phone + '\''
                + ", ttsEnabled=" + ttsEnabled
                + '}';
    }
}
