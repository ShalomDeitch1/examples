# waiting-room-core

Shared, non-technology-specific building blocks for the `tech-waiting-room/*` examples.

```mermaid
flowchart LR
  C[Controller in tech module] --> S[WaitingRoomStore]
  C --> J["Join Publisher (tech-specific)"]
  G["Granter (tech-specific)"] --> S
  G --> H[GrantHistory]
```
