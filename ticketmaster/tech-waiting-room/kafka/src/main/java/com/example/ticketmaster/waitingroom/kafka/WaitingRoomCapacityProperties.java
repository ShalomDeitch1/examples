package com.example.ticketmaster.waitingroom.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "waitingroom.capacity")
public record WaitingRoomCapacityProperties(int maxActive) {
}
