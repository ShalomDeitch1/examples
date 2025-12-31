# Notifications — overview

This folder compares four ways to notify a user that they are **out of the waiting room**.

All examples use Spring MVC (Java 21, Spring Boot 3.5.9).

## Options

| Option | Delivery | Client complexity | Server complexity | Notes |
|---|---|---:|---:|---|
| In-app notification feed | client polls | low | low | simplest; more latency and polling traffic |
| Long-polling | client polls, server waits | low | medium | fewer requests than polling; near real-time |
| SSE | server push | low-medium | medium | great UX; needs connection management |
| Webhook callback | server → client | medium | medium | requires public callback endpoint + signatures |

## Subprojects

- [`app-notification/`](app-notification/README.md) — User-app polls Ticketmaster for notifications (regular polling).
- [`long-polling/`](long-polling/README.md) — User-app polls, but server holds connection until status changes.
- [`sse/`](sse/README.md) — User subscribes to SSE stream from Ticketmaster.
- [`webhook/`](webhook/README.md) — Ticketmaster calls user-app endpoint when ACTIVE.
