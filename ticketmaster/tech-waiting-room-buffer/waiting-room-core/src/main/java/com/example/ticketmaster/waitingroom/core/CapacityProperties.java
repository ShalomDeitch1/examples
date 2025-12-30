/**
 * Why this exists in this repo:
 * - Demonstrates how capacity-style controls can be configured consistently across tech implementations.
 *
 * Real system notes:
 * - Capacity limits are typically enforced with atomic operations in a shared store (DB/Redis) or at the edge (gateway), not per-instance memory.
 *
 * How it fits this example flow:
 * - Used where the example needs a simple capacity knob (kept as properties to stay tech-agnostic).
 */
package com.example.ticketmaster.waitingroom.core;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "waitingroom.capacity")
public record CapacityProperties(int maxActive) {
}
