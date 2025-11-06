package com.mts;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileLogger {

    private static final String LOG_FILE = "stream_log.txt";

    public void writeLog(StreamSession session) {
        try (FileWriter writer = new FileWriter(LOG_FILE, true)) {
            writer.write("=== Stream Log for " + session.getUser().getUsername() + " ===\n");
            for (ChatMessage msg : session.getMessages()) {
                writer.write(msg.toString() + "\n");
            }
            writer.write("\n");
            System.out.println("✅ Stream session logged successfully.");
        } catch (IOException e) {
            System.err.println("⚠️ Error writing log: " + e.getMessage());
        }
    }

    public String readLog() {
        try {
            return Files.readString(Paths.get(LOG_FILE));
        } catch (IOException e) {
            return "⚠️ No logs found.";
        }
    }
}