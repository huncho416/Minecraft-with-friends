#!/usr/bin/env bash
# Start the VPS in-game testing stack.
# Usage: ./scripts/vps-up.sh [--monitoring] [--no-build]
set -euo pipefail

cd "$(dirname "$0")/.."

COMPOSE=(docker compose -f docker-compose.vps.yml)
BUILD_FLAG="--build"

for arg in "$@"; do
    case "${arg}" in
        --monitoring) COMPOSE+=(--profile monitoring) ;;
        --no-build) BUILD_FLAG="" ;;
        *) echo "usage: $0 [--monitoring] [--no-build]" >&2; exit 2 ;;
    esac
done

mkdir -p servers/hub servers/skyblock-1 servers/geyser

echo "[vps-up] starting VPS test stack"
"${COMPOSE[@]}" up -d ${BUILD_FLAG}
"${COMPOSE[@]}" ps
