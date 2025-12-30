/**
 * HTTP API for the token/session waiting room.
 *
 * <p>Endpoints are intentionally small:
 * <ul>
 *   <li>Create a session and enqueue it (join).
 *   <li>Read session status (poll).
 *   <li>Leave a session (release capacity).
 * </ul>
 */
package com.example.ticketmaster.waitingroom.tokensession.api;
