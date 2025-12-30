package com.example.ticketmaster.waitingroom.core;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "waitingroom.capacity")
public record CapacityProperties(int maxActive) {
}
