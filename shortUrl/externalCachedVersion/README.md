# Internal Cached Version - URL Shortener

This version adds an **internal (in-memory) cache** using Spring Cache + Caffeine to improve read performance.

## Running Multiple Instances for Testing

### Option 1: Via IDE Run Configurations

Create two separate run configurations in your IDE:

**Server A:**
- Main class: `com.example.shorturl.ShortUrlApplication`
- VM options: `-Dserver.port=8081`
- Program arguments: (none)

**Server B:**
- Main class: `com.example.shorturl.ShortUrlApplication`
- VM options: `-Dserver.port=8082`
- Program arguments: (none)

Both will share the same H2 database file (`${java.io.tmpdir}/shortUrldb`) but have separate caches.

### Option 2: Via Command Line

```bash
# Terminal 1 - Server A
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081

# Terminal 2 - Server B
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8082
```

### Testing the Stale Cache Problem

1. **Create a short URL on Server A:**
   ```bash
   curl -X POST http://localhost:8081/shorturl -H "Content-Type: application/json" -d "{\"url\":\"https://example.com\"}"
   # Returns: {"shortUrl":"abc123xyz"}
   ```

2. **Read from Server A (populates Cache A):**
   ```bash
   curl -I http://localhost:8081/shorturl/abc123xyz
   # Returns: 302 Found
   ```

3. **Read from Server B (populates Cache B):**
   ```bash
   curl -I http://localhost:8082/shorturl/abc123xyz
   # Returns: 302 Found
   ```

4. **Delete from Server A (evicts Cache A, updates DB):**
   ```bash
   curl -X DELETE http://localhost:8081/shorturl/abc123xyz
   # Returns: 204 No Content
   ```

5. **Read from Server A (cache evicted, checks DB):**
   ```bash
   curl -I http://localhost:8081/shorturl/abc123xyz
   # Returns: 404 Not Found ✓
   ```

6. **Read from Server B (STALE CACHE!):**
   ```bash
   curl -I http://localhost:8082/shorturl/abc123xyz
   # Returns: 302 Found ✗ (Should be 404, but cache is stale!)
   ```

7. **Wait 2+ seconds for TTL to expire, then read from Server B again:**
   ```bash
   sleep 3
   curl -I http://localhost:8082/shorturl/abc123xyz
   # Returns: 404 Not Found ✓ (Cache expired, DB checked)
   ```

## Debugging the Test

To debug `MultiServerInconsistencyTest`:

1. Set breakpoints in the test method at key HTTP calls
2. Run the test in debug mode (right-click → Debug Test)
3. The two Spring contexts will start in the same JVM, so you can step through and inspect variables
4. Watch the console for "Creating URL on Server A..." messages to track progress

## Cache Configuration

- **Type:** Caffeine (in-memory)
- **TTL:** 2 seconds (`expireAfterWrite=2s`)
- **Cache name:** `urls`
- **Key:** Short URL ID

See `application.properties` for configuration details.
