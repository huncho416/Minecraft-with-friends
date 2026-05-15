#!/usr/bin/env bash
# Follow logs for the VPS test stack or one service.
# Usage: ./scripts/vps-logs.sh [service]
set -euo pipefail

cd "$(dirname "$0")/.."

docker compose -f docker-compose.vps.yml logs -f --tail=200 "$@"
