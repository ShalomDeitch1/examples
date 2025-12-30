/**
 * Why this exists in this repo:
 * - A small response object for the "observability" endpoint, independent of the queue technology.
 *
 * Real system notes:
 * - Production observability is usually metrics + tracing + structured logs (not an in-process DTO assembled from in-memory state).
 *
 * How it fits this example flow:
 * - Controllers build this from {@code RequestStore.counts()} and {@code ProcessingHistory.list()}.
 */
package com.example.ticketmaster.waitingroom.core;

import java.util.List;

public record Observability(
    RequestStore.Counts counts,
    List<ProcessingHistory.ProcessingBatch> batches
) {
}
