package com.example.ticketmaster.waitingroom.kafka;

import com.example.ticketmaster.waitingroom.core.push.JoinBacklog;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(KafkaProperties.class)
public class KafkaPushConfiguration {

  @Bean
  public JoinBacklog joinBacklog() {
    return new JoinBacklog();
  }

  @Bean
  public NewTopic joinsTopic(KafkaProperties properties) {
    return new NewTopic(properties.topic(), 1, (short) 1);
  }
}
