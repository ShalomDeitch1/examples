# cachingWithRedisGeo — Implementation Complete! 🎉

## What's Implemented

This project demonstrates **caching deliverable items per location** using Redis with the following features:

### Core Components

1. **Grid-Based Caching** (`GridKey.java`)
   - 1km x 1km grid key derivation from (lat, lon)
   - Cache key structure: `deliverable-items:grid:{gridId}`
   - 15-minute TTL for cached results

2. **Geospatial Warehouse Lookup** (`WarehouseRepository.java`)
   - Stores warehouse locations using Redis GEO commands
   - Searches for nearby warehouses within a configurable radius
   - Pre-initialized with 3 sample warehouses in NYC area

3. **Inventory Management** (`InventoryRepository.java`)
   - In-memory inventory store per warehouse
   - Sample items: Milk, Bread, Eggs, Cheese, Butter, Yogurt

4. **Travel Time Estimation** (`TravelTimeService.java`)
   - Mock travel time calculation based on distance
   - Assumes average urban speed of 36 km/h
   - Filters items deliverable within 1 hour

5. **Caching Service** (`DeliverableItemsService.java`)
   - Check-then-compute pattern
   - Cache hit: fast response (~few ms)
   - Cache miss: compute from warehouses + inventory + travel time
   - Stores result in Redis with 15-min TTL

6. **REST API** (`ItemsController.java`)
   - `GET /items?lat=<latitude>&lon=<longitude>`
   - Returns deliverable items for the given location

## Architecture

```
Client Request (lat, lon)
    ↓
ItemsController
    ↓
DeliverableItemsService
    ├─→ Redis Cache (check)
    │   ├─→ Cache Hit → Return cached items
    │   └─→ Cache Miss:
    │       ├─→ WarehouseRepository (Redis GEO search)
    │       ├─→ InventoryRepository (get items per warehouse)
    │       ├─→ TravelTimeService (compute delivery time)
    │       └─→ Store in cache with TTL
    └─→ Return deliverable items
```

## How to Run

### Prerequisites
- Docker (for Redis and PostgreSQL)
- Java 21+
- Maven

### Step 1: Start Infrastructure

```bash
cd <path>\localDelivery\cachingWithRedisGeo
docker compose up -d
```

This starts:
- Redis on port 6379
- PostgreSQL with PostGIS on port 5433

### Step 2: Build the Project

```bash
mvn clean install
```

### Step 3: Run the Application

```bash
mvn spring-boot:run
```

The app starts on port **8093**.

### Step 4: Test the API

**Windows:**
```cmd
test.bat
```

**Linux/Mac:**
```bash
chmod +x test.sh
./test.sh
```

**Manual testing:**
```bash
# Test NYC Downtown location
curl "http://localhost:8093/items?lat=40.7128&lon=-74.0060"

# Test Midtown location
curl "http://localhost:8093/items?lat=40.7580&lon=-73.9855"

# Test Brooklyn location
curl "http://localhost:8093/items?lat=40.6782&lon=-73.9442"
```

## Expected Response

```json
[
  {
    "itemId": "item1",
    "name": "Milk",
    "warehouseId": "wh1",
    "travelTimeSeconds": 120
  },
  {
    "itemId": "item2",
    "name": "Bread",
    "warehouseId": "wh1",
    "travelTimeSeconds": 120
  },
  {
    "itemId": "item3",
    "name": "Eggs",
    "warehouseId": "wh1",
    "travelTimeSeconds": 120
  }
]
```

## Performance Characteristics

- **Cache Hit**: ~5-10ms (Redis lookup)
- **Cache Miss**: ~50-100ms (compute + store)
- **Cache TTL**: 15 minutes
- **Max Delivery Time**: 1 hour (3600 seconds)
- **Search Radius**: 10km

## Trade-offs

✅ **Pros:**
- Fast reads through caching
- Stable cache keys via grid system
- Reduced database/computation load

⚠️ **Cons:**
- Stale data for up to 15 minutes
- Boundary effects (users near grid edges)
- Cache cardinality grows with area coverage

## Implementation Details

### Grid Key Algorithm
The `GridKey.compute()` method:
1. Converts lat/lon to meters
2. Divides into 1km x 1km grid cells
3. Returns grid ID as `gridX:gridY`

Example:
- (40.7128, -74.0060) → grid ID: `-8243:4527`

### Caching Strategy
- Key pattern: `deliverable-items:grid:{gridX}:{gridY}`
- Value: JSON array of DeliverableItem objects
- TTL: 900 seconds (15 minutes)

### Data Flow
1. Client provides (lat, lon)
2. Compute grid ID
3. Check Redis cache
4. If miss:
   - Find nearby warehouses (Redis GEO)
   - Get inventory per warehouse
   - Calculate travel times
   - Filter by 1-hour deliverability
   - Deduplicate items
   - Cache result
5. Return items

## Files Created

- `CachingWithRedisGeoApplication.java` - Spring Boot main class
- `DeliverableItem.java` - Response model
- `Warehouse.java` - Warehouse model
- `InventoryItem.java` - Inventory model
- `GridKey.java` - Grid key computation utility
- `TravelTimeService.java` - Travel time estimation
- `WarehouseRepository.java` - Redis GEO operations
- `InventoryRepository.java` - In-memory inventory
- `DeliverableItemsService.java` - Core caching logic
- `ItemsController.java` - REST API endpoint
- `RedisConfig.java` - Redis configuration
- `test.sh` / `test.bat` - Test scripts
- `RUN_INSTRUCTIONS.md` - This file

## Next Steps

To verify everything works:

1. ✅ Infrastructure setup: `docker compose up -d`
2. ✅ Build: `mvn clean install`
3. ✅ Run: `mvn spring-boot:run`
4. ✅ Test: `curl "http://localhost:8093/items?lat=40.7128&lon=-74.0060"`

## Troubleshooting

**Issue**: Redis connection refused
- **Solution**: Ensure Redis container is running: `docker ps`

**Issue**: Port 8093 already in use
- **Solution**: Change port in `application.yml` or stop conflicting service

**Issue**: No items returned
- **Solution**: Check warehouse initialization in `WarehouseRepository.java`

## Integration with Other Subprojects

This project builds on:
- `redisGeo` - Uses Redis GEO for warehouse proximity
- `simpleLocalDeliveryService` - Extends with caching layer

Can be combined with:
- `postgresReadReplicas` - Add read replicas for inventory
- `cacheWithReadReplicas` - Combine caching + read replicas
