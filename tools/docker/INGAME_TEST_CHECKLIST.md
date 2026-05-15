# MythicPvP In-Game Testing Checklist

Use this checklist on the VPS after starting the Pterodactyl-style Compose stack.

## Preflight

- [ ] Docker Engine and Docker Compose v2 are installed.
- [ ] `tools/docker/.env` exists and has the VPS IP or hostname set in `VOICE_HOST`.
- [ ] Public firewall allows Java test ports `25566/tcp` and `25567/tcp`.
- [ ] Public firewall allows Bedrock `19132/udp`.
- [ ] Public firewall allows voice UDP ports `24454/udp` and `24455/udp` if voice is being tested.
- [ ] Admin ports stay local-only or are protected by firewall rules: `3000`, `8080`, `8090`, `8091`, `9090`, `3001`.
- [ ] `docker compose -f tools/docker/docker-compose.vps.yml config` succeeds.
- [ ] `bash ./tools/docker/scripts/vps-up.sh` starts the stack.
- [ ] `docker compose -f tools/docker/docker-compose.vps.yml ps` shows SpacetimeDB, MythicCord, Geyser, Hub, and Skyblock running.
- [ ] STDB module publishing completes successfully in `stdb-publish` logs.
- [ ] Hub and Skyblock logs show no repeated startup exceptions.

## Connection

- [ ] Java client can join Hub at `<VPS_IP>:25566`.
- [ ] Java client can join Skyblock/test shard at `<VPS_IP>:25567`.
- [ ] Bedrock client can join Geyser at `<VPS_IP>:19132`.
- [ ] MythicCord health responds through an SSH tunnel or on the VPS: `curl http://127.0.0.1:8080/health`.
- [ ] MythicCord metrics respond: `curl http://127.0.0.1:8080/metrics`.
- [ ] SpacetimeDB health responds: `curl http://127.0.0.1:3000/health`.

## Core Server Checks

- [ ] Join, leave, and first-login messages render with the expected MythicPvP formatting.
- [ ] Tab list header/footer render correctly.
- [ ] Scoreboard title, labels, values, and player counts render correctly.
- [ ] Default rank prefix, chat color, tab prefix, and nametag color render correctly.
- [ ] Rank grant, rank display, and permission checks work.
- [ ] Command blocker rejects blocked commands with the expected message.
- [ ] Essentials commands work: spawn, teleport, message/reply, feed/heal, fly, vanish-related checks if enabled.
- [ ] Staff mode can be enabled and disabled without inventory loss.
- [ ] Staff tools perform their expected actions.
- [ ] Staff channels and staff notifications render only to permitted staff.
- [ ] Chat controls work: mute, clear, slow mode, broadcast, announcements.
- [ ] Punishment and appeal commands complete their expected flows.

## Phase 3 Systems

- [ ] Credit balance can be viewed.
- [ ] Credit shop purchase flow works and rejects insufficient credits cleanly.
- [ ] Cosmetic crate roll consumes the correct currency/item.
- [ ] Cosmetic crate roll awards an item according to configured rewards.
- [ ] Limited cosmetic rewards display and persist correctly.
- [ ] Cosmetic menu opens and lists owned cosmetics.
- [ ] Cosmetic equip flow applies the selected cosmetic.
- [ ] Cosmetic withdraw flow gives a tradable/sellable item when supported.
- [ ] Cosmetic redeem flow returns withdrawn cosmetics to the menu when supported.
- [ ] Cosmetic filters and sorting work when configured.
- [ ] Friends flow works: add, accept, remove, list.
- [ ] Party flow works: invite, accept, leave, disband.
- [ ] Mail flow works: send, receive, read, delete.
- [ ] Offline rewards are delivered after reconnect.
- [ ] Hub server selector opens and sends players to expected test targets or displays a safe fallback.

## Persistence And Restart

- [ ] Stop the stack with `./tools/docker/scripts/vps-down.sh`.
- [ ] Start it again with `bash ./tools/docker/scripts/vps-up.sh --no-build`.
- [ ] Player inventory and location state survive restart where expected.
- [ ] Credits, cosmetics, ranks, punishments, friends, parties, mail, and offline rewards persist.
- [ ] Hub and Skyblock bind-mounted folders contain logs, configs, plugins, worlds, and crash reports.
- [ ] Restarting one service with `bash ./tools/docker/scripts/vps-restart.sh hub` does not require restarting STDB.
- [ ] No database reconnect spam appears after restarting a backend server.

## Operations

- [ ] Follow all logs with `bash ./tools/docker/scripts/vps-logs.sh`.
- [ ] Follow one service with `bash ./tools/docker/scripts/vps-logs.sh hub`.
- [ ] Inspect server files under `tools/docker/servers/hub` and `tools/docker/servers/skyblock-1`.
- [ ] Update a config file in a server folder and restart only that service.
- [ ] Start monitoring with `bash ./tools/docker/scripts/vps-up.sh --monitoring`.
- [ ] Access Grafana through an SSH tunnel to `127.0.0.1:3001`.
- [ ] Confirm idle TPS/performance is acceptable for the VPS size.
- [ ] Confirm memory usage stays inside the configured Java heap sizes.

## SSH Tunnel Examples

Run these from your local machine, replacing `user@vps` with your VPS login:

```sh
ssh -L 3000:127.0.0.1:3000 -L 8080:127.0.0.1:8080 -L 3001:127.0.0.1:3001 user@vps
```

Then open:

- SpacetimeDB: `http://127.0.0.1:3000`
- MythicCord health: `http://127.0.0.1:8080/health`
- Grafana: `http://127.0.0.1:3001`

## Acceptance Notes

- [ ] No repeated stack traces during startup.
- [ ] No repeated STDB reconnect messages.
- [ ] No missing plugin dependency errors.
- [ ] No placeholder text appears in player-facing messages.
- [ ] Direct backend testing works even if MythicCord traffic routing is not ready yet.
