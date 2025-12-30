package com.example.ticketmaster.waitingroom.tokensession.store;

import com.example.ticketmaster.waitingroom.tokensession.model.TokenSession;
import com.example.ticketmaster.waitingroom.tokensession.model.TokenSessionStatus;
import java.util.Optional;

public interface SessionStore {
  void createWaiting(String sessionId, String eventId, String userId);

  Optional<TokenSessionStatus> getStatus(String sessionId);

  void markActive(String sessionId);

  void markLeft(String sessionId);

  boolean isActive(String sessionId);

  int activeCount();

  Optional<TokenSession> get(String sessionId);
}
