#!/usr/bin/env bash
set -euo pipefail

# Resolve script directory robustly (works when called via symlink or from any CWD)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$SCRIPT_DIR/.."
cd "$ROOT_DIR"

# If infra containers are already running, skip infra startup so this script is idempotent.
if docker ps --filter "name=optimized-postgres-primary" --format "{{.Names}}" | grep -q . && \
   docker ps --filter "name=optimized-redis" --format "{{.Names}}" | grep -q .; then
  echo "Infra appears to be running; skipping infra-up.sh"
else
  "$SCRIPT_DIR/infra-up.sh"
fi

echo "Building app... (showing Maven output)"
# Show full build output so the user sees progress
mvn -DskipTests package

JAR=$(ls -1 target/*SNAPSHOT.jar | head -n 1)
if [ -z "${JAR:-}" ]; then
  echo "Could not find built jar under target/*.jar" >&2
  exit 1
fi

# Start the app only if nothing is listening on the HTTP port yet.
wait_for_postgres() {
  # Poll for Postgres readiness with exponential backoff (faster feedback than a fixed long wait).
  local attempts=${SMOKE_DB_ATTEMPTS:-20}
  local base_delay=${SMOKE_DB_BASE_DELAY_SECONDS:-1}
  local max_delay=${SMOKE_DB_MAX_DELAY_SECONDS:-5}
  echo "Polling Postgres readiness up to ${attempts} attempts (base ${base_delay}s, max ${max_delay}s)"
  for i in $(seq 1 "$attempts"); do
    # Prefer checking containerized postgres if present
    if docker ps --filter "name=optimized-postgres-primary" --format '{{.Names}}' | grep -q .; then
      if docker exec optimized-postgres-primary pg_isready -U local -d local_delivery >/dev/null 2>&1; then
        echo "Postgres ready in container optimized-postgres-primary (attempt ${i})"
        return 0
      fi
    else
      if command -v pg_isready >/dev/null 2>&1; then
        if pg_isready -h localhost -p 5470 -U local >/dev/null 2>&1; then
          echo "Postgres ready on host:5470 (attempt ${i})"
          return 0
        fi
      else
        if ss -ltn 2>/dev/null | grep -q ':5470\b'; then
          echo "Detected listening socket on host:5470 (assuming Postgres) (attempt ${i})"
          return 0
        fi
      fi
    fi

    # compute exponential backoff delay
    delay=$(( base_delay * (2 ** (i - 1)) ))
    if [ "$delay" -gt "$max_delay" ]; then
      delay=$max_delay
    fi
    echo "Postgres not ready yet (attempt ${i}/${attempts}), sleeping ${delay}s..."
    sleep "$delay"
  done
  echo "Timed out waiting for Postgres after ${attempts} attempts"
  return 1
}

if curl -fsS --max-time 1 "http://localhost:8097/items?lat=40.7128&lon=-74.0060" >/dev/null 2>&1; then
  echo "App already responding on :8097; skipping java -jar"
  APP_PID=""
else
  # Ensure Postgres is reachable before starting the app to avoid immediate JDBC failures
  if ! wait_for_postgres; then
    echo "Warning: Postgres did not become ready in time; continuing to start the app (it may retry)."
  fi
  echo "Starting app: $JAR"
  mkdir -p "$ROOT_DIR/logs"
  # Redirect app logs to a file so the script's terminal isn't tied to the process.
  nohup java -jar "$JAR" >"$ROOT_DIR/logs/smoke-app.log" 2>&1 &
  APP_PID=$!
  echo "App started (pid=$APP_PID), logs: $ROOT_DIR/logs/smoke-app.log"
  # Start a background tail so the user sees startup logs while the script waits for HTTP
  if [ -f "$ROOT_DIR/logs/smoke-app.log" ]; then
    tail -n 200 -f "$ROOT_DIR/logs/smoke-app.log" &
    TAIL_PID=$!
  fi
fi

cleanup() {
  if [ -n "${APP_PID:-}" ]; then
    echo "Stopping app (pid=$APP_PID)"
    kill "$APP_PID" >/dev/null 2>&1 || true
    # wait up to 5 seconds for graceful exit
    for i in 1 2 3 4 5; do
      if kill -0 "$APP_PID" >/dev/null 2>&1; then
        sleep 1
      else
        break
      fi
    done
    if kill -0 "$APP_PID" >/dev/null 2>&1; then
      echo "App did not stop after SIGTERM; sending SIGKILL"
      kill -9 "$APP_PID" >/dev/null 2>&1 || true
    fi
    wait "$APP_PID" 2>/dev/null || true
  fi
  if [ -n "${TAIL_PID:-}" ]; then
    kill "$TAIL_PID" >/dev/null 2>&1 || true
    wait "$TAIL_PID" 2>/dev/null || true
  fi
}
trap cleanup EXIT

echo "Waiting for HTTP..."
# Poll HTTP endpoint with exponential backoff to provide faster visible feedback.
MAX_HTTP_ATTEMPTS=${SMOKE_HTTP_ATTEMPTS:-20}
BASE_HTTP_DELAY=${SMOKE_HTTP_BASE_DELAY_SECONDS:-1}
MAX_HTTP_DELAY=${SMOKE_HTTP_MAX_DELAY_SECONDS:-5}
for attempt in $(seq 1 "$MAX_HTTP_ATTEMPTS"); do
  if curl -fsS --max-time 2 "http://localhost:8097/items?lat=40.7128&lon=-74.0060" >/dev/null 2>&1; then
    echo "HTTP endpoint responded (attempt ${attempt})"
    break
  fi
  delay=$(( BASE_HTTP_DELAY * (2 ** (attempt - 1)) ))
  if [ "$delay" -gt "$MAX_HTTP_DELAY" ]; then
    delay=$MAX_HTTP_DELAY
  fi
  echo "HTTP not ready (attempt ${attempt}/${MAX_HTTP_ATTEMPTS}), sleeping ${delay}s..."
  sleep "$delay"
  if [ "$attempt" -eq "$MAX_HTTP_ATTEMPTS" ]; then
    echo "Timed out waiting for HTTP after ${MAX_HTTP_ATTEMPTS} attempts"
    echo "--- App log (tail 200) ---"
    if [ -f "$ROOT_DIR/logs/smoke-app.log" ]; then
      tail -n 200 "$ROOT_DIR/logs/smoke-app.log"
    else
      echo "No app log found at $ROOT_DIR/logs/smoke-app.log"
    fi
    echo "--- docker compose ps ---"
    docker compose ps || true
    echo "--- docker compose logs (postgres, redis) ---"
    docker compose logs --no-color --tail=200 optimized-postgres-primary optimized-postgres-replica optimized-redis || true
    echo "--- host netstat (listening ports) ---"
    if command -v ss >/dev/null 2>&1; then
      ss -ltn || true
    else
      netstat -ltn || true
    fi
    exit 1
  fi
done

echo "GET /items (should populate cache):"
curl -sS "http://localhost:8097/items?lat=40.7128&lon=-74.0060" | jq .

echo "Create order:"
ORDER_ID=$(curl -sS -X POST http://localhost:8097/orders \
  -H 'Content-Type: application/json' \
  -d '{"customerId":"20000000-0000-0000-0000-000000000001","lines":[{"itemId":"10000000-0000-0000-0000-000000000001","qty":1}]}' | jq -r '.orderId')

echo "Created order: $ORDER_ID"

echo "Confirm payment:"
curl -sS -X POST "http://localhost:8097/orders/$ORDER_ID/confirm-payment" \
  -H 'Content-Type: application/json' \
  -d '{"success": true}' | jq .

echo "Redis keys (sample):"
docker exec optimized-redis redis-cli KEYS 'items:grid:*' | head -n 50 || true
