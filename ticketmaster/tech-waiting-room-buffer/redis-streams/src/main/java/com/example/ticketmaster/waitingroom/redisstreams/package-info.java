/**
 * Why this exists in this repo:
 * - Demonstrates Redis Streams as the pipe for the waiting-room buffer, using pull (scheduled polling) semantics.
 *
 * Real system notes:
 * - Production consumers manage consumer groups, pending entries, and durability; in-memory request state/history is replaced by durable state + metrics.
 *
 * How it fits this example flow:
 * - HTTP appends request IDs to a stream; a scheduled poller reads/acks up to N per tick; observability reads core state.
 *
 * Redis Streams implementation of the "waiting room buffer".
 *
 * <h2>What Redis Streams are used for</h2>
 * Redis Streams are the <em>pipe</em>: join requests are appended to a stream, and consumers read entries.
 * This module runs in <b>pull mode</b>:
 * a scheduled poller reads from the stream on a fixed tick.
 *
 * <h2>Flow (pull mode)</h2>
 * <ul>
 *   <li>HTTP enqueues by appending to the stream (via {@code PullJoinPublisher}).</li>
 *   <li>{@code PullGrantPoller} reads entries and marks corresponding requests processed.</li>
 *   <li>Observability reads from {@code RequestStore} + {@code ProcessingHistory}.</li>
 * </ul>
 *
 * <h2>Redis Streams-specific notes</h2>
 * <ul>
 *   <li><b>Consumer groups</b>: production setups usually use groups; initialization can be required on first run.</li>
 *   <li><b>Pending entries</b>: if a consumer crashes, entries may remain pending and need claiming/cleanup depending on the demo goals.</li>
 *   <li><b>Idempotency</b>: duplicates/replays are possible; keep processing idempotent.</li>
 * </ul>
 */
package com.example.ticketmaster.waitingroom.redisstreams;
