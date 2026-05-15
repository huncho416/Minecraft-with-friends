# MythicPvP Docker

One-command spin-up for the MythicPvP network: SpacetimeDB, MythicCord, Geyser, Folia servers, REST API, Prometheus, and Grafana.

## Layout

| File / dir | What it does |
|---|---|
| `docker-compose.yml` | Full network: STDB, proxy, Geyser, Hub, Skyblock #1, API, Prometheus, Grafana |
| `docker-compose.dev.yml` | Lightweight: STDB + single Hub, no proxy, no monitoring |
| `docker-compose.vps.yml` | VPS in-game testing stack with Pterodactyl-style bind-mounted server roots |
| `Dockerfile.folia` | Folia 1.21.1 image with suite jars plus Simple Voice Chat and SimpleVoice-Geyser resolved from Modrinth |
| `Dockerfile.geyser` | Geyser Standalone image downloaded from the official Geyser downloads API |
| `Dockerfile.mythiccord` | Rust proxy image in standalone registry/admin mode by default; enable `MYTHICCORD_FEATURES=with-infrarust` after vendoring Infrarust |
| `Dockerfile.api` | Ktor gateway image stub that returns `503 not_implemented` until `api-suite/` exists |
| `folia/` | Server config defaults, voice config, user plugin mount, and entrypoint |
| `geyser/` | Geyser Standalone config template plus `packs/` for Bedrock `.mcpack` files |
| `monitoring/` | Prometheus scrape config and Grafana datasource/dashboard provisioning |
| `scripts/` | `up.sh`/`up.ps1`, `down.sh`/`down.ps1`, and VPS helper scripts |

## Quick Start

Linux / macOS:

```sh
cd tools/docker
cp .env.example .env
./scripts/up.sh
./scripts/up.sh dev
```

Windows PowerShell:

```powershell
cd tools\docker
copy .env.example .env
.\scripts\up.ps1
.\scripts\up.ps1 dev
```

## VPS In-Game Testing

The VPS stack is designed for hands-on network testing without requiring a full Pterodactyl Panel/Wings install. Each game server has one container, explicit env-driven startup settings, and a bind-mounted server root under `tools/docker/servers/`, which mirrors how Pterodactyl exposes files for a server.

```sh
cd tools/docker
cp .env.example .env
mkdir -p servers/hub servers/skyblock-1 servers/geyser
bash ./scripts/vps-up.sh
bash ./scripts/vps-logs.sh hub
bash ./scripts/vps-restart.sh hub
bash ./scripts/vps-down.sh
```

Use `bash ./scripts/vps-up.sh --monitoring` to also start Prometheus and Grafana. The checklist for manual testing lives in [`INGAME_TEST_CHECKLIST.md`](INGAME_TEST_CHECKLIST.md).

The default player-facing VPS ports are:

| Port | Service | Notes |
|---|---|---|
| 25566 | Hub direct | Recommended Java test entry point |
| 25567 | Skyblock/test direct | Recommended Java test shard |
| 19132/udp | Geyser | Bedrock entry point |
| 24454/udp | Hub voice | Optional voice testing |
| 24455/udp | Skyblock voice | Optional voice testing |

Admin and database ports bind to `127.0.0.1` by default through `ADMIN_BIND`. Use SSH tunnels instead of exposing them publicly:

```sh
ssh -L 3000:127.0.0.1:3000 -L 8080:127.0.0.1:8080 -L 3001:127.0.0.1:3001 user@vps
```

Direct backend testing is the default because MythicCord traffic routing depends on the full Infrarust path. MythicCord still runs in the VPS stack for registry, health, drain, and metrics testing.

## Ports

| Port | Service | Notes |
|---|---|---|
| 25565 | MythicCord | Production Java entry point once the full Infrarust feature is enabled |
| 25566 | Hub direct | Java testing without the proxy |
| 25567 | Skyblock #1 direct | Java testing without the proxy |
| 19132/udp | Geyser | Bedrock entry point |
| 24454/udp | Hub voice | Simple Voice Chat UDP |
| 24455/udp | Skyblock #1 voice | Host mapping to backend `24454/udp` |
| 8090 | Hub voice web | SimpleVoice-Geyser web bridge |
| 8091 | Skyblock #1 voice web | Host mapping to backend `8090` |
| 3000 | SpacetimeDB HTTP | `http://localhost:3000/health` |
| 8080 | MythicCord control plane | `/health`, `/metrics`, `POST /admin/drain` |
| 8081 | REST API gateway | Stub until `api-suite/` lands |
| 9090 | Prometheus | `http://localhost:9090` |
| 3001 | Grafana | `http://localhost:3001` |

