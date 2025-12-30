package com.example.ticketmaster.waitingroom.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "waitingroom.kafka")
public record KafkaProperties(String topic) {
}
