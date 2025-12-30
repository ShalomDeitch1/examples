/**
 * RabbitMQ pull-mode variant of the waiting-room buffer.
 *
 * <p><b>Why this exists in this repo</b>:
 * this module demonstrates "RabbitMQ in pull mode" using {@code basicGet} from a scheduled poller instead of {@code @RabbitListener}.</p>
 *
 * <p><b>Real system notes</b>:
 * production systems usually tune QoS/prefetch, retries, DLQs, and observability around queue depth and consumer lag.
 * This demo is intentionally small and uses an in-memory state store (see {@code waiting-room-core}).</p>
 *
 * <p><b>How it fits this example flow</b>:
 * HTTP enqueues to an exchange/queue; {@code PullGrantPoller} pulls up to {@code waitingroom.processing.batch-size} messages per tick,
 * marks requests processed, records a processing batch, and acknowledges the messages.</p>
 */
package com.example.ticketmaster.waitingroom.rabbitmq.pull;
