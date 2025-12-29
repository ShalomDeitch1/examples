package com.example.ticketmaster.waitingroom.tokensession.queue;

public interface JoinQueue {
  void enqueue(String sessionId, String eventId, String userId);
}
