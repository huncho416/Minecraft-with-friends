#!/usr/bin/env bash
# Stop the VPS in-game testing stack.
# Usage: ./scripts/vps-down.sh [--wipe]
set -euo pipefail

cd "$(dirname "$0")/.."

WIPE="${1:-}"
if [ "${WIPE}" = "--wipe" ]; then
    echo "[vps-down] stopping stack and deleting named database/monitoring volumes"
    docker compose -f docker-compose.vps.yml --profile monitoring down -v
elif [ -z "${WIPE}" ]; then
    echo "[vps-down] stopping stack; bind-mounted server files are preserved"
    docker compose -f docker-compose.vps.yml --profile monitoring down
else
    echo "usage: $0 [--wipe]" >&2
    exit 2
fi
