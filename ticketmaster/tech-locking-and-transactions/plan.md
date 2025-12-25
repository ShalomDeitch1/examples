# Plan — locking + transactions overview

## Goal
Document and later implement seat reservation + purchase flows with strong correctness, showing multiple locking strategies.

Primary learning outcomes:
- How single-store transactions prevent double booking.
- How conditional writes + transactional writes provide the same guarantees in DynamoDB.
- How to stay correct when operations span multiple data stores (idempotency + sagas).

## TODO
- [ ] Define shared conceptual data model (`SeatInventory`, `Order`, `PaymentIntent`).
- [ ] Define explicit order and seat state transitions (happy path + failures).
- [ ] Define idempotency strategy for all mutation endpoints (`confirm`, `finalize`).
- [ ] Define standard error codes and retry guidance for clients.
- [ ] Ensure each leaf subproject has: tech choices, API sketch, diagrams, trade-offs, and curl examples.

## Acceptance criteria
- Leaf subprojects clearly show how they prevent double-booking.
- Diagrams cover success and failure/compensation flows.
- Idempotency behavior is unambiguous (retries safe; conflicting bodies rejected).
