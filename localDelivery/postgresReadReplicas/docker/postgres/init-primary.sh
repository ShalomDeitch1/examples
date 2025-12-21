#!/bin/bash
set -euo pipefail

# This script runs inside the official postgres image during initialization.
# It creates a replication user and enables settings required for streaming replication.

# Use shell expansion for the password (unquoted heredoc) so the server
# receives a normal SQL string literal instead of a psql variable token.
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-SQL
DO \$do\$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'replicator') THEN
    CREATE ROLE replicator WITH REPLICATION LOGIN PASSWORD '${REPLICATOR_PASSWORD}';
  ELSE
    ALTER ROLE replicator WITH REPLICATION LOGIN PASSWORD '${REPLICATOR_PASSWORD}';
  END IF;
END
\$do\$;
SQL

# Allow replication connections from the network
# Note: postgres:16 defaults to SCRAM password hashing; use scram-sha-256 here.
# Also allow host connections (so the app on the host can connect via the mapped port).
if ! grep -qE '^host\s+replication\s+replicator\s+0\.0\.0\.0/0\s+scram-sha-256\s*$' "$PGDATA/pg_hba.conf"; then
  echo "host replication replicator 0.0.0.0/0 scram-sha-256" >> "$PGDATA/pg_hba.conf"
fi

if ! grep -qE '^host\s+all\s+all\s+0\.0\.0\.0/0\s+scram-sha-256\s*$' "$PGDATA/pg_hba.conf"; then
  echo "host all all 0.0.0.0/0 scram-sha-256" >> "$PGDATA/pg_hba.conf"
fi

# Tweak server settings via ALTER SYSTEM (writes to postgresql.auto.conf)
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-SQL
  ALTER SYSTEM SET wal_level = 'replica';
  ALTER SYSTEM SET listen_addresses = '*';
  ALTER SYSTEM SET max_wal_senders = 5;
  ALTER SYSTEM SET max_wal_size = '1GB';
  ALTER SYSTEM SET wal_keep_size = '64MB';
  SELECT pg_reload_conf();
SQL

echo "Primary init script completed"
