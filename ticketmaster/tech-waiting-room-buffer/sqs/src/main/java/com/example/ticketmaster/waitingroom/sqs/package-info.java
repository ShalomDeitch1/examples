/**
 * Why this exists in this repo:
 * - Demonstrates SQS as the pipe for the waiting-room buffer, using pull (scheduled polling) semantics.
 *
 * Real system notes:
 * - Production systems tune polling/visibility timeouts, batch delete, idempotency, and avoid per-item DB writes under load.
 *
 * How it fits this example flow:
 * - HTTP sends request IDs to SQS; a scheduled poller reads and processes up to N per tick; observability reads core state.
 *
 * SQS implementation of the "waiting room buffer".
 *
 * <h2>What SQS is used for</h2>
 * SQS is the <em>pipe</em>: join requests are sent to an SQS queue and later read by pollers.
 * This module runs in <b>pull mode</b>:
 * the scheduler polls SQS on a fixed tick (no listener callbacks).
 *
 * <h2>Flow (pull mode)</h2>
 * <pre>{@code
 * mermaid
 * flowchart LR
 *   A[HTTP POST /api/waiting-room/requests] --> P[PullJoinPublisher]
 *   P -->|SendMessage| Q[(SQS queue)]
 *   T[@Scheduled PullGrantPoller] -->|ReceiveMessage| Q
 *   T --> S[RequestStore]
 *   T --> H[ProcessingHistory]
 *   A --> O[HTTP GET /api/waiting-room/observability]
 *   O --> S
 *   O --> H
 * }
 * </pre>
 *
 * <h2>SQS-specific notes</h2>
 * <ul>
 *   <li><b>At-least-once delivery</b>: duplicates are possible; processing should be idempotent.</li>
 *   <li><b>Visibility timeout</b>: a message can reappear if not deleted in time.</li>
 *   <li><b>Polling</b>: throughput depends on poll frequency and batch size; this module uses the shared {@code waitingroom.processing.*} settings.</li>
 * </ul>
 */
package com.example.ticketmaster.waitingroom.sqs;
