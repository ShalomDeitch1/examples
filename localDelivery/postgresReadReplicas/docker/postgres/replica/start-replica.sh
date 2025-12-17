#!/bin/bash
set -euo pipefail

# Replica startup script for the demo. If PGDATA is empty, perform a base backup
# from the primary and start Postgres as a replica. Otherwise just start postgres.

: ${PRIMARY_HOST:=postgres-primary}
: ${PRIMARY_PORT:=5432}
: ${REPLICATOR_PASSWORD:=replicator}

export PGPASSWORD="$REPLICATOR_PASSWORD"

if [ -z "$(ls -A "$PGDATA" 2>/dev/null || true)" ]; then
  echo "PGDATA is empty - running base backup from primary ($PRIMARY_HOST:$PRIMARY_PORT)"
  until pg_isready -h "$PRIMARY_HOST" -p "$PRIMARY_PORT"; do
    echo "Waiting for primary..."
    sleep 1
  done

  # Wait until the primary has applied migrations (detect by presence of a known table)
  echo "Waiting for primary to show migrated schema (checking for 'customers' table)..."
  until PGPASSWORD="$REPLICATOR_PASSWORD" psql -h "$PRIMARY_HOST" -p "$PRIMARY_PORT" -U replicator -d "$POSTGRES_DB" -tAc "SELECT EXISTS(SELECT 1 FROM pg_catalog.pg_tables WHERE schemaname='public' AND tablename='customers');" | grep -q "t"; do
    echo "migrations not detected yet; sleeping 1s..."
    sleep 1
  done

  echo "Migrations detected on primary; performing base backup"
  pg_basebackup -h "$PRIMARY_HOST" -p "$PRIMARY_PORT" -D "$PGDATA" -U replicator -Fp -Xs -P -R
  chown -R postgres:postgres "$PGDATA"
  echo "Base backup complete. Starting replica postgres"
fi

# Exec the original postgres entrypoint/command (CMD)
exec "$@"
