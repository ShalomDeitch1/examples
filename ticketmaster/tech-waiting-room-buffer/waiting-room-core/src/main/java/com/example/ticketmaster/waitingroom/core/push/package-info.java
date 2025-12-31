/**
 * Why this exists in this repo:
 * - Shared helper(s) used by push-based pipes (Kafka/RabbitMQ) to bridge listener callbacks into tick/batch processing.
 *
 * Real system notes:
 * - In-process buffering is not durable and is per-instance; production designs typically process in the listener or use durable internal queues.
 *
 * How it fits this example flow:
 * - Listener callbacks enqueue request IDs into {@link com.example.ticketmaster.waitingroom.core.push.GroupCollector}.
 *   A scheduled processor then drains up to N IDs per tick.
 *
 * Helpers used by <b>push</b> integrations (Kafka/RabbitMQ).
 */
package com.example.ticketmaster.waitingroom.core.push;
