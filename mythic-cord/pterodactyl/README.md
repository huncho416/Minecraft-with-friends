# MythicCord — Pterodactyl

This directory holds Pterodactyl integration artifacts for managing MythicCord proxy instances on a self-hosted Pterodactyl panel.

## Files

| File | Purpose |
|---|---|
| [`egg-mythiccord.json`](egg-mythiccord.json) | Importable egg. Defines install script, startup command, env variables, and Docker image |

## Importing the egg

1. Pterodactyl Admin → **Nests** → pick a nest (or create one called "MythicPvP") → **Import Egg**
2. Upload `egg-mythiccord.json`
3. Create a server using the new egg

The default install script downloads the latest release from `github.com/mythicpvp/mythic-cord/releases` (musl binary, x86_64 or aarch64) and writes a starter `mythiccord.toml` if one doesn't already exist.

## Env-driven configuration

The egg defines one variable per knob in [`proxy/src/config.rs`](../proxy/src/config.rs). Env always wins over the TOML file, so you can leave `mythiccord.toml` checked in and override per-server in the panel UI.

## Graceful shutdown

Pterodactyl's stop signal is `^C` (SIGINT) by default. The proxy responds to both SIGINT and SIGTERM:

1. Flips status → `Draining` (stops accepting new logins, tells STDB)
2. Waits ~500ms for in-flight reducer calls to flush
3. Sends a final heartbeat with status=`Offline`
4. Exits 0

Pterodactyl's stop-grace is 30 seconds by default — that's plenty for a clean drain.

## Manual drain (e.g. before a deploy)

The admin HTTP surface accepts:

```
POST http://<container>:8080/admin/drain
```

Useful for rolling deploys: drain proxy A, wait for sessions to bleed out, then issue the actual stop.

## Health & metrics

| Endpoint | What it returns |
|---|---|
| `GET /health`  | JSON: `{status, shard, role, registry_known_shards}`. 200 if healthy/draining/degraded; 503 if offline |
| `GET /metrics` | Prometheus exposition: `mythiccord_status` and `mythiccord_registry_shards` |

Wire the metrics endpoint into the Prometheus job already defined in [`tools/docker/monitoring/prometheus.yml`](../../tools/docker/monitoring/prometheus.yml) — the `mythiccord` job there points at `mythiccord:8080/metrics`.
