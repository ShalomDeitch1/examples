# Plan — in-app notification feed (polling)

## Goal
Define and later implement a polling-based notification feed for leaving the waiting room.

## TODO
- [ ] Define notifications table schema and query pattern (`since` cursor).
- [ ] Implement `GET /api/users/{userId}/notifications`.
- [ ] Implement user app poller and UI placeholder endpoint (or logs).
- [ ] Add tests for cursor correctness and idempotent reads.

## Acceptance criteria
- Notifications are ordered and cursor-based.
- Polling is safe under retries and duplicates.
