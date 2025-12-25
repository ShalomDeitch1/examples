# Plan — real Ticketmaster implementation

## Goal
Implement a “real” Ticketmaster aligned to HelloInterview: fast reads, strong booking correctness, waiting room, notifications, caching, and two-phase payments.

## TODO

### API + domain
- [ ] Define domain models: Event, Seat, SeatInventory, Order, Ticket, WaitingRoomSession.
- [ ] Define endpoints + idempotency headers.

### Postgres
- [ ] Schema migrations.
- [ ] Seat reservation with locking (start with pessimistic, optionally add optimistic).
- [ ] Reservation TTL job.

### Waiting room (Redis Streams)
- [ ] Stream per event, consumer group `granter`.
- [ ] Capacity gating (max active selectors per event).
- [ ] Crash recovery for pending entries.

### Notifications
- [ ] SSE endpoint using `SseEmitter`.
- [ ] Fallback polling endpoint for session status.

### Caching
- [ ] Caffeine short TTL for ultra-hot reads.
- [ ] Redis cache with versioned hot keys for availability.
- [ ] Redis pipelining for multi-key reads.

### Payments (mocked but realistic)
- [ ] Implement create order → confirm (authorize) → finalize (capture).
- [ ] Handle timeouts, retries, and idempotency.

### Tests
- [ ] Unit tests for business logic.
- [ ] Integration tests with Testcontainers for Postgres + Redis.
- [ ] At least one end-to-end happy path test.

## Acceptance criteria
- Under concurrent booking attempts, no double booking occurs.
- Waiting room enforces a configured max active selection count.
- All flows are testable locally with a single command per module.
