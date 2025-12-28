package com.example.ticketmaster.waitingroom.core;

import java.util.List;

public record WaitingRoomObservability(
    WaitingRoomRequestStore.WaitingRoomCounts counts,
    List<ProcessingHistory.ProcessingBatch> batches
) {
}
