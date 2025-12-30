package com.example.ticketmaster.waitingroom.core;

import java.util.List;

public record Observability(
    RequestStore.Counts counts,
    List<ProcessingHistory.ProcessingBatch> batches
) {
}
