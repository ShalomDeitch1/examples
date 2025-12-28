package com.example.ticketmaster.waitingroom.redisstreams;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "waitingroom.redis")
public record RedisStreamsWaitingRoomProperties(String stream, String consumerGroup, String consumerName) {
}
