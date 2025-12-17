# Tasks — postgresReadReplicas

## Infra
- Create docker compose for:
  - primary postgres
  - at least one replica (streaming replication)
- Document how to inspect replication lag

## App wiring
- Configure 2 datasources:
  - `primaryDataSource` for writes
  - `replicaDataSource` for reads
- Implement routing:
  - reads (GET /items) -> replica
  - writes (POST /orders) -> primary

## Demonstration
- Add a way to simulate replica lag
- Show a scenario where a write is not immediately visible on replica

## Tests
- Unit tests for routing logic
- integration tests (may be heavy) that assert reads go to replica datasource
