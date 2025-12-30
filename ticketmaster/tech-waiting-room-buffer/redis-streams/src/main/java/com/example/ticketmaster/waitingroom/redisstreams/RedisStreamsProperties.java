/**
 * Why this exists in this repo:
 * - Holds Redis Streams-specific configuration (stream key, group name, consumer name, etc.) for the demo.
 *
 * Real system notes:
 * - Production uses environment-specific stream/group names and carefully managed retention/trim policies.
 *
 * How it fits this example flow:
 * - Used by the initializer, publisher, and poller to operate on the same stream/group.
 */
package com.example.ticketmaster.waitingroom.redisstreams;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "waitingroom.redis")
public record RedisStreamsProperties(String stream, String consumerGroup, String consumerName) {
}
