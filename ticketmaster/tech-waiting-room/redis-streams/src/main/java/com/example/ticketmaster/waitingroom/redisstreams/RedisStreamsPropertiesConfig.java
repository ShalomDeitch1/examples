package com.example.ticketmaster.waitingroom.redisstreams;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({RedisStreamsWaitingRoomProperties.class, WaitingRoomCapacityProperties.class, WaitingRoomGrantProperties.class})
public class RedisStreamsPropertiesConfig {
}

