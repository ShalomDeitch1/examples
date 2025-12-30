/**
 * Why this exists in this repo:
 * - Small status enum to keep the demo’s domain model explicit and readable.
 *
 * Real system notes:
 * - Status often becomes a richer state machine (timeouts, retries, dead-lettering) and is usually persisted.
 *
 * How it fits this example flow:
 * - Requests start in WAITING and move to PROCESSED when the scheduled processor handles them.
 */
package com.example.ticketmaster.waitingroom.core;

public enum RequestStatus {
  WAITING,
  PROCESSED
}
