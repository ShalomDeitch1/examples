package com.example.localdelivery.optimized.config;

import com.example.localdelivery.optimized.service.CacheVersionService;
import com.example.localdelivery.optimized.util.GridKey;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.lang.reflect.Method;
import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory redisConnectionFactory, ObjectMapper objectMapper) {
        RedisSerializationContext.SerializationPair<Object> pair =
                RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer(objectMapper));

        RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(15))
                .serializeValuesWith(pair);

        return RedisCacheManager.builder(RedisCacheWriter.nonLockingRedisCacheWriter(redisConnectionFactory))
                .cacheDefaults(cacheConfig)
                .build();
    }

    @Bean("gridVersionKeyGen")
    public org.springframework.cache.interceptor.KeyGenerator gridVersionKeyGen(CacheVersionService versionService) {
        return new org.springframework.cache.interceptor.KeyGenerator() {
            @Override
            public Object generate(Object target, Method method, Object... params) {
                // Expect (double lat, double lon) as params for ItemsService.listDeliverableItems
                double lat = ((Number) params[0]).doubleValue();
                double lon = ((Number) params[1]).doubleValue();
                String gridId = GridKey.compute(lat, lon);
                long version = versionService.getVersion(gridId);
                return gridId + ":v" + version;
            }
        };
    }
}
