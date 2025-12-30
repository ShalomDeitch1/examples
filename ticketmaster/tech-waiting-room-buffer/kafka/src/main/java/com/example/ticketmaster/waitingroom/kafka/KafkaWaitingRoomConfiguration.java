package com.example.ticketmaster.waitingroom.kafka;

import com.example.ticketmaster.waitingroom.core.push.JoinBacklog;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(KafkaWaitingRoomProperties.class)
public class KafkaWaitingRoomConfiguration {

  @Bean
  public JoinBacklog joinBacklog() {
    return new JoinBacklog();
  }

  @Bean
  public NewTopic waitingRoomJoinsTopic(KafkaWaitingRoomProperties properties) {
    return new NewTopic(properties.topic(), 1, (short) 1);
  }
}
