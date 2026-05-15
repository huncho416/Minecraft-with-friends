#!/bin/sh
# Publish mythic_stdb.wasm to the SpacetimeDB host. Idempotent: re-publishing
# the same module is a no-op; schema-incompatible changes will fail loudly.
set -eu

STDB_HOST="${STDB_HOST:-http://spacetimedb:3000}"
STDB_MODULE="${STDB_MODULE:-mythicpvp}"
WASM="/tmp/mythic_stdb.wasm"

echo "[publish] target host=${STDB_HOST} module=${STDB_MODULE}"

# Wait for STDB to be reachable. Compose's `depends_on: healthy` should make
# this immediate, but networks lie — give it 30s before giving up.
i=0
until wget -S -O- "${STDB_HOST}" 2>&1 | grep -Eq 'HTTP/[0-9.]+ (200|400|404)'; do
    i=$((i + 1))
    if [ "$i" -ge 30 ]; then
        echo "[publish] STDB never became reachable at ${STDB_HOST}" >&2
        exit 1
    fi
    sleep 1
done

spacetime server set-default "${STDB_HOST}" >/dev/null 2>&1 || true
spacetime publish --bin-path "${WASM}" "${STDB_MODULE}"
echo "[publish] ok"
