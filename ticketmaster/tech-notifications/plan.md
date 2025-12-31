# Plan — notifications overview

## Goal
Implement three notification approaches (webhook, in-app notifications, SSE) to demonstrate how to notify a user that they are **out of the waiting room**.

## Implementation Details
- **No Common Event Model**: Each sub-project is independent and self-contained.
- **Testing**: Each sub-project must include `curl` commands in its `README.md` for manual verification.
- **Documentation**: Each sub-project must include a sequence diagram.

## Sub-projects Plan

### 1. App Notification (Polling)
- [ ] Create `README.md` with sequence diagram.
- [ ] Implement Spring Boot app (Java 21, Spring Boot 3.5.9).
- [ ] Implement polling endpoint.

### 2. SSE (Server-Sent Events)
- [ ] Create `README.md` with sequence diagram.
- [ ] Implement Spring Boot app.
- [ ] Implement SSE subscription and event push.

### 3. Webhook
- [ ] Create `README.md` with sequence diagram.
- [ ] Implement Spring Boot app.
- [ ] Implement webhook callback logic (sender).

## Acceptance criteria
- Each leaf subproject includes a sequence diagram.
- Each leaf subproject has clear instructions and `curl` commands for testing.
- All projects use Java 21 and Spring Boot 3.5.9.
