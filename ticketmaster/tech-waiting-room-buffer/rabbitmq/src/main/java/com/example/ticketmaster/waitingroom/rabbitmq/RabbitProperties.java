/**
 * Why this exists in this repo:
 * - Holds RabbitMQ-specific configuration (queue name, etc.) for the demo.
 *
 * Real system notes:
 * - Broker topology (exchanges/bindings) is usually infrastructure-managed and versioned; apps just reference names.
 *
 * How it fits this example flow:
 * - Used by publisher/listener annotations and wiring to target the correct queue.
 */
package com.example.ticketmaster.waitingroom.rabbitmq;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "waitingroom.rabbitmq")
public record RabbitProperties(String exchange, String queue, String routingKey) {
}
