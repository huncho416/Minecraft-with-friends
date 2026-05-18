#!/bin/sh
# Publish mythic_stdb.wasm. Uses a persistent identity stored in
# /root/.config/spacetime (mount a volume there). The FIRST run on a fresh
# STDB takes ownership; every subsequent run reuses the same identity so
# additive schema bumps publish cleanly.
set -eu

STDB_HOST="${STDB_HOST:-http://spacetimedb:3000}"
STDB_MODULE="${STDB_MODULE:-mythicpvp}"
WASM="/tmp/mythic_stdb.wasm"
IDENTITY_NAME="mythic-publisher"

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

# Register the host (idempotent) so saved identities resolve.
spacetime server add --no-fingerprint --url "${STDB_HOST}" --default mythic-host 2>/dev/null || true
spacetime server set-default mythic-host 2>/dev/null || true

# Create persistent identity if absent. Subsequent runs reuse the saved key.
if ! spacetime identity list 2>/dev/null | grep -q "${IDENTITY_NAME}"; then
    echo "[publish] no saved identity; creating ${IDENTITY_NAME}"
    spacetime identity new --name "${IDENTITY_NAME}" --no-email 2>&1 | tail -5
fi
spacetime identity set-default "${IDENTITY_NAME}" 2>/dev/null || true

publish_attempt() {
    spacetime publish --server "${STDB_HOST}" -y \
        --bin-path "${WASM}" "${STDB_MODULE}" 2>&1
}

OUTPUT="$(publish_attempt || true)"
echo "${OUTPUT}"
if echo "${OUTPUT}" | grep -q "403 Forbidden"; then
    echo "[publish] 403 ownership mismatch; trying --clear-database (one-time data loss)"
    spacetime publish --server "${STDB_HOST}" -y \
        --clear-database --bin-path "${WASM}" "${STDB_MODULE}" 2>&1 || {
        echo "[publish] STDB still rejects our identity. The module is owned by an" >&2
        echo "[publish] identity we don't have. Wipe stdb-vps-data volume and retry:" >&2
        echo "[publish]   docker compose -f docker-compose.vps.yml stop spacetimedb stdb-publish" >&2
        echo "[publish]   docker volume rm mythicpvp-vps_stdb-vps-data" >&2
        echo "[publish]   docker compose -f docker-compose.vps.yml up -d spacetimedb stdb-publish" >&2
        exit 1
    }
elif echo "${OUTPUT}" | grep -qiE "^Error|Caused by"; then
    echo "[publish] publish failed" >&2
    exit 1
fi
echo "[publish] ok"
