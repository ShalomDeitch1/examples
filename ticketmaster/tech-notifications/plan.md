# Plan — notifications overview

## Goal
Document three notification approaches (webhook, in-app notifications, SSE) including idempotency, correlation, and failure modes.

## TODO
- [ ] Define common event format: `WaitingRoomActivated(sessionId, userId, eventId, issuedAt)`.
- [ ] Write leaf subproject docs for webhook/app-notification/SSE.
- [ ] Add security notes (HMAC signatures, replay protection) for webhook.

## Acceptance criteria
- Each leaf subproject includes a sequence diagram and a clear retry/idempotency story.
