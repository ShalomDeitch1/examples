/**
 * Why this exists in this repo:
 * - Holds the tech-agnostic core model so the Kafka/Rabbit/SQS/Redis modules only demonstrate “pipe integration”.
 *
 * Real system notes:
 * - You would replace the in-memory store/history with durable state + metrics/tracing; in-memory state is per-instance and not scalable.
 *
 * How it fits this example flow:
 * - Tech modules enqueue request IDs; scheduled processors mark requests processed in {@code RequestStore} and append to {@code ProcessingHistory}.
 *
 * Core building blocks shared by all tech implementations of the "waiting room buffer".
 *
 * <h2>Core mental model</h2>
 * <ol>
 *   <li><b>Enqueue</b> a request via a technology-specific publisher (Kafka/Rabbit/SQS/Redis Streams).</li>
 *   <li><b>Process on a fixed tick</b> (scheduled) in batches, marking requests as processed.</li>
 *   <li><b>Observe</b> counts and processing history via a dedicated endpoint.</li>
 * </ol>
 *
 * <h2>Shared types</h2>
 * <ul>
 *   <li>{@code RequestStore}: in-memory store of requests and their status.</li>
 *   <li>{@code ProcessingHistory}: per-tick record of processed request IDs ("batches").</li>
 *   <li>{@code ProcessingProperties}: batch size and tick rate (shared config).</li>
 *   <li>{@code Observability}: DTO-style view for the HTTP API.</li>
 * </ul>
 *
 * <h2>Push vs pull</h2>
 * <ul>
 *   <li><b>Push</b> implementations (Kafka/RabbitMQ) receive callbacks and write to {@code core.push.groupCollector}.</li>
 *   <li><b>Pull</b> implementations (SQS/Redis Streams) poll the queue/stream directly on the tick.</li>
 * </ul>
 */
package com.example.ticketmaster.waitingroom.core;
