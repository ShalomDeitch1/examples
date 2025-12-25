```markdown
# Plan — multi-db-saga (minimal)

Goal: runnable, testable saga across two DBs:
- Inventory DB owns seat status
- Payments DB owns payment authorization

Acceptance criteria:
- On payment decline, reservation is compensated (seat becomes AVAILABLE)
- On success, seat becomes CONFIRMED and payment is AUTHORIZED
- Retrying with same sagaId is safe (idempotent)
```
