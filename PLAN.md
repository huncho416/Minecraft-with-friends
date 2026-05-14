# 🏗️ MythicPvP — Grand Master Plan (v2)

> **Server Name:** &#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP
> **Colors:** Brand gradient pink-to-white/grey (`#F529BE`, `#FD37F0`, `#F639EA`, `#DD35C4`, `#F63DF1`, `#EA21FF`, `#FFFFFF`, `#D2D8E0`, `#DDDBD9`) · Success `#9CFF9C` · Failure `#FF8A8A`
> **Version:** 1.21.11 (accepts clients 1.21.x+) · **Server:** Folia · **Proxy:** MythicCord (SpacerCord fork)
> **Language:** Java 21 (server plugins), Rust (proxy) · **Build:** Maven
> **Database:** SpacetimeDB (sole database — no Redis) · **Bedrock:** Geyser + Voice Chat
> **Team:** 2 developers · **Hosting:** Multi-region bare metal/VPS
> **Anti-Cheat:** Custom solution (owner-provided, not in scope of this plan)

---

## 📐 High-Level Architecture

```mermaid
graph TB
    subgraph Internet
        Java["☕ Java Players"]
        Bedrock["📱 Bedrock Players via Geyser"]
        Website["🌐 Website"]
    end

    subgraph "DDoS Protection"
        TCP["TCPShield / Cloudflare Spectrum"]
    end

    subgraph "MythicCord (SpacerCord Fork — Rust)"
        Proxy["MythicCord Proxy"]
        STDB_Driver["SpacetimeDB Async Driver"]
        Geyser["Geyser Integration"]
        VoiceChat["SimpleVoice-Geyser"]
    end

    subgraph "Game Servers (Java/Folia)"
        Hub["Hub/Lobby"]
        SB1["Skyblock Shard #1"]
        SB2["Skyblock Shard #2"]
        SBN["Skyblock Shard #N"]
    end

    subgraph "Data Layer"
        STDB["SpacetimeDB (self-hosted)"]
    end

    subgraph "Observability"
        OTel["OpenTelemetry"]
        Grafana["Prometheus + Grafana"]
        Sentry["Sentry Error Tracking"]
    end

    subgraph "API Layer"
        REST["REST API Gateway (Ktor)"]
    end

    Java --> TCP --> Proxy
    Bedrock --> TCP --> Geyser --> Proxy
    Proxy --> Hub & SB1 & SB2 & SBN
    Proxy --> STDB_Driver --> STDB
    Hub & SB1 & SB2 & SBN --> STDB
    Website --> REST --> STDB
    VoiceChat --> Hub & SB1
    Proxy --> OTel --> Grafana
    Hub & SB1 --> Sentry
```

---

## 🎨 Branding & Hex System

Gradient identity: `&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP`

The **HexAPI** parses `&#RRGGBB` tags across all text surfaces. The custom **MythicPvP font** (via resource pack) is used in scoreboards, tab, menus, and nametags for a premium branded look.

---

## 🔧 Tech Stack

| Layer | Technology | Role |
|-------|-----------|------|
| **DDoS** | TCPShield / Cloudflare Spectrum | L4 proxy protection |
| **Proxy** | MythicCord (SpacerCord/Infrarust fork, Rust) | Routing, SpacetimeDB, Geyser |
| **Game Server** | Folia 1.21.11 (Java 21, accepts 1.21.x+ clients) | Regionized multithreaded MC |
| **Server Plugins** | Java 21 + Maven multi-module | All gameplay logic |
| **Database** | SpacetimeDB (self-hosted, sole DB) | Persistence, real-time sync, pub/sub |
| **Bedrock** | Geyser (integrated in proxy) | Bedrock→Java translation |
| **Voice** | SimpleVoice-Geyser | Proximity voice chat |
| **Resource Pack** | MythicPvP custom (`mythic` namespace) | Custom textures, models, fonts |
| **Web API** | Ktor | REST gateway with JWT + rate limiting |
| **Website** | Next.js + SpacetimeDB TS SDK | Real-time frontend |
| **CI/CD** | GitHub Actions | Build → test → package → deploy |
| **Monitoring** | OpenTelemetry + Prometheus + Grafana | Metrics, traces, dashboards |
| **Errors** | Sentry | Runtime error tracking |
| **Containers** | Docker Compose | One-command network spin-up |

---

## 📦 Repository Structure

```
mythicpvp/
├── pom.xml                              # Parent Maven POM
│
├── mythic-suite/                        # ══ THE FOUNDATION SUITE (23 modules) ══
│   ├── suite-api/                       # Core interfaces & contracts
│   ├── suite-packet/                    # Internal packet abstraction
│   ├── suite-hex/                       # HexAPI — hex color parsing
│   ├── suite-command/                   # CommandAPI — Aikar ACF-style + command blocker
│   ├── suite-tab/                       # TabAPI — per-player tab list
│   ├── suite-scoreboard/               # ScoreboardAPI — per-player boards
│   ├── suite-nametag/                   # NametagAPI — rank prefixes, hex
│   ├── suite-menu/                      # MenuAPI — chest GUI builder
│   ├── suite-hologram/                  # HologramAPI — floating displays
│   ├── suite-skin/                      # SkinAPI — skin fetching/caching
│   ├── suite-config/                    # ConfigAPI — YAML hot-reload + all text/message surfaces
│   ├── suite-database/                  # DatabaseAPI — SpacetimeDB Java client
│   ├── suite-protocol/                  # ProtocolAPI — cross-server messaging
│   ├── suite-scheduler/                 # SchedulerAPI — Folia-safe scheduling
│   ├── suite-economy/                   # EconomyAPI — multi-currency
│   ├── suite-permission/               # PermissionAPI — rank-based perms
│   ├── suite-item/                      # ItemAPI — item builder with hex lore
│   ├── suite-cooldown/                  # CooldownAPI — universal cooldowns
│   ├── suite-event/                     # EventAPI — custom event bus
│   ├── suite-chat/                      # ChatAPI — channels, filtering, hex
│   ├── suite-format/                    # FormatAPI — money/time/duration formatting
│   ├── suite-resourcepack/             # ResourcePackAPI — custom textures/fonts
│   ├── suite-cosmetic/                  # CosmeticAPI — hats, titles, particles
│   └── suite-disguise/                  # DisguiseAPI — skin/name/tab spoofing
│
├── mythic-core/                         # Core plugin (friends, party, mail, punish)
├── mythic-hub/                          # Hub/Lobby plugin
├── mythic-skyblock/                     # Skyblock gamemode
│   ├── skyblock-api/
│   ├── skyblock-islands/
│   ├── skyblock-pvp/
│   ├── skyblock-skills/
│   ├── skyblock-economy/
│   └── skyblock-events/
│
├── mythic-cord/                         # MythicCord (SpacerCord fork, Rust)
├── api-suite/                           # External REST APIs (Ktor)
├── web/                                 # Next.js website
├── TexturePack/                         # Resource pack (SMP Deluxe base + custom)
│
├── tools/
│   ├── docker/
│   │   ├── docker-compose.yml           # Full network: proxy + shards + STDB
│   │   ├── docker-compose.dev.yml       # Dev: single shard + STDB
│   │   ├── Dockerfile.folia             # Folia server image
│   │   ├── Dockerfile.mythiccord        # MythicCord proxy image
│   │   └── Dockerfile.api              # API gateway image
│   ├── monitoring/                      # Prometheus + Grafana configs
│   └── scripts/                         # Deploy automation
│
└── docs/
```

