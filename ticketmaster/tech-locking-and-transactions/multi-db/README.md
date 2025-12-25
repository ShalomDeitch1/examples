# Multi-DB workflows: Redis locks, sagas, and 2PC

When the booking state spans multiple data stores (e.g., Postgres for seats + separate payments DB + cache), you must choose a coordination strategy.

This doc shows three approaches:
1) External lock (Redis-based distributed lock)
2) Saga (orchestration + compensations)
3) Two-phase commit (2PC) (educational)

## Tech choices
- Postgres for seat inventory (example)
- Redis for distributed locks
- Payment provider is mocked

## API sketch

- `POST /api/orders` (create intent)
- `POST /api/orders/{orderId}:confirm` (reserve seats + authorize payment)
- `POST /api/orders/{orderId}:finalize` (capture + issue)

When multi-DB is involved, all these endpoints must be idempotent via `Idempotency-Key`.

## 1) Redis distributed lock (lease)

Key idea: lock on `(eventId, seatId)` with a lease and unique value.

```mermaid
sequenceDiagram
  participant TM as Ticketmaster
  participant R as Redis
  participant PG as Postgres

  TM->>R: SET lock:seat:e1:s9 <token> NX PX 2000
  alt lock acquired
    TM->>PG: TX: reserve seat / update order
    TM->>R: DEL lock:seat:e1:s9 (only if token matches)
  else lock denied
    TM-->>TM: return 409 / retry later
  end
```

Notes:
- Always include token-check on release (Lua script).
- Lease must be long enough for worst-case critical section or use renewal.

## 2) Saga orchestration (recommended when multi-DB)

Key idea: each step commits locally; failures trigger compensations.

```mermaid
sequenceDiagram
  participant C as Client
  participant TM as Ticketmaster (Saga Orchestrator)
  participant PG as Postgres
  participant Pay as Payment Provider

  C->>TM: confirm(orderId, seats, paymentToken)
  TM->>PG: reserve seats (TX)
  TM->>Pay: authorize(amount)
  alt authorize ok
    TM->>PG: mark order AUTHORIZED
    TM-->>C: 202 AUTHORIZED
  else authorize failed
    TM->>PG: compensation: release seats
    TM-->>C: 402 PAYMENT_FAILED
  end
```

## 3) Two-phase commit (2PC) (educational)

Key idea: coordinator asks each resource to "prepare" then "commit".

```mermaid
flowchart LR
  Coord[2PC Coordinator]
  PG[(Postgres)]
  PayDB[(Payments DB)]

  Coord -->|prepare| PG
  Coord -->|prepare| PayDB
  PG -->|prepared| Coord
  PayDB -->|prepared| Coord
  Coord -->|commit| PG
  Coord -->|commit| PayDB
```

Why usually avoided:
- A stuck coordinator can block resources (availability impact).
- Operational complexity is high; modern systems prefer sagas + idempotency.

## End-to-end purchase diagram (multi-DB mindset)

```mermaid
sequenceDiagram
  participant C as Client
  participant TM as Ticketmaster
  participant PG as Postgres
  participant Pay as Payment Provider

  C->>TM: POST /api/orders
  TM->>PG: create order
  TM-->>C: orderId

  C->>TM: POST /api/orders/{id}:confirm
  TM->>PG: reserve seats
  TM->>Pay: authorize
  Pay-->>TM: authId
  TM-->>C: AUTHORIZED

  C->>TM: POST /api/orders/{id}:finalize
  TM->>Pay: capture
  TM->>PG: mark SOLD + issue ticket
  TM-->>C: ticket
```
