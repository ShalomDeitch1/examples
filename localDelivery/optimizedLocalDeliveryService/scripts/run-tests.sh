#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

"$(dirname "$0")/infra-up.sh"

echo "Running unit tests + integration tests (Failsafe)..."
mvn -q verify

echo "Done."
