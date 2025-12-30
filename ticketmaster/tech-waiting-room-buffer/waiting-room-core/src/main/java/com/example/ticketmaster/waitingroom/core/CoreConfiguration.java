package com.example.ticketmaster.waitingroom.core;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({ProcessingProperties.class, CapacityProperties.class})
public class CoreConfiguration {

  @Bean
  public RequestStore requestStore() {
    return new RequestStore();
  }

  @Bean
  public ProcessingHistory processingHistory() {
    return new ProcessingHistory();
  }
}