---

## 🏛️ THE MYTHIC SUITE — Foundation (23 Modules)

> **The suite must be complete before any gameplay code.** Every future gamemode builds on these APIs.

### Modules 1–23

| # | Module | Purpose | Status |
|---|--------|---------|--------|
| 1 | `suite-api` | Core interfaces, ServiceRegistry, Currency, MythicPlayer | ✅ Done |
| 2 | `suite-packet` | Internal packet action/session abstraction for display and disguise modules | ✅ Done |
| 3 | `suite-hex` | `&#RRGGBB` parsing, gradients, MiniMessage integration, custom fonts | ✅ Done |
| 4 | `suite-command` | Aikar-style annotations, permission-hidden tab-complete, command blocker config | ✅ Done |
| 5 | `suite-tab` | Per-player tab with hex, rank sorting, custom font support | ✅ Done |
| 6 | `suite-scoreboard` | Packet-based per-player boards, animated titles, custom font | ✅ Done |
| 7 | `suite-nametag` | Packet-level nametags with hex prefix/suffix, glow colors | ✅ Done |
| 8 | `suite-menu` | Chest GUI builder, pagination, click cooldowns | ✅ Done |
| 9 | `suite-hologram` | Packet holograms, per-player, animated, leaderboard type | ✅ Done |
| 10 | `suite-skin` | Mojang fetch + cache, NPC skins, head textures | ✅ Done |
| 11 | `suite-config` | YAML config wrapper, hot-reload, multi-file manager, configurable text surfaces | ✅ Done |
| 12 | `suite-format` | Money shorthand (1K/1M/1.5B/1T), duration, time ago, date/time | ✅ Done |
| 13 | `suite-database` | SpacetimeDB Java WS client, subscriptions, reducers | ✅ Done |
| 14 | `suite-protocol` | Cross-server messaging via pub/sub | ✅ Done |
| 15 | `suite-scheduler` | Folia RegionScheduler/EntityScheduler abstraction | ✅ Done |
| 16 | `suite-economy` | Coins/Points/Gems, multi-currency management | ✅ Done |
| 17 | `suite-permission` | Ranks with hex colors, inheritance, wildcards | ✅ Done |
| 18 | `suite-item` | Fluent builder, hex lore, custom model data, PDC helpers | ✅ Done |
| 19 | `suite-cooldown` | Named cooldowns, per-player, thread-safe | ✅ Done |
| 20 | `suite-event` | Custom event bus, priorities, cancellation | ✅ Done |
| 21 | `suite-chat` | Channels, hex formatting, spam/ad/toxicity filter | ✅ Done |
| 22 | `suite-resourcepack` | Custom model/font registry, pack URL management | ✅ Done |
| 23 | `suite-cosmetic` | Hats, titles, particles, ownership/equip system | ✅ Done |
| — | `suite-disguise` | Skin/name/rank override state management and integrations | ✅ Done |

### ConfigAPI Text Contract

Every player-facing message and display surface must be configurable through YAML. This includes chat formats, chat prefixes, scoreboard titles and lines, tab headers/footers, nametags, hologram lines, menu labels, resource-pack prompts, cooldown/economy/permission feedback, command responses, disguise text, and any future gameplay messages. Modules should resolve text through `ConfigText` or a module-specific YAML wrapper and provide sensible defaults that are written when keys are missing.

### Command Visibility Contract

Players must only be able to run, see, or tab-complete commands they have permission to use. `suite-command` filters registered command aliases, subcommands, root `/` tab-completion, and command list packets by permission before the client can discover them. It also owns `command-blocker.yml`, which can explicitly block or permission-gate commands such as `/pl`, `/plugins`, `/bukkit:pl`, `/?`, `/bukkit:?`, `/help`, and version aliases. Blocked commands return the configurable blocked message and are removed from tab-complete unless the sender has the configured command permission or bypass permission.

### NEW Module 19: `suite-format` — FormatAPI

Universal number/time/date formatting used by economy, scoreboards, chat, and all display modules.

- **Money shorthand:** `1000` → `1K`, `1500000` → `1.5M`, `1000000000` → `1B`, `1900000000000` → `1.9T`
- **Full commas:** `1,500,000` for detailed views
- **Duration:** `3d 2h 15m 30s`, compact `02:15:30`, words `3 days 2 hours`
- **Time ago:** `5m ago`, `3h ago`, `2d ago`
- **Date/Time:** `MM/dd/yyyy`, `MM/dd/yyyy HH:mm`, `HH:mm:ss`
- **Time parsing:** `"30m"` → 1800000ms, `"2d"` → 172800000ms
- **Percent:** `0.75` → `75%`
  ```java
  MythicFormat.money(1500000);    // "$1.5M"
  MythicFormat.number(2500000000L); // "2.5B"
  MythicFormat.duration(90061000); // "1d 1h 1m 1s"
  MythicFormat.timeAgo(timestamp); // "3h ago"
  MythicFormat.parseTime("30m");   // 1800000
  ```

### NEW Module 20: `suite-resourcepack` — ResourcePackAPI

Manages the custom texture pack (`TexturePack/` with the `mythic` namespace).

- **Pack serving:** Host pack via HTTP, auto-send to players on join
- **Bedrock conversion:** Auto-convert Java pack for Geyser/Bedrock clients
- **Custom model data registry:** Map items to custom textures programmatically
  ```java
  ResourcePack.register("mythic_sword", Material.DIAMOND_SWORD, 10001);
  // ItemAPI integration: MythicItem.create(Material.DIAMOND_SWORD).model("mythic_sword")
  ```
- **Custom font registry:** Register bitmap/TTF fonts for scoreboard, tab, menus
  ```java
  MythicFont.register("mythic_title", "mythic:font/title");
  // Use in HexAPI: MythicHex.font("mythic_title", "&#FF00F8MythicPvP")
  ```
- **Pack versioning:** Hash-based, force re-download on update
- **Hot-swap textures:** Update textures without server restart (re-send pack)

### NEW Module 20: `suite-cosmetic` — CosmeticAPI

Cosmetic system powered by resource pack custom models. **EULA compliant** — cosmetic only, no gameplay advantage.

- **Cosmetic types:**
  - **Hats** — Custom head models via resource pack (equipped via helmet slot with custom model data)
  - **Titles** — Text tags above/below nametag (e.g., "⚔ Champion", "🌟 Mythic")
  - **Particles** — Persistent particle trails (walk, idle, attack)
  - **Kill Effects** — Visual/sound on player kill
  - **Win Effects** — Visual on KOTH/event win
  - **Chat Tags** — Cosmetic prefixes/icons in chat
- **Unlock system:** Achievement rewards, event prizes, rank bundles, crates, lootboxes, and credit purchases
- **Equip GUI:** `/cosmetics` menu via MenuAPI with filters for type, rarity, limited status, owned/unowned, equipped state, and tradable state
- **Redeem / itemize flow:** Players can redeem cosmetics into the account-bound cosmetics menu, equip them from that menu, or withdraw eligible cosmetics as physical items for trading or selling.
- **Crate rewards:** Cosmetic crates are network-wide `mythic-core` content. Each crate defines weighted item pools with per-item roll percentages and optional limited-item windows.
- **Preview system:** Try-before-buy particle/hat preview
- **SpacetimeDB persistence:** Owned cosmetics + equipped loadout per player

### NEW Module 21: `suite-disguise` — DisguiseAPI

Full player disguise system. Uses the **same internal packet layer** as `suite-nametag` and `suite-tab` — **no external PacketEvents dependency required**. Our suite builds a lightweight NMS packet abstraction internally.

