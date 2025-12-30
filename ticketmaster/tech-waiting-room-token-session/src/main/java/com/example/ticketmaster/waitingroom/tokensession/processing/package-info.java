/**
 * Background processing that grants capacity.
 *
 * <p>The scheduler periodically reads join events from Redis Streams and activates sessions up to the
 * configured capacity.
 */
package com.example.ticketmaster.waitingroom.tokensession.processing;
