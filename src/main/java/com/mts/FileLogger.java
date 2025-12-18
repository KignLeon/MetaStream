package com.mts;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * LO8: Text File I/O - Reads and writes stream logs
 * LO7: Exception Handling - Proper error handling for I/O operations
 */
public class FileLogger {
    private static final String LOG_FILE = "stream_log.txt";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Log a chat message
     * LO8: File I/O - Writing to text file
     */
    public void logChat(String user, String message) {
        // LO7: Exception Handling
        try (FileWriter fw = new FileWriter(LOG_FILE, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            
            String timestamp = LocalDateTime.now().format(FORMATTER);
            out.println("[" + timestamp + "] " + user + ": " + message);
            
        } catch (IOException e) {
            System.err.println("❌ FileLogger Error: " + e.getMessage());
        }
    }

    /**
     * Write complete session log
     * LO8: File I/O - Writing structured data to text file
     * LO7: Exception Handling - Comprehensive error handling
     */
    public void writeLog(StreamSession session) {
        if (session == null) {
            System.err.println("⚠️ Cannot write log: session is null");
            return;
        }

        try (FileWriter fw = new FileWriter(LOG_FILE, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            
            // Write session header
            out.println();
            out.println("==================================================");
            out.println("Stream Session Log");
            out.println("==================================================");
            out.println("User: " + session.getUser().getUsername());
            out.println("Session ID: " + session.getSessionId());
            
            // Format timestamps
            if (session.getStartTime() != null) {
                out.println("Started: " + session.getStartTime().format(FORMATTER));
            }
            
            if (session.getEndTime() != null) {
                out.println("Ended: " + session.getEndTime().format(FORMATTER));
            }
            
            // Session statistics
            out.println("Duration: " + session.getDuration());
            out.println("Total Messages: " + session.getTotalMessages());
            out.println("Peak Viewers: " + session.getPeakViewerCount());
            
            out.println();
            out.println("Chat Messages:");
            out.println("--------------------------------------------------");
            
            // Note: Individual chat messages are already logged via logChat()
            // This section header is for organization
            
            out.println();
            
            out.flush();
            
        } catch (IOException e) {
            // LO7: Exception Handling - Proper error reporting
            System.err.println("⚠️ Error writing session log: " + e.getMessage());
            e.printStackTrace();
        }
    }
}