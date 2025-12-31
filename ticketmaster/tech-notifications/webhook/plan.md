# Plan — webhook notifications

## Goal
Implement a minimal webhook callback flow with signing and idempotent receiving.

## Done
- [x] Define signing scheme (HMAC SHA-256 over `timestampSeconds.idempotencyKey`).
- [x] Implement webhook sender (no retry/backoff in this demo).
- [x] Implement user app receiver endpoint + signature/timestamp verification.
- [x] Add tests for signature verification, timestamp skew, and idempotent handling.

## Optional improvements (not needed for the demo)
- Add sender retries with exponential backoff + dead-lettering.
- Add persistent storage for inbox/dedup keys.
- Add per-callback secrets + rotation.

## Acceptance criteria
- Duplicate deliveries do not create duplicate user-visible notifications.
- Invalid signatures are rejected.
