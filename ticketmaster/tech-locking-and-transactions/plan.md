# Plan — locking + transactions overview

## Goal
Document and later implement seat reservation + purchase flows with strong correctness, showing multiple locking strategies.

## TODO
- [ ] Define shared data model (`SeatInventory`, `Order`, `PaymentIntent`).
- [ ] Write leaf subproject docs and plans.
- [ ] Add explicit idempotency strategy for all mutation endpoints.

## Acceptance criteria
- Leaf subprojects clearly show how they prevent double-booking.
- Diagrams cover success and failure/compensation flows.
