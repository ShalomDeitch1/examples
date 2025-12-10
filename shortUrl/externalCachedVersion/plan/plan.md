# External Cached Version (Redis) Implementation Plan

## Goal
Create `externalCachedVersion` to solve the consistency issues of `internalCachedVersion`. Use Redis as a shared external cache so that all application instances see the same cached data.

## Tasks

### 1. Project Setup
- [x] Copy `internalCachedVersion` to `externalCachedVersion`.
- [ ] Update `pom.xml`:
    - Rename to `short-url-external-cache`.
    - Remove `caffeine`.
    - Add `spring-boot-starter-data-redis`.
    - Add `testcontainers` for robust testing (optional, but recommended).

### 2. Configuration
- [ ] Update `application.properties`:
    - Set `spring.cache.type=redis`.
    - Configure Redis connection (default localhost:6379 is usually fine, but good to be explicit or allow overrides).
    - Configure Cache TTL (Redis specific config might be needed for TTL, usually via `RedisCacheConfiguration` bean or properties if supported).

### 3. Implementation
- [ ] `UrlService.java`: Annotations `@Cacheable` and `@CacheEvict` remain the same!
- [ ] `ShortUrlApplication.java`: `@EnableCaching` remains the same.
- [ ] Add `RedisConfig.java` (Optional): If we need specific TTLs per cache, we might need a config class. simple property `spring.cache.redis.time-to-live` handles it in newer Boot versions.

### 4. Testing
- [ ] `MultiServerConsistencyTest`:
    - Based on `MultiServerInconsistencyTest`.
    - **Goal**: Verify that when Server A deletes, Server B sees the change.
    - Setup: Start Server A and B (pointing to same H2 file AND same Redis).
    - Flow:
        1. Create on A.
        2. Read A (cache).
        3. Read B (cache hit).
        4. Delete A.
        5. Read B -> **Should be 404** (Cache invalidated centrally).
- [ ] `CachePerformanceTest`: Verify it's still fast (though maybe 2ms vs 0.1ms).

### 5. Documentation
- [ ] Update root `README.md`.
- [ ] Include Mermaid graph showing App -> Redis.
- [ ] Discuss remaining issues (Latency vs Internal, Availability of Redis).

## Dependencies
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<!-- For Testing -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```
