package com.mts;

/**
 * LO4: Inheritance & Polymorphism - Implements NotificationService interface
 */
public class TTSNotifier implements NotificationService {

    @Override
    public void sendNotification(String message) {
        // LO4: Polymorphism - Different implementation of interface method
        // Simulated TTS (no external API for demo)
        System.out.println("🔊 [TTS SIMULATED] " + message);
    }
}
