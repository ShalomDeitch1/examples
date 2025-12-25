package com.example.ticketmaster.locking.multidb.saga;

import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentStore {

    private final JdbcTemplate jdbc;
    private final PaymentGateway gateway;

    public PaymentStore(@Qualifier("paymentsJdbc") JdbcTemplate jdbc, PaymentGateway gateway) {
        this.jdbc = jdbc;
        this.gateway = gateway;
    }

    public String authorize(UUID sagaId, int amountCents) {
        Objects.requireNonNull(sagaId);

        String existing = getStatusOrNull(sagaId);
        if (existing != null) {
            return existing;
        }

        boolean ok = gateway.authorize(sagaId, amountCents);
        String status = ok ? "AUTHORIZED" : "DECLINED";

        jdbc.update(
                "INSERT INTO payments (saga_id, amount_cents, status) VALUES (?::uuid, ?, ?) ON CONFLICT (saga_id) DO NOTHING",
                sagaId.toString(),
                amountCents,
                status
        );

        return getStatusOrNull(sagaId);
    }

    public String getStatusOrNull(UUID sagaId) {
        Objects.requireNonNull(sagaId);

        return jdbc.query(
                "SELECT status FROM payments WHERE saga_id=?::uuid",
                rs -> rs.next() ? rs.getString(1) : null,
                sagaId.toString()
        );
    }
}
