# Plan — waiting room (queueing) overview

## Goal
Provide comparable, minimal designs for implementing a Ticketmaster waiting room using multiple queueing technologies, and document why the “real implementation” chooses Redis Streams.

## TODO
- [ ] Define shared waiting room state model (`WAITING`, `ACTIVE`, `EXPIRED`) and SLA (timeouts, max active selectors).
- [ ] Document a shared API contract used across all queue implementations.
- [ ] Implement and document subprojects:
  - [ ] `sqs/`
  - [ ] `rabbitmq/`
  - [ ] `kafka/`
  - [ ] `redis-streams/`
- [ ] Add a fairness note (FIFO vs priority, per-user limits).

## Acceptance criteria
- Each tech subproject has its own `README.md` and `plan.md`.
- Each tech subproject includes a clear flow diagram + sequence diagram.
- The overview README links to each tech subproject and compares trade-offs.
