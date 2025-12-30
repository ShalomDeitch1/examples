/**
 * Why this exists in this repo:
 * - Immutable representation of a waiting-room request used consistently across all pipe technologies.
 *
 * Real system notes:
 * - This would typically map to a DB row/document (plus indexes) and might include idempotency keys, tenant fields, and audit metadata.
 *
 * How it fits this example flow:
 * - Created by {@code RequestStore}, transitioned by schedulers/pollers, returned via the HTTP API.
 */
package com.example.ticketmaster.waitingroom.core;

import java.time.Instant;

public record Request(
    String id,
    String eventId,
    String userId,
    RequestStatus status,
    Instant createdAt,
    Instant processedAt
) {
}