- **Disguise capabilities:**
  - Change displayed skin (GameProfile spoofing via PlayerInfo packets)
  - Change displayed username (nametag + tab + chat)
  - Change rank appearance (prefix/suffix override)
  - Full "nick" mode: random name + default skin
- **Implementation:** Intercepts outgoing `ClientboundPlayerInfoUpdatePacket` and `ClientboundAddEntityPacket` via our internal packet layer. Respawns the player entity for observers to apply skin changes.
- **Staff features:**
  - `/disguise <name>` — disguise as specific player
  - `/disguise random` — random nick
  - `/undisguise` — revert
  - Staff can see through disguises (visible in tab hover)
- **Integration:** Works with NametagAPI, TabAPI, ChatAPI — all respect disguise state
- **Folia-safe:** All packet operations scheduled on the correct region thread

---

## 🔒 Security & Proxy Hardening

### Network Protection
- **TCPShield** in front of MythicCord for L4 DDoS mitigation
- **MythicCord Rust-native rate limiting:** Login throttle, connection flood protection
- **Bot filtering:** Handshake validation, join velocity checks
- **VPN/proxy detection:** IP intelligence API integration in MythicCord

### Backend Protection
- All Folia backends run `online-mode=false`
- MythicCord forwards player UUID + IP (Velocity-style `modern` forwarding)
- Backend plugin validates forwarded identity via shared secret
- Direct backend connections rejected (firewall + secret check)

### API Security
- JWT tokens for website sessions (short-lived + refresh tokens)
- API key management for external consumers
- Rate limiting per endpoint (token bucket)
- CORS whitelist for web origins
- Secrets managed via environment variables (never in config files)

### Store, Credits & EULA Compliance
- **Store provider:** Tebex is the external checkout surface for purchasing account credits.
- **Credits:** Credits are the network currency players spend on eligible store/catalog items such as ranks, lootboxes, crate rolls, and cosmetic unlock opportunities.
- **Cosmetic crates:** Cosmetic crates can be opened on any backend server because the cosmetic system belongs to `mythic-core`, not `mythic-hub`.
- **Compliance guardrail:** Real-money and credit-purchased rewards must not create pay-to-win gameplay advantages. Any rank, crate, lootbox, or cosmetic reward that touches gameplay must be reviewed before implementation.
- **Odds transparency:** Lootboxes/crates must expose item pools, per-item roll chances, and limited-item availability in the UI/config before purchase or roll.

---

## 💾 Reliability, Backups & Disaster Recovery

### SpacetimeDB Backups
- **Automated snapshots:** Every 6 hours via cron + SpacetimeDB CLI export
- **Continuous commit log:** SpacetimeDB's built-in WAL provides point-in-time recovery
- **Off-site backup:** Snapshots synced to separate storage (S3-compatible)
- **Failover testing:** Monthly DR drill — restore from backup to staging

### Island Backups
- **Auto-snapshot:** Island schematic saved on every significant modification
- **Versioned history:** Last 5 snapshots retained per island in SpacetimeDB blobs
- **Player-triggered:** `/is backup` (cooldown-gated) for manual save

### Economy Rollback
- **Transaction log:** Every economy mutation logged with timestamp, source, amount
- **Rollback reducer:** SpacetimeDB reducer to revert transactions by time range or player
- **Dupe detection:** Anomaly detection on balance changes (flag >2σ deviations)

### Monitoring & Alerting
- **MythicCord:** OpenTelemetry traces + metrics (built into Infrarust)
- **Folia servers:** TPS, region count, entity count → `server_registry` SpacetimeDB table
- **Dashboards:** Grafana with panels for player count, TPS per shard, DB latency, error rates
- **Alerts:** Prometheus alerting → Discord webhook for TPS < 18, shard disconnect, DB latency > 50ms
- **Error tracking:** Sentry SDK in all Java plugins for runtime exception capture

---

## ⚡ Performance & Scaling

### Multi-Region Hosting
- **Proxy layer:** MythicCord instances per region (US, EU) behind geo-DNS
- **Game servers:** Shards co-located with proxy in each region
- **SpacetimeDB:** Primary instance in main region, read replicas if needed
- **Player routing:** MythicCord routes to lowest-latency shard matching player's region

### Shard Assignment
- MythicCord `player_routing` reducer considers: shard load, player's island location, party members, region proximity
- **Fallback:** If target shard is full/unhealthy, route to next-best shard
- **Health checks:** `server_registry` table with heartbeat, TPS, player count per shard

### Folia Optimization
- Pre-generate all worlds (void skyblock + PvP zone terrain)
- Aggressive entity limits per island region
- View distance tuning per shard type (Hub: 8, Skyblock: 6, PvP: 10)
- Packet compression enabled, chunk batching
- Suite scheduler abstraction tested heavily against Folia edge cases

### Load Testing
- Early load tests in Phase 1 (SpacetimeDB throughput) and Phase 2 (proxy connections)
- Bot framework for simulating 500+ concurrent players
- Profiling SpacetimeDB memory usage under sustained load

---

## 🐳 Docker Compose — One-Command Network

`tools/docker/docker-compose.yml` spins up the entire network:

```yaml
# Simplified structure — full version in repo
services:
  spacetimedb:
    image: clockworklabs/spacetimedb
    ports: ["3000:3000"]
    volumes: [stdb-data:/stdb]

  mythiccord:
    build: ./Dockerfile.mythiccord
    ports: ["25565:25565", "8080:8080"]
    depends_on: [spacetimedb]
    environment:
      STDB_URI: "http://spacetimedb:3000"

  hub:
    build: ./Dockerfile.folia
    environment:
      SERVER_TYPE: hub
      STDB_URI: "http://spacetimedb:3000"
    depends_on: [spacetimedb, mythiccord]

  skyblock-1:
    build: ./Dockerfile.folia
    environment:
      SERVER_TYPE: skyblock
      SHARD_ID: "1"
      STDB_URI: "http://spacetimedb:3000"
    depends_on: [spacetimedb, mythiccord]

  prometheus:
    image: prom/prometheus
    volumes: [./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml]

  grafana:
    image: grafana/grafana
    ports: ["3001:3000"]
    depends_on: [prometheus]

volumes:
  stdb-data:
```

`docker-compose.dev.yml` — lightweight dev variant (single shard, no monitoring).

---

## 👥 Social Features (in `mythic-core`)

### Friends System
- `/friend add/remove/list` — cross-shard via SpacetimeDB
- Online notifications on login
- Jump-to-friend server (`/friend tp <name>`)

### Party System
- `/party create/invite/disband/chat`
- Co-transfer: entire party moves to same shard
- Party-wide island access

### Mail & Offline Rewards
- `/mail send <player> <message>` — delivered on next login
- Offline reward accumulation (daily login streaks, idle crop growth)

---

## 🗄️ SpacetimeDB Schema

