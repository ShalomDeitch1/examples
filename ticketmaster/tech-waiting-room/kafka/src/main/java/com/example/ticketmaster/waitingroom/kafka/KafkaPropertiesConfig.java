package com.example.ticketmaster.waitingroom.kafka;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(KafkaWaitingRoomProperties.class)
public class KafkaPropertiesConfig {
}

