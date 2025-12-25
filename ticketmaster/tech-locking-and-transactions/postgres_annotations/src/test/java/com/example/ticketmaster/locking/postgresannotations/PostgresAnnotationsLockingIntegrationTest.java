package com.example.ticketmaster.locking.postgresannotations;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(classes = PostgresAnnotationsLockingApplication.class)
class PostgresAnnotationsLockingIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("locking")
            .withUsername("locking")
            .withPassword("locking");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    SeatReservationService seatReservationService;

    @BeforeEach
    void resetSchema() {
        jdbcTemplate.execute("drop table if exists seat_inventory");
        jdbcTemplate.execute(
                "create table seat_inventory (" +
                        "  event_id text not null," +
                        "  seat_id text not null," +
                        "  status text not null," +
                        "  order_id text null," +
                        "  version bigint not null default 0," +
                        "  primary key (event_id, seat_id)" +
                        ")"
        );
        jdbcTemplate.update(
                "insert into seat_inventory(event_id, seat_id, status, order_id, version) values (?,?,?,?,?)",
                "e1",
                "s1",
                "AVAILABLE",
                null,
                0
        );
    }

    @Test
    void pessimistic_locking_allows_only_one_winner() throws Exception {
        List<Integer> results = runTwoThreadsTogether(
            () -> seatReservationService.reservePessimistic("e1", "s1", "o1"),
            () -> seatReservationService.reservePessimistic("e1", "s1", "o2")
        );

        long successes = results.stream().filter(i -> i == 1).count();
        assertThat(successes).isEqualTo(1);

        String finalStatus = jdbcTemplate.queryForObject(
                "select status from seat_inventory where event_id=? and seat_id=?",
                String.class,
                "e1",
                "s1"
        );
        assertThat(finalStatus).isEqualTo("RESERVED");
    }

    @Test
    void optimistic_locking_allows_only_one_winner() throws Exception {
        List<Integer> results = runTwoThreadsTogether(
            () -> seatReservationService.reserveOptimistic("e1", "s1", "o1"),
            () -> seatReservationService.reserveOptimistic("e1", "s1", "o2")
        );

        long successes = results.stream().filter(i -> i == 1).count();
        assertThat(successes).isEqualTo(1);

        long version = jdbcTemplate.queryForObject(
                "select version from seat_inventory where event_id=? and seat_id=?",
                Long.class,
                "e1",
                "s1"
        );
        assertThat(version).isEqualTo(1L);
    }

    private static List<Integer> runTwoThreadsTogether(Callable<Boolean> a, Callable<Boolean> b) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);

            Callable<Boolean> wrappedA = () -> {
                ready.countDown();
                start.await(5, TimeUnit.SECONDS);
                return a.call();
            };
            Callable<Boolean> wrappedB = () -> {
                ready.countDown();
                start.await(5, TimeUnit.SECONDS);
                return b.call();
            };

            Future<Boolean> fa = executor.submit(wrappedA);
            Future<Boolean> fb = executor.submit(wrappedB);

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            int ra = getAsIntOrDefault(fa, 10, TimeUnit.SECONDS, 0);
            int rb = getAsIntOrDefault(fb, 10, TimeUnit.SECONDS, 0);

            return List.of(ra, rb);
        } finally {
            executor.shutdownNow();
        }
    }

    private static int getAsIntOrDefault(Future<Boolean> f, long timeout, TimeUnit unit, int defaultValue) {
        try {
            Boolean b = f.get(timeout, unit);
            return (b != null && b) ? 1 : 0;
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
