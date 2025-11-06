package com.mts;

import java.util.ArrayList;
import java.util.List;

public class StreamSession {
    private final User user;
    private final List<ChatMessage> messages;

    public StreamSession(User user) {
        this.user = user;
        this.messages = new ArrayList<>();
    }

    public void startSession() {
        System.out.println("🎬 Stream started for " + user.getUsername());
    }

    public void stopSession() {
        System.out.println("🛑 Stream ended for " + user.getUsername());
    }

    public void addMessage(ChatMessage msg) {
        messages.add(msg);
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public User getUser() {
        return user;
    }
}