package com.example.ticketmaster.waitingroom.core;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

class WaitingRoomStoreTest {

  @Test
  void createWaiting_requiresEventIdAndUserId() {
    WaitingRoomStore store = new WaitingRoomStore(Clock.systemUTC());

    assertThatThrownBy(() -> store.createWaiting(null, "U1"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> store.createWaiting(" ", "U1"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> store.createWaiting("E1", null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> store.createWaiting("E1", " "))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void activate_marksActive() {
    WaitingRoomStore store = new WaitingRoomStore(Clock.systemUTC());

    WaitingRoomSession first = store.createWaiting("E1", "U1");
    WaitingRoomSession second = store.createWaiting("E1", "U2");

    store.activate(first.id());
    store.activate(second.id());

    assertThat(store.get(first.id())).get().extracting(WaitingRoomSession::status).isEqualTo(WaitingRoomSessionStatus.ACTIVE);
    assertThat(store.get(second.id())).get().extracting(WaitingRoomSession::status).isEqualTo(WaitingRoomSessionStatus.ACTIVE);
  }

  @Test
  void createWaiting_assignsMonotonicIds_andAllItemsExist() {
    WaitingRoomStore store = new WaitingRoomStore(Clock.systemUTC());

    WaitingRoomSession s1 = store.createWaiting("E1", "U1");
    WaitingRoomSession s2 = store.createWaiting("E1", "U2");
    WaitingRoomSession s3 = store.createWaiting("E1", "U3");

    assertThat(s1.id()).isEqualTo("1");
    assertThat(s2.id()).isEqualTo("2");
    assertThat(s3.id()).isEqualTo("3");

    assertThat(store.get(s1.id())).get().isEqualTo(s1);
    assertThat(store.get(s2.id())).get().isEqualTo(s2);
    assertThat(store.get(s3.id())).get().isEqualTo(s3);
  }

  @Test
  void heartbeat_doesNotChangeExpiredSession() {
    Instant base = Instant.parse("2025-01-01T00:00:00Z");
    Clock clock = Clock.fixed(base, ZoneOffset.UTC);
    WaitingRoomStore store = new WaitingRoomStore(clock);

    WaitingRoomSession session = store.createWaiting("E1", "U1");
    store.expire(session.id());

    WaitingRoomSession afterHeartbeat = store.heartbeat(session.id());
    assertThat(afterHeartbeat.status()).isEqualTo(WaitingRoomSessionStatus.EXPIRED);
  }

  @Test
  void tryActivateIfCapacityAllows_respectsMaxActive() {
    WaitingRoomStore store = new WaitingRoomStore(Clock.systemUTC());

    WaitingRoomSession s1 = store.createWaiting("E1", "U1");
    WaitingRoomSession s2 = store.createWaiting("E1", "U2");

    assertThat(store.tryActivateIfCapacityAllows(s1.id(), 1)).isTrue();
    assertThat(store.tryActivateIfCapacityAllows(s2.id(), 1)).isFalse();

    store.expire(s1.id());
    assertThat(store.tryActivateIfCapacityAllows(s2.id(), 1)).isTrue();
  }
}
