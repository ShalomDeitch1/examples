package com.example.ticketmaster.waitingroom.sqs;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({SqsWaitingRoomProperties.class, WaitingRoomCapacityProperties.class, WaitingRoomGrantProperties.class})
public class SqsPropertiesConfig {
}

