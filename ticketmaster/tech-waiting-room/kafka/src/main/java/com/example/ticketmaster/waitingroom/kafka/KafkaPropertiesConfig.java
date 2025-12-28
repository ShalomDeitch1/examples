package com.example.ticketmaster.waitingroom.kafka;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({KafkaWaitingRoomProperties.class, WaitingRoomCapacityProperties.class, WaitingRoomGrantProperties.class})
public class KafkaPropertiesConfig {
}

