#!/usr/bin/env bash
# Show container and port status for the VPS test stack.
set -euo pipefail

cd "$(dirname "$0")/.."

docker compose -f docker-compose.vps.yml --profile monitoring ps
