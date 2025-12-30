/**
 * Why this exists in this repo:
 * - Single place to wire core beans so each tech module can stay focused on “pipe integration”.
 *
 * Real system notes:
 * - Core components would be backed by real infrastructure (DB/Redis/metrics), and configuration would vary by environment.
 *
 * How it fits this example flow:
 * - Provides {@code RequestStore} + {@code ProcessingHistory} and enables the shared {@code @ConfigurationProperties} types.
 */
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
