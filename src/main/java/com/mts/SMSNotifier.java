package com.mts;

/**
 * LO4: Inheritance & Polymorphism - Implements NotificationService interface
 */
public class SMSNotifier implements NotificationService {

    @Override
    public void sendNotification(String message) {
        // LO4: Polymorphism - Different implementation of interface method
        // Simulated SMS (no external API for demo)
        System.out.println("📱 [SMS SIMULATED] " + message);
    }
}
