# Plan — multi-DB workflows

## Goal
Define and later implement multi-store correctness with Redis locks, sagas, and a 2PC educational comparison.

## TODO
- [ ] Implement Redis lock helper (token + Lua release script).
- [ ] Implement saga orchestrator for confirm/finalize with compensations.
- [ ] Add idempotency keys for confirm/finalize.
- [ ] Add integration tests simulating retries (duplicate requests, duplicate events).

## Acceptance criteria
- System remains correct under retries and partial failures.
- Saga compensation restores seat availability when payment fails.
