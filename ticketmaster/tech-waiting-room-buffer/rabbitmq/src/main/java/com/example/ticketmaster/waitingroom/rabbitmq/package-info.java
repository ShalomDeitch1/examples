/**
 * Why this exists in this repo:
 * - Demonstrates RabbitMQ as the pipe for the waiting-room buffer, using push delivery bridged into tick processing.
 *
 * Real system notes:
 * - Production needs explicit ack/retry/DLQ strategy, idempotency, and external observability; in-process buffering is not durable.
 *
 * How it fits this example flow:
 * - HTTP publishes to a queue; {@code @RabbitListener} buffers IDs; a scheduled tick drains a batch into {@code RequestStore}.
 *
 * RabbitMQ implementation of the "waiting room buffer".
 *
 * <h2>What RabbitMQ is used for</h2>
 * RabbitMQ is the <em>pipe</em>: join requests are published to a queue and delivered to consumers.
 * This module runs in <b>push mode</b>:
 * the broker delivers messages to our listener callback.
 *
 * <h2>Flow (push mode)</h2>
 * <ul>
 *   <li>HTTP enqueues by publishing to the queue (via {@code PushJoinPublisher}).</li>
 *   <li>{@code @RabbitListener} receives messages and stores request IDs into {@code groupCollector}.</li>
 *   <li>{@code @Scheduled PushGrantScheduler} drains up to {@code waitingroom.processing.batch-size} per tick and marks requests processed.</li>
 *   <li>Observability reads from {@code RequestStore} + {@code ProcessingHistory}.</li>
 * </ul>
 *
 * <h2>RabbitMQ-specific notes</h2>
 * <ul>
 *   <li><b>Acknowledgements</b>: message redelivery can occur if the consumer fails; duplicates are possible.</li>
 *   <li><b>Prefetch / throughput</b>: broker delivery rate may exceed tick consumption; the backlog smooths that into fixed-rate processing.</li>
 * </ul>
 */
package com.example.ticketmaster.waitingroom.rabbitmq;
