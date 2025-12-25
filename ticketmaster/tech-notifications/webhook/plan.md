# Plan — webhook notifications

## Goal
Define and later implement a webhook callback flow with signing, retries, and idempotent receiving.

## TODO
- [ ] Define signing scheme and canonical JSON rules.
- [ ] Add webhook sender with retry policy.
- [ ] Add user app receiver endpoint + signature verification.
- [ ] Add tests for signature verification and idempotent handling.

## Acceptance criteria
- Duplicate deliveries do not create duplicate user-visible notifications.
- Invalid signatures are rejected.
