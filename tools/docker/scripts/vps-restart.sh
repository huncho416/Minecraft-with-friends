#!/usr/bin/env bash
# Restart one VPS stack service.
# Usage: ./scripts/vps-restart.sh <service>
set -euo pipefail

cd "$(dirname "$0")/.."

if [ "$#" -ne 1 ]; then
    echo "usage: $0 <service>" >&2
    exit 2
fi

docker compose -f docker-compose.vps.yml restart "$1"
docker compose -f docker-compose.vps.yml ps "$1"
