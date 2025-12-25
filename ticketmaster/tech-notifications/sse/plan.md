# Plan — SSE notifications

## Goal
Define and later implement SSE-based notifications for leaving the waiting room using Spring MVC.

## TODO
- [ ] Implement `SseEmitter` endpoint keyed by `sessionId`.
- [ ] Track connections (in-memory map keyed by sessionId).
- [ ] Emit events on status transition to ACTIVE.
- [ ] Add tests using MockMvc (basic) and a small integration test (optional).

## Acceptance criteria
- Client receives exactly one `WAITING_ROOM_ACTIVE` event (even if server retries internally).
- Connection timeouts and reconnect behavior are documented.
