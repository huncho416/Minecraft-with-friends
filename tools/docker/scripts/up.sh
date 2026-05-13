#!/usr/bin/env bash
# Bring up the full MythicPvP network.
# Usage: ./up.sh           -> full network (docker-compose.yml)
#        ./up.sh dev       -> dev variant (docker-compose.dev.yml)
set -euo pipefail

cd "$(dirname "$0")/.."

case "${1:-full}" in
    full) FILE="docker-compose.yml" ;;
    dev)  FILE="docker-compose.dev.yml" ;;
    *) echo "usage: $0 [full|dev]" >&2; exit 2 ;;
esac

echo "[up] starting via ${FILE}"
docker compose -f "${FILE}" up -d --build
docker compose -f "${FILE}" ps
