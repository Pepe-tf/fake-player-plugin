# ⌨️ Commands

> **Complete FPP Command Reference - v1.6.0**  
> All commands use `/fpp` · aliases `/fakeplayer` and `/fp`  
> Tab-completion available for all commands and parameters

---

## 📋 Command Overview

| Command | Permission | Description |
|---------|-----------|-------------|
| [`/fpp help`](#-fpp-help) | *(everyone)* | Interactive GUI help - paginated, permission-filtered |
| [`/fpp spawn`](#-fpp-spawn) | `fpp.user.spawn` / `fpp.spawn` | Spawn fake player(s) |
| [`/fpp despawn`](#-fpp-despawn) | `fpp.delete` | Remove bot(s) |
| [`/fpp list`](#-fpp-list) | `fpp.list` | List all active bots |
| [`/fpp info`](#-fpp-info) | `fpp.info` / `fpp.info.user` | Bot details and session history |
| [`/fpp inventory`](#-fpp-inventory) | `fpp.inventory` | Open bot's 54-slot inventory GUI |
| [`/fpp move`](#-fpp-move) | `fpp.move` | Navigate bot to player with A* pathfinding |
| [`/fpp xp`](#-fpp-xp) | `fpp.user.xp` | Transfer bot XP to yourself |
| [`/fpp cmd`](#-fpp-cmd) | `fpp.cmd` | Execute or store commands on a bot |
| [`/fpp mine`](#-fpp-mine) | `fpp.mine` | Continuous or one-shot block mining |
| [`/fpp chat`](#-fpp-chat) | `fpp.chat` | Control fake chat globally or per-bot |
| [`/fpp freeze`](#-fpp-freeze) | `fpp.freeze` | Freeze/unfreeze bot movement |
| [`/fpp swap`](#-fpp-swap) | `fpp.swap` | Toggle / manage bot swap/rotation |
| [`/fpp peaks`](#-fpp-peaks) | `fpp.peaks` | Time-based bot pool scheduler |
| [`/fpp tp`](#-fpp-tp) | `fpp.tp` | Teleport to bot location |
| [`/fpp tph`](#-fpp-tph) | `fpp.user.tph` / `fpp.tph` | Teleport bot(s) to you |
| [`/fpp rank`](#-fpp-rank) | `fpp.rank` | Set bot LuckPerms group |
| [`/fpp settings`](#-fpp-settings) | `fpp.settings` | Open in-game settings GUI |
| [`/fpp reload`](#-fpp-reload) | `fpp.reload` | Hot-reload configurations |
| [`/fpp migrate`](#-fpp-migrate) | `fpp.migrate` | Data migration and backups |
| [`/fpp stats`](#-fpp-stats) | `fpp.stats` | Server statistics |
| [`/fpp lpinfo`](#-fpp-lpinfo) | `fpp.lpinfo` | LuckPerms integration info |
| [`/fpp alert`](#-fpp-alert) | `fpp.alert` | Broadcast admin message (proxy) |
| [`/fpp sync`](#-fpp-sync) | `fpp.sync` | Config push/pull across proxy |

---

## 📖 Detailed Command Reference

### 🆘 `/fpp help`

```bash
/fpp help [page]
```

**Description:** Opens an interactive **54-slot double-chest GUI** - paginated, permission-filtered.  
Console senders receive text output instead.

**Features:**
- Dynamically filters to only commands you can use
- Each command has a semantic Material icon, description, usage modes, and permission node
- Up to 45 commands per page; ◄/▶ navigation arrows; ✕ close button

**Examples:**
```bash
/fpp help          # Open GUI (or page 1 for console)
/fpp help 2        # Jump to page 2 (console only)
```

---

### 🎭 `/fpp spawn`

```bash
/fpp spawn [amount] [--name <name>] [--skin <skin>] [--group <group>]
/fpp spawn [amount] [world] [x y z] [--name <name>]
```

**Description:** Spawn one or more fake players at your location or specific coordinates.

**Parameters:**
- `amount` - Number of bots to spawn (default: 1)
- `--name <name>` - Specific bot name
- `--skin <skin>` - Skin to apply
- `--group <group>` - LuckPerms group to assign
- `world x y z` - Spawn at explicit coordinates (admin only)

**Permissions:**
- `fpp.user.spawn` - Spawn personal bots (limited by `fpp.spawn.limit.<num>`)
- `fpp.spawn` - Spawn unlimited admin bots

**Examples:**
```bash
/fpp spawn                               # 1 random-name bot at your feet
/fpp spawn 5                             # 5 random-name bots
/fpp spawn --name Steve                  # Named "Steve"
/fpp spawn --name Admin --group staff    # With LP group
/fpp spawn world 100 64 200              # Spawn at coordinates
```

**Bot Limits:**  
Grant `fpp.spawn.limit.<N>` (1-100) to set user-tier caps. FPP picks the highest node the player has.  
Bypass global cap: `fpp.bypass.maxbots` · Bypass cooldown: `fpp.bypass.cooldown`

---

### 🗑️ `/fpp despawn`

```bash
/fpp despawn <name|all|random [n]>
```

**Aliases:** `/fpp delete`, `/fpp remove`

**Description:** Remove fake players from the server.

**Examples:**
```bash
/fpp despawn Steve        # Remove Steve
/fpp despawn all          # Remove all bots
/fpp despawn random 3     # Remove 3 random bots
```

**Permission:** `fpp.delete`

---

### 📋 `/fpp list`

```bash
/fpp list
```

**Description:** List all active bots with uptime, world, and status.  
In **NETWORK mode** remote bots from other proxy servers appear in a "Remote Bots" section with their server-id tag.

**Permission:** `fpp.list`

---

### ℹ️ `/fpp info`

```bash
/fpp info bot <name>
/fpp info spawner <name>
```

**Description:** Query the session database. `bot` returns sessions for a bot name; `spawner` returns sessions for a player who spawned bots.

**Permissions:** `fpp.info` (any bot) · `fpp.info.user` (own bots only, user-tier)

---

### 📦 `/fpp inventory`

```bash
/fpp inventory <bot>
/fpp inv <bot>
```

**Description:** Opens the bot's full **54-slot double-chest inventory GUI**.

**Layout:**
| Row | Contents |
|-----|----------|
| 1-3 | Main inventory storage (slots 0-26) |
| 4 | Hotbar (slots 27-35) |
| 5 | Label bar |
| 6 | Helmet · Chestplate/Elytra · Leggings · Boots · Offhand |

Equipment slots enforce type restrictions. Items can be moved freely.

**Shortcut:** Right-clicking a bot entity in the world opens this GUI directly.  
> If the bot has a stored right-click command (set via `/fpp cmd --add`), right-clicking runs that command instead of opening this GUI.

**Permission:** `fpp.inventory`

---

### 🧭 `/fpp move`

```bash
/fpp move <bot> <player>
```

**Description:** Navigate a bot to an online player using **server-side A* pathfinding**.

**Move types supported:**
| Type | Description |
|------|-------------|
| WALK | Standard movement on flat terrain |
| ASCEND | Step up one block |
| DESCEND | Step down safely |
| PARKOUR | Jump across a gap (requires `pathfinding.parkour: true`) |
| BREAK | Break an obstructing block (requires `pathfinding.break-blocks: true`) |
| PLACE | Bridge a gap (requires `pathfinding.place-blocks: true`) |

**Behaviour:**
- Path recalculates when target moves >3.5 blocks or every 60 ticks
- Stuck detection: 8 ticks without movement triggers jump + recalculation
- Max range: 64 blocks · Max search nodes: 2 000 (4 000 with advanced options)
- Navigation stops when bot or target goes offline

**Config** (`config.yml`):
```yaml
pathfinding:
  parkour: false          # Enable gap jumps
  break-blocks: false     # Break obstructing blocks
  place-blocks: false     # Place blocks to bridge gaps
  place-material: DIRT    # Material used for bridging
```

**Permission:** `fpp.move`

---

### ⭐ `/fpp xp`

```bash
/fpp xp <bot>
```

**Description:** Transfer all of a bot's XP (levels + progress bar) to yourself. The bot's XP is cleared immediately.

**Details:**
- Imposes a 30-second cooldown on the bot's XP orb pickup after collection
- `body.pick-up-xp: false` in config disables all bot XP pickup globally

**Permission:** `fpp.user.xp` (user-tier, included in `fpp.use`)

---

### 💻 `/fpp cmd`

```bash
/fpp cmd <bot> <command...>           # Execute a command as the bot
/fpp cmd <bot> --add <command...>     # Store a right-click command
/fpp cmd <bot> --clear                # Remove the stored command
/fpp cmd <bot> --show                 # Display the stored command
```

**Description:** Dispatch a command as a bot, or manage its stored right-click command.

**Details:**
- Uses `Bukkit.dispatchCommand()` - bypasses `PlayerCommandPreprocessEvent`, so `BotCommandBlocker` does not interfere
- Right-clicking a bot with a stored command runs it instead of opening the inventory GUI

**Permission:** `fpp.cmd`

**Examples:**
```bash
/fpp cmd Steve give @p diamond 1          # Steve gives you a diamond immediately
/fpp cmd Steve --add give @p emerald 1    # Right-clicking Steve now gives emeralds
/fpp cmd Steve --show                     # Display stored command
/fpp cmd Steve --clear                    # Remove stored command
```

---

### ⛏️ `/fpp mine`

```bash
/fpp mine <bot>           # Start continuous mining
/fpp mine <bot> once      # Mine one block then stop
/fpp mine <bot> stop      # Stop mining this bot
/fpp mine stop            # Stop all mining bots
```

**Description:** The bot mines the block it is looking at.

| Mode | Behaviour |
|------|-----------|
| Creative | Instant break with 5-tick cooldown between blocks |
| Survival | Progressive mining with `destroyBlockProgress` packets; speed matches standard tool rates |

Mining tasks auto-cancel when the bot goes offline. Runs as a 1-tick repeating task per active miner.

**Permission:** `fpp.mine`

---

### 💬 `/fpp chat`

```bash
/fpp chat [on|off|status]
/fpp chat <bot> [on|off|say <msg>|tier <tier>|mute [seconds]|info]
/fpp chat all <on|off|tier <tier>|mute [seconds]>
```

**Description:** Control the fake chat system globally or per-bot. No arguments **toggles** the global state.

**Chat tiers:** `quiet` · `passive` · `normal` · `active` · `chatty` · `reset` (random)

**Permission:** `fpp.chat`

**Examples:**
```bash
/fpp chat                            # Toggle on/off
/fpp chat on
/fpp chat Steve tier chatty          # Steve chats very frequently
/fpp chat Steve mute 60              # Silence Steve for 60 seconds
/fpp chat Steve say hello everyone   # Force Steve to say something now
/fpp chat all off                    # Silence all bots
```

---

### ❄️ `/fpp freeze`

```bash
/fpp freeze <name|all> [on|off|toggle]
```

**Description:** Freeze or unfreeze bot movement and AI. Frozen bots are shown with ❄ in `/fpp list` and `/fpp stats`.

**Permission:** `fpp.freeze`

---

### 🔄 `/fpp swap`

```bash
/fpp swap [on|off|status|now <bot>|list|info <bot>]
```

**Description:** Manage the bot session-rotation system. No arguments **toggles** the current state.

| Subcommand | Description |
|-----------|-------------|
| `on` / `off` | Enable or disable swap |
| `status` | Active sessions, offline-waiting count, min-online floor, next swap time |
| `now <bot>` | Immediately swap out a specific bot |
| `list` | List bots with scheduled sessions + time remaining |
| `info <bot>` | Personality, cycle count, time until next leave |

**Permission:** `fpp.swap`

---

### ⏰ `/fpp peaks`

```bash
/fpp peaks [on|off|status|next|force|list|wake [name]|sleep <name>]
```

**Description:** Time-window bot pool scheduler. Scales the bot pool up/down based on configured time windows. Requires `swap.enabled: true`.

| Subcommand | Description |
|-----------|-------------|
| `status` | Current window, active fraction, sleeping count |
| `next` | Time until next transition + its fraction |
| `force` | Immediately apply current window's fraction |
| `list` | Show all sleeping bots with saved locations |
| `wake [name]` | Wake a specific sleeping bot (or all) |
| `sleep <name>` | Manually put a bot to sleep |

**Permission:** `fpp.peaks`

---

### 📍 `/fpp tp`

```bash
/fpp tp <name>
```

**Description:** Teleport yourself to a bot's current location.

**Permission:** `fpp.tp`

---

### 🏠 `/fpp tph`

```bash
/fpp tph [name|all|@mine]
```

**Description:** Teleport bot(s) to your current location.

**Permissions:** `fpp.user.tph` (own bots) · `fpp.tph` (any bot)

---

### 👑 `/fpp rank`

```bash
/fpp rank <bot> <group|clear>
/fpp rank random <group> [num|all]
/fpp rank list
```

**Description:** Assign LuckPerms groups to bots at runtime - no respawn needed.

**Permission:** `fpp.rank`

**Examples:**
```bash
/fpp rank Steve admin         # Assign admin group to Steve
/fpp rank Steve clear         # Reset to default group
/fpp rank random vip 3        # Give 3 random bots the vip group
/fpp rank list                # Show all bots' current LP groups
```

---

### ⚙️ `/fpp settings`

```bash
/fpp settings
```

**Description:** Opens the **in-game settings GUI** - a 6-row chest with 7 category tabs.

**Categories:** General · Body · Chat · Swap · Peak Hours · PvP · Pathfinding

- Toggle items flip boolean config values on click
- Numeric/double items close chest and prompt for chat input (60-second timeout)
- Reset button resets current category page to JAR defaults
- All changes apply live without `/fpp reload`

**Permission:** `fpp.settings`

---

### 🔃 `/fpp reload`

```bash
/fpp reload
```

**Description:** Hot-reload all configuration without restart.

**Reloads:** config.yml · language files · bot name pool · bot message pool · skin repository · LuckPerms display names · subsystem states (chat loops, swap sessions, peak hours)

**Permission:** `fpp.reload`

---

### 🔧 `/fpp migrate`

```bash
/fpp migrate status
/fpp migrate backup
/fpp migrate backups
/fpp migrate config
/fpp migrate lang|names|messages
/fpp migrate db export [file]
/fpp migrate db merge <file>
/fpp migrate db tomysql
```

**Description:** Data migration, YAML sync, backup, and export utilities.

| Subcommand | Description |
|-----------|-------------|
| `status` | Config version, file sync health, DB schema, backup count |
| `backup` | Create a manual timestamped backup of all plugin data |
| `config` | Re-run the full config migration chain |
| `lang` / `names` / `messages` | Force-sync the YAML file from the bundled JAR |
| `db export [file]` | Export session data to CSV |
| `db merge <file>` | Merge an external database file |
| `db tomysql` | Migrate SQLite data to MySQL |

**Permission:** `fpp.migrate`

---

### 📊 `/fpp stats`

```bash
/fpp stats
```

**Description:** Live statistics panel - bot count, frozen count, TPS, database totals, memory usage, config status.

**Permission:** `fpp.stats`

---

### 🔗 `/fpp lpinfo`

```bash
/fpp lpinfo [bot-name]
```

**Description:** LuckPerms integration diagnostics - integration status, bot group configuration, weight, prefix/suffix data, tab-list integration.

**Permission:** `fpp.lpinfo`

---

### 📣 `/fpp alert`

```bash
/fpp alert <message>
```

**Description:** Broadcast a formatted admin message to all servers on the proxy network (requires NETWORK mode).

**Permission:** `fpp.alert`

---

### 🔗 `/fpp sync`

```bash
/fpp sync push [file]
/fpp sync pull [file]
/fpp sync status [file]
/fpp sync check [file]
```

**Description:** Push or pull config files across the proxy network (requires NETWORK mode + MySQL).

**Synced files:** `config.yml` · `bot-names.yml` · `bot-messages.yml` · `language/en.yml`  
**Never synced:** `database.server-id` · `database.mysql.*` · `debug`

**Permission:** `fpp.sync`

---

## 🎮 Usage Patterns

### 🚀 Quick Start
```bash
/fpp spawn 5             # Spawn 5 bots
/fpp list                # Verify they appeared
/fpp chat on             # Enable chat messages
/fpp despawn all         # Clean up
```

### 👤 Personal Bot (User)
```bash
/fpp spawn --name MyBot  # Spawn personal bot
/fpp tph MyBot           # Bring to you
/fpp xp MyBot            # Collect its XP
/fpp despawn MyBot       # Remove it
```

### 🔧 Admin Interaction Tools (v1.6.0)
```bash
/fpp inventory Steve          # Inspect Steve's inventory
/fpp move Steve Notch         # Navigate Steve to Notch
/fpp mine Steve               # Start Steve mining
/fpp cmd Steve --add give @p diamond 1   # Set Steve's right-click command
```

### 👑 Server Management
```bash
/fpp spawn 20             # Populate server
/fpp swap on              # Enable auto-rotation
/fpp chat on              # Enable realistic chat
/fpp peaks on             # Enable time-window scheduling
/fpp stats                # Monitor performance
```

---

## 🔐 Permission Quick Reference

```yaml
# Admin wildcard (default: op)
fpp.op → grants all admin commands

# User wildcard (default: true for all players)
fpp.use → grants:
  fpp.user.spawn     # spawn own bot
  fpp.user.tph       # tph own bot
  fpp.user.xp        # collect bot XP (new in v1.6.0)
  fpp.info.user      # view own bot info
  fpp.spawn.limit.1  # 1-bot personal limit

# Personal bot limits (FPP picks the highest node)
fpp.spawn.limit.1 / 2 / 3 / 5 / 10 / 15 / 20 / 50 / 100

# New in v1.6.0
fpp.inventory   # bot inventory GUI
fpp.move        # A* pathfinding navigation
fpp.cmd         # execute / store commands on bots
fpp.mine        # bot block mining
```

For the full permission node list, see [Permissions](Permissions.md).

---

**🎉 Master these commands and become an FPP expert!**

For configuration options see [Configuration](Configuration.md), for permission setup see [Permissions](Permissions.md).

