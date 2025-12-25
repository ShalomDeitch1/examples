# Plan — caching + hot keys

## Goal
Document and later implement caching that protects Postgres during spikes, including pipelining and hot-key sharding.

## TODO
- [ ] Define cache key conventions and TTLs.
- [ ] Add internal cache (Caffeine) and external cache (Redis) with fallback.
- [ ] Implement pipelined Redis reads for multi-key fetch.
- [ ] Implement availability version bumping on writes.
- [ ] Add tests demonstrating reduced DB calls under load (integration test with counters).

## Acceptance criteria
- Read endpoints can serve mostly from cache.
- Writes safely invalidate/bypass hot keys using versioning.
