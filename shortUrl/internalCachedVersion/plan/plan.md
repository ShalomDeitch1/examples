# Internal Cached Version Implementation Plan

## Goal
Create `internalCachedVersion` based on `databaseVersion`. Introduce an internal (in-memory) cache to improve read performance over the slow database. Demonstrate the problem of cache inconsistency in a multi-server environment where deletions/updates are not propagated to other servers' caches.

## Tasks

### 1. Project Setup
- [x] Copy `databaseVersion` to `internalCachedVersion`.
- [ ] Rename artifact in `pom.xml` to `short-url-internal-cache`.
- [ ] Add `spring-boot-starter-cache` and `caffeine` (for TTL support) dependencies.

### 2. Implement Caching
- [ ] Enable Caching: Add `@EnableCaching` to `ShortUrlApplication`.
- [ ] Configure Cache: Use caffeine or simple map with TTL.
    -   Define a TTL (e.g., 2 seconds) to ensure eventual consistency without waiting too long in tests.
- [ ] Update `UrlService`:
    -   Annotate `resolve` method with `@Cacheable(value = "urls", key = "#shortId")`.
    -   Annotate `shorten`? No, usually we cache reads.

### 3. Implement Delete Functionality
- [ ] Add `delete(String shortId)` to `UrlService`.
    -   Delete from DB.
    -   Annotate with `@CacheEvict(value = "urls", key = "#shortId")`.
- [ ] Add `DELETE /shorturl/{shortId}` to `UrlController`.

### 4. Implementation Details
-   **Delay**: Keep the AOP delay from the previous version to prove the cache makes things faster.
-   **TTL**: Use `Caffeine` logic or a custom Schedule if Caffeine is overkill, but Caffeine is standard for Spring Boot internal caching with TTL.

### 5. Testing & Validation
-   **Performance Test**:
    -   `CachePerformanceTest`:
        1.  Measure time for first `resolve` (Cache Miss -> DB (Slow)).
        2.  Measure time for second `resolve` (Cache Hit -> Fast).
        3.  Assert 2nd < 1st.

-   **Consistency Test (The Problem)**:
    -   `MultiServerInconsistencyTest`:
        -   This is tricky in a standard integration test because we need two separate ApplicationContexts with *separate* internal caches but *shared* H2 file DB.
        -   **Strategy**:
            -   Start SERVER_A (Context 1) on Port X.
            -   Start SERVER_B (Context 2) on Port Y.
            -   Both point to `jdbc:h2:file:./target/shortUrldb`.
            -   Create ShortURL on A.
            -   Read on A (caches on A).
            -   Read on B (caches on B).
            -   DELETE on A (removes from DB, evicts A's cache).
            -   Read on B -> **Should success (return deleted data)** because B's cache is stale.
            -   Wait TTL.
            -   Read on B -> Should 404 (cache expired, DB checked).

### 6. Documentation
- [ ] Update `README.md` in root to point to `internalCachedVersion`.
- [ ] Detail the "Stale Cache" problem in the new project's README or the root one.

## Dependencies to Add
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

## Configuration (application.properties)
```properties
spring.cache.type=caffeine
spring.cache.caffeine.spec=expireAfterWrite=2s
```
