package com.example.ticketmaster.waitingroom.rabbitmq;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({RabbitWaitingRoomProperties.class, WaitingRoomCapacityProperties.class, WaitingRoomGrantProperties.class})
public class RabbitPropertiesConfig {
}

