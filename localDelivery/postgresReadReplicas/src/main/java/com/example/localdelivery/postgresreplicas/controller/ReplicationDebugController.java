package com.example.localdelivery.postgresreplicas.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/debug")
public class ReplicationDebugController {

    private final JdbcTemplate primaryJdbc;
    private final JdbcTemplate replicaJdbc;

    public ReplicationDebugController(JdbcTemplate primaryJdbcTemplate, JdbcTemplate replicaJdbcTemplate) {
        this.primaryJdbc = primaryJdbcTemplate;
        this.replicaJdbc = replicaJdbcTemplate;
    }

    @GetMapping("/lsn")
    public Map<String, Object> lsn() {
        String primaryLsn = primaryJdbc.queryForObject("SELECT pg_current_wal_lsn()::text", String.class);
        String replicaLsn = replicaJdbc.queryForObject("SELECT pg_last_wal_replay_lsn()::text", String.class);
        Long diff = primaryJdbc.queryForObject("SELECT COALESCE(pg_wal_lsn_diff(pg_current_wal_lsn(), (SELECT pg_last_wal_replay_lsn())), 0)", Long.class);
        return Map.of(
                "primary_lsn", primaryLsn,
                "replica_lsn", replicaLsn,
                "lag_bytes", diff
        );
    }
}
