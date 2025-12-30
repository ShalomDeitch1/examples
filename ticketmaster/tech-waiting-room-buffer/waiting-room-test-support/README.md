# waiting-room-test-support

Shared test helpers for the `tech-waiting-room/*` integration tests.

```mermaid
sequenceDiagram
  participant T as Test
  participant API as WaitingRoom API
  T->>API: POST /requests (x100)
  loop until all processed
    T->>API: GET /observability
  end
```
