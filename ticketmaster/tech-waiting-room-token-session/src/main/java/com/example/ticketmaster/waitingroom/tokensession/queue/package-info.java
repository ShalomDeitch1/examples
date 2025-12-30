/**
 * Queue abstraction and Redis Streams implementation.
 *
 * <p>The join queue records "join" events into a Redis Stream.
 * A separate cursor store keeps track of the last processed stream entry.
 */
package com.example.ticketmaster.waitingroom.tokensession.queue;
