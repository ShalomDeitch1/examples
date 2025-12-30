/**
 * Why this exists in this repo:
 * - Keeps integration tests focused on the end-to-end flow rather than duplicating HTTP boilerplate per module.
 *
 * Real system notes:
 * - Larger systems often use shared test SDKs, contract tests, and richer fixtures; this is intentionally minimal for readability.
 *
 * How it fits this example flow:
 * - Tests enqueue requests and read /observability until processing history shows expected progress.
 *
 * Integration-test support for the waiting-room-buffer modules.
 *
 * <p>Provides a small HTTP client ({@code TestClient}) and DTOs used by the tech-specific integration tests.
 * Tests intentionally avoid relying on stable "batch grouping" since real queue delivery timing varies;
 * instead, assertions focus on counts and observable processed history.</p>
 */
package com.example.ticketmaster.waitingroom.testsupport;
