package com.mts;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FileLogger {

    private static final String LOG_FILE = "stream_log.txt";

    public static void logSession(StreamSession session) {
        try (PrintWriter out = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            out.println("=== Stream Session Logged at " + now.format(formatter) + " ===");
            // Fixed: changed getUser() to getUsername()
            out.println("User: " + session.getUsername());
            out.println("Session ID: " + session.getSessionId());
            out.println("Duration: " + session.getDuration());
            out.println("Chat Log:");

            // Fixed: changed getMessages() to getChatMessages()
            if (session.getChatMessages() != null) {
                for (ChatMessage msg : session.getChatMessages()) {
                    out.println("[" + msg.getTimestamp() + "] " + msg.getAuthor() + ": " + msg.getText());
                }
            }
            out.println("==================================================");
            out.println();
        } catch (IOException e) {
            System.err.println("Error writing to log file: " + e.getMessage());
        }
    }
}
