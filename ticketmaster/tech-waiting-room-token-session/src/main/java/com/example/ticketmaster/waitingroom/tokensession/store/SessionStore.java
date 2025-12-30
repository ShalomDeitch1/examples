package com.example.ticketmaster.waitingroom.tokensession.store;

import com.example.ticketmaster.waitingroom.tokensession.model.WaitingRoomSession;
import com.example.ticketmaster.waitingroom.tokensession.model.WaitingRoomSessionStatus;
import java.util.Optional;

public interface SessionStore {
  void createWaiting(String sessionId, String eventId, String userId);

  Optional<WaitingRoomSessionStatus> getStatus(String sessionId);

  void markActive(String sessionId);

  void markLeft(String sessionId);

  boolean isActive(String sessionId);

  int activeCount();

  Optional<WaitingRoomSession> get(String sessionId);
}
