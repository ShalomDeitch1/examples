/**
 * Why this exists in this repo:
 * - Wires Kafka-specific beans (publisher, listener, backlog buffer) for the push-mode example.
 *
 * Real system notes:
 * - Production wiring includes error handling, retries/backoff, DLQs, consumer concurrency, and observability.
 *
 * How it fits this example flow:
 * - Connects the Kafka pipe to the shared core (listener → backlog, scheduler drains backlog, controller reads core state).
 */
package com.example.ticketmaster.waitingroom.kafka;

import com.example.ticketmaster.waitingroom.core.push.GroupCollector;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(KafkaProperties.class)
public class KafkaPushConfiguration {

  @Bean
  public GroupCollector joinBacklog() {
    return new GroupCollector();
  }

  @Bean
  public NewTopic joinsTopic(KafkaProperties properties) {
    return new NewTopic(properties.topic(), 1, (short) 1);
  }
}