```mermaid
erDiagram
    PLAYERS {
        string uuid PK
        string username
        string rank
        long coins
        long points
        long gems
        string current_server
        string region
        timestamp first_join
        timestamp last_seen
    }
    ISLANDS {
        string island_id PK
        string owner_uuid FK
        int level
        long total_points
        string shard_id
        int size_tier
    }
    SKILLS {
        string player_uuid FK
        string skill_type
        long xp
        int level
    }
    STATS {
        string player_uuid FK
        string stat_type
        long value_daily
        long value_weekly
        long value_alltime
    }
    LEADERBOARDS {
        string board_type
        string timeframe
        string player_uuid FK
        long score
        int rank
    }
    MESSAGES {
        string channel
        string sender_uuid
        string content
        timestamp sent_at
    }
    PUNISHMENTS {
        string id PK
        string target_uuid FK
        string staff_uuid FK
        string type
        string reason
        string evidence
        timestamp expires_at
        boolean appealed
    }
    TRANSACTIONS {
        string id PK
        string player_uuid FK
        string currency
        long amount
        string source
        timestamp created_at
    }
    COSMETICS {
        string player_uuid FK
        string cosmetic_id
        boolean equipped
        timestamp unlocked_at
    }
    FRIENDS {
        string player_uuid FK
        string friend_uuid FK
        timestamp added_at
    }
    MAIL {
        string id PK
        string sender_uuid
        string recipient_uuid FK
        string content
        boolean read
        timestamp sent_at
    }
    SERVER_REGISTRY {
        string shard_id PK
        string region
        int player_count
        float tps
        string status
        timestamp last_heartbeat
    }

    PLAYERS ||--o| ISLANDS : owns
    PLAYERS ||--o{ SKILLS : has
    PLAYERS ||--o{ STATS : tracks
    PLAYERS ||--o{ LEADERBOARDS : ranked_in
    PLAYERS ||--o{ PUNISHMENTS : receives
    PLAYERS ||--o{ TRANSACTIONS : logs
    PLAYERS ||--o{ COSMETICS : owns
    PLAYERS ||--o{ FRIENDS : has
    PLAYERS ||--o{ MAIL : receives
```

---

## ⚠️ SpacetimeDB Risk Mitigations

| Risk | Mitigation |
|------|-----------|
| **Java client maturity** | `suite-database` is critical path — 2 weeks dedicated. Test reconnection, subscription scaling with 500+ concurrent players, reducer latency. |
| **Schema evolution** | Reducer-based migrations. Version field in schema. Test upgrades on staging before prod. |
| **RAM limits** | Profile memory under load. Purge old `MESSAGES`/`TRANSACTIONS` rows periodically. Archive cold data. |
| **Single point of failure** | Automated backups every 6hr. Fast restore tested monthly. Future: explore SpacetimeDB replication. |

---

## 🗓️ Development Phases (2-person team)

### Phase 1 — Mythic Suite (Weeks 1–8) ✅ COMPLETE

> **All 26 reactor modules compile and test — BUILD SUCCESS.**

| Week | Dev A | Dev B | Status |
|------|-------|-------|--------|
| 1–2 | `suite-database` (CRITICAL PATH) | `suite-hex`, `suite-config`, `suite-format` | ✅ |
| 2–3 | `suite-database` (continued), `suite-scheduler` | `suite-item`, `suite-menu` | ✅ |
| 3–4 | `suite-command` + command blocker, `suite-event`, `suite-packet` | `suite-tab`, `suite-scoreboard` | ✅ |
| 4–5 | `suite-economy`, `suite-permission` | `suite-nametag`, `suite-hologram` | ✅ |
| 5–6 | `suite-protocol`, `suite-cooldown` | `suite-resourcepack`, `suite-skin` | ✅ |
| 6–7 | `suite-chat` (incl. filtering) | `suite-cosmetic` | ✅ |
| 7–8 | `suite-disguise`, integration tests | Full suite integration tests | ✅ |

---

### Phase 2 — MythicCord + Geyser + Docker (Weeks 9–12)

| Task | Owner | Status |
|------|-------|--------|
| **Fork from Infrarust** (not SpacerCord — clean rebase, refactored as `mythic-cord/`) | Dev A | ✅ |
| SpacetimeDB module: sessions, routing, punishments, registry, economy, social, gameplay | Dev A | ✅ |
| Java mirror in `suite-database/schema/` (constants, DTOs, typed reducer client, version check) | Dev A | ✅ |
| MythicCord workspace: `stdb-bridge`, `plugin-routing`, `proxy` crates | Dev A | ✅ |
| Pterodactyl egg JSON + management surface (admin HTTP, SIGTERM drain, JSON logs) | Dev A | ✅ |
| Docker Compose: full network + dev variant (STDB, proxy, Folia, API, monitoring) | Dev A | ✅ |
| `stdb-publish` container — auto-publishes WASM module on `compose up` | Dev A | ✅ |
| Monitoring stack: Prometheus + Grafana provisioning, starter dashboard | Dev B | ✅ |
| Vendor `infrarust/` subtree via `mythic-cord/tools/vendor-infrarust.{sh,ps1}` | Dev A | ✅ |
| Geyser integration + Bedrock resource pack conversion | Dev B | ✅ |
| SimpleVoice-Geyser deployment + proximity config | Dev B | ✅ |
| Sentry integration in Java suite | Dev B | ✅ |

#### Deliverables landed

| Module | Path | Notes |
|---|---|---|
| **STDB schema** | [`mythic-cord/stdb/`](mythic-cord/stdb/) | 10 modules, 28 tables, 43 reducers, `SCHEMA_VERSION=2` (Phase 3 added rank_definitions, rank_grants, punishment_templates, punishment_blacklist; widened punishments) |
| **Java mirror** | [`mythic-suite/suite-database/schema/`](mythic-suite/suite-database/src/main/java/net/mythicpvp/suite/database/schema/) | 28 files (5 enums, 18 DTO records, `MythicSchema` typed client, `SchemaVersion` boot check) — **17 new tests, all green** |
| **Rust bridge** | [`mythic-cord/stdb-bridge/`](mythic-cord/stdb-bridge/) | Driver thread + `StdbHandle` + `MythicStdbClient` (typed reducer wrappers) |
| **Routing helpers** | [`mythic-cord/plugin-routing/`](mythic-cord/plugin-routing/) | `pick_shard` (mirrors STDB pure fn), `RegistryView` (live snapshot of `server_registry` table), heartbeat task. Pure helpers consumed by the sidecar — no Infrarust plugin hooks (modern Infrarust v2 has no per-connection routing API; backend selection is config-file driven). |
| **MythicCord sidecar** | [`mythic-cord/proxy/`](mythic-cord/proxy/) | Standalone binary that registers in STDB, exposes admin HTTP, and (with `--features with-infrarust`) runs `ConfigExporter` — debounced loop that subscribes to `RegistryView` and writes one `<shard-id>.toml` per healthy server into Infrarust's `servers_dir`, pruning stale files. Infrarust runs as a separate process beside the sidecar and hot-reloads when the directory changes. |
| **Vendored Infrarust** | [`mythic-cord/infrarust/`](mythic-cord/infrarust/) | `v2.0.0-alpha.6` git subtree pulled by `tools/vendor-infrarust.ps1`. AGPL-3.0 with plugin exception; `mythic-cord/LICENSE` carries both terms. |
| **Pterodactyl egg** | [`mythic-cord/pterodactyl/egg-mythiccord.json`](mythic-cord/pterodactyl/egg-mythiccord.json) | 11 env variables, install script downloads musl release, default config seeded on first install |
| **Docker scaffold** | [`tools/docker/`](tools/docker/) | `docker-compose{,dev}.yml`, 5 Dockerfiles, monitoring + provisioning, up/down scripts |
| **Geyser deployment** | [`tools/docker/geyser/`](tools/docker/geyser/) | Standalone Bedrock sidecar on UDP `19132`, runtime-rendered Geyser target defaults to Hub and switches to MythicCord when traffic support is enabled, Bedrock pack mount at `geyser/packs/`, process-backed `.mcpack` conversion hook |
| **Voice deployment** | [`tools/docker/folia/voicechat-server.properties`](tools/docker/folia/voicechat-server.properties) | Simple Voice Chat + SimpleVoice-Geyser Modrinth resolution baked into Folia image, proximity defaults, UDP/web bridge ports |
| **Sentry bootstrap** | [`MythicErrorTracker`](mythic-suite/suite-api/src/main/java/net/mythicpvp/suite/api/error/MythicErrorTracker.java) | Shared Java suite Sentry initializer with Docker env wiring and shaded SDK runtime |

