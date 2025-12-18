package com.mts;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * LO7: Exception Handling - Try/catch for file operations LO8: Text File I/O -
 * Reads and writes stream logs
 */
public class FileLogger {

    private static final String LOG_FILE = "stream_log.txt";

    /**
     * LO8: Text File I/O - Writes session data to text file LO7: Exception
     * Handling - Try/catch for IOException
     */
    public void writeLog(StreamSession session) {
        try (FileWriter writer = new FileWriter(LOG_FILE, true)) {
            writer.write("=".repeat(50) + "\n");
            writer.write("Stream Session Log\n");
            writer.write("=".repeat(50) + "\n");
            writer.write("User: " + session.getUser().getUsername() + "\n");
            writer.write("Session ID: " + session.getSessionId() + "\n");
            writer.write("Started: " + session.getStartedAt() + "\n");
            writer.write("Ended: " + session.getEndedAt() + "\n");
            writer.write("Duration: " + session.getDuration() + "\n");
            writer.write("Total Messages: " + session.getTotalMessages() + "\n");
            writer.write("Peak Viewers: " + session.getPeakViewerCount() + "\n");
            writer.write("\nChat Messages:\n");
            writer.write("-".repeat(50) + "\n");

            for (ChatMessage msg : session.getMessages()) {
                writer.write(msg.toString() + "\n");
            }

            writer.write("\n");
            System.out.println("✅ Stream session logged to " + LOG_FILE);

        } catch (IOException e) {
            // LO7: Exception Handling
            System.err.println("⚠️ Error writing log: " + e.getMessage());
            throw new RuntimeException("Failed to write log file", e);
        }
    }

    /**
     * LO8: Text File I/O - Reads log file content LO7: Exception Handling -
     * Try/catch for IOException
     */
    public String readLog() {
        try {
            if (!Files.exists(Paths.get(LOG_FILE))) {
                return "No logs found. Start and stop a stream to generate logs.";
            }
            return Files.readString(Paths.get(LOG_FILE));

        } catch (IOException e) {
            // LO7: Exception Handling
            System.err.println("⚠️ Error reading log: " + e.getMessage());
            return "Error reading log file: " + e.getMessage();
        }
    }
}
