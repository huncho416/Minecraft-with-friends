# mythic-cord

Rust workspace for the MythicPvP proxy stack.

| Crate | Purpose | Status |
|---|---|---|
| [`stdb/`](stdb/) | SpacetimeDB module — schema + reducers shared by proxy and game servers | ✅ |
| [`stdb-bridge/`](stdb-bridge/) | Driver thread + typed reducer client (Rust mirror of Java's `MythicSchema`) | ✅ |
| [`plugin-routing/`](plugin-routing/) | Lifecycle hooks, registry mirror, shard picker, heartbeat | ✅ |
| [`proxy/`](proxy/) | Binary entry point + admin HTTP + signal handling | ✅ standalone build |
| `infrarust/` | Vendored upstream snapshot (run `tools/vendor-infrarust.sh` to fetch) | ⏳ |
| [`pterodactyl/`](pterodactyl/) | Importable Pterodactyl egg JSON | ✅ |

## Architecture

```
   ┌──────────────────────────────────────────────────────────────┐
   │  mythic-cord workspace                                        │
   │                                                               │
   │   proxy   ──depends on──►  plugin-routing  ──►  stdb-bridge   │
   │     │                          │                    │         │
   │     │                   (feature: with-infrarust)   │         │
   │     │                          ▼                    │         │
   │     └─────────────────►  infrarust/* (vendored)     │         │
   │                                                     ▼         │
   │                                       ┌─────────────────────┐ │
   │                                       │ WebSocket driver    │ │
   │                                       │ task (single tokio  │ │
   │                                       │ task, bounded mpsc) │ │
   │                                       └──────────┬──────────┘ │
   └──────────────────────────────────────────────────┼────────────┘
                                                      │
                                                      ▼
                                            SpacetimeDB host
                                            (publishes stdb/ module)
```

## Two operating modes

The `proxy` binary has one `--features with-infrarust` cargo feature that toggles between:

| Mode | Default | What it does |
|---|---|---|
| Standalone (default) | ✅ | Announces, heartbeats, mirrors `server_registry`, serves admin HTTP. **No Minecraft traffic.** Useful for verifying STDB plumbing and ops integration before the upstream subtree is vendored |
| Full | needs `with-infrarust` | Wraps Infrarust's accept loop, installs the routing plugin, accepts Minecraft handshakes on 25565 |

This split means the workspace **builds today** even though Infrarust isn't vendored yet — and the full build is one `cargo build --features with-infrarust` away after the vendor script runs.

## Bootstrap

```sh
# 1. Build the stdb module + standalone proxy (works immediately):
cargo build --release -p mythic-stdb --target wasm32-unknown-unknown
cargo build --release -p mythiccord

# 2. Pull Infrarust and re-build with full integration:
./tools/vendor-infrarust.sh                # Linux/macOS
.\tools\vendor-infrarust.ps1               # Windows
cargo build --release --features with-infrarust -p mythiccord
```

## Crate details

### `stdb/`
SpacetimeDB module. WASM artifact built with `cargo build -p mythic-stdb --target wasm32-unknown-unknown`. Published to the running STDB by [`tools/docker/Dockerfile.stdb-publish`](../tools/docker/Dockerfile.stdb-publish) on every `docker compose up`. Schema and reducers are described in [`stdb/`](stdb/) and pinned by `SCHEMA_VERSION` constants on both sides.

### `stdb-bridge/`
The proxy's view of STDB. Single-task WebSocket driver that multiplexes reducer calls and table subscriptions through a clone-able `StdbHandle`. Pattern lifted from SpacerCord's `infrarust-spacetimedb` crate (driver thread + bounded mpsc + handle facade); reducer bindings rewritten against the `mythic-stdb` schema. Public API:

- `spawn_driver(DriverConfig) -> (StdbHandle, JoinHandle)`
- `MythicStdbClient::new(handle)` — typed wrapper, one method per reducer
- `assert_schema_version(handle)` — boot-time version check (mirror of Java's `SchemaVersion.assertMatches`)

### `plugin-routing/`
Lifecycle and routing logic, gated behind the `with-infrarust` feature.

- `router::pick_shard` — pure function, identical formula to `mythic_stdb::sessions::pick_shard`, unit-tested
- `registry_view::RegistryView` — local mirror of `server_registry`, kept current by a subscription task
- `heartbeat::run` — announce + 5s heartbeat task
- `integration::install` — registers `PreLoginEvent` / `DisconnectEvent` handlers on Infrarust's event bus

### `proxy/`
`mythiccord` binary. Signal-aware (SIGTERM/SIGINT → drain → 500ms grace → offline → exit), admin HTTP on `:8080` (`/health`, `/metrics`, `POST /admin/drain`), layered config (defaults < TOML file < env). Pterodactyl-egg-compatible env var names — see [`pterodactyl/egg-mythiccord.json`](pterodactyl/egg-mythiccord.json).

## Cherry-picked from SpacerCord

We **fork Infrarust directly**, not SpacerCord. SpacerCord is a reference for one pattern:

- Single-task WebSocket driver with a clone-able `Handle` facade — reimplemented in `stdb-bridge/src/{driver,handle}.rs`. Their `module_bindings/` are not copied; ours are typed against the actual `mythic-stdb` schema.

Nothing else from SpacerCord is inherited — its `stdb-module/` schema is a placeholder (`PlayerProfile` only) and would conflict with `mythic-stdb`.

## License

The proxy stack is **AGPL-3.0-or-later** with Infrarust's plugin exception preserved (see `LICENSE`). The Java suite under `mythic-suite/` is licensed separately at the repo root.

## Roadmap

- [x] Schema module (`stdb/`)
- [x] Java suite mirror (in `mythic-suite/suite-database/`)
- [x] STDB bridge (driver + typed client)
- [x] Routing plugin (registry mirror, shard picker, heartbeat)
- [x] Standalone proxy binary (admin HTTP, signal-driven drain)
- [x] Pterodactyl egg
- [ ] Vendor Infrarust subtree (`tools/vendor-infrarust.sh`)
- [ ] Hook integration tests against an in-process STDB
- [x] Geyser + SimpleVoice sidecar (Docker deployment)
