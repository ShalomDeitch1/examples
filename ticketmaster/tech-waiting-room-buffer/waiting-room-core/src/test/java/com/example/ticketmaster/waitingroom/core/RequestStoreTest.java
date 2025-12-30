package com.example.ticketmaster.waitingroom.core;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

class RequestStoreTest {

  @Test
  void createWaiting_requiresEventIdAndUserId() {
    RequestStore store = new RequestStore(Clock.systemUTC());

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
    RequestStore store = new RequestStore(Clock.systemUTC());

    Request r1 = store.createWaiting("E1", "U1");
    Request r2 = store.createWaiting("E1", "U2");
    Request r3 = store.createWaiting("E1", "U3");

    assertThat(r1.id()).isEqualTo("1");
    assertThat(r2.id()).isEqualTo("2");
    assertThat(r3.id()).isEqualTo("3");

    assertThat(store.get(r1.id())).get().isEqualTo(r1);
    assertThat(store.get(r2.id())).get().isEqualTo(r2);
    assertThat(store.get(r3.id())).get().isEqualTo(r3);
  }

  @Test
  void markProcessed_marksProcessed_andAffectsCounts() {
    RequestStore store = new RequestStore(Clock.systemUTC());
    Request r1 = store.createWaiting("E1", "U1");
    Request r2 = store.createWaiting("E1", "U2");

    assertThat(store.counts().waiting()).isEqualTo(2);
    assertThat(store.counts().processed()).isEqualTo(0);

    assertThat(store.markProcessed(r1.id())).isTrue();
    assertThat(store.get(r1.id())).get().extracting(Request::status).isEqualTo(RequestStatus.PROCESSED);
    assertThat(store.get(r2.id())).get().extracting(Request::status).isEqualTo(RequestStatus.WAITING);
    assertThat(store.counts().waiting()).isEqualTo(1);
    assertThat(store.counts().processed()).isEqualTo(1);
  }
}
