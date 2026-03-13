# ꜰᴀᴋᴇ ᴘʟᴀʏᴇʀ ᴘʟᴜɢɪɴ (FPP)

> Spawn realistic fake players on your Paper server — complete with tab list, server list count, join/leave/kill messages, staggered join/leave delays, in-world physics bodies, real-player-equivalent chunk loading, skin support, bot swap/rotation, fake chat, session database tracking, LuckPerms integration, and full hot-reload configuration.

![Version](https://img.shields.io/badge/version-1.1.4-0079FF?style=flat-square)
![MC](https://img.shields.io/badge/Minecraft-1.21.x-0079FF?style=flat-square)
![Platform](https://img.shields.io/badge/platform-Paper-0079FF?style=flat-square)
![Java](https://img.shields.io/badge/Java-21-0079FF?style=flat-square)
[![Modrinth](https://img.shields.io/badge/Modrinth-FPP-00AF5C?style=flat-square&logo=modrinth)](https://modrinth.com/plugin/fake-player-plugin-(fpp))
[![Discord](https://img.shields.io/badge/Discord-Join%20Server-5865F2?style=flat-square&logo=discord&logoColor=white)](https://discord.gg/ZhsstSJb)

---

## ✦ Features

| Feature | Description |
|---|---|
| **Tab list** | Fake players appear in the tab list for every online and future player |
| **Server list count** | Fake players increment the online player count shown on the server list |
| **Join / leave messages** | Broadcast to all players and console — fully customisable in `language/en.yml` |
| **Kill messages** | Broadcast when a real player kills a bot (toggleable) |
| **In-world physics body** | Bots spawn as `Mannequin` entities — pushable, damageable, solid (toggleable) |
| **Custom nametag** | ArmorStand above the Mannequin displays the bot's display name |
| **Head AI** | Bot body faces the nearest player within configurable range using smooth interpolation |
| **Staggered join / leave** | Each bot joins and leaves with a random per-bot delay so it looks like real players |
| **Skin support** | `auto` mode — Paper resolves the real Mojang skin from the bot's name automatically |
| **Default skin** | When skins are disabled, bots use the default Steve/Alex appearance |
| **Death & respawn** | Bots can respawn at their last known location, or leave the server permanently |
| **Combat** | Bots take damage and play player hurt sounds; they cannot target or attack |
| **Real-player chunk loading** | Bots load chunks in spiral order exactly like a real player — mobs spawn, redstone ticks, farms run; world-border clamped; movement-delta detection skips redundant updates |
| **Session stats** | Each bot tracks damage taken, death count, uptime, and last chunk position internally |
| **Bot swap / rotation** | Bots automatically leave and rejoin with new names — with personality archetypes, time-of-day bias, farewell/greeting chat, and AFK-kick simulation |
| **Fake chat** | Bots send random chat messages from `bot-messages.yml` (toggleable, hot-reloadable) |
| **LuckPerms integration** | Detects installed LuckPerms and prepends the default-group prefix to every bot display name (toggleable) |
| **Uptime tracking** | `/fpp list` shows each bot's name, formatted uptime, location, and who spawned it |
| **Database** | All bot sessions (who spawned, where, when, removal reason, last position) stored in SQLite or MySQL |
| **Persistence** | Active bots survive server restarts — they leave on shutdown and rejoin on startup at their last position |
| **Dynamic help** | Help command auto-discovers all registered sub-commands — no manual update needed when adding commands |
| **Clickable pagination** | Help pages have clickable ← prev / next → buttons |
| **Plugin info screen** | Bare `/fpp` shows version, author, active bot count, and a clickable Modrinth link |
| **Hex colour + small-caps** | Styled with `#0079FF` accent and Unicode small-caps throughout |
| **Fully translatable** | All player-facing text in `language/en.yml`; internal layout strings clearly marked |
| **Hot reload** | `/fpp reload` reloads config, language, name pool, and message pool instantly |
| **Bot name pool** | Names loaded from `bot-names.yml` — falls back to `Bot<number>` when the pool is exhausted |
| **User-tier commands** | Non-admins can spawn their own limited bots and teleport them with `fpp.user.*` |
| **Permission-based limits** | Per-player bot limits via `fpp.bot.<num>` permission nodes |

---

## ✦ Requirements

| Requirement | Version |
|---|---|
| [Paper](https://papermc.io/downloads/paper) | 1.21.x (tested on 1.21.11) |
| Java | 21+ |
| [PacketEvents](https://github.com/retrooper/packetevents) | 2.x (shaded / provided) |
| [LuckPerms](https://luckperms.net) *(optional)* | 5.x — auto-detected, not required |

> SQLite JDBC is bundled — no setup needed for local storage.  
> MySQL connector is also included if you prefer an external database.

---

## ✦ Installation

1. Download `fpp-1.1.4.jar` from [Modrinth](https://modrinth.com/plugin/fake-player-plugin-(fpp)).
2. Place it in your server's `plugins/` folder.
3. Restart the server — default config files are generated automatically.
4. Edit `plugins/FakePlayerPlugin/config.yml`, `language/en.yml`, `bot-names.yml`, and `bot-messages.yml` as desired.
5. Run `/fpp reload` to apply changes without restarting.

---

## ✦ Commands

All sub-commands are under `/fpp` (aliases: `/fakeplayer`, `/fp`).

> Type bare `/fpp` to see plugin info — version, active bots, and a Modrinth link.

### Admin Commands

| Command | Permission | Description |
|---|---|---|
| `/fpp help [page]` | `fpp.help` | Paginated help menu with clickable ← / → navigation |
| `/fpp spawn [amount] [--name <name>]` | `fpp.spawn` | Spawn fake player(s) at your location; optional count and custom name |
| `/fpp delete <name\|all>` | `fpp.delete` | Delete a bot by name, or delete all bots at once |
| `/fpp list` | `fpp.list` | List all active bots with name, uptime, world, coordinates, and spawner |
| `/fpp chat [on\|off\|status]` | `fpp.chat` | Toggle or query the bot fake-chat system |
| `/fpp swap [on\|off\|status]` | `fpp.swap` | Toggle or query the bot swap/rotation system |
| `/fpp reload` | `fpp.reload` | Hot-reload config, language, name pool, and message pool |
| `/fpp info` | `fpp.info` | Show total session count and current active bots from the database |
| `/fpp info bot <name>` | `fpp.info` | Live status + full spawn history for a specific bot name |
| `/fpp info spawner <name>` | `fpp.info` | All bots ever spawned by a specific player |
| `/fpp tp [botname]` | `fpp.tp` | Teleport yourself to any active bot |

### User Commands

| Command | Permission | Description |
|---|---|---|
| `/fpp spawn` | `fpp.user.spawn` | Spawn your personal bot (limited by `fpp.bot.<num>`, default 1) |
| `/fpp tph [botname]` | `fpp.user.tph` | Teleport your own bot(s) to your current position |
| `/fpp info [botname]` | `fpp.user.info` | View world, coordinates, and uptime of your own bots |

### Examples

```
/fpp                        — plugin info screen (version, bots, Modrinth link)
/fpp spawn                  — spawn 1 fake player at your position
/fpp spawn 10               — spawn 10 fake players with staggered join delays
/fpp spawn --name Steve     — spawn 1 bot named "Steve"
/fpp spawn 5 --name Steve   — spawn 5 bots; first is "Steve", rest are random
/fpp delete Steve           — remove the bot named Steve with a leave message
/fpp delete all             — remove all bots with staggered leave messages
/fpp list                   — show all active bots with uptime and location
/fpp chat on / off / status — toggle or check fake chat
/fpp swap on / off / status — toggle or check bot swap system
/fpp help [page]            — paginated help with clickable navigation
/fpp reload                 — hot-reload all configuration
/fpp info                   — database stats (total sessions, active bots)
/fpp info bot Steve         — live info + spawn history of bot "Steve"
/fpp info spawner El_Pepes  — all bots spawned by El_Pepes
/fpp tp Steve               — teleport yourself to bot "Steve"
/fpp tph                    — teleport your bot to you (if you own exactly one)
/fpp tph Steve              — teleport your bot named "Steve" to you
```

---

## ✦ Permissions

### Admin Permissions

| Permission | Default | Description |
|---|---|---|
| `fpp.*` | `op` | Grant ALL FPP permissions (admin wildcard) |
| `fpp.help` | `true` | View the help menu |
| `fpp.spawn` | `op` | Admin spawn — no bot limit, supports `--name` and multi-spawn |
| `fpp.spawn.multiple` | `op` | Spawn more than one bot at a time |
| `fpp.spawn.name` | `op` | Use `--name` to spawn with a custom name |
| `fpp.delete` | `op` | Delete bots by name |
| `fpp.delete.all` | `op` | Delete all bots at once |
| `fpp.list` | `op` | List all active bots |
| `fpp.chat` | `op` | Toggle bot fake-chat |
| `fpp.swap` | `op` | Toggle bot swap/rotation |
| `fpp.reload` | `op` | Reload plugin configuration |
| `fpp.info` | `op` | Full database query for any bot or spawner |
| `fpp.tp` | `op` | Teleport yourself to any bot |
| `fpp.bypass.maxbots` | `op` | Bypass the global `limits.max-bots` cap |

### User Permissions

| Permission | Default | Description |
|---|---|---|
| `fpp.user.*` | `true` | Grant all user-facing commands (all players by default) |
| `fpp.user.spawn` | `true` | Spawn bots up to the player's personal limit |
| `fpp.user.tph` | `true` | Teleport own bots to yourself |
| `fpp.user.info` | `true` | View own bot location and uptime |

### Bot Limit Nodes

Assign the highest node the player should receive. FPP picks the highest matching `fpp.bot.<num>` the player holds.

| Permission | Limit |
|---|---|
| `fpp.bot.1` | 1 bot *(default via `fpp.user.*`)* |
| `fpp.bot.2` | 2 bots |
| `fpp.bot.3` | 3 bots |
| `fpp.bot.5` | 5 bots |
| `fpp.bot.10` | 10 bots |
| `fpp.bot.15` | 15 bots |
| `fpp.bot.20` | 20 bots |
| `fpp.bot.50` | 50 bots |
| `fpp.bot.100` | 100 bots |

> **LuckPerms example** — give a VIP group 5 bots:
> ```
> /lp group vip permission set fpp.user.spawn true
> /lp group vip permission set fpp.bot.5 true
> ```

---

## ✦ Configuration

Located at `plugins/FakePlayerPlugin/config.yml`. Run `/fpp reload` to apply changes without restarting.

```yaml
# ─────────────────────────────────────────────────────────────────────────────
#  ꜰᴀᴋᴇ ᴘʟᴀʏᴇʀ ᴘʟᴜɢɪɴ  ·  config.yml  ·  v1.1.4
# ─────────────────────────────────────────────────────────────────────────────

language: en
debug: false

update-checker:
  enabled: true

limits:
  max-bots: 1000
  user-bot-limit: 1
  spawn-presets: [1, 5, 10, 15, 20]

bot-name:
  admin-format: '{bot_name}'
  user-format: 'bot-{spawner}-{num}'

luckperms:
  use-prefix: true

skin:
  mode: auto            # auto | fetch | disabled
  clear-cache-on-reload: true

body:
  enabled: true

persistence:
  enabled: true

join-delay:
  min: 0
  max: 5

leave-delay:
  min: 0
  max: 5

messages:
  join-message: true
  leave-message: true
  kill-message: false

combat:
  max-health: 20.0
  hurt-sound: true

death:
  respawn-on-death: false
  respawn-delay: 60
  suppress-drops: true

# Bots load chunks in spiral order like a real player.
chunk-loading:
  enabled: true
  radius: 6             # 0 = auto (matches server simulation-distance)
  update-interval: 20   # ticks between chunk-ticket refreshes

head-ai:
  look-range: 8.0
  turn-speed: 0.3

collision:
  walk-radius: 0.85
  walk-strength: 0.22
  max-horizontal-speed: 0.30
  hit-strength: 0.45
  bot-radius: 0.90
  bot-strength: 0.14

swap:
  enabled: false
  session-min: 120
  session-max: 600
  rejoin-delay-min: 5
  rejoin-delay-max: 45
  jitter: 30
  reconnect-chance: 0.15
  afk-kick-chance: 5
  farewell-chat: true
  greeting-chat: true
  time-of-day-bias: true

fake-chat:
  enabled: false
  require-player-online: true
  chance: 0.75
  interval:
    min: 5
    max: 10

database:
  mysql-enabled: false
  mysql:
    host: "localhost"
    port: 3306
    database: "fpp"
    username: "root"
    password: ""
    use-ssl: false
    pool-size: 5
    connection-timeout: 30000
  location-flush-interval: 30
  session-history:
    max-rows: 20
```

---

## ✦ Chunk Loading (v1.1.4)

Bots load chunks **exactly like a real player** does in vanilla Minecraft:

| Behaviour | How it works |
|---|---|
| **Spiral order** | Chunks are ticketed closest-first (spiral from bot centre outward), matching Paper's chunk-send priority queue |
| **Player-equivalent tickets** | Uses `World.addPluginChunkTicket()` — Paper counts these as real player-level load sources (mobs spawn, redstone ticks, crops grow) |
| **Movement-delta detection** | Ticket set is only recomputed when the bot crosses into a new chunk — zero wasted work for stationary bots |
| **World-border clamping** | Chunks outside the world border are automatically excluded |
| **Configurable radius** | `chunk-loading.radius` (default `6`). Set to `0` to auto-match the server's simulation-distance |
| **Configurable interval** | `chunk-loading.update-interval` (default `20` ticks = 1 s). Lower = more responsive to knockback; higher = less CPU overhead |
| **Instant release** | Tickets are released immediately when a bot is deleted, dies, or the plugin is disabled — no orphaned loaded chunks |
| **Live position** | Uses the live `Mannequin` entity position first; falls back to last recorded `spawnLocation` when the body is absent |

---

## ✦ Bot Session Stats (v1.1.4)

Each `FakePlayer` object now tracks rich session metadata internally:

| Field | Description |
|---|---|
| `getUptime()` | `java.time.Duration` since spawn |
| `getUptimeFormatted()` | Human-readable string: `1h 23m 45s` |
| `getLiveLocation()` | Most accurate current position (prefers live Mannequin body) |
| `getTotalDamageTaken()` | Cumulative damage received this session |
| `getDeathCount()` | Deaths during this session |
| `isAlive()` | `false` while dead (between death and respawn) |
| `hasMovedChunk(cx, cz)` | Fast O(1) movement check used by ChunkLoader |

---

## ✦ Bot Names & Messages

| File | Purpose |
|---|---|
| `bot-names.yml` | Pool of names randomly assigned to bots. Edit freely and run `/fpp reload`. Names must be 1–16 characters, letters/digits/underscore only. |
| `bot-messages.yml` | Pool of chat messages bots randomly send. Supports `{name}` and `{random_player}` placeholders. |

When the name pool is exhausted, FPP generates names automatically (`Bot1234`).  
Names longer than 16 characters in the pool are automatically skipped — the server will never crash from a bad name.

---

## ✦ Bot Display Names

Bot display names (tab list, nametag, join/leave messages) are fully configurable in `config.yml`:

- **Admin bots** — `bot-name.admin-format` with `{bot_name}` placeholder
- **User bots** — `bot-name.user-format` with `{spawner}` and `{num}` placeholders
- **LuckPerms prefix** — when `luckperms.use-prefix: true` and LuckPerms is installed, the default-group prefix is automatically prepended to every bot display name

### Examples

| Config value | In-game result |
|---|---|
| `{bot_name}` | `Steve` |
| `<#0079FF>[bot-{bot_name}]</#0079FF>` | `[bot-Steve]` in blue |
| `<gray>[bot-{spawner}-{num}]</gray>` | `[bot-El_Pepes-1]` in gray |
| LuckPerms `§7` prefix + `{bot_name}` | `§7Steve` |

---

## ✦ Bot Swap / Rotation System

When `swap.enabled: true`, each bot automatically leaves and rejoins with a fresh name after a configurable session length, creating organic-looking server activity:

- **Personality archetypes** — VISITOR (short stay), REGULAR (normal), LURKER (long stay)
- **Session growth** — bots that have swapped many times gradually stay longer
- **Time-of-day bias** — longer sessions during peak evening hours, shorter overnight
- **Farewell & greeting chat** — optional messages before leaving / after rejoining
- **Reconnect simulation** — configurable probability the bot rejoins with the same name
- **AFK-kick simulation** — small chance of an extended rejoin gap

---

## ✦ Skin Modes

| Mode | Description |
|---|---|
| `auto` *(default)* | `Mannequin.setProfile(name)` — Paper + client resolve the real skin automatically. Requires online-mode. |
| `fetch` | Plugin fetches texture value + signature from Mojang API asynchronously. Cached per session. Works on offline-mode servers. |
| `disabled` | No skin applied — bots use the default Steve/Alex appearance. |

---

## ✦ Database

FPP records every bot session for auditing and analytics:

| Field | Description |
|---|---|
| `bot_name` | Internal Minecraft name of the bot |
| `bot_uuid` | UUID assigned to the bot |
| `spawned_by` | Player who ran `/fpp spawn` |
| `world_name` | World where the bot was spawned |
| `spawn_x/Y/Z` | Spawn coordinates |
| `last_x/Y/Z` | Last known position (updated every `location-flush-interval` seconds) |
| `spawned_at` | Timestamp of spawn |
| `removed_at` | Timestamp of removal |
| `remove_reason` | `DELETED`, `DIED`, `SHUTDOWN`, or `SWAP` |

Use `/fpp info` to query the database in-game.

**Backends:**
1. **MySQL** — set `database.mysql-enabled: true` and fill in connection details
2. **SQLite** — automatic local fallback; stored at `plugins/FakePlayerPlugin/data/fpp.db`

---

## ✦ Language

All player-facing text lives in `plugins/FakePlayerPlugin/language/en.yml`.  
Edit and run `/fpp reload` — no restart required.  
Colours use **MiniMessage** format: `<#0079FF>text</#0079FF>`, `<gray>`, `<bold>`, etc.

The file is split into two sections:

| Section | Notes |
|---|---|
| **INTERNAL LAYOUT STRINGS** | Structural keys (`divider`, `help-entry`, `info-screen-header`, `modrinth-label`). Safe to restyle; do not rename or remove. |
| **PLAYER-FACING MESSAGES** | All error, command feedback, broadcast, and status messages. Safe to edit freely. |

---

## ✦ LuckPerms Integration

FPP auto-detects LuckPerms at startup. When installed and `luckperms.use-prefix: true`:

- The **default group's prefix** (e.g. `§7`) is prepended to every bot's display name in the tab list, nametag, and join/leave messages
- Makes bots blend in naturally with real players who share the same prefix
- Disable with `luckperms.use-prefix: false` to use only the `bot-name.*` format colours

---

## ✦ Changelog

### v1.1.4 *(2026-03-12)*

#### Chunk Loading — Complete Rewrite
- **Spiral ticket order** — chunks are added from centre outward, matching Paper's chunk-send priority queue so nearby chunks are always loaded first
- **Movement-delta detection** — ChunkLoader tracks each bot's last chunk position; the full spiral recomputation is skipped entirely when the bot hasn't crossed a chunk boundary — zero CPU waste for stationary bots
- **World-border clamping** — chunks outside the configured world border are automatically excluded from ticket sets in both the spiral builder and the boundary checker
- **`update-interval` config** — new `chunk-loading.update-interval` key (default `20` ticks) controls how often the loader polls bot positions; tune lower for heavily knocked-around bots, higher for static lobby bots
- **Auto-radius** — setting `chunk-loading.radius: 0` now reads the server's live `Bukkit.getSimulationDistance()` and uses that as the radius
- **Instant release on removal** — `releaseForBot()` removes all tickets immediately when a bot is deleted, killed, or the plugin disables; no orphaned loaded chunks
- **`totalTickets()` diagnostic** — new public method returns total plugin chunk tickets held across all bots for `/fpp info` display

#### FakePlayer Model Enhancements
- **`getLiveLocation()`** — returns Mannequin body position when alive, falls back to `spawnLocation`; used by DB flush and ChunkLoader for maximum accuracy
- **`getUptime()` / `getUptimeFormatted()`** — `Duration`-based uptime, formatted as `1h 23m 45s`
- **Session stats** — `totalDamageTaken`, `deathCount`, `isAlive()`, `tabRefreshCount` tracked per session
- **Chunk tracking helpers** — `hasMovedChunk(cx, cz)`, `setLastChunk(cx, cz)` for O(1) movement detection without Location allocation

#### Config & DB
- DB location flush uses `getLiveLocation()` instead of raw body reference — handles body-less bots (tab-only mode) correctly
- `Config.chunkLoadingUpdateInterval()` accessor added

### v1.0.15 *(2026-03-11)*
- Join/leave messages broadcast correctly to all players
- LuckPerms prefix pipeline fixed — no more legacy `§`-code parse failures
- Config cleaned up — removed `admin-prefix`/`user-prefix`, added `luckperms.use-prefix` toggle
- Modrinth plugin page link replaces GitHub link in `/fpp` info screen (clickable, label only — no raw URL shown)
- `en.yml` reorganised into INTERNAL / PLAYER-FACING sections with per-entry placeholder docs

### v1.0.0-rc1 *(2026-03-08)*
- First stable release candidate
- Full permission system with `fpp.user.*`, `fpp.bot.<num>` limit nodes, LuckPerms display name support
- User-tier commands: `/fpp spawn`, `/fpp tph`, `/fpp info` (own bots only)
- Bot persistence across server restarts (leave on shutdown, rejoin at last position on startup)
- O(1) entity lookup via entity-id index in `FakePlayerManager`
- Reflection hot-path caching in `PacketHelper`
- Reservoir-sampling name picker — no full candidate list allocation per spawn

### v0.1.5
- Bot swap / rotation system with personality archetypes, time-of-day bias, AFK-kick simulation
- MySQL + SQLite database backend for full session tracking
- `/fpp info` database query command
- `bot-messages.yml` fake-chat message pool (1 000 messages)
- Staggered join/leave delays for realistic server activity

### v0.1.0
- Initial release: tab list, join/leave messages, Mannequin body, head AI, collision/push system

---

## ✦ License

© 2026 Bill_Hub — All Rights Reserved.  
See [LICENSE](https://github.com/Pepe-tf/Fake-Player-Plugin-Public-/blob/main/LICENSE) for full terms.  
Contact: [Discord](https://discord.gg/ZhsstSJb) — `Bill_Hub`

---

*Built for Paper 1.21.x · Java 21 · FPP v1.1.4 · [Modrinth](https://modrinth.com/plugin/fake-player-plugin-(fpp))*