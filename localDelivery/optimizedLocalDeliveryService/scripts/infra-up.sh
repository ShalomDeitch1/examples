#!/usr/bin/env bash
set -euo pipefail

# Resolve script directory robustly (works when called via symlink or from any CWD)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$SCRIPT_DIR/.."
cd "$ROOT_DIR"

echo "Starting Redis + Postgres (primary + replica)..."

# Decide whether to start the postgres-primary service or assume an existing server is present.
# If host port 5470 is already listening or a container named optimized-postgres-primary exists,
# don't ask compose to create a new primary (to avoid port-allocation failures).
SKIP_PRIMARY=0
if ss -ltn 2>/dev/null | grep -q ':5470\b'; then
  echo "Host TCP port 5470 already in use; will skip creating postgres-primary container."
  SKIP_PRIMARY=1
fi
if docker ps --filter "name=optimized-postgres-primary" --format '{{.Names}}' | grep -q .; then
  echo "Found existing container named optimized-postgres-primary; will skip creating primary."
  SKIP_PRIMARY=1
fi

# Compose service list to bring up. Skip primary when appropriate to avoid bind conflicts.
SERVICES=(redis postgres-replica)
if [ "$SKIP_PRIMARY" -eq 0 ]; then
  SERVICES=(redis postgres-primary postgres-replica)
fi

# Try to start the selected services. If a port-allocation error occurs for the primary,
# retry without the primary (assume an external/other container owns the port).
set +e
docker compose up -d --remove-orphans "${SERVICES[@]}"
RC=$?
set -e
if [ $RC -ne 0 ]; then
  echo "docker compose returned exit code $RC. Checking for port-allocation issues..."
  # If port 5470 is allocated, retry without primary
  if docker compose up -d --remove-orphans redis postgres-replica >/dev/null 2>&1; then
    echo "Retry succeeded when skipping postgres-primary. Continuing."
  else
    echo "Warning: docker compose failed to start requested services. Continuing and attempting to use any existing services."
  fi
fi

# Helper: find a running container name that publishes a given host port
find_container_by_host_port() {
  local port="$1"
  docker ps --format '{{.Names}} {{.Ports}}' | awk -v p=":${port}->" 'index($0, p) {print $1; exit}'
}

# Determine which container to use for primary Postgres checks
POSTGRES_CN=""
if docker ps --filter "name=optimized-postgres-primary" --format '{{.Names}}' | grep -q .; then
  POSTGRES_CN=optimized-postgres-primary
else
  # try to find any container exposing host port 5470
  POSTGRES_CN=$(find_container_by_host_port 5470 || true)
fi

if [ -n "$POSTGRES_CN" ]; then
  echo "Using Postgres container: $POSTGRES_CN"
  echo "Waiting for Postgres primary to accept connections..."
  until docker exec "$POSTGRES_CN" pg_isready -U local -d local_delivery >/dev/null 2>&1; do
    sleep 1
  done
else
  # If nothing obvious, check if the host port 5470 is already listening (external Postgres)
  if ss -ltn 2>/dev/null | grep -q ':5470\b'; then
    echo "Host TCP port 5470 is already in use; assuming an existing Postgres and skipping container wait"
  else
    echo "Warning: could not find postgres container nor host port 5470 listening; docker compose may have failed to start postgres"
  fi
fi

# Redis
if docker ps --filter "name=optimized-redis" --format '{{.Names}}' | grep -q .; then
  REDIS_CN=optimized-redis
else
  # try to find any container exposing host port 6379
  REDIS_CN=$(find_container_by_host_port 6379 || true)
fi

if [ -n "$REDIS_CN" ]; then
  echo "Waiting for Redis ($REDIS_CN) to be reachable..."
  until docker exec "$REDIS_CN" redis-cli PING 2>/dev/null | grep -q PONG; do
    sleep 1
  done
else
  if ss -ltn 2>/dev/null | grep -q ':6379\b'; then
    echo "Host TCP port 6379 is already in use; assuming an existing Redis and skipping container wait"
  else
    echo "Warning: could not find redis container nor host port 6379 listening; docker compose may have failed to start redis"
  fi
fi

echo "Infra status (docker compose ps):"
docker compose ps || true
