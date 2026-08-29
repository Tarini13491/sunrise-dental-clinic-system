#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
PORT="${PORT:-45321}"
echo "Starting Sunrise Dental Clinic on http://127.0.0.1:${PORT}/"
mvn -q -DskipTests compile exec:java