## Service Status

| Service | Status |
|---|---|
| SpacetimeDB | Real, using `clockworklabs/spacetime:latest` |
| Folia + suite jars | Real, boots 1.21.1 servers with suite plugins plus voice plugins |
| Geyser | Real, standalone container on UDP `19132`, pointed at Hub by default and switchable to MythicCord when the proxy accepts traffic |
| Simple Voice Chat | Real, Bukkit/Folia plugin resolved from Modrinth and seeded with proximity defaults |
| SimpleVoice-Geyser | Real, Bukkit plugin resolved from Modrinth and exposed on the web bridge port |
| Prometheus + Grafana | Real, datasource and starter dashboard auto-provisioned |
| MythicCord | Real standalone mode for registry/admin/metrics; Minecraft traffic requires the Infrarust vendored feature |
| REST API | Stub until `api-suite/` exists |

Until the full Infrarust feature is enabled, Java clients can connect directly to `localhost:25566` and Bedrock clients can connect to `localhost:19132`. Set `GEYSER_REMOTE_ADDRESS=mythiccord` after MythicCord is built with traffic support.

## Folia Config Knobs

| Env var | Effect |
|---|---|
| `SERVER_TYPE` | `hub`, `skyblock`, etc. Used in MOTD and suite logic |
| `SHARD_ID` | Unique identifier, written to `server-name=` |
| `VIEW_DISTANCE` | Written into `server.properties` |
| `ONLINE_MODE` | Default `false`; set `true` for direct-connect testing |
| `VOICE_HOST` | External hostname/IP advertised by Simple Voice Chat |
| `VOICE_PORT` | Backend voice UDP port, default `24454` |
| `GEYSER_REMOTE_ADDRESS` / `GEYSER_REMOTE_PORT` / `GEYSER_REMOTE_AUTH_TYPE` | Java target rendered into Geyser config at container start |
| `SENTRY_DSN` / `SENTRY_ENVIRONMENT` / `SENTRY_RELEASE` | Error tracking bootstrap inputs for Java plugins |
| `JAVA_OPTS` | Heap and GC flags |
| `STDB_URI` / `STDB_MODULE` | Forwarded to plugins through env |

User plugins can be dropped into [`folia/plugins/`](folia/plugins/) and are copied into each Folia container on first boot.

## Bedrock And Voice

Geyser Standalone renders [`geyser/config.template.yml`](geyser/config.template.yml) into `/data/config.yml` at container start. Bedrock resource packs belong in [`geyser/packs/`](geyser/packs/) as `.mcpack` files. The Java `ResourcePackManager` can run a configured conversion process through `CommandBedrockPackConverter`, while the Docker stack exposes the final pack directory to Geyser.

Simple Voice Chat and SimpleVoice-Geyser are resolved during the Folia image build from Modrinth for `FOLIA_VERSION`. Java voice uses UDP `24454`; the Bedrock web bridge is exposed on `8090` for Hub and `8091` for Skyblock #1.

## Modern Forwarding

`folia/paper-global.yml` has Velocity modern forwarding disabled with a placeholder secret. When MythicCord ships full traffic support:

1. Set `proxies.velocity.enabled: true` in `paper-global.yml`.
2. Set a real `secret` and pass the same one to the proxy.
3. Set `ONLINE_MODE=false` on Folia and `online-mode=true` on the proxy.
4. Firewall direct Folia ports so only the proxy can reach them.

## Build Pin

`Dockerfile.folia` defaults to the latest Folia 1.21.1 build at image-build time. Pin a build for reproducibility:

```sh
docker compose build --build-arg FOLIA_VERSION=1.21.1 --build-arg FOLIA_BUILD=42 hub skyblock-1
```

## Phase 2 Follow-Ups

- [ ] Replace `Dockerfile.api` with a real Ktor build once `api-suite/` exists
- [ ] Expose Folia metrics on `:9100` and tighten the Prometheus `folia` job
- [ ] Replace the placeholder Grafana dashboard with a real TPS, player count, and DB latency board
