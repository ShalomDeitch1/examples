/**
 * Why this exists in this repo:
 * - Demonstrates Kafka as the pipe for the waiting-room buffer, and how to connect push delivery into tick-based processing.
 *
 * Real system notes:
 * - You’d use strong schemas, idempotency, consumer scaling, and external observability; an in-process handoff buffer is not durable.
 *
 * How it fits this example flow:
 * - HTTP enqueues to Kafka; a listener buffers IDs; a scheduled tick drains a batch into {@code RequestStore} and records history.
 *
 * Kafka implementation of the "waiting room buffer".
 *
 * <h2>What Kafka is used for</h2>
 * Kafka is the <em>pipe</em>: join requests are appended to a topic and then delivered to consumers.
 * In this module we deliberately run in <b>push mode</b>:
 * Kafka calls our listener when messages arrive.
 *
 * <h2>Flow (push mode)</h2>
 * <pre>{@code
 * mermaid
 * flowchart LR
 *   A[HTTP POST /api/waiting-room/requests] --> B[PushJoinPublisher]
 *   B -->|produce| K[(Kafka topic)]
 *   K -->|consume @KafkaListener| L[PushJoinListener]
 *   L --> Q[groupCollector]
 *   T[@Scheduled PushGrantScheduler] --> Q
 *   T --> S[RequestStore]
 *   T --> H[ProcessingHistory]
 *   A --> O[HTTP GET /api/waiting-room/observability]
 *   O --> S
 *   O --> H
 * }
 * </pre>
 *
 * <h2>Why the backlog exists</h2>
 * The core model is "enqueue requests" and then "process a batch on a fixed tick".
 * Kafka delivery is event-driven; {@code groupCollector} bridges event delivery into the tick-based scheduler.
 *
 * <h2>Kafka-specific notes</h2>
 * <ul>
 *   <li><b>At-least-once delivery</b>: duplicates are possible on retries/rebalances; processing should be idempotent.</li>
 *   <li><b>Consumer groups</b>: scale-out means multiple consumers; ordering is per partition.</li>
 *   <li><b>Back-pressure</b>: the backlog is the local buffer; the scheduler drains it at {@code waitingroom.processing.batch-size} per tick.</li>
 * </ul>
 */
package com.example.ticketmaster.waitingroom.kafka;
