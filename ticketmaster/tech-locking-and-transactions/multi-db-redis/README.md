# Multi-DB (Redis lock)

Smallest possible demo of a **Redis distributed lock** for a critical section that spans multiple stores.

This example implements:
- Acquire: `SET key token NX PX ttl`
- Release: Lua script that deletes **only if** the stored token matches

## Tech choices
- Spring Boot 3.5.9 (no web server)
- Jedis
- Testcontainers Redis

## How to run

```bash
mvn test
```

## Acquire + release

```mermaid
sequenceDiagram
  participant App as App
  participant R as Redis

  App->>R: SET lock:seat:s1 <token> NX PX 5000
  alt acquired
    App->>R: EVAL (release only if token matches)
  else denied
    App-->>App: return false / retry
  end
```

## Why token-check release matters

```mermaid
flowchart TD
  A[Client A acquires lock] --> B[TTL expires]
  B --> C[Client B acquires lock]
  C --> D{A blindly DELs key?}
  D -->|yes| E[Bug: B loses its lock]
  D -->|no, token-check| F[Safe]
```

