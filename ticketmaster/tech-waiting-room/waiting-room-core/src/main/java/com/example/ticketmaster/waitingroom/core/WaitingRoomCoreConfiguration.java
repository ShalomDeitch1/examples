package com.example.ticketmaster.waitingroom.core;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({WaitingRoomProcessingProperties.class})
public class WaitingRoomCoreConfiguration {

  @Bean
  public WaitingRoomRequestStore waitingRoomRequestStore() {
    return new WaitingRoomRequestStore();
  }

  @Bean
  public ProcessingHistory processingHistory() {
    return new ProcessingHistory();
  }

  @Bean
  public ProcessingBatcher processingBatcher(WaitingRoomProcessingProperties processing, ProcessingHistory history) {
    return new ProcessingBatcher(processing.batchSize(), history);
  }
}
