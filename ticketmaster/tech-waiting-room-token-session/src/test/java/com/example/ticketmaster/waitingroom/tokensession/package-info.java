/**
 * Integration tests for the token/session waiting room.
 *
 * <p>Uses Testcontainers to boot a real Redis and verifies the full join -> grant -> leave flow over HTTP.
 */
package com.example.ticketmaster.waitingroom.tokensession;
