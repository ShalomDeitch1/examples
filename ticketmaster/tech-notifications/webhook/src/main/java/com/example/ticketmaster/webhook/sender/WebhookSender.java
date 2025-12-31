package com.example.ticketmaster.webhook.sender;

import com.example.ticketmaster.webhook.model.WaitingRoomActiveWebhook;

public interface WebhookSender {
    void send(String callbackUrl, WaitingRoomActiveWebhook payload, long timestampSeconds, String signature);
}
