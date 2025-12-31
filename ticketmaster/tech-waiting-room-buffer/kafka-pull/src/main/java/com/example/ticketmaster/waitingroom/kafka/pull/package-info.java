/**
 * Kafka pull-mode variant of the waiting-room buffer.
 *
 * <p><b>Why this exists in this repo</b>:
 * this module demonstrates "Kafka in pull mode": the scheduled processor explicitly polls Kafka and commits offsets after processing.</p>
 *
 * <p><b>Real system notes</b>:
 * production Kafka consumers typically also handle rebalances, retries, dead-letter topics, schema evolution, and careful commit strategies.
 * This demo is intentionally minimal and in-memory (see {@code waiting-room-core}).</p>
 *
 * <p><b>How it fits this example flow</b>:
 * HTTP enqueues to Kafka; {@code PullGrantPoller} polls up to {@code waitingroom.processing.batch-size} per tick, marks those requests
 * processed, records a processing batch, and commits offsets for the processed records.</p>
 */
package com.example.ticketmaster.waitingroom.kafka.pull;
