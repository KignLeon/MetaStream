package com.mts;

public class SMSNotifier implements NotificationService {

    @Override
    public void sendNotification(String message) {
        // For now, simulate Twilio SMS behavior
        System.out.println("📱 [SMS SENT] " + message);
    }
}