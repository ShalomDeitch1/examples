package com.example.ticketmaster.locking.multidb.saga;

import java.util.UUID;

public interface PaymentGateway {
    boolean authorize(UUID sagaId, int amountCents);
}
