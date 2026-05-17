#!/bin/sh
# Publish mythic_stdb.wasm to the SpacetimeDB host. Persists identity in
# /root/.config/spacetime (mount a volume there for re-publish to work past
# the first run). On 403 ownership-mismatch, falls back to --clear-database
# which wipes data but re-establishes ownership with the persistent identity.
set -eu

STDB_HOST="${STDB_HOST:-http://spacetimedb:3000}"
STDB_MODULE="${STDB_MODULE:-mythicpvp}"
WASM="/tmp/mythic_stdb.wasm"

echo "[publish] target host=${STDB_HOST} module=${STDB_MODULE}"

i=0
until wget -S -O- "${STDB_HOST}" 2>&1 | grep -Eq 'HTTP/[0-9.]+ (200|400|404)'; do
    i=$((i + 1))
    if [ "$i" -ge 30 ]; then
        echo "[publish] STDB never became reachable at ${STDB_HOST}" >&2
        exit 1
    fi
    sleep 1
done

# Try a normal publish first. If 403, fall back to --clear-database so the
# persistent identity owns the module going forward. Data loss is one-time:
# after this run the identity is stable in the mounted volume and future
# publishes (additive schema changes) succeed without clearing.
publish_attempt() {
    spacetime publish --server "${STDB_HOST}" --anonymous -y \
        --bin-path "${WASM}" "${STDB_MODULE}" 2>&1
}

OUTPUT="$(publish_attempt || true)"
echo "${OUTPUT}"
if echo "${OUTPUT}" | grep -q "403 Forbidden"; then
    echo "[publish] 403 ownership mismatch; falling back to --clear-database"
    spacetime publish --server "${STDB_HOST}" --anonymous -y \
        --clear-database --bin-path "${WASM}" "${STDB_MODULE}"
elif echo "${OUTPUT}" | grep -qiE "error|fail"; then
    echo "[publish] publish failed" >&2
    exit 1
fi
echo "[publish] ok"
