package com.example.ticketmaster.waitingroom.redisstreams;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RedisStreamsProperties.class)
public class RedisStreamsPullConfiguration {
}