#### Design decisions

- **Forked Infrarust directly, not SpacerCord.** SpacerCord stays a reference — its driver-thread / clone-able-handle pattern is cherry-picked into [`stdb-bridge`](mythic-cord/stdb-bridge/), not its `module_bindings/` (which would conflict with our schema).
- **AGPL-3.0** carries from Infrarust. The proxy stack is AGPL-3.0-or-later with the upstream plugin exception preserved (closed-source plugins still permitted). The Java suite under `mythic-suite/` is licensed separately at the repo root.
- **Sidecar architecture, not in-process plugin.** Modern Infrarust v2 doesn't expose per-connection routing hooks to plugins (the only public lifecycle action is `deny`, not redirect; `ConfigService` is read-only). Backend selection is config-file driven — Infrarust watches `servers_dir/` and hot-reloads on file change. So MythicCord runs as a separate process beside Infrarust: it owns `servers_dir/`, regenerates one TOML per healthy `server_registry` row, and lets Infrarust's built-in file watcher pick up changes. Two cargo features select runtime mode: default standalone (admin HTTP + heartbeat only); `--features with-infrarust` adds the `ConfigExporter` loop.
- **Schema versioning is a strict gate.** `SCHEMA_VERSION` is asserted on both the Java side (`SchemaVersion.assertMatches`) and the Rust side (`assert_schema_version`) at boot. Mismatched suite vs STDB module refuses to start.
- **`pick_shard` lives twice** — once in [`mythic-stdb`](mythic-cord/stdb/src/sessions.rs) for cross-shard transfers, once in [`plugin-routing`](mythic-cord/plugin-routing/src/router.rs) for the proxy hot path. Identical formula, both unit-tested, no network round-trip per login.
- **Pterodactyl integration is real**, not a placeholder. SIGTERM/SIGINT triggers drain → status flip → 500ms grace → final offline heartbeat → exit 0. Admin HTTP exposes `/health`, `/metrics`, `POST /admin/drain` for manual rolling deploys.

#### Verification (last run)

| Surface | Tool | Result |
|---|---|---|
| Full Maven reactor | `mvn -B -ntp test` | **BUILD SUCCESS** — all 26 modules, ~38 tests pass |
| Docker Compose syntax | `docker compose -f tools/docker/docker-compose.yml config --quiet` | Clean after Geyser, voice, Sentry wiring |
| Docker Compose dev syntax | `docker compose -f tools/docker/docker-compose.dev.yml config --quiet` | Clean after voice and Sentry wiring |
| `suite-database` schema package | `mvn -pl mythic-suite/suite-database test` | **20/20 green** (`MythicSchemaTest` 12, `DtoRoundTripTest` 5, `SpacetimeConnectionTest` 3) |
| `mythic-stdb` (SpacetimeDB module) | `cargo check -p mythic-stdb --target wasm32-unknown-unknown` | Clean — 0 errors, 0 warnings |
| `mythiccord-stdb-bridge` | `cargo test -p mythiccord-stdb-bridge` | **3/3 green** |
| `mythiccord-plugin-routing` | `cargo test -p mythiccord-plugin-routing` | **4/4 green** |
| `mythiccord` (sidecar, with-infrarust) | `cargo test -p mythiccord --features with-infrarust` | **3/3 green** (`config_export::*` write/prune/fingerprint) |
| `mythiccord` (sidecar, standalone) | `cargo build -p mythiccord` | Clean |

Toolchain: Maven 3.9.9 + Microsoft OpenJDK 21.0.11 for Java; Rust 1.94.1 (`x86_64-pc-windows-gnu`) + winlibs MinGW-w64 14.2.0 for Rust. All toolchains installed under `.build-tools/` (gitignored) so the host environment stays clean.

---

### Phase 3 — Core Plugin + Hub (Weeks 13–16)

`mythic-core` is the shared network essentials and staff suite installed on every backend server. It owns core commands, staff tooling, punishments, ranks, prefixes, suffixes, chat controls, announcements, broadcasts, tablist, scoreboard, nametags, and cross-server staff communication. Every player-facing message, staff format, punishment line, command response, tablist entry, scoreboard line, prefix, suffix, broadcast, and announcement must be configurable through YAML.

| Task | Owner | Status |
|------|-------|--------|
| `mythic-core`: Maven module, plugin bootstrap, command API vararg support, default YAML resources | Dev A | ✅ |
| `mythic-core`: server identity, Folia-safe scheduler wiring via `MythicScheduler` (ChatGuard/PlayerSessionListener/MainThreadHydrationSink/ChatPromptService), ordered shutdown (config save → UI manager clear → STDB disconnect) | Dev A | ✅ |
| `mythic-core`: ranks (existing colors/prefixes/suffixes/permissions) + cosmetic bundle integration — `RankCosmeticBundles` loads `ranks.<id>.bundled-cosmetics` from YAML; `RankBundleGrantHook` attached via `GrantService.setGrantObserver` auto-grants bundled cosmetics through `CosmeticManager.grantCosmetic` on every `/grant`, audit-logged | Dev A | ✅ |
| `mythic-core`: STDB persistence layer — schema v2 tables/reducers + `PersistenceGateway` wiring for ranks, grants, punishments, templates, blacklist | Dev A | ✅ |
| `mythic-core`: STDB hydration / cross-server read path — subscribe to all 5 Phase 3 tables, dispatch to services via `CoreHydrationSink`, main-thread-safe via `MainThreadHydrationSink` | Dev A | ✅ |
| `mythic-core`: command blocker coverage — `command-blocker.yml` ships with full per-command perm map for all `mythic.core.*` commands + noisy info commands (pl, plugins, ?, help, version, tps, mem); permission-hidden tab completion + execute rejection inherited from `suite-command`'s `CommandBlocker` listeners (PlayerCommandSendEvent, TabCompleteEvent, PlayerCommandPreprocessEvent) | Dev A | ✅ |
| `mythic-core`: essentials commands `/gmc`, `/gms`, `/gamemode`, `/tp`, `/tphere`, `/help`, `/discord` | Dev A | ✅ |
| `mythic-core`: 5 staff chats (`/staffchat`, `/builderchat`, `/managementchat`, `/adminchat`, `/ownerchat`) over `core:staff-chat` → `BukkitStaffAudience` renders to permitted players + console with `messages.staff.format`. Sender's rank/color resolved via `GrantService.activeRank` so cross-server messages carry the right prefix | Dev A | ✅ |
| `mythic-core`: staff join/quit notifications via `StaffPresenceListener` (gated by `mythic.core.staff.notify`) → `core:staff-presence` channel → `BukkitStaffPresenceAudience` renders `messages.staff.{join,quit,switch}` to permitted players + console. Server-switch path is ready (service supports it) — proxy emits the SWITCH event in Phase 2 | Dev A | ✅ |
| `mythic-core`: `/staffmode` toggle — `StaffModeSnapshot` captures + restores inventory/armor/offhand/gamemode/flight; vanish hides staff from non-staff viewers; tool palette (INSPECT/FREEZE/RANDOM_TELEPORT/DISABLE) in `staff-mode.yml`; `StaffModeToolListener` dispatches right-clicks (entity-target tools via `PlayerInteractEntityEvent`, no-target via `PlayerInteractEvent`); frozen-player movement enforcement; logout-while-in-staff-mode auto-restores | Dev A | ✅ |
| `mythic-core`: punishments — bans/tempbans/mutes/tempmutes/warns/pardons/history (existing) + `/appeal <message>` for player + `/appeals review approve\|deny <id> [notes]` for staff via `PersistenceGateway.appealOpen`/`appealReview` → STDB `appeal_open`/`appeal_review` reducers; `CoreAuditLog` writes timestamped `key=value` lines to `plugins/MythicCore/audit.log` for every appeal action | Dev A | ✅ |
| `mythic-core`: silent punishment flag `-s` for all punishment commands, including `/ban -s <player>` | Dev A | ✅ |
| `mythic-core`: chat management `/chat mute`, `/chat slow <seconds>`, `/chat clear`, local and network scopes via `ChatGuard` listener + `ChatControlService` shard-aware scope filtering | Dev A | ✅ |
| `mythic-core`: `/broadcast <message…>` cross-server via `core:broadcast` channel + rotating announcements driven by `MythicScheduler.runTimer` (interval, format, message list in `announcements.yml`); origin-shard skip prevents echo loops | Dev A | ✅ |
| `mythic-core`: tablist, scoreboard, nametag formatting bound to YAML + ranks via `DisplayService` + `PlayerSessionListener`, with cosmetic chat-tag/title placeholders + disguise rank/name override | Dev A | ✅ |
| `mythic-core`: friends, party, mail, offline rewards | Dev B | |
| `mythic-core`: network-wide cosmetics economy — `/cosmetics` redeem/equip/filter menu, itemize/withdraw eligible cosmetics for trade/sale, Tebex credit balance integration, credit-spend hooks for ranks/lootboxes/crates, and weighted cosmetic crate rolls with per-item odds + limited-item windows | Dev B | |
| `mythic-hub`: spawn, server selector, and hub activities using `mythic-core` services | Dev B | |
| Resource pack: finalize MythicPvP custom font, rebrand `smpd` → `mythic` namespace | Dev A | ✅ |

