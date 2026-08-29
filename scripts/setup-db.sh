#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SOCKET="${MYSQL_SOCKET:-/var/run/mysqld/mysqld.sock}"

echo "Creating sunrise_dental database and application user..."
sudo mysql --socket="$SOCKET" <<'SQL'
CREATE DATABASE IF NOT EXISTS sunrise_dental CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'sunrise'@'localhost' IDENTIFIED BY 'sunrise123';
CREATE USER IF NOT EXISTS 'sunrise'@'127.0.0.1' IDENTIFIED BY 'sunrise123';
GRANT ALL PRIVILEGES ON sunrise_dental.* TO 'sunrise'@'localhost';
GRANT ALL PRIVILEGES ON sunrise_dental.* TO 'sunrise'@'127.0.0.1';
FLUSH PRIVILEGES;
SET GLOBAL log_bin_trust_function_creators = 1;
SQL

echo "Loading tables..."
sudo mysql --socket="$SOCKET" sunrise_dental < "$ROOT/database/schema.sql"
echo "Loading functions, procedures and triggers..."
sudo mysql --socket="$SOCKET" sunrise_dental < "$ROOT/database/routines.sql"
echo "Loading reference data..."
sudo mysql --socket="$SOCKET" sunrise_dental < "$ROOT/database/seed.sql"
echo "MySQL is ready. Staff accounts are created the first time the Java application starts."
