# ShortURL Implementation Evolution

This repository contains a series of sub-projects, each implementing a URL shortening service. The goal is to demonstrate the evolution of a system architecture from a naive implementation to a robust, high-performance solution. We start with the simplest possible version and incrementally address limitations like persistence, latency, and scalability.

## 1. Simplest Version (In-Memory)

The first iteration is a basic implementation that stores URL mappings in memory. It is fast but data is lost when the application restarts.

[Link to Project](./simplestVersion)

```mermaid
graph LR
    User -- Request Short URL --> App[Application]
    App -- Response Short URL --> User
    App -- Store in Map --> Memory[(In-Memory Map)]
```

**Pros:**
* Extremely fast.
* Simple to implement.

**Cons:**
* **Data Loss:** All mappings are lost on restart.
* **Scalability:** Limited by server RAM.

---

## 2. Database Version (Persistence)

To solve the data loss problem, we introduce a persistent database. However, accessing a database introduces network latency. In this specific implementation, we also simulate random latency via Aspect-Oriented Programming (AOP) to mimic real-world slow queries or network issues.

[Link to Project](./databaseVersion)

```mermaid
graph LR
    User -- Request Short URL --> App[Application]
    App -- Response Short URL --> User
    App -- SQL Insert/Select --> DB[(Relational Database)]
    subgraph "Latency Simulation"
    AOP[AOP Interceptor] -- Intercepts DB Calls --> App
    end
```

**Improvements:**
* **Persistence:** Data survives application restarts.

**Remaining Issues:**
* **Latency:** Database calls are significantly slower than memory access.
* **Bottleneck:** The database becomes a central point of failure and a performance bottleneck under high load.

---

## 3. Internal Cached Version

To mitigate the latency introduced by the database, we add an **Internal (In-Memory) Cache**. Frequently accessed URLs are stored in the application's memory.

[Link to Project](./internalCachedVersion)

```mermaid
graph LR
    User -- Request Short URL --> App[Application]
    App -- Check Cache --> Cache{Internal Cache}
    Cache -- Hit --> App
    Cache -- Miss --> DB[(Relational Database)]
    DB -- Data --> Cache
    App -- Response --> User
```

**Improvements:**
* **Performance:** Read operations are fast (Memory speed) for cached items.
* **Reduced DB Load:** Fewer requests hit the database.

**Remaining Issues:**
* **Consistency:** If multiple application instances are running, one might update/delete a record (clearing its own cache), but other instances will still hold the **stale** data in their caches until the TTL expires.
* **Memory Limit:** Cache size is limited by the application's heap.

---

## 4. External Cached Version (Redis)

To solve the consistency issue in a multi-server environment, we replace the internal cache with an **External Shared Cache** (Redis). All application instances connect to the same Redis instance.

[Link to Project](./externalCachedVersion)

```mermaid
graph LR
    User -- Request --> App1[App Instance 1]
    User -- Request --> App2[App Instance 2]
    
    App1 -- Check/Evict --> Redis[(Shared Redis Cache)]
    App2 -- Check/Evict --> Redis
    
    Redis -- Miss --> DB[(Relational Database)]
    DB -- Data --> Redis
```

**Improvements:**
* **Consistency:** When one instance updates/deletes data, it invalidates the cache in Redis, so all other instances immediately see the change (or get a cache miss and fetch from DB).
* **Scalability:** Cache size is independent of application heap.

**Remaining Issues:**
* **Availability:** Redis becomes another critical component. If Redis goes down, load spikes on the DB.
* **Network Latency:** Accessing Redis is slower than internal memory (but much faster than DB).

---

## Architectural Notes & Future Improvements

### Short ID Generation
All current implementations use a **UUID-based approach** that generates random IDs and checks for collisions. This has performance issues:

**Current Approach:**
```java
do {
    shortId = generateShortId();  // Generate random UUID-based short ID
} while (repository.existsById(shortId));  // Check if it exists
```

**Problems:**
1. **Database Round Trip:** Each collision requires a database query
2. **Index Required:** Needs index on `short_id` column for acceptable performance
3. **Increasing Collision Rate:** As the database fills, collision probability increases
4. **Not Scalable:** Multiple instances could generate the same ID simultaneously

**Better Approaches:**
1. **Counter-Based IDs:** Use a database sequence or distributed counter (e.g., Redis INCR)
   - Pros: No collisions, O(1) generation
   - Cons: Sequential IDs may be predictable
2. **Snowflake IDs:** Combine timestamp + worker ID + sequence
   - Pros: Distributed-safe, time-ordered, no central coordination
   - Cons: Longer IDs (64-bit)
3. **Base62 Encoding of Auto-Increment:** Use database auto-increment, encode in Base62
   - Pros: Short, guaranteed unique
   - Cons: Requires database round-trip for ID generation

### Read/Write Traffic Separation

URL shorteners have **highly asymmetric traffic**:
- **Writes (shortening URLs):** Low volume (maybe 1% of traffic)
- **Reads (redirects):** High volume (99% of traffic)

**Current Implementation:**
All versions use a single service handling both reads and writes. This works for small scale but becomes a bottleneck.

**Production Architecture Should Separate:**

```mermaid
graph TB
    Users[Users/Clients]
    
    Users -->|POST /api/shorten<br/>Low Volume| WriteLB[Write Load Balancer]
    Users -->|GET http://short.url/abc123<br/>High Volume - 302 Redirect| ReadLB[Read Load Balancer]
    
    WriteLB --> WriteService1[Write Service 1]
    WriteLB --> WriteService2[Write Service 2]
    
    ReadLB --> ReadService1[Read Service 1]
    ReadLB --> ReadService2[Read Service 2]
    ReadLB --> ReadService3[Read Service 3]
    
    WriteService1 --> MasterDB[(Master Database)]
    WriteService2 --> MasterDB
    
    MasterDB -->|Replication| ReadReplica1[(Read Replica 1)]
    MasterDB -->|Replication| ReadReplica2[(Read Replica 2)]
    
    ReadService1 --> Redis[(Shared Cache)]
    ReadService2 --> Redis
    ReadService3 --> Redis
    
    Redis -.->|Cache Miss| ReadReplica1
    Redis -.->|Cache Miss| ReadReplica2
```

**Benefits:**
- **Independent Scaling:** Scale read services independently (horizontally) based on traffic
- **Resource Optimization:** Read services can be stateless, lightweight
- **Availability:** Read services can fall back to replicas if cache fails
- **Performance:** Read path optimized for speed (cache-first), write path optimized for consistency
- **Cost Efficiency:** Fewer write instances needed

**Implementation:**
- Different spring-boot applications or modules
- Separate deployment/scaling policies
- Write endpoint: `POST /api/shorten` → returns `http://short.url/abc123`
- Read endpoint: `GET http://short.url/abc123` → 302 redirect to original URL


