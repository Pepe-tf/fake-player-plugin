# ꜰᴀᴋᴇ ᴘʟᴀʏᴇʀ ᴘʟᴜɢɪɴ (FPP)

> Spawn realistic fake players on your Paper server — complete with tab list, server list count, join/leave/kill messages, staggered join/leave delays, in-world physics bodies, skin support, bot swap/rotation, fake chat, session database tracking, and full hot-reload configuration.

![Version](https://img.shields.io/badge/version-1.0.0-0079FF?style=flat-square)
![MC](https://img.shields.io/badge/Minecraft-1.21.x-0079FF?style=flat-square)
![Platform](https://img.shields.io/badge/platform-Paper-0079FF?style=flat-square)
![Java](https://img.shields.io/badge/Java-21-0079FF?style=flat-square)

---

## ✦ Features

| Feature | Description |
|---|---|
| **Tab list** | Fake players appear in the tab list for all online and future players |
| **Server list count** | Fake players increment the online player count shown on the server list |
| **Join / leave messages** | Vanilla-style join and leave messages broadcast automatically |
| **Kill messages** | Broadcast when a real player kills a bot (toggleable) |
| **In-world physics body** | Bots spawn as Mannequin entities — pushable, damageable, solid (toggleable) |
| **Custom nametag** | Invisible ArmorStand riding the Mannequin displays the bot's name in white |
| **Head AI** | Bot body faces the nearest player within range using smooth interpolation |
| **Staggered join / leave** | Each bot joins and leaves with a random delay so it looks like real players |
| **Skin support** | `auto` mode — Paper resolves the real Mojang skin from the bot's name automatically |
| **Default skin** | When skins are disabled, bots receive the default Steve/Alex skin |
| **Death & respawn** | Bots can respawn at their last location after dying, or leave permanently |
| **Combat** | Bots take damage with player hurt sounds; cannot target or attack players |
| **Chunk loading** | Bots keep chunks loaded around them like a real player (toggleable) |
| **Bot swap / rotation** | Bots automatically leave and rejoin with a new name, with personality archetypes, time-of-day bias, farewell/greeting chat, and AFK-kick simulation |
| **Fake chat** | Bots send random chat messages from `bot-messages.yml` (toggleable, hot-reloadable) |
| **Uptime tracking** | `/fpp list` shows each bot's name, uptime, location, and who spawned it |
| **Database** | All bot sessions (who spawned, where, when, removal reason) stored in SQLite or MySQL |
| **Persistence** | Active bots survive server restarts — they leave on shutdown and rejoin on startup |
| **Dynamic help** | Help command auto-discovers all registered sub-commands — no manual updates needed |
| **Clickable pagination** | Help pages have clickable prev/next buttons |
| **Hex colour + small-caps** | Styled with `#0079FF` accent and Unicode small-caps throughout |
| **Fully translatable** | All messages in `language/en.yml` |
| **Hot reload** | `/fpp reload` reloads config, language, name pool, and message pool instantly |
| **Bot name pool** | Names loaded from `bot-names.yml` — falls back to `BotXXXX` when pool is exhausted |
| **User-tier commands** | Non-admins can spawn their own limited bots and teleport them with `fpp.user.*` |
| **Permission-based limits** | Per-player bot limits via `fpp.bot.<num>` permission nodes |

---

## ✦ Requirements

- **Paper** 1.21.x (tested on 1.21.11)
- **Java** 21+
- No external runtime dependencies (SQLite JDBC and MySQL connector are shaded in)

---

## ✦ Installation

1. Download `fpp.jar` from the releases page.
2. Place it in your server's `plugins/` folder.
3. Restart the server.
4. Edit `plugins/FakePlayerPlugin/config.yml`, `language/en.yml`, `bot-names.yml`, and `bot-messages.yml` as desired.
5. Run `/fpp reload` to apply any changes without restarting.

---

## ✦ Commands

All sub-commands are under `/fpp` (aliases: `/fakeplayer`, `/fp`).

### Admin Commands

| Command | Permission | Description |
|---|---|---|
| `/fpp help [page]` | `fpp.help` | Paginated help menu with clickable prev/next navigation |
| `/fpp spawn [amount] [--name <name>]` | `fpp.spawn` | Spawn fake player(s) at your location with optional custom name |
| `/fpp delete <name\|all>` | `fpp.delete` | Delete a bot by name (or all bots) with a staggered leave message |
| `/fpp list` | `fpp.list` | List all active bots with name, uptime, location, and spawner |
| `/fpp chat [on\|off\|status]` | `fpp.chat` | Toggle or check the bot fake-chat system |
| `/fpp swap [on\|off\|status]` | `fpp.swap` | Toggle or check the bot swap/rotation system |
| `/fpp reload` | `fpp.reload` | Reload config, language file, name pool, and message pool |
| `/fpp info` | `fpp.info` | Show total session count and active bots from the database |
| `/fpp info bot <name>` | `fpp.info` | Show live status + spawn history for a specific bot name |
| `/fpp info spawner <name>` | `fpp.info` | Show all bots ever spawned by a specific player |
| `/fpp tp [botname]` | `fpp.tp` | Teleport yourself to any active bot |

### User Commands

| Command | Permission | Description |
|---|---|---|
| `/fpp spawn` | `fpp.user.spawn` | Spawn your personal bot (limited by `fpp.bot.<num>`, default 1) |
| `/fpp tph [botname]` | `fpp.user.tph` | Teleport your own bot(s) to you |
| `/fpp info [botname]` | `fpp.user.info` | View world, coordinates, and uptime of your own bots |

### Examples

```
/fpp spawn              — spawns 1 fake player at your position
/fpp spawn 10           — spawns 10 fake players with staggered join delays
/fpp spawn --name Steve — spawns 1 bot named "Steve"
/fpp delete Steve       — removes the bot named Steve with a leave message
/fpp delete all         — removes all bots with staggered leave delays
/fpp list               — shows all active bots with uptime and location
/fpp chat               — shows current fake-chat status
/fpp chat on            — enables fake chat
/fpp chat off           — disables fake chat
/fpp swap on            — enables the bot swap/rotation system
/fpp swap off           — disables the bot swap/rotation system
/fpp help               — shows page 1 of the help menu
/fpp help 2             — shows page 2
/fpp reload             — hot-reloads all configuration
/fpp info               — shows database stats (total sessions, active bots)
/fpp info bot Steve     — shows live info + spawn history of the bot named Steve
/fpp info spawner El_Pepes — shows all bots spawned by El_Pepes
/fpp tp Steve           — teleports you to the bot named Steve
/fpp tph                — teleports your bot to you (if you own exactly one)
/fpp tph Steve          — teleports your bot named Steve to you
```

---

## ✦ Permissions

### Admin Permissions

| Permission | Default | Description |
|---|---|---|
| `fpp.*` | `op` | Grants ALL FPP permissions (admin wildcard) |
| `fpp.help` | `true` | View the help menu |
| `fpp.spawn` | `op` | Admin spawn — no bot limit, supports `--name` and multi-spawn |
| `fpp.spawn.multiple` | `op` | Spawn more than one bot at a time |
| `fpp.spawn.name` | `op` | Use `--name` flag to spawn with a custom name |
| `fpp.delete` | `op` | Delete bots by name |
| `fpp.delete.all` | `op` | Delete all bots at once |
| `fpp.list` | `op` | List all active bots |
| `fpp.chat` | `op` | Toggle bot fake-chat on/off |
| `fpp.swap` | `op` | Toggle bot swap/rotation on/off |
| `fpp.reload` | `op` | Reload plugin configuration |
| `fpp.info` | `op` | Full database query for any bot or spawner |
| `fpp.tp` | `op` | Teleport yourself to any bot |
| `fpp.bypass.maxbots` | `op` | Bypass the global `max-bots` cap |

### User Permissions

| Permission | Default | Description |
|---|---|---|
| `fpp.user.*` | `true` | Grants all user-facing commands (everyone by default) |
| `fpp.user.spawn` | `true` | Spawn bots up to the player's personal limit |
| `fpp.user.tph` | `true` | Teleport own bots to yourself |
| `fpp.user.info` | `true` | View own bot location and uptime |

### Bot Limit Nodes

Assign the highest node the player should be allowed. The plugin picks the highest matching `fpp.bot.<num>` the player has.

| Permission | Limit |
|---|---|
| `fpp.bot.1` | 1 bot *(included in `fpp.user.*` by default)* |
| `fpp.bot.2` | 2 bots |
| `fpp.bot.3` | 3 bots |
| `fpp.bot.5` | 5 bots |
| `fpp.bot.10` | 10 bots |
| `fpp.bot.15` | 15 bots |
| `fpp.bot.20` | 20 bots |
| `fpp.bot.50` | 50 bots |
| `fpp.bot.100` | 100 bots |

---

## ✦ Configuration

Located at `plugins/FakePlayerPlugin/config.yml`. Run `/fpp reload` to apply changes without restart.

```yaml
language: en

debug:
  enabled: false

fake-player:
  max-bots: 1000            # 0 = unlimited
  user-bot-limit: 1         # fallback limit for fpp.user.spawn players
  spawn-body: true          # spawn a Mannequin entity as the bot's body
  persist-on-restart: true  # save bots on shutdown and restore on next start

  skin:
    mode: auto              # auto | fetch | disabled
    clear-cache-on-reload: true

  join-delay:
    min: 0                  # ticks (20 = 1 second)
    max: 5

  leave-delay:
    min: 0
    max: 5

  combat:
    max-health: 20.0
    hurt-sound: true

  death:
    respawn-on-death: false  # false = leave permanently on death
    respawn-delay: 60
    suppress-drops: true

  messages:
    join-message: true
    leave-message: true
    kill-message: false

  chunk-loading:
    enabled: true
    radius: 6

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
    session-min: 120        # seconds
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
  mysql:
    enabled: false
    host: "localhost"
    port: 3306
    database: "fpp"
    username: "root"
    password: ""
    use-ssl: false
```

> **SQLite fallback:** When MySQL is disabled or unreachable, FPP automatically uses SQLite stored at `plugins/FakePlayerPlugin/data/fpp.db`.

---

## ✦ Bot Names & Messages

| File | Purpose |
|---|---|
| `bot-names.yml` | Pool of names bots are randomly assigned. Edit freely and run `/fpp reload`. |
| `bot-messages.yml` | Pool of chat messages bots randomly send. Supports `{name}` and `{random_player}` placeholders. |

When the name pool is exhausted FPP falls back to generated names (`Bot1234`).

---

## ✦ Bot Swap / Rotation System

When `fake-player.swap.enabled: true`, each bot automatically leaves and rejoins with a fresh name after a configurable session length. This creates organic server activity:

- **Personality archetypes** — VISITOR (short stay), REGULAR (normal), LURKER (long stay)
- **Session growth** — bots that have swapped many times gradually stay longer
- **Time-of-day bias** — longer sessions in peak evening hours, shorter at night
- **Farewell & greeting chat** — optional messages before leaving / after rejoining
- **Reconnect simulation** — configurable chance the bot rejoins with the same name
- **AFK-kick simulation** — low chance of an extended rejoin gap

---

## ✦ Skin Modes

| Mode | Description |
|---|---|
| `auto` *(default)* | `Mannequin.setProfile(name)` — Paper + client resolve the real skin automatically. Requires online-mode server. |
| `fetch` | Plugin fetches texture value + signature from Mojang API in the background. Cached per session. Works on offline-mode servers. |
| `disabled` | No skin applied — bots use the default Steve/Alex skin. |

---

## ✦ Database

FPP records every bot session in a database for auditing and analytics:

- **Who** spawned the bot and **when**
- **Where** it was spawned (world + coordinates)
- **Last known position** (updated periodically)
- **Removal reason** (`DELETED`, `DIED`, `SHUTDOWN`, `SWAP`)

Use `/fpp info` to query the database in-game.

**Backends (in priority order):**
1. **MySQL** — set `database.mysql.enabled: true` and fill in credentials
2. **SQLite** — automatic local fallback; stored at `plugins/FakePlayerPlugin/data/fpp.db`

---

## ✦ Language

All player-facing text lives in `plugins/FakePlayerPlugin/language/en.yml`.  
Edit it and run `/fpp reload` — no restart needed.  
Colours use MiniMessage format: `<#0079FF>text</#0079FF>`.

---

## ✦ Changelog

### v1.0.0 *(2026-03-08)*
- First stable release
- Full permission system with `fpp.user.*`, `fpp.bot.<num>` limit nodes, and LuckPerms support
- User-tier commands: `/fpp spawn`, `/fpp tph`, `/fpp info` (own bots only)
- Bot persistence across server restarts (leave on shutdown, rejoin on startup at last position)
- O(1) entity lookup via entity-id index (performance)
- Reflection hot-path caching in `PacketHelper` (no per-call method scanning)
- Allocation-free `BotChatAI` sync loop
- Reservoir-sampling name picker (no full candidate list allocation per spawn)
- Config default values aligned with `config.yml`; removed deprecated `showSkin()` method
- Cleaned `config.yml`: removed unused `spawn-count-presets.user` section, fixed all comment inaccuracies

### v0.1.5
- Bot swap / rotation system with personality archetypes, time-of-day bias, AFK-kick simulation
- MySQL + SQLite database backend for session tracking
- `/fpp info` database query command
- `bot-messages.yml` fake chat message pool (1000 messages)
- Staggered join/leave delays for realistic server activity
- Chunk loading around bots like a real player

### v0.1.0
- Initial release: tab list, join/leave messages, Mannequin body, head AI, collision/push system

---

*Built for Paper 1.21.x · Java 21 · FPP v1.0.0*
