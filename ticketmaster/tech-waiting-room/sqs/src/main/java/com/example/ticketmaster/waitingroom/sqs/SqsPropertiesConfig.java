package com.example.ticketmaster.waitingroom.sqs;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.example.ticketmaster.waitingroom.core.WaitingRoomCoreConfiguration;

@Configuration
@EnableConfigurationProperties({SqsWaitingRoomProperties.class})
public class SqsPropertiesConfig {
}

