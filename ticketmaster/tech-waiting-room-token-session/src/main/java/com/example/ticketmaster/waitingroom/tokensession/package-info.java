/**
 * Token/session-based waiting room implementation.
 *
 * <p>Concept: when a user "joins" the waiting room, the API creates a server-side session record with
 * status {@code WAITING} and enqueues the session ID into a Redis Stream. A background scheduler
 * periodically "grants" sessions by marking them {@code ACTIVE} up to a configured capacity.
 *
 * <p>This demonstrates the classic "virtual queue" UX:
 * {@code WAITING -> ACTIVE -> LEFT}.
 */
package com.example.ticketmaster.waitingroom.tokensession;
