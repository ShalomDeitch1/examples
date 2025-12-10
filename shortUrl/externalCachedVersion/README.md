# External Cached Version - URL Shortener

This version uses an **External (Redis) Cache** to ensure consistency across multiple application instances.

## Running Multiple Instances for Testing

### Prerequisites
- **Redis**: You must have Redis running locally on port 6379 (or configure `application.properties`).
  ```bash
  docker run -p 6379:6379 -d redis:alpine
  ```

### Option 1: Via IDE Run Configurations

Create two separate run configurations in your IDE:

**Server A:**
- Main class: `com.example.shorturl.ShortUrlApplication`
- VM options: `-Dserver.port=8081`

**Server B:**
- Main class: `com.example.shorturl.ShortUrlApplication`
- VM options: `-Dserver.port=8082`

Both will share the same H2 database file **AND** the same Redis instance.

### Option 2: Via Command Line

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8082
```

### Verifying Consistency

1. **Create a short URL on Server A:**
   ```bash
   curl -X POST http://localhost:8081/shorturl -H "Content-Type: application/json" -d "{\"url\":\"https://example.com\"}"
   # Returns: {"shortUrl":"abc123xyz"}
   ```

2. **Read from Server A (populates Redis):**
   ```bash
   curl -I http://localhost:8081/shorturl/abc123xyz
   # Returns: 302 Found
   ```

3. **Read from Server B (hits Redis):**
   ```bash
   curl -I http://localhost:8082/shorturl/abc123xyz
   # Returns: 302 Found (Cache Hit)
   ```

4. **Delete from Server A (evicts from Redis):**
   ```bash
   curl -X DELETE http://localhost:8081/shorturl/abc123xyz
   # Returns: 204 No Content
   ```

5. **Read from Server B (CONSISTENT!):**
   ```bash
   curl -I http://localhost:8082/shorturl/abc123xyz
   # Returns: 404 Not Found ✓ (Immediately consistent!)
   ```

## Cache Configuration

- **Type:** Redis
- **TTL:** 1 minute (configurable)
- **Host/Port:** localhost:6379

