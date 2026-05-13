# MythicPvP — Docker

One-command spin-up for the entire MythicPvP network: SpacetimeDB,
MythicCord proxy, Folia game servers, REST API, Prometheus + Grafana.

## Layout

| File / dir | What it does |
|---|---|
| `docker-compose.yml` | Full network: STDB, proxy, Hub, Skyblock #1, API, Prometheus, Grafana |
| `docker-compose.dev.yml` | Lightweight: STDB + single Hub, no proxy, no monitoring |
| `Dockerfile.folia` | Folia 1.21.1 image. Two-stage: Maven builds `mythic-suite`, runtime is JRE 21. Suite jars baked in at `/opt/folia/suite/` and copied into `/data/plugins/` on first boot |
| `Dockerfile.mythiccord` | Rust proxy image. **Currently a stub** that forwards 25565→Hub via socat; replace builder stage when the SpacerCord fork lands under `mythic-cord/` |
| `Dockerfile.api` | Ktor gateway image. **Currently a stub** that returns `503 not_implemented`; replace when `api-suite/` exists |
| `folia/` | Server config defaults (eula, server.properties, bukkit.yml, spigot.yml, paper-global.yml) and entrypoint |
| `monitoring/` | Prometheus scrape config + Grafana datasource/dashboard provisioning |
| `scripts/` | `up.sh`/`up.ps1`, `down.sh`/`down.ps1` |

## Quick start

**Linux / macOS:**
```sh
cd tools/docker
cp .env.example .env       # optional — edit Grafana password etc.
./scripts/up.sh            # full network
# or
./scripts/up.sh dev        # STDB + single Hub
```

**Windows (PowerShell):**
```powershell
cd tools\docker
copy .env.example .env
.\scripts\up.ps1
# or
.\scripts\up.ps1 dev
```

## Ports

| Port | Service | Notes |
|---|---|---|
| 25565 | MythicCord (proxy) | Production entry point |
| 25566 | Hub (direct) | For testing without the proxy |
| 25567 | Skyblock #1 (direct) | For testing without the proxy |
| 3000  | SpacetimeDB HTTP | `wget http://localhost:3000/health` |
| 8080  | MythicCord control plane / metrics | Stub returns 200 until proxy lands |
| 8081  | REST API gateway | Stub returns 503 until `api-suite/` lands |
| 9090  | Prometheus | http://localhost:9090 |
| 3001  | Grafana | http://localhost:3001 (admin / $GRAFANA_PASSWORD) |

## What's stubbed vs. real

| Service | Status |
|---|---|
| SpacetimeDB | **Real** — `clockworklabs/spacetime:latest` |
| Folia + suite jars | **Real** — boots a 1.21.1 server with all 24 suite plugins mounted |
| Prometheus + Grafana | **Real** — datasource and starter dashboard auto-provisioned |
| MythicCord | **Stub** — forwards proxy port to Hub directly. Replace `Dockerfile.mythiccord` builder stage when the Rust crate exists |
| REST API | **Stub** — returns `503 not_implemented` |

Until the proxy is real, connect Minecraft clients directly to `localhost:25566` (Hub) to bypass the stub.

## Folia config knobs

Set on each Folia service in compose. Defaults in [`folia/server.properties`](folia/server.properties) are seeded into `/data/` on first boot, then overridden per env var:

| Env var | Effect |
|---|---|
| `SERVER_TYPE` | `hub`, `skyblock`, etc. Used in MOTD and (eventually) suite logic |
| `SHARD_ID` | Unique identifier, written to `server-name=` |
| `VIEW_DISTANCE` | Written into `server.properties` |
| `ONLINE_MODE` | Default `false` (proxy handles auth); set `true` for direct-connect testing |
| `JAVA_OPTS` | Heap and GC flags |
| `STDB_URI` / `STDB_MODULE` | Forwarded to plugins via env |

User plugins can be dropped into [`folia/plugins/`](folia/plugins/) — that directory is mounted read-only into every Folia container and takes precedence over the suite jars.

## Modern forwarding (Velocity / MythicCord)

`folia/paper-global.yml` has Velocity modern forwarding disabled with a placeholder secret. When MythicCord ships modern-forwarding support:

1. Set `proxies.velocity.enabled: true` in `paper-global.yml`
2. Set a real `secret` (and pass the same one to the proxy via env)
3. Set `ONLINE_MODE=false` on Folia and `online-mode=true` on the proxy
4. Firewall the direct Folia ports (25566, 25567) so only the proxy can reach them

## Build pin

`Dockerfile.folia` defaults to the latest Folia 1.21.1 build at image-build time. Pin a specific build for reproducibility:

```sh
docker compose build \
  --build-arg FOLIA_VERSION=1.21.1 \
  --build-arg FOLIA_BUILD=42 \
  hub skyblock-1
```

## Phase 2 follow-ups

- [ ] Replace `Dockerfile.mythiccord` builder stage with the real Rust crate once SpacerCord fork lands
- [ ] Replace `Dockerfile.api` with a real Ktor build once `api-suite/` exists
- [ ] Add Geyser sidecar / Geyser-Standalone container once integration approach is chosen
- [ ] Add SimpleVoice-Geyser config + UDP port mapping
- [ ] Expose Folia metrics on `:9100` (prometheus-exporter plugin or suite-side `/metrics`) and tighten the Prometheus `folia` job
- [ ] Add Sentry DSN env wiring once the Sentry SDK lands in the suite
- [ ] Replace the placeholder Grafana dashboard with a real TPS / player-count / DB-latency board
