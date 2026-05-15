#!/bin/bash
set -euo pipefail

DATA_DIR="${DATA_DIR:-/data}"
DEFAULTS_DIR="/opt/folia/defaults"
PACKAGED_PLUGINS_DIR="/opt/folia/plugins"
VOICE_PLUGIN_DIR="${VOICE_PLUGIN_DIR:-/opt/folia/voice-plugins}"
USER_PLUGINS_DIR="${USER_PLUGINS_DIR:-/opt/folia/user-plugins}"
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
mkdir -p "${DATA_DIR}/plugins/voicechat"
seed "${DEFAULTS_DIR}/voicechat-server.properties" "${DATA_DIR}/plugins/voicechat/voicechat-server.properties"

apply_property() {
    local file="$1" key="$2" value="$3"
    if grep -q "^${key}=" "${file}"; then
        sed -i "s|^${key}=.*|${key}=${value}|" "${file}"
    else
        echo "${key}=${value}" >> "${file}"
    fi
}
apply_property "${DATA_DIR}/server.properties" view-distance "${VIEW_DISTANCE:-8}"
apply_property "${DATA_DIR}/server.properties" online-mode "${ONLINE_MODE:-true}"
apply_property "${DATA_DIR}/server.properties" server-port "${SERVER_PORT:-${P_SERVER_PORT:-25565}}"
apply_property "${DATA_DIR}/server.properties" server-name "${SHARD_ID:-mythic}"
apply_property "${DATA_DIR}/server.properties" motd "MythicPvP ${SERVER_TYPE:-server} (${SHARD_ID:-shard})"
apply_property "${DATA_DIR}/plugins/voicechat/voicechat-server.properties" port "${VOICE_PORT:-24454}"
apply_property "${DATA_DIR}/plugins/voicechat/voicechat-server.properties" voice_host "${VOICE_HOST:-}"

mkdir -p "${DATA_DIR}/plugins"
rm -rf "${DATA_DIR}/plugins/.paper-remapped"
find "${DATA_DIR}/plugins" -maxdepth 1 -type f \( \
    -name 'suite-*.jar' -o \
    -name 'original-suite-*.jar' -o \
    -name 'simplevoice-geyser.jar' \
\) -delete

SERVER_TYPE_LOWER="$(printf '%s' "${SERVER_TYPE:-hub}" | tr '[:upper:]' '[:lower:]')"

is_plugin_allowed() {
    local name_lower
    name_lower="$(printf '%s' "$1" | tr '[:upper:]' '[:lower:]')"
    case "${name_lower}" in
        mythic-hub*)        [ "${SERVER_TYPE_LOWER}" = "hub" ];      return $? ;;
        mythic-skyblock*)   [ "${SERVER_TYPE_LOWER}" = "skyblock" ]; return $? ;;
        mythic-pvp*)        [ "${SERVER_TYPE_LOWER}" = "pvp" ];      return $? ;;
        mythic-event*)      [ "${SERVER_TYPE_LOWER}" = "event" ];    return $? ;;
        *) return 0 ;;
    esac
}

remove_disallowed_plugin() {
    local name="$1"
    if [ -f "${DATA_DIR}/plugins/${name}" ]; then
        rm -f "${DATA_DIR}/plugins/${name}"
        echo "[entrypoint] removed disallowed plugin ${name} (SERVER_TYPE=${SERVER_TYPE_LOWER})"
    fi
}

install_jars() {
    local source_dir="$1"
    if compgen -G "${source_dir}/*.jar" > /dev/null; then
        for jar in "${source_dir}"/*.jar; do
            name=$(basename "${jar}")
            case "${name}" in
                *-tests.jar|*-sources.jar|*-javadoc.jar) continue ;;
            esac
            if ! is_plugin_allowed "${name}"; then
                remove_disallowed_plugin "${name}"
                continue
            fi
            if [ ! -f "${DATA_DIR}/plugins/${name}" ] || ! cmp -s "${jar}" "${DATA_DIR}/plugins/${name}"; then
                cp "${jar}" "${DATA_DIR}/plugins/${name}"
                echo "[entrypoint] installed plugin ${name}"
            fi
        done
    fi
}

install_jars "${PACKAGED_PLUGINS_DIR}"
install_jars "${VOICE_PLUGIN_DIR}"
install_jars "${USER_PLUGINS_DIR}"

echo "[entrypoint] starting Folia (type=${SERVER_TYPE:-?} shard=${SHARD_ID:-?})"
exec java ${JAVA_OPTS} -jar "${SERVER_JAR}" --nogui
