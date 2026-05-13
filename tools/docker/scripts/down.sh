#!/usr/bin/env bash
# Tear down the MythicPvP network.
# Usage: ./down.sh              -> stop containers, keep volumes
#        ./down.sh dev          -> dev variant
#        ./down.sh full --wipe  -> also delete volumes (destructive!)
set -euo pipefail

cd "$(dirname "$0")/.."

VARIANT="${1:-full}"
WIPE="${2:-}"

case "${VARIANT}" in
    full) FILE="docker-compose.yml" ;;
    dev)  FILE="docker-compose.dev.yml" ;;
    *) echo "usage: $0 [full|dev] [--wipe]" >&2; exit 2 ;;
esac

if [ "${WIPE}" = "--wipe" ]; then
    echo "[down] stopping ${FILE} and removing volumes (DESTRUCTIVE)"
    docker compose -f "${FILE}" down -v
else
    echo "[down] stopping ${FILE} (volumes preserved)"
    docker compose -f "${FILE}" down
fi
