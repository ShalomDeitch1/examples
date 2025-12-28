package com.example.ticketmaster.waitingroom.redisstreams;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class WaitingRoomStoreTest {

  @Test
  void createWaitingRequiresFields() {
    WaitingRoomStore store = new WaitingRoomStore(Clock.systemUTC());

    assertThatThrownBy(() -> store.createWaiting("", "U1"))
        .isInstanceOf(IllegalArgumentException.class);

    assertThatThrownBy(() -> store.createWaiting("E1", ""))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void tryActivateRespectsCapacity() {
    Clock clock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);
    WaitingRoomStore store = new WaitingRoomStore(clock);

    WaitingRoomSession s1 = store.createWaiting("E1", "U1");
    WaitingRoomSession s2 = store.createWaiting("E1", "U2");

    assertThat(store.tryActivateIfCapacityAllows(s1.id(), 1)).isTrue();
    assertThat(store.tryActivateIfCapacityAllows(s2.id(), 1)).isFalse();
  }
}