#### Current Phase 3 Implementation Notes

- ✅ Dynamic completion provider foundation exists in `suite-command` and is wired into initial `mythic-core` rank/grant commands.
- ✅ Initial rank/grant service foundation exists with rank ids, color, dye, prefix, suffix, weight, parent, permissions, staff/donator flags, and independent chat/tab/nametag display formats.
- ✅ Initial grant commands exist for `/grant`, `/grants`, `/cgrant`, `/cleargrants`, and `/rankeditor`, with permission-aware completions for players, ranks, durations, reasons, booleans, and rank fields.
- ✅ `/grant <username>` now opens the core grant menu flow with rank selection, duration presets, custom duration chat input, reason presets, custom reason chat input, and final confirmation.
- ✅ Runtime rank editor command mutations now exist for setting rank fields and adding or removing permissions, including independent chat, tab, and nametag formatting fields.
- ✅ Chat prompt handling exists for menu-backed custom values and is covered by MockBukkit tests.
- ✅ Initial punishment template, handbook, history, clear-history, and `/punish` menu flow exists with category selection, template selection, proof entry, silent toggle, clear-inventory toggle, and execution summary.
- ✅ Punishment template admin commands exist for add, edit, and remove with live template completions and YAML-backed default seed templates.
- ✅ Initial essentials command implementation exists for `/gmc`, `/gms`, `/gamemode`, `/tp`, `/tphere`, `/help`, and `/discord`, backed by YAML-configurable response text.
- ✅ Essentials completions include gamemode values plus permission-aware target-player completions for gamemode and teleport-other flows.
- ✅ **Persistence layer** — `mythic-stdb` schema bumped to v2 with new `rank_definitions`, `rank_grants`, `punishment_templates`, `punishment_blacklist` tables; `punishments` widened with `target_name`, `staff_name`, `silent`, `clear_inventory`, `server`, and `proof` (renamed from `evidence`); 12 new reducers (`rank_define`, `rank_remove`, `grant_issue`, `grant_deactivate`, `grant_remove_inactive`, `grant_clear`, `grant_expire`, `template_upsert`, `template_remove`, `blacklist_add`, `blacklist_revoke`, `punish_clear_history`). `RankService` / `GrantService` / `PunishmentService` route every mutation through a `PersistenceGateway` which forwards to STDB in production and is a no-op in tests. Java mirror (`MythicSchema`, DTOs, enums) updated in lockstep; both sides assert `SCHEMA_VERSION = 2` at boot.
- ✅ **Hydration / read path** — `PersistenceGateway.hydrate(HydrationSink)` opens subscriptions to all five Phase 3 tables and dispatches row events into `apply*` / `remove*` methods on the services that bypass the gateway (no echo loops). DTO → domain conversion lives in `StdbPersistenceGateway`; `CoreHydrationSink` routes to services and tracks the blacklist; `MainThreadHydrationSink` reschedules to the Bukkit primary thread for `PermissionManager` safety. STDB-assigned ids are preserved on the way in and the auto-inc id generators are bumped to stay ahead of them. Closes the cross-server gap: server B now sees server A's writes within one subscription delivery.
- ✅ **Display tier** — `DisplayService` reads each player's active rank from `GrantService` / `RankService`, resolves `%player%` / `%rank%` / `%server%` / `%online%` / `%chat_prefix%` / `%tab_prefix%` / `%nametag_prefix%` / etc. via a tiny `PlaceholderResolver`, and pushes the resolved templates through `TabManager.setLayout` / `setEntry`, `NametagManager.setNametag`, and `BoardManager.create` / `setLines`. `PlayerSessionListener` triggers `apply` on join and `clear` on quit, plus a deferred `applyAll` so existing players' tabs reflect arrivals/departures and the `%online%` counter updates. `RankService.setDisplayRefresher` and `GrantService.setDisplayRefresher` callbacks let mutations propagate to the display tier without coupling rank-tier tests to it. Configuration sourced from `tablist.yml` / `scoreboard.yml` (existing) and the new `nametag.yml`.
- ✅ **Chat management** — `/chat mute|unmute|slow <seconds>|clear|status [local|network]`. `ChatControlService` extended with origin-shard tracking (LOCAL state from another shard is dropped at apply time), per-player slow-mode bookkeeping (`registerMessage` returns the wait-millis, 0 if allowed), and a `ClearListener` callback for clear pulses. `ChatGuard` listener cancels `AsyncPlayerChatEvent` when chat is muted or the sender is in cool-down (bypass via `mythic.core.chat.bypass`), and floods 100 blank lines on a clear pulse. Scope defaults to LOCAL so a typo doesn't accidentally mute the whole network. All staff-facing strings live in `messages.yml` under `messages.chat-control.*`.
- ✅ **Rank/grant menu text → YAML** — `RankMenuText` reads operator-overridable strings from `menus.yml` (`rank.*` subtree): grant flow titles, click hints, confirm/cancel labels, editor section headers, plus configurable `duration-presets` and `reason-presets` lists. `GrantFlowService` and `RankEditorCommand` take the bundle via constructor; old constructors fall back to `RankMenuText.DEFAULTS`.
- ✅ **Rank editor click flows** — `RankEditorMenuService` extends `/rankeditor <rank>` from a 3-row read-only display into a navigable editor. Overview menu links to a Field editor (name, color, dye, weight, prefix/suffix, parent, plus toggle tiles for staff/donator), a Format editor (chat/tab/nametag prefix + format), and a paginated Permission menu (click-to-remove existing nodes, "Add Permission" button opens a chat prompt for new ones). Every click routes through `RankService.setField` / `addPermission` / `removePermission` so STDB persistence + display refresh fire automatically.
- ✅ Login enforcement — `PunishmentLoginGuard` consults the hydrated blacklist + active login-blocking punishments on `AsyncPlayerPreLoginEvent` and rejects with the configured kick reason. Bypass via `mythic.core.punish.bypass` (only honored once player object exists). Per-network state via STDB hydration so all servers see the same answer.
- ✅ Quoted multi-word template title parsing — `QuotedArgs` helper accepts `"Chat Offense #1"` shape; `PunishmentAddCommand` tries quoted first, falls back to legacy pipe (`title | information`).
- ✅ **Punishment menu text → YAML** — `PunishmentMenuText` reads operator-overridable strings from `menus.yml` (titles, button names, click hints, state labels). Falls back to historical hard-coded defaults when the file is missing or partial.
- ✅ **Display tier — cosmetics + disguises** — `DisplayService` now exposes `%cosmetic_chat_tag%` and `%cosmetic_title%` placeholders sourced from `CosmeticManager.getEquipped` → `CosmeticManager.get(id).displayName()` (empty string when nothing's equipped or the catalog hasn't hydrated, so old templates without the tokens stay untouched). `DisguiseManager.getDisplayName` is the source of `%player%` so disguised staff appear as the disguise name in tab/scoreboard/nametag, and `DisguiseManager.getRankOverride` swaps the rank used for prefix/format resolution so the visible rank matches the disguise. The "real name" reveal for staff with `canSeeThrough` is still applied at packet time by `DisguiseManager.getVisibleName`, not here.
- ✅ **STDB cosmetic-grant persistence** — `RankBundleGrantHook` now resolves each bundled cosmetic's type via `CosmeticManager.get(id).type()` and persists the grant through `PersistenceGateway.cosmeticGrant` → `MythicSchema.cosmeticGrant` (source `RANK_BUNDLE`, reference = rank id). Skips the persistence call if the catalog hasn't hydrated yet so the local grant + audit log still happen.
- ✅ **Per-gamemode scoreboard layouts** — `scoreboard.yml` now supports a `gamemodes:` section keyed by case-insensitive server-id prefix. `DisplayService.loadTemplates` picks the first matching key (e.g. `hub-1` matches `hub`) and falls back to the top-level `scoreboard:` block. Hub / Skyblock / Practice templates ship as defaults.
- ✅ **PlaceholderAPI bridge (optional)** — `PapiBridge` is a reflection-based pass-through that runs `PlaceholderAPI.setPlaceholders(player, text)` over tablist + scoreboard text after the suite's own `%token%` resolver. No hard dependency: PAPI is `softdepend` in `plugin.yml`, so when it's absent the bridge is a no-op. Cached method handle, one-shot warn on lookup failure so a broken install doesn't spam logs.
- ✅ **Strict-lint pass** — `pom.xml` now compiles with `-Xlint:all`. Resolved warnings: `AsyncPlayerChatEvent` → modern `io.papermc.paper.event.player.AsyncChatEvent` in `ChatGuard` + `ChatPromptService` (test rewritten to mock the event); `MythicConfig` constructor `this`-escape closed by making `load()` final; `SchemaVersionMismatchException` got a `serialVersionUID`; `ResourcePackManager.sendTo` migrated from the deprecated `Player.setResourcePack(String)` to `player.sendResourcePacks(ResourcePackRequest)` with a deterministic per-URL pack id.
- ✅ **ChatPromptService quit cleanup** — pending prompts are dropped on `PlayerQuitEvent` so disconnected players don't accumulate in the in-memory map on long-running servers.
- ✅ **Essentials polish** — `/gmc` now also responds to `gm1`/`creative`, `/gms` to `gm0`/`survival`, `/tp` to `teleport`, `/tphere` to `tpme`. `CoreEssentialsService` writes `GAMEMODE` / `TELEPORT` / `TELEPORT_HERE` lines to `CoreAuditLog` (with from/to gamemode and destination world for context). Teleports route through `MythicScheduler.runOnEntity` on Folia so cross-region teleports land on the entity's region thread; on vanilla / Paper / MockBukkit they stay synchronous so callers and tests observe the new position immediately.
- ✅ **Social foundation** — `SocialService` now owns local friend request/friend link, party/member, and mail inbox state while forwarding writes through the existing STDB social reducers. `StdbPersistenceGateway` hydrates `friends`, `friend_requests`, `parties`, `party_members`, and `mail`; `/friend`, `/party`, and `/mail` commands are registered in `mythic-core`; join-time unread mail notices are configurable through `messages.yml`.

