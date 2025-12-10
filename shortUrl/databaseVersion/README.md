# Database Version - URL Shortener

This version introduces **Persistence** using an H2 Database (File-based). It also simulates **Network Latency** to mimic real-world scenarios.

## Features
- **Persistence:** URLs are stored in an H2 database file (`shortUrldb`), so they survive application restarts.
- **Latency Simulation:** An AOP Aspect (`DelayAspect`) adds a random delay (default ~500ms) to `findById` operations to simulate a slow database or network.

## Running the Application

```bash
mvn spring-boot:run
```

## Testing Latency

You can verify the latency by timing the response:

```bash
curl -w "%{time_total}\n" -o /dev/null -s http://localhost:8080/shorturl/{shortId}
# Expected: > 0.500 seconds
```

## Dependencies
- `spring-boot-starter-data-jpa`
- `h2`
- `spring-boot-starter-aop`
