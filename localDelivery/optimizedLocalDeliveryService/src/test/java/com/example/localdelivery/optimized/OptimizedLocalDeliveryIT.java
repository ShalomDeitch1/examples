package com.example.localdelivery.optimized;

import com.example.localdelivery.optimized.service.CacheVersionService;
import com.example.localdelivery.optimized.util.GridKey;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "server.port=0"
})
class OptimizedLocalDeliveryIT {

    private static final UUID SEEDED_CUSTOMER_ALICE = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID SEEDED_ITEM_MILK = UUID.fromString("10000000-0000-0000-0000-000000000001");

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    @Qualifier("primaryJdbcTemplate")
    private JdbcTemplate primaryJdbc;

    @Autowired
    @Qualifier("replicaJdbcTemplate")
    private JdbcTemplate replicaJdbc;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private CacheVersionService cacheVersionService;

    @Test
    void primaryAndReplicaRolesAreCorrect() {
        Boolean primaryInRecovery = primaryJdbc.queryForObject("select pg_is_in_recovery()", Boolean.class);
        Boolean replicaInRecovery = replicaJdbc.queryForObject("select pg_is_in_recovery()", Boolean.class);

        assertNotNull(primaryInRecovery);
        assertNotNull(replicaInRecovery);

        assertFalse(primaryInRecovery, "primary should not be in recovery");
        assertTrue(replicaInRecovery, "replica should be in recovery");
    }

    @Test
    void itemsEndpointWritesToRedisCache() {
        awaitReplicaSeedData();

        double lat = 40.7128;
        double lon = -74.0060;

        String gridId = GridKey.compute(lat, lon);
        long version = cacheVersionService.getVersion(gridId);
        String cacheKey = cacheVersionService.dataKey(gridId, version);

        redisTemplate.delete(cacheKey);

        ResponseEntity<String> response = rest.getForEntity("/items?lat={lat}&lon={lon}", String.class, lat, lon);
        assertTrue(response.getStatusCode().is2xxSuccessful());

        String cached = redisTemplate.opsForValue().get(cacheKey);
        assertNotNull(cached, "expected /items to populate the versioned cache key");

        Long ttlSeconds = redisTemplate.getExpire(cacheKey, TimeUnit.SECONDS);
        assertNotNull(ttlSeconds);
        assertTrue(ttlSeconds > 0, "expected cache entry to have TTL");

        // Geo index should exist (indexed by WarehouseGeoIndexer at startup)
        assertTrue(Boolean.TRUE.equals(redisTemplate.hasKey("warehouses:geo")), "expected Redis GEO index to exist");
    }

    @Test
    void placingAnOrderBumpsCacheVersion() {
        awaitReplicaSeedData();

        double[] customerLatLon = primaryJdbc.queryForObject(
                "select latitude, longitude from customers where customer_id = ?",
                (rs, rowNum) -> new double[]{rs.getDouble("latitude"), rs.getDouble("longitude")},
                SEEDED_CUSTOMER_ALICE
        );
        assertNotNull(customerLatLon);

        String gridId = GridKey.compute(customerLatLon[0], customerLatLon[1]);
        long before = cacheVersionService.getVersion(gridId);

        Map<String, Object> body = Map.of(
                "customerId", SEEDED_CUSTOMER_ALICE.toString(),
                "lines", List.of(Map.of(
                        "itemId", SEEDED_ITEM_MILK.toString(),
                        "qty", 1
                ))
        );

        ResponseEntity<Map> place = rest.postForEntity("/orders", body, Map.class);
        assertTrue(place.getStatusCode().is2xxSuccessful());
        assertNotNull(place.getBody());
        assertNotNull(place.getBody().get("orderId"));

        long after = cacheVersionService.getVersion(gridId);
        assertTrue(after >= before + 1, "expected placing an order to bump the customer grid cache version");
    }

    private void awaitReplicaSeedData() {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(45));

        while (Instant.now().isBefore(deadline)) {
            try {
                Integer count = replicaJdbc.queryForObject(
                        "select count(*) from customers",
                        Integer.class
                );
                if (count != null && count > 0) {
                    return;
                }
            } catch (Exception ignored) {
                // replica may not be ready yet (base backup / initial connection)
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }

        fail("Replica did not show seeded data within timeout. Ensure `docker compose up -d` is running.");
    }
}