Planned **network cosmetic economy** belongs to `mythic-core`, not `mythic-hub`: `/cosmetics` will handle redeem/equip/filter flows, eligible cosmetic item withdrawal for trade/sale, Tebex credit spending, and weighted crate/lootbox rolls across every backend server.

#### Command Completion Requirements

- All `mythic-core` commands must provide permission-aware tab completions through `suite-command`.
- Players must only see commands, subcommands, online player names, rank ids, durations, punishment categories, punishment template titles, and editor options they are permitted to use.
- Console-supported commands must expose useful completions where Bukkit supports them, especially `/cgrant`, punishment template management, and history commands.
- Completion providers must be backed by live core services where possible so rank ids, permissions, punishment categories, and punishment templates stay current after runtime edits.
- Command completions must respect the command blocker so hidden commands are not exposed through `/`, `/command <tab>`, namespace aliases, or subcommand suggestions.

#### Ranks And Grants Requirements

- `/grant <username>` opens a menu flow for granting ranks: rank selection, duration selection, reason selection, and final confirmation.
- The rank selection menu displays every grantable rank as a dye based on the rank color configured in the rank editor.
- Rank selection lore must include staff-rank status, purchasable/donator status, prefix, suffix, parent/inheritance, weight, permission list, and available permission edit actions.
- The duration menu must include `1d`, `7d`, `30d`, `90d`, `365d`, permanent, and custom chat input. Custom input must accept values like `1d`, `7d`, `30d`, `perm`, and `permanent`.
- The reason menu must include preset reasons for staff rank, rank upgrade, purchased rank, and custom chat input.
- The final grant confirmation menu must contain a summary item showing target username, rank, duration, and reason, plus green confirm and red cancel items.
- `/grants <username>` opens a grant-history menu showing active and inactive grants. Each grant is displayed as a dye based on rank color while active, and as grey dye when inactive.
- Grant lore must display duration, executor, reason, remaining time for temporary grants, active/inactive state, and click actions.
- Left clicking an active grant makes it inactive without removing it from history. Right clicking an inactive grant removes it from the user's visible grant history.
- `/cgrant <username> <rank> <duration> <reason>` must grant ranks from console and from permitted players.
- `/cleargrants <username>` must clear the player's entire grant history and remove active grants.
- Rank grants must preserve history by default; inactive grants remain visible until explicitly removed.
- The rank editor must support both command and menu editing for prefix, suffix, weight, color, dye material, parent/inheritance, permission list, staff-rank toggle, donator/purchasable toggle, chat formatting, tab formatting, and nametag formatting.

