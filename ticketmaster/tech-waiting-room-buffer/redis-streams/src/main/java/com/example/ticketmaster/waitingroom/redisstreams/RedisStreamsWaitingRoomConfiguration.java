package com.example.ticketmaster.waitingroom.redisstreams;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RedisStreamsWaitingRoomProperties.class)
public class RedisStreamsWaitingRoomConfiguration {
}
