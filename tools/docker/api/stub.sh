#!/bin/sh
# REST API placeholder. Serves a not-implemented JSON on every request.
set -eu

echo "[api-stub] not the real Ktor gateway — replace once api-suite/ lands"

while true; do
  printf 'HTTP/1.1 503 Service Unavailable\r\nContent-Type: application/json\r\nContent-Length: 47\r\n\r\n{"error":"not_implemented","service":"api-stub"}' \
    | nc -l -p 8080 -q 1 >/dev/null 2>&1 || true
done
