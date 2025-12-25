# Plan — simplest Ticketmaster

## Goal
Implement a single-service Ticketmaster baseline with strong correctness and a mocked two-phase payment flow.

## TODO
- [ ] Create Maven project `ticketmaster/simplest-implementation`.
- [ ] Define Postgres schema and migrations.
- [ ] Implement browse endpoints (events, seats).
- [ ] Implement order lifecycle: create → confirm (reserve + authorize) → finalize (capture + ticket).
- [ ] Implement reservation TTL reaper job.
- [ ] Unit tests for business logic.
- [ ] Integration tests with Postgres Testcontainers.
- [ ] Add curl-based README test steps.

## Acceptance criteria
- Double booking is impossible under concurrent requests.
- Tests are runnable with a single command.
