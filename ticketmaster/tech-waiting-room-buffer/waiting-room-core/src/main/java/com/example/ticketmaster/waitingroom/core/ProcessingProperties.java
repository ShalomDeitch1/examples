/**
 * Why this exists in this repo:
 * - Central configuration for tick processing (batch size, rate) shared by all implementations.
 *
 * Real system notes:
 * - You’d tune these with load testing and might dynamically adjust based on lag/throughput; defaults here are demo-friendly.
 *
 * How it fits this example flow:
 * - Schedulers/pollers read these properties to decide how many items to process per tick.
 */
package com.example.ticketmaster.waitingroom.core;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "waitingroom.processing")
public record ProcessingProperties(int batchSize) {
  public ProcessingProperties {
    if (batchSize <= 0) {
      throw new IllegalArgumentException("batchSize must be > 0");
    }
  }
}
