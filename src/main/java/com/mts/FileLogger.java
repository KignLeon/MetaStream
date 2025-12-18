package com.mts;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;

/**
 * LO8: Text File I/O - Reads and writes stream logs
 */
public class FileLogger {
    private static final String LOG_FILE = "stream_log.txt";

    public void logChat(String user, String message) {
        // LO7: Exception Handling
        try (FileWriter fw = new FileWriter(LOG_FILE, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            
            String timestamp = LocalDateTime.now().toString();
            out.println("[" + timestamp + "] " + user + ": " + message);
            
        } catch (IOException e) {
            System.err.println("❌ FileLogger Error: " + e.getMessage());
        }
    }
}