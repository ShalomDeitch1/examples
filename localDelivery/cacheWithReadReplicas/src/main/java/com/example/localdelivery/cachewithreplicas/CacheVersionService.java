package com.example.localdelivery.cachewithreplicas;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class CacheVersionService {

    private final RedisTemplate<String, String> redisTemplate;

    public CacheVersionService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public long getVersion(String gridId) {
        String key = versionKey(gridId);
        String value = redisTemplate.opsForValue().get(Objects.requireNonNull(key));
        if (value == null) {
            // Initialize to 1 for readability.
            redisTemplate.opsForValue().set(Objects.requireNonNull(key), Objects.requireNonNull("1"));
            return 1;
        }
        return Long.parseLong(value);
    }

    public long bumpVersion(String gridId) {
        String versionKey = versionKey(gridId);
        Long updated = redisTemplate.opsForValue().increment(Objects.requireNonNull(versionKey));

        // Track when we bumped so readers can temporarily avoid replica lag.
        // (This is advisory only; normal TTL-based caching is still used.)
        redisTemplate.opsForValue().set(
            Objects.requireNonNull(lastWriteKey(gridId)),
            Objects.requireNonNull(String.valueOf(System.currentTimeMillis()))
        );

        return updated == null ? 1 : updated;
    }

    public String versionKey(String gridId) {
        return "items:grid:" + gridId + ":version";
    }

    public String dataKey(String gridId, long version) {
        return "items:grid:" + gridId + ":v" + version;
    }

    public long getLastWriteMillis(String gridId) {
        String value = redisTemplate.opsForValue().get(Objects.requireNonNull(lastWriteKey(gridId)));
        if (value == null) {
            return 0;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public String lastWriteKey(String gridId) {
        return "items:grid:" + gridId + ":lastWriteMs";
    }
}
