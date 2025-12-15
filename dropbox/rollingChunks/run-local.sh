#!/usr/bin/env bash
set -euo pipefail

# run-local.sh - convenience entrypoint for Stage 4 (rollingChunks) from within this module.
# Usage: ./run-local.sh
# Opens UI at: http://localhost:8080

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
DROPBOX_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

"$DROPBOX_DIR/run-local.sh" "$SCRIPT_DIR"
