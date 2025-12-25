#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

echo "Stopping containers (and removing volumes)..."
docker compose down -v
