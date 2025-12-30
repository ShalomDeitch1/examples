/**
 * Why this exists in this repo:
 * - Holds Kafka-specific configuration for this demo (topic name).
 *
 * Real system notes:
 * - In production you typically centralize topic naming, retention, partitions, and schema evolution.
 *
 * How it fits this example flow:
 * - Used by both publisher and poller to target the correct Kafka topic.
 */
package com.example.ticketmaster.waitingroom.kafka.pull;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "waitingroom.kafka")
public record KafkaPullProperties(String topic) {
}
