package com.example.ticketmaster.waitingroom.tokensession.queue;

public interface StreamCursorStore {
  String getLastId();

  void setLastId(String recordId);
}
