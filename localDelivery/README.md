# Local Delivery (GoPuff-style) — Example Suite

This folder contains a set of subprojects that demonstrate different architecture and data-layer techniques for a local delivery service (warehouses, customers, and orders).

The system goals (shared across subprojects):
- Listing deliverable items: target p99 ~100ms (prefer availability over strict consistency)
- Placing orders: target ~2s (prefer consistency over availability)
- Handle at least 1000 concurrent users placing orders

The examples intentionally evolve from “get it working” to “optimize the read path”:

## Subprojects

1. `postgresGeo`
   - Basic geospatial queries using PostgreSQL + PostGIS (warehouse relevance queries)

2. `redisGeo`
   - Basic geospatial queries using Redis GEO commands (warehouse proximity)

3. `cachingWithRedisGeo`
   - Cache “deliverable items per location-grid” with Redis TTL to hit the 100ms read goal

4. `postgresReadReplicas`
   - Read replicas via PostgreSQL streaming replication for read-heavy endpoints

5. `cacheWithReadReplicas`
   - Combine Redis caching + read replicas, plus cache versioning to reduce stale reads

6. `simpleLocalDeliveryService`
   - A simple working service (no geo/caching optimizations yet)

7. `optimizedLocalDeliveryService`
   - Optimized version combining grids + Redis cache + (optional) Redis GEO + read replicas

Each subproject has its own `README.md` (concept explanation, architecture diagram, and run instructions) plus a task list under `plan/`.
