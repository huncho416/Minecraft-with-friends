#!/usr/bin/env bash
# Vendor Infrarust into mythic-cord/infrarust/ as a clean snapshot.
# Records the upstream commit in VENDOR.md so future re-baselines are
# a one-command operation.
#
# Usage:
#   ./tools/vendor-infrarust.sh                       # default: v2.0.0-alpha.6
#   ./tools/vendor-infrarust.sh v2.0.0-alpha.7        # specific tag
#   ./tools/vendor-infrarust.sh main                  # branch tip (not recommended)
#
# Idempotent: re-runs delete the old snapshot and re-extract.
set -euo pipefail

REF="${1:-v2.0.0-alpha.6}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TARGET="$ROOT/infrarust"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

echo "[vendor] target ref: $REF"

if ! command -v git >/dev/null; then
    echo "git is required" >&2
    exit 1
fi

git clone --depth 1 --branch "$REF" \
    https://github.com/Shadowner/Infrarust.git "$TMP/Infrarust"

UPSTREAM_SHA=$(cd "$TMP/Infrarust" && git rev-parse HEAD)
UPSTREAM_DATE=$(cd "$TMP/Infrarust" && git log -1 --format=%ci HEAD)

# Wipe and re-extract — vendoring is intentionally destructive.
if [ -d "$TARGET" ]; then
    echo "[vendor] removing existing $TARGET"
    rm -rf "$TARGET"
fi

# Copy everything except the upstream .git/.github so this stays a clean
# snapshot, not a sub-repo.
mkdir -p "$TARGET"
( cd "$TMP/Infrarust" && \
    tar --exclude='.git' --exclude='.github' -cf - . ) | \
    ( cd "$TARGET" && tar -xf - )

cat > "$ROOT/VENDOR.md" <<EOF
# Vendored upstream — Infrarust

This snapshot was extracted from https://github.com/Shadowner/Infrarust by
\`mythic-cord/tools/vendor-infrarust.sh\`. Don't edit files under
\`infrarust/\` by hand; instead add MythicCord-specific code in
\`mythic-cord/proxy/\`, \`mythic-cord/plugin-routing/\`, or
\`mythic-cord/stdb-bridge/\` — those crates depend on the vendored API
and survive re-baselines untouched.

## Current pin

| Key | Value |
|---|---|
| Ref      | \`$REF\` |
| Commit   | \`$UPSTREAM_SHA\` |
| Date     | $UPSTREAM_DATE |

## Re-baselining

\`\`\`sh
./tools/vendor-infrarust.sh v2.0.0-alpha.7   # or whatever ref
\`\`\`

After re-vendoring, run:

\`\`\`sh
cargo build --workspace --features with-infrarust
\`\`\`

If anything in \`plugin-routing/integration.rs\` fails to compile, the
upstream API moved — fix the integration file and bump
\`mythiccord-plugin-routing\` so consumers know.

## Why a vendored snapshot vs a git submodule

- Single \`cargo build\` works on a fresh clone with no extra steps
- Pterodactyl/CI builds don't need submodule init
- Re-baseline is \`tools/vendor-infrarust.sh <ref>\`, history of pins
  shows up in this file's commit history

## License

Infrarust is AGPL-3.0 with a plugin exception. \`mythic-cord/LICENSE\`
preserves both terms verbatim. Closed-source plugins remain permitted.
EOF

# Un-comment the vendor-managed dep blocks in dependent crates. Cargo
# resolves `path = ...` deps even when optional, so we keep them
# commented until the subtree exists.
uncomment_block() {
    local file="$1"
    # Strip the leading `# ` from lines between the BEGIN/END markers.
    awk '
        />>> BEGIN VENDOR-MANAGED BLOCK/ { in_block=1; print; next }
        />>> END VENDOR-MANAGED BLOCK/   { in_block=0; print; next }
        in_block && /^# / { sub(/^# /, ""); print; next }
        { print }
    ' "$file" > "$file.new" && mv "$file.new" "$file"
}
uncomment_block "$ROOT/plugin-routing/Cargo.toml"
uncomment_block "$ROOT/proxy/Cargo.toml"

# Also flip the `with-infrarust` feature in plugin-routing to actually
# pull in the deps now that they exist.
sed -i 's|^with-infrarust = \[\]$|with-infrarust = ["dep:infrarust-api", "dep:infrarust-core"]|' \
    "$ROOT/plugin-routing/Cargo.toml"
sed -i 's|^    "mythiccord-plugin-routing/with-infrarust",$|    "dep:infrarust",\n    "mythiccord-plugin-routing/with-infrarust",|' \
    "$ROOT/proxy/Cargo.toml"

echo "[vendor] wrote $ROOT/VENDOR.md"
echo "[vendor] un-commented vendor-managed blocks in plugin-routing/proxy"
echo "[vendor] done — $TARGET"
echo "[vendor] next: cargo build --workspace --features with-infrarust"
