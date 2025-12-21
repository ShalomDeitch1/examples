package com.example.localdelivery.optimized.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class CacheVersionService {

    private final RedisTemplate<String, String> redisTemplate;

    public CacheVersionService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public long getVersion(String gridId) {
        String key = versionKey(gridId);
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            redisTemplate.opsForValue().set(key, "1");
            return 1;
        }
        return Long.parseLong(value);
    }

    public long bumpVersion(String gridId) {
        Long updated = redisTemplate.opsForValue().increment(versionKey(gridId));
        return updated == null ? 1 : updated;
    }

    public String versionKey(String gridId) {
        return "items:grid:" + gridId + ":version";
    }

    public String dataKey(String gridId, long version) {
        return "items:grid:" + gridId + ":v" + version;
    }
}
