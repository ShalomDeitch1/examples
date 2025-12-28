package com.example.ticketmaster.waitingroom.sqs;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "waitingroom.capacity")
public record WaitingRoomCapacityProperties(int maxActive) {
}
