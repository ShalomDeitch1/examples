/**
 * Why this exists in this repo:
 * - Demonstrates “grant/group sizing” as configuration in a tech-agnostic way.
 *
 * Real system notes:
 * - Real grant sizing is driven by downstream capacity and is often adaptive; a single static value rarely works under varying load.
 *
 * How it fits this example flow:
 * - When used, controls how much work is released/processed in a unit.
 */
package com.example.ticketmaster.waitingroom.core;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "waitingroom.grant")
public record GrantProperties(int groupSize) {
  public GrantProperties {
    if (groupSize <= 0) {
      throw new IllegalArgumentException("groupSize must be > 0");
    }
  }
}
