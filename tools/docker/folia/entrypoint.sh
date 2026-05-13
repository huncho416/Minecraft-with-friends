#!/bin/bash
# Folia entrypoint. Seeds /data with default config files on first boot,
# copies suite jars into plugins/, and execs the server.
set -euo pipefail

DATA_DIR="${DATA_DIR:-/data}"
DEFAULTS_DIR="/opt/folia/defaults"
SUITE_DIR="/opt/folia/suite"
SERVER_JAR="/opt/folia/folia.jar"

cd "${DATA_DIR}"

seed() {
    local src="$1"
    local dst="$2"
    if [ ! -f "${dst}" ]; then
        cp "${src}" "${dst}"
        echo "[entrypoint] seeded ${dst}"
    fi
}

seed "${DEFAULTS_DIR}/eula.txt" "${DATA_DIR}/eula.txt"
seed "${DEFAULTS_DIR}/server.properties" "${DATA_DIR}/server.properties"
seed "${DEFAULTS_DIR}/bukkit.yml" "${DATA_DIR}/bukkit.yml"
seed "${DEFAULTS_DIR}/spigot.yml" "${DATA_DIR}/spigot.yml"
mkdir -p "${DATA_DIR}/config"
seed "${DEFAULTS_DIR}/paper-global.yml" "${DATA_DIR}/config/paper-global.yml"

# Apply env-driven overrides to server.properties (idempotent).
apply() {
    local key="$1" value="$2" file="${DATA_DIR}/server.properties"
    if grep -q "^${key}=" "${file}"; then
        sed -i "s|^${key}=.*|${key}=${value}|" "${file}"
    else
        echo "${key}=${value}" >> "${file}"
    fi
}
apply view-distance "${VIEW_DISTANCE:-8}"
apply online-mode "${ONLINE_MODE:-false}"
apply server-name "${SHARD_ID:-mythic}"
apply motd "MythicPvP ${SERVER_TYPE:-server} (${SHARD_ID:-shard})"

# Sync suite jars into plugins/. Mounted user plugins (read-only volume) win.
mkdir -p "${DATA_DIR}/plugins"
if compgen -G "${SUITE_DIR}/*.jar" > /dev/null; then
    for jar in "${SUITE_DIR}"/*.jar; do
        name=$(basename "${jar}")
        # Skip tests/sources/javadoc artifacts.
        case "${name}" in
            *-tests.jar|*-sources.jar|*-javadoc.jar) continue ;;
        esac
        if [ ! -f "${DATA_DIR}/plugins/${name}" ]; then
            cp "${jar}" "${DATA_DIR}/plugins/${name}"
            echo "[entrypoint] installed plugin ${name}"
        fi
    done
fi

echo "[entrypoint] starting Folia (type=${SERVER_TYPE:-?} shard=${SHARD_ID:-?})"
exec java ${JAVA_OPTS} -jar "${SERVER_JAR}" --nogui
