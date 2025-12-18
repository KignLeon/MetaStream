package com.mts;

import java.time.LocalDateTime;

/**
 * LO1: OOP Principles - Simple encapsulated class LO2: Arrays - Stored in
 * ArrayList<ChatMessage>
 */
public class ChatMessage {

    private final String author;
    private final String text;
    private final LocalDateTime timestamp;

    public ChatMessage(String author, String text) {
        this.author = author;
        this.text = text;
        this.timestamp = LocalDateTime.now();
    }

    public String getAuthor() {
        return author;
    }

    public String getText() {
        return text;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "[" + timestamp + "] " + author + ": " + text;
    }
}
