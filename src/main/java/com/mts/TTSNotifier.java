package com.mts;

public class TTSNotifier implements NotificationService {

    @Override
    public void sendNotification(String message) {
        // Simulate text-to-speech (you could later use an actual TTS API)
        System.out.println("🔊 [TTS READ] " + message);
    }
}