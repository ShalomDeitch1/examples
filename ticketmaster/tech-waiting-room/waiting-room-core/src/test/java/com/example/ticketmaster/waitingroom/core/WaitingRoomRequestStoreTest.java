package com.example.ticketmaster.waitingroom.core;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

class WaitingRoomRequestStoreTest {

  @Test
  void createWaiting_requiresEventIdAndUserId() {
    WaitingRoomRequestStore store = new WaitingRoomRequestStore(Clock.systemUTC());

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
  void createWaiting_assignsMonotonicIds_andAllItemsExist() {
    WaitingRoomRequestStore store = new WaitingRoomRequestStore(Clock.systemUTC());

    WaitingRoomRequest r1 = store.createWaiting("E1", "U1");
    WaitingRoomRequest r2 = store.createWaiting("E1", "U2");
    WaitingRoomRequest r3 = store.createWaiting("E1", "U3");

    assertThat(r1.id()).isEqualTo("1");
    assertThat(r2.id()).isEqualTo("2");
    assertThat(r3.id()).isEqualTo("3");

    assertThat(store.get(r1.id())).get().isEqualTo(r1);
    assertThat(store.get(r2.id())).get().isEqualTo(r2);
    assertThat(store.get(r3.id())).get().isEqualTo(r3);
  }

  @Test
  void markProcessed_marksProcessed_andAffectsCounts() {
    WaitingRoomRequestStore store = new WaitingRoomRequestStore(Clock.systemUTC());
    WaitingRoomRequest r1 = store.createWaiting("E1", "U1");
    WaitingRoomRequest r2 = store.createWaiting("E1", "U2");

    assertThat(store.counts().waiting()).isEqualTo(2);
    assertThat(store.counts().processed()).isEqualTo(0);

    assertThat(store.markProcessed(r1.id())).isTrue();
    assertThat(store.get(r1.id())).get().extracting(WaitingRoomRequest::status).isEqualTo(WaitingRoomRequestStatus.PROCESSED);
    assertThat(store.get(r2.id())).get().extracting(WaitingRoomRequest::status).isEqualTo(WaitingRoomRequestStatus.WAITING);
    assertThat(store.counts().waiting()).isEqualTo(1);
    assertThat(store.counts().processed()).isEqualTo(1);
  }
}
