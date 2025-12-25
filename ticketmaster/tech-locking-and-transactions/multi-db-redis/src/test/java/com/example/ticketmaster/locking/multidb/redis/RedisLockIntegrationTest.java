package com.example.ticketmaster.locking.multidb.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(classes = MultiDbRedisLockingApplication.class)
class RedisLockIntegrationTest {

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("app.redis.host", redis::getHost);
        registry.add("app.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    RedisLock redisLock;

    @Autowired
    JedisPool jedisPool;

    @BeforeEach
    void clear() {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del("lock:seat:s1");
        }
    }

    @Test
    void only_one_thread_can_acquire_the_lock() throws Exception {
        String tokenA = UUID.randomUUID().toString();
        String tokenB = UUID.randomUUID().toString();

        List<Boolean> results = runTwoThreadsTogether(
                () -> redisLock.tryAcquire("lock:seat:s1", tokenA, Duration.ofSeconds(5)),
                () -> redisLock.tryAcquire("lock:seat:s1", tokenB, Duration.ofSeconds(5))
        );

        assertThat(results.stream().filter(Boolean::booleanValue).count()).isEqualTo(1);
    }

    @Test
    void only_owner_can_release() {
        String tokenOwner = UUID.randomUUID().toString();
        String tokenOther = UUID.randomUUID().toString();

        assertThat(redisLock.tryAcquire("lock:seat:s1", tokenOwner, Duration.ofSeconds(5))).isTrue();
        assertThat(redisLock.release("lock:seat:s1", tokenOther)).isFalse();
        assertThat(redisLock.release("lock:seat:s1", tokenOwner)).isTrue();
    }

    private static List<Boolean> runTwoThreadsTogether(Callable<Boolean> a, Callable<Boolean> b) throws Exception {
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

            return List.of(
                    fa.get(10, TimeUnit.SECONDS),
                    fb.get(10, TimeUnit.SECONDS)
            );
        } finally {
            executor.shutdownNow();
        }
    }
}
