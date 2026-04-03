# Configuration

FPP is configured through `plugins/FakePlayerPlugin/config.yml`.  
All changes take effect immediately after running `/fpp reload` — no server restart required.

---

## Sections at a Glance

| Section | Purpose |
|---------|---------|
| [`language`](#language) | Active language file |
| [`debug`](#debug) | Verbose console logging |
| [`bot-name`](#bot-display-names) | Admin/user format templates and tab-list format |
| [`fake-player`](#fake-player) | Core bot behaviour |
| [`fake-player.skin`](#skin) | Skin system mode |
| [`body`](#body) | Physical Mannequin entity, pushable & damageable toggles |
| [`fake-player.persist-on-restart`](#persistence) | Save/restore bots across restarts |
| [`fake-player.join-delay`](#join-delay) | Staggered join timing |
| [`fake-player.leave-delay`](#leave-delay) | Staggered leave timing |
| [`fake-player.combat`](#combat) | Health and hurt sounds |
| [`fake-player.death`](#death--respawn) | Respawn or leave on death |
| [`fake-player.messages`](#messages) | Join/leave/kill message toggles |
| [`fake-player.chunk-loading`](#chunk-loading) | Keep chunks loaded like a real player |
| [`fake-player.head-ai`](#head-ai) | Head-tracking AI |
| [`fake-player.collision`](#collision--push) | Push physics |
| [`fake-player.swap`](#swap-system) | Bot rotation settings |
| [`fake-chat`](#fake-chat) | Bot chat AI + message format |
| [`tab-list`](#tab-list) | Tab-list header/footer and bot visibility toggle |
| [`database`](#database) | SQLite / MySQL storage |

---

## language

```yaml
language: en
```

The filename (without `.yml`) of the language file inside `plugins/FakePlayerPlugin/language/`.  
Default is `en`, which maps to `language/en.yml`.

To add a new language, copy `en.yml`, translate it, and set this value to the new file's name.

---

## debug

```yaml
debug: false  # Legacy master switch - enables all categories below
```

When `true`, FPP prints verbose diagnostic messages to the console — useful for bug reports and development.  
**Disable in production** — it is very noisy.

### Granular Debug Logging *(v1.5.0+)*

For more precise debugging, use per-subsystem toggles instead of the global `debug` flag:

```yaml
logging:
  debug:
    startup: false      # Startup flow, reloads, compatibility, general lifecycle
    nms: false          # Reflection, fake connection setup, NMS internals
    packets: false      # Tab-list/entity packet sending and packet constructor selection
    luckperms: false    # LuckPerms group sync and prefix refresh details
    network: false      # Velocity/Bungee plugin messaging and remote bot sync
    config-sync: false  # Network config push/pull activity and YAML sync internals
    skin: false         # Skin subsystem debugging
    database: false     # Database init, migrations, flushes, and session persistence
```

**Usage Examples:**

| Scenario | Enable |
|----------|--------|
| Bot command blocking verification | `nms: true` |
| Lobby spawn protection verification | `nms: true` |
| Troubleshooting skin loading | `skin: true` |
| Diagnosing LuckPerms prefix issues | `luckperms: true` |
| Debugging proxy network chat | `network: true` |
| Config sync problems | `config-sync: true` |

**Bot Protection Debug Output (v1.5.6+):**

When `nms: true` is enabled, you'll see:

```
[FPP] BotCommandBlocker: blocked command (LOWEST) for BotName: /give BotName diamond_sword
[FPP] BotCommandBlocker: cleared command suggestions for BotName
[FPP] BotSpawnProtection: protecting BotName from teleports for 5 ticks
[FPP] BotSpawnProtection: blocked PLUGIN teleport for BotName from world (100,64,200) to lobby (0,100,0)
[FPP] BotSpawnProtection: removed protection for BotName
```

> **Tip:** Set only the category you need — this keeps logs clean and focused.

---

## fake-player

### max-bots

```yaml
fake-player:
  max-bots: 1000
```

The global maximum number of bots that can be active at any time.  
`0` means unlimited. Players with `fpp.bypass.maxbots` ignore this cap.

### user-bot-limit

```yaml
fake-player:
  user-bot-limit: 1
```

The default personal bot limit for players with `fpp.user.spawn`.  
This is the fallback when the player has no `fpp.bot.<num>` node.  
Override per-player or per-group with the `fpp.bot.<num>` permission nodes.

### spawn-count-presets

```yaml
fake-player:
  spawn-count-presets:
    admin: [ 1, 5, 10, 15, 20 ]
```

Tab-complete suggestions for the amount argument in `/fpp spawn`.  
Admin users see these presets. User-tier players always see only `1`.

---

## Bot Display Names

```yaml
bot-name:
  admin-format: '{bot_name}'
  user-format:  'bot-{spawner}-{num}'
  tab-list-format: '{prefix}{bot_name}{suffix}'
```

| Key | Description |
|-----|-------------|
| `admin-format` | Display-name template for bots spawned by admins (`fpp.spawn`). Placeholder: `{bot_name}` — the name drawn from `bot-names.yml`. |
| `user-format` | Display-name template for bots spawned by non-admin users (`fpp.user.spawn`). Placeholders: `{bot_name}`, `{spawner}` (the player's name), `{num}` (sequential bot index). |
| `tab-list-format` | **Full tab-list / nametag display format.** Applied as the final step after `admin-format` / `user-format` is resolved. |

### `tab-list-format` placeholders

| Placeholder | Value |
|-------------|-------|
| `{prefix}` | LuckPerms group prefix (empty when `luckperms.use-prefix: false`) |
| `{bot_name}` | Resolved bot name after `admin-format` / `user-format` substitution |
| `{suffix}` | LuckPerms group suffix (empty when `luckperms.use-prefix: false`) |
| `%any_papi%` | Any PlaceholderAPI placeholder — evaluated server-wide (null player context) |

**Examples:**

| Format | Result |
|--------|--------|
| `"{prefix}{bot_name}{suffix}"` | Default — prefix + name + suffix |
| `"&8[&7Bot&8] {bot_name}"` | Static gray `[Bot]` label before the name |
| `"{prefix}{bot_name}{suffix} <gray>%server_uptime%"` | Appends server uptime via PAPI |

## Skin

```yaml
fake-player:
  skin:
    mode: auto
    clear-cache-on-reload: true
```

Controls how bots get their Minecraft player skin.

| Mode | Description |
|------|-------------|
| `auto` | *(Recommended)* Calls `Mannequin.setProfile(name)` — Paper and the Minecraft client resolve the correct Mojang skin automatically. Zero HTTP calls. Requires online-mode server. |
| `fetch` | The plugin fetches the texture value and signature from the Mojang API in the background and injects it into the Mannequin. Works on offline-mode servers. Results are cached per session. |
| `disabled` | No skin applied. Bots display the default Steve / Alex skin. |

`clear-cache-on-reload` — when `true` and `mode: fetch`, the skin texture cache is cleared every time `/fpp reload` is run.

## Body

```yaml
body:
  enabled: true
  pushable: true
  damageable: true
```

Controls whether a physical **Mannequin** entity is spawned for each bot and how it interacts with the world.

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `enabled` | boolean | `true` | Spawn a visible Mannequin entity in the world. When `false`, bots appear only in the tab list with join/leave messages — no entity is placed. |
| `pushable` | boolean | `true` | Allow players and other entities to physically push the bot's body. When `false`, the Mannequin is completely immovable — walk-into, hit-knockback, and bot-vs-bot separation forces are all ignored. Hot-reloadable via `/fpp reload`. |
| `damageable` | boolean | `true` | Allow the bot's body to take damage and be killed. When `false`, the Mannequin is invulnerable — hits register the hurt sound (if `combat.hurt-sound: true`) but deal no damage. Hot-reloadable via `/fpp reload`. |

## Persistence

```yaml
fake-player:
  persist-on-restart: true
```

| Value | Effect |
|-------|--------|
| `true` | Active bots leave with a leave message on server shutdown and rejoin automatically after the server restarts. Their last-known position (not spawn point) is restored. |
| `false` | Bots are removed permanently on shutdown. |

---

## Join Delay

```yaml
fake-player:
  join-delay:
    min: 0    # ticks
    max: 5    # ticks (≈0.25 s)
```

When spawning multiple bots, each bot waits a random number of ticks (between `min` and `max`) before appearing.  
`20 ticks = 1 second`. Set both to `0` for instant simultaneous spawning.

This makes batch spawns look like natural player joins rather than a single-frame flood.

---

## Leave Delay

```yaml
fake-player:
  leave-delay:
    min: 0    # ticks
    max: 5    # ticks (≈0.25 s)
```

Same concept as join delay, applied when removing multiple bots (`/fpp delete all`).  
Each bot's leave message and entity removal are staggered by a random delay in this range.

---

## Combat

```yaml
fake-player:
  combat:
    max-health: 20.0
    hurt-sound: true
```

| Option | Description |
|--------|-------------|
| `max-health` | Health bots spawn with. Default player health is `20.0`. |
| `hurt-sound` | Play the `entity.player.hurt` sound when a bot takes damage. |

---

## Death & Respawn

```yaml
fake-player:
  death:
    respawn-on-death: false
    respawn-delay: 60
    suppress-drops: true
```

| Option | Description |
|--------|-------------|
| `respawn-on-death` | `true` → bot respawns at its last known location after dying. `false` → bot leaves the server permanently on death. |
| `respawn-delay` | Ticks to wait before respawning (20 = 1 second). Only used when `respawn-on-death: true`. |
| `suppress-drops` | Prevent item drops when a bot dies. Recommended to keep `true`. |

---

## Messages

```yaml
fake-player:
  messages:
    join-message: true
    leave-message: true
    kill-message: false
```

| Option | Description |
|--------|-------------|
| `join-message` | Broadcast a vanilla-style join message when a bot is spawned. |
| `leave-message` | Broadcast a vanilla-style leave message when a bot is deleted or dies. |
| `kill-message` | Broadcast a kill message when a player kills a bot (e.g. "Steve was slain by El_Pepes"). |

---

## Chunk Loading

```yaml
fake-player:
  chunk-loading:
    enabled: true
    radius: 6
```

| Option | Description |
|--------|-------------|
| `enabled` | Keep chunks loaded around each bot like a real player. Mobs spawn, redstone ticks, and crops grow. |
| `radius` | Chunk radius to keep loaded. Keep at or below your server's `view-distance`. |

---

## Head AI

```yaml
fake-player:
  head-ai:
    look-range: 8.0
    turn-speed: 0.3
```

| Option | Description |
|--------|-------------|
| `look-range` | Radius (blocks) within which a bot rotates its head to face the nearest player. Set to `0` to disable. |
| `turn-speed` | Interpolation factor (0.0–1.0). `1.0` = instant snap. `0.1` = very slow smooth turn. |

---

## Collision / Push

```yaml
fake-player:
  collision:
    walk-radius: 0.85
    walk-strength: 0.22
    max-horizontal-speed: 0.30
    hit-strength: 0.45
    bot-radius: 0.90
    bot-strength: 0.14
```

| Option | Description |
|--------|-------------|
| `walk-radius` | Distance (blocks) at which a player walking into a bot triggers a push. |
| `walk-strength` | Impulse applied when a player walks into a bot. |
| `max-horizontal-speed` | Maximum horizontal speed a bot can reach from any push. |
| `hit-strength` | Impulse when a player punches a bot. |
| `bot-radius` | Radius at which two bots push each other apart. |
| `bot-strength` | Impulse for bot-vs-bot separation. |

---

## Swap System

```yaml
fake-player:
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
```

See [Swap System](Swap-System.md) for a full explanation of every option.

---

## Fake Chat

```yaml
fake-chat:
  enabled: false
  require-player-online: true
  chance: 0.75
  interval:
    min: 5
    max: 10
  chat-format: "&7{bot_name}: {message}"
```

| Key | Description |
|-----|-------------|
| `enabled` | Master toggle for the entire fake-chat system. |
| `require-player-online` | Suppress bot messages when no real players are online. |
| `chance` | Roll probability (0.0–1.0) per interval tick. |
| `interval.min` / `interval.max` | Seconds between each bot's own messages (random range). |
| `chat-format` | **Full chat line format.** Supports MiniMessage tags and legacy `&` codes. |

**`chat-format` placeholders:**

| Placeholder | Value |
|-------------|-------|
| `{prefix}` | LuckPerms group prefix (empty when `luckperms.use-prefix: false`) |
| `{bot_name}` | Bot display name |
| `{suffix}` | LuckPerms group suffix (empty when `luckperms.use-prefix: false`) |
| `{message}` | Text drawn from `bot-messages.yml` |

**`chat-format` examples:**

| Format string | Result |
|---------------|--------|
| `"&7{bot_name}: {message}"` | Gray name, default colon style (default) |
| `"&7{prefix}{bot_name}&7: {message}"` | LP prefix before name |
| `"{prefix}{bot_name}{suffix}&7: {message}"` | Full prefix + suffix wrap |
| `"<gray>{bot_name}</gray>: {message}"` | MiniMessage gray |
| `"<{bot_name}> {message}"` | IRC-style `<BotName> hello` |
| `"<gradient:#ff0000:#0000ff>{bot_name}</gradient>: {message}"` | Gradient name |

See [Fake Chat](Fake-Chat.md) for a full explanation.

---

## Tab List

```yaml
tab-list:
  enabled: true
```

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `enabled` | boolean | `true` | **Whether bots appear as entries in the player tab list.** `true` = bots show in the tab list. `false` = bots are invisible in the tab list; they still count toward the server player count shown in the multiplayer server-list screen. Hot-reloadable via `/fpp reload`. |

## Database

```yaml
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

| Option | Description |
|--------|-------------|
| `mysql.enabled` | `true` to use MySQL. `false` (or if MySQL is unreachable) falls back to SQLite. |
| `host` | MySQL server hostname or IP. |
| `port` | MySQL port (default: 3306). |
| `database` | MySQL database name. |
| `username` | MySQL username. |
| `password` | MySQL password. |
| `use-ssl` | Enable SSL for the MySQL connection. |

When MySQL is disabled or unreachable, FPP automatically uses a local **SQLite** database at:  
`plugins/FakePlayerPlugin/data/fpp.db`

> See [Database](Database.md) for schema details and how to query records.
