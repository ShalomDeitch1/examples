# redisGeo

This subproject demonstrates **basic geospatial functionality in Redis** using GEO commands.

Focus:
- Store warehouse locations with `GEOADD`
- Query nearby warehouses via `GEOSEARCH` / `GEORADIUS`
- Use key design that supports “prefix-like” access patterns (e.g., city/region prefixes)

## Architecture

```mermaid
graph LR
  Client[Client] --> API[Spring Boot API]
  API --> Redis[(Redis)]
  Redis --> Geo[(GEO index per key)]
```

## How to Run

Prereqs:
- Docker
- Java 21+
- Maven

Start Redis:

```bash
docker compose up -d
```

Run the app:

```bash
mvn spring-boot:run
```

## Trade-offs / Notes

- Redis GEO queries are fast and easy to operationalize.
- Redis GEO uses geohash internally; accuracy is “good enough” for proximity queries, but you still need to think about units, radius cutoffs, and bounding.

## Task list

See `plan/TASKS.md`.
