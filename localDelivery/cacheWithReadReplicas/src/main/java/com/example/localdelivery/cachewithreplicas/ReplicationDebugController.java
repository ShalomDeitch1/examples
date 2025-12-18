package com.example.localdelivery.cachewithreplicas;

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
        Long diff = primaryJdbc.queryForObject(
                "SELECT COALESCE(pg_wal_lsn_diff(pg_current_wal_lsn(), (SELECT pg_last_wal_replay_lsn())), 0)",
                Long.class
        );
        return Map.of(
                "primary_lsn", primaryLsn,
                "replica_lsn", replicaLsn,
                "lag_bytes", diff
        );
    }

    @GetMapping("/db")
    public Map<String, Object> db() {
        Boolean primaryRecovery = primaryJdbc.queryForObject("SELECT pg_is_in_recovery()", Boolean.class);
        Boolean replicaRecovery = replicaJdbc.queryForObject("SELECT pg_is_in_recovery()", Boolean.class);

        Integer primaryPort = primaryJdbc.queryForObject("SELECT inet_server_port()", Integer.class);
        Integer replicaPort = replicaJdbc.queryForObject("SELECT inet_server_port()", Integer.class);

        return Map.of(
                "primary_is_in_recovery", primaryRecovery,
                "replica_is_in_recovery", replicaRecovery,
                "primary_server_port", primaryPort,
                "replica_server_port", replicaPort
        );
    }
}