#### Rank Display Formatting Requirements

- Each rank must support independent chat, tab, and nametag prefix/format fields.
- All rank display fields must support Mythic hex strings and existing suite hex parsing.
- Core chat formatting must use the rank's chat prefix and chat format.
- Core tab formatting must use the rank's tab prefix and tab format.
- Core nametag formatting must use the rank's nametag prefix and nametag format.
- A rank's generic prefix/suffix must remain available as fallback values, but display-specific formats must take precedence.
- Rank display changes must refresh affected chat, tablist, nametag, and scoreboard state through the core display services.

#### Punishment Menus And Handbook Requirements

- `/punish <username>` opens the punishment execution menu flow for the selected target.
- The first punishment menu displays categories `WARN`, `MUTE`, `BAN`, and `BLACKLIST`, each represented by a distinct wool color.
- Selecting a category opens a paginated menu of punishment templates for that category, including offence-number titles such as `Cheating #1`.
- Punishment template lore must show duration, category, title, and additional staff guidance.
- Selecting a punishment opens the proof menu with no-proof state, proof summary, chat-entered proof input, and proof confirmation.
- After proof confirmation, the final punishment confirmation menu must show clear-inventory toggle, silent-execution toggle, punishment summary, and execute item.
- The silent toggle must use the existing silent punishment behavior so no public broadcast is sent when silent is enabled.
- `/punishments` opens a read-only staff handbook containing every category and every punishment template. It must never execute punishments.
- `/history <username>` opens punishment history for the target.
- `/clearpunishments <username>` and `/clearhistory <username>` clear punishment history.
- `/punishmentadd <category> <duration> <title> <additional information>` adds a punishment template.
- `/punishmentedit <title>` opens an editor menu for title, category, duration, and additional information.
- `/punishmentremove <title>` removes a punishment template.
- Punishment templates must include category, duration, title, and additional information.

#### Cosmetics, Credits, Crates, And Lootboxes Requirements

- Cosmetics are a `mythic-core` network system installed on every backend server, not a hub-only preview feature.
- `/cosmetics` must open a menu where players can redeem cosmetic items into account ownership, equip owned cosmetics, filter/search by type/rarity/limited/owned/equipped/tradable state, and view item details.
- Eligible cosmetics must be withdrawable from the cosmetics menu as physical items that can be traded or sold, then redeemed back into ownership by the receiving player.
- Cosmetic ownership, equipped loadouts, itemized/withdrawn state, crate definitions, roll history, and limited-item windows must persist network-wide through SpacetimeDB.
- Tebex purchases grant account credits. Credits can be spent through server-side flows on ranks, lootboxes, crate rolls, and other configured catalog entries.
- Cosmetic crates can be won or opened on any server. Each crate must define a weighted item pool, visible per-item roll percentages, rarity/limited metadata, and any limited-time availability window.
- Crate/lootbox rolls must be auditable and deterministic enough for dispute review: store player, crate id, consumed credit/item source, rolled item, odds snapshot, and timestamp.
- Credit and crate rewards must be permission/config gated and reviewed for EULA compliance before any gameplay-affecting item or rank benefit is added.

#### Persistence And Config Requirements

- Rank definitions, rank grants, grant history, punishment templates, proof, blacklist records, and punishment history must persist network-wide.
- YAML must configure all menu titles, item names, lore, prompts, confirmation text, error text, command responses, rank display defaults, punishment template seed data, grant duration presets, grant reason presets, and rank editor labels.
- Runtime history and state must use the database/service layer rather than only local YAML.
- YAML seed data may create default ranks and punishment templates when no database state exists.
- Persistence must use existing SpacetimeDB tables/reducers where possible and add Phase 3 schema/reducer entries only for missing rank grants, rank definitions, punishment templates, proof, blacklist, or clear-history behavior.

#### Phase 3 Test Additions

- Add tests for permission-aware tab completions.
- Add tests for essentials gamemode, teleport, help, discord, and permission-aware target completions.
- Add tests for grant flows, grant history, inactive grant behavior, `/cgrant`, and `/cleargrants`.
- Add tests for rank editor commands and menus.
- Add tests for independent chat, tab, and nametag rank formatting.
- Add tests for `/punish` menu flow, proof entry, silent toggle, clear-inventory toggle, handbook templates, history, and template management.
- Run `mvn -B -ntp -pl mythic-core -am test`, then full `mvn -B -ntp test`, before committing implementation work.
- Run a Java source comment scan over changed Java files before committing implementation work.

#### Phase 3 Completion Rule

- Do not mark a Phase 3 row complete only because the row exists in this plan.
- Mark a Phase 3 row complete only after detailed requirements are present, implementation exists, relevant tests pass, the source comment scan is clean for changed Java files, and the work is committed and pushed to `main`.

Core protocol channels: `core:staff-chat`, `core:staff-presence`, `core:broadcast`, `core:chat-control`, and `core:punishment-update`. Core persistence uses the existing SpacetimeDB schema where possible, with Phase 3 migrations added only for missing audit, chat-control, or staff-presence state.

---
### Phase 4 — Skyblock Core (Weeks 17–24)

- Island management, sharding, Slime-format storage
- Economy, shops, auction house
- Custom enchantments (Common → Mythic)
- Quests, milestones, progression tracks

---

### Phase 5 — PvP & Events (Weeks 25–30)

- PvP zones, combat tag, kill rewards, killstreaks
- KOTH system (4hr schedule, capture points, buffs)
- Airdrop system (player-threshold triggers, tiered loot)
- Points system (PvP weighted highest), Island Top

---

### Phase 6 — Skills & Leaderboards (Weeks 31–36)

- Mining, Farming, Fishing, Combat — XP curves, abilities
- Leaderboards: Daily/Weekly/Monthly/All-time per category
- In-game hologram displays + `/leaderboard` GUI
- Automated payouts per timeframe

---

### Phase 7 — API Suite & Website (Weeks 37–42)

- REST gateway (Ktor): JWT auth, rate limiting, Swagger docs
- Skin/nametag rendering API, leaderboard API, forum API
- Next.js website: Home, Leaderboards, Profiles, Forums, Clan, Store, Staff Panel
- MC account linking, real-time updates via SpacetimeDB TS SDK

---

## 📊 Phase Summary

| Phase | Name | Weeks | Key Deliverables |
|-------|------|-------|-----------------|
| **1** | **Mythic Suite** ⭐ | 1–8 | All 23 foundation APIs, YAML-configurable text surfaces, tested and documented |
| **2** | **MythicCord + Docker** ✅ | 9–12 | STDB schema (wasm32 build clean), Java mirror (20/20 tests), Rust bridge (3/3 tests), routing helpers (4/4 tests), MythicCord sidecar with config exporter (3/3 tests), Pterodactyl egg, Docker scaffold + monitoring, Geyser sidecar, voice deployment, Sentry bootstrap, Infrarust v2.0.0-alpha.6 vendored as git subtree |
| **3** | Core + Hub | 13–16 | Network-wide essentials/staff suite, punishments with silent mode, ranks, cosmetics/credits/crates, YAML-driven tab/scoreboard/chat, friends/party, hub, resource pack |
| **4** | Skyblock Core | 17–24 | Islands, economy, enchants, quests |
| **5** | PvP & Events | 25–30 | PvP zones, KOTH, airdrops, points |
| **6** | Skills & Leaderboards | 31–36 | 4 skills, leaderboard system |
| **7** | API + Website | 37–42 | REST APIs, Next.js website, forums |
