package com.example.ticketmaster.locking.multidb.redis;

import org.springframework.stereotype.Component;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.SetParams;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

@Component
public class RedisLock {
    private static final String RELEASE_SCRIPT = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

    private final JedisPool jedisPool;

    public RedisLock(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    public boolean tryAcquire(String lockKey, String token, Duration ttl) {
        Objects.requireNonNull(lockKey);
        Objects.requireNonNull(token);
        Objects.requireNonNull(ttl);

        try (Jedis jedis = jedisPool.getResource()) {
            String result = jedis.set(lockKey, token, SetParams.setParams().nx().px(ttl.toMillis()));
            return "OK".equals(result);
        }
    }

    public boolean release(String lockKey, String token) {
        Objects.requireNonNull(lockKey);
        Objects.requireNonNull(token);

        try (Jedis jedis = jedisPool.getResource()) {
            Object result = jedis.eval(RELEASE_SCRIPT, List.of(lockKey), List.of(token));
            return Long.valueOf(1L).equals(result);
        }
    }
}
