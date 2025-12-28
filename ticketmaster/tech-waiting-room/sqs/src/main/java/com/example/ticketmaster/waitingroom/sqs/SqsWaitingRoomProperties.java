package com.example.ticketmaster.waitingroom.sqs;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "waitingroom.sqs")
public record SqsWaitingRoomProperties(
    String queueName,
    String region,
    String accessKey,
    String secretKey,
    String endpoint,
    boolean autoCreateQueue
) {
}
