# Notifications — overview

This folder compares three ways to notify a user that they are **out of the waiting room**.

All examples use Spring MVC (Java 21, Spring Boot 3.5.9).

## Options

| Option | Delivery | Client complexity | Server complexity | Notes |
|---|---|---:|---:|---|
| Webhook callback | server → client | medium | medium | requires public callback endpoint + signatures |
| In-app notification feed | client polls | low | low | simplest; more latency and polling traffic |
| SSE | server push | low-medium | medium | great UX; needs connection management |

## Subprojects

- `webhook/` — Ticketmaster calls user-app endpoint when ACTIVE.
- `app-notification/` — User-app polls Ticketmaster for notifications.
- `sse/` — User subscribes to SSE stream from Ticketmaster.
