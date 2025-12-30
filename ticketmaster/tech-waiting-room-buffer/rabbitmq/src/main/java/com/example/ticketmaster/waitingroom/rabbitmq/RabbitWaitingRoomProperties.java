package com.example.ticketmaster.waitingroom.rabbitmq;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "waitingroom.rabbitmq")
public record RabbitWaitingRoomProperties(String exchange, String queue, String routingKey) {
}
