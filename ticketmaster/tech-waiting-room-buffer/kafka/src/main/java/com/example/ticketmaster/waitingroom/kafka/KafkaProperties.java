/**
 * Why this exists in this repo:
 * - Holds Kafka-specific configuration (topic, etc.) for the demo.
 *
 * Real system notes:
 * - Real deployments separate infra config (brokers, security, partitions) from app config and manage it per environment.
 *
 * How it fits this example flow:
 * - Used by the publisher/listener to write to and read from the Kafka topic.
 */
package com.example.ticketmaster.waitingroom.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "waitingroom.kafka")
public record KafkaProperties(String topic) {
}
