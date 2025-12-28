package com.example.ticketmaster.waitingroom.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {

  @Bean
  public NewTopic waitingRoomJoinsTopic(KafkaWaitingRoomProperties properties) {
    return new NewTopic(properties.topic(), 1, (short) 1);
  }
}
