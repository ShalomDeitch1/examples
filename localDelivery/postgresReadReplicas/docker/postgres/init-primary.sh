#!/bin/bash
set -euo pipefail

# This script runs inside the official postgres image during initialization.
# It creates a replication user and enables settings required for streaming replication.

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-SQL
  CREATE ROLE replicator WITH REPLICATION LOGIN PASSWORD :'${REPLICATOR_PASSWORD}';
SQL

# Allow replication connections from the network
cat >> "$PGDATA/pg_hba.conf" <<'HBA'
# allow replication user to connect
host replication replicator 0.0.0.0/0 md5
HBA

# Tweak server settings via ALTER SYSTEM (writes to postgresql.auto.conf)
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-SQL
  ALTER SYSTEM SET wal_level = 'replica';
  ALTER SYSTEM SET max_wal_senders = '5';
  ALTER SYSTEM SET max_wal_size = '1GB';
  ALTER SYSTEM SET wal_keep_size = '64MB';
  SELECT pg_reload_conf();
SQL

echo "Primary init script completed"
