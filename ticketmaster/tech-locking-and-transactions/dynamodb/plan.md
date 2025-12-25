# Plan — DynamoDB locking

## Goal
Define and later implement a DynamoDB-based seat reservation + ordering flow using conditional updates and transactions.

## TODO
- [ ] Design single-table or multi-table layout for `SeatInventory`, `Order`, `PaymentIntent`.
- [ ] Implement conditional reserve.
- [ ] Implement transactional write to bind seat to order.
- [ ] Add idempotency for confirm/finalize.
- [ ] Integration tests using LocalStack.

## Acceptance criteria
- Conditional writes prevent double booking.
- Transactional write binds order and seat consistently.
