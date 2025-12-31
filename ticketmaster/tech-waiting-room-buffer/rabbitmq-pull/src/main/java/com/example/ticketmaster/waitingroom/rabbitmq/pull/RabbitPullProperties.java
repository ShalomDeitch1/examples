/**
 * Why this exists in this repo:
 * - Holds RabbitMQ-specific configuration (exchange/queue/routing key) for the pull-mode demo.
 *
 * Real system notes:
 * - In production, topology is usually managed externally; apps reference stable names and use well-defined message schemas.
 *
 * How it fits this example flow:
 * - Used by publisher and poller to publish to / pull from the correct queue.
 */
package com.example.ticketmaster.waitingroom.rabbitmq.pull;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "waitingroom.rabbitmq")
public record RabbitPullProperties(String exchange, String queue, String routingKey) {
}
