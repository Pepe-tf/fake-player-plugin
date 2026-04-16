# FakePlayerPlugin — Command Reference

**Version:** 1.6.5  
**Last Updated:** April 16, 2026

---

## Command Design Convention

All commands follow a single, predictable pattern:

```
/fpp <command> [target(s)] [action] [--flags]
```

| Slot | Purpose | Examples |
|------|---------|---------|
| `command` | Sub-command name | `spawn`, `move`, `mine` |
| `target(s)` | Which bot(s) | `BotName`, `all`, `--count <n>` |
| `action` | Named sub-action | `add`, `remove`, `list` |
| `--flags` | Modifiers / options | `--stop`, `--once`, `--random` |

**Argument notation:**
- `<required>` — must be provided
- `[optional]` — may be omitted
- `|` — pick one alternative
- `...` — one or more values

**Universal flags (where supported):**

| Flag | Meaning |
|------|---------|
| `--stop` | Stop the current task |
| `--once` | Run action once then stop |
| `--count <n>` | Target *n* random bots |
| `--random` | Randomise selection or order |
| `--all` | Target every bot (same as `all`) |

**Deprecated flags** are still accepted but print a warning:

| Deprecated | Replacement |
|-----------|------------|
| `--num <n>` | `--count <n>` |

---

## Table of Contents

- [Core Commands](#core-commands)
- [Bot Control & Movement](#bot-control--movement)
- [Bot Inventory & Actions](#bot-inventory--actions)
- [Chat & AI](#chat--ai)
- [LuckPerms Integration](#luckperms-integration)
- [Network & Proxy](#network--proxy)
- [Quick Reference](#quick-reference)

---

## Core Commands

### `/fpp help`
**Aliases:** None &nbsp;|&nbsp; **Permission:** `fpp.help`

```
/fpp help [page]
```

Opens an interactive GUI (players) or paginated text list (console). Automatically filters to commands you can use.

| Example | Result |
|---------|--------|
| `/fpp help` | Page 1 / opens GUI |
| `/fpp help 2` | Page 2 |

---

### `/fpp spawn`
**Aliases:** None &nbsp;|&nbsp; **Permission:** `fpp.spawn` · `fpp.spawn.user`

```
/fpp spawn [count] [world [x y z]] [--name <name>]
```

Spawns one or more bots at your position or given coordinates.

| Example | Result |
|---------|--------|
| `/fpp spawn` | 1 bot at your location |
| `/fpp spawn 5` | 5 bots at your location |
| `/fpp spawn 10 world 100 64 200` | 10 bots at coordinates |
| `/fpp spawn --name Alice` | Named bot (requires `fpp.spawn.name`) |

---

### `/fpp despawn`
**Aliases:** None &nbsp;|&nbsp; **Permission:** `fpp.delete`

```
/fpp despawn <name>
/fpp despawn all
/fpp despawn --count <n>
/fpp despawn --random [--count <n>]
```

Removes bots by name, count, or randomly. `--num` is accepted but deprecated in favour of `--count`.

| Example | Result |
|---------|--------|
| `/fpp despawn Bot1` | Remove one specific bot |
| `/fpp despawn all` | Remove all bots (`fpp.delete.all`) |
| `/fpp despawn --count 10` | Remove 10 oldest bots |
| `/fpp despawn --random --count 5` | Remove 5 random bots |
| `/fpp despawn --num 3` | ⚠ Deprecated → use `--count` |

> Bulk operations are blocked while bot restoration is in progress on startup.

---

### `/fpp list`
**Aliases:** None &nbsp;|&nbsp; **Permission:** `fpp.list`

```
/fpp list [page]
```

Lists all active bots (local + remote in NETWORK mode).

---

### `/fpp info`
**Aliases:** None &nbsp;|&nbsp; **Permission:** `fpp.info` · `fpp.info.user`

```
/fpp info [bot|spawner] <name>
```

| Example | Result |
|---------|--------|
| `/fpp info Bot1` | Session history for a bot |
| `/fpp info spawner Steve` | All bots spawned by Steve (admin only) |

---

### `/fpp reload`
**Aliases:** None &nbsp;|&nbsp; **Permission:** `fpp.reload`

```
/fpp reload [all|config|lang|chat|ai|skins|secrets|swap]
```

Hot-reloads configuration and subsystems without restart.

| Target | Effect |
|--------|--------|
| *(omit)* or `all` | Full reload |
| `config` | `config.yml` only |
| `lang` | Language file |
| `chat` | Chat messages + settings |
| `ai` / `secrets` | AI conversation system + API keys |
| `skins` | Skin repository |
| `swap` | Bot swap system |

---

### `/fpp stats`
**Aliases:** None &nbsp;|&nbsp; **Permission:** `fpp.stats`

```
/fpp stats
```

Live plugin statistics: active bots, frozen count, uptime, DB metrics, system info.

---

### `/fpp settings`
**Aliases:** None &nbsp;|&nbsp; **Permission:** `fpp.settings`

```
/fpp settings
```

Opens an interactive in-game GUI with 7 categories (General, Body, Chat, Swap, Peak Hours, PvP, Pathfinding).

---

### `/fpp migrate`
**Aliases:** None &nbsp;|&nbsp; **Permission:** `fpp.migrate`

```
/fpp migrate <backup|backups|status|config|lang|names|messages|db>
```

| Sub-command | Effect |
|-------------|--------|
| `backup` | Create manual backup |
| `backups` | List stored backups |
| `status` | Config version, file sync health, DB stats |
| `config` | Re-run config migration chain |
| `lang` | Force-sync `language/en.yml` from JAR |
| `names` | Force-sync `bot-names.yml` |
| `messages` | Force-sync `bot-messages.yml` |
| `db export` | Export database to YAML |
| `db merge <file>` | Import YAML sessions into DB |
| `db tomysql` | Convert SQLite → MySQL |

---

## Bot Control & Movement

### `/fpp tp`
**Aliases:** None &nbsp;|&nbsp; **Permission:** `fpp.tp`

```
/fpp tp [botname]
```

Teleports you to a bot's location.

---

### `/fpp tph`
**Aliases:** None &nbsp;|&nbsp; **Permission:** `fpp.tph`

```
/fpp tph [botname]
```

Teleports a bot (or all your bots) to your location.

---

### `/fpp move`
**Aliases:** None &nbsp;|&nbsp; **Permission:** `fpp.move`

```
/fpp move <bot|all> --to <player>
/fpp move <bot|all> --wp <route> [--random]
/fpp move <bot|all> --stop
```

Navigates a bot to a player or along a named waypoint patrol, using server-side A* pathfinding.

| Example | Result |
|---------|--------|
| `/fpp move Bot1 --to Steve` | Bot follows Steve |
| `/fpp move Bot1 Steve` | *(legacy bare syntax, still works)* |
| `/fpp move all --to Steve` | All bots follow Steve |
| `/fpp move Bot1 --wp patrol` | Sequential waypoint patrol |
| `/fpp move Bot1 --wp patrol --random` | Random-order patrol |
| `/fpp move Bot1 --stop` | Stop navigation |
| `/fpp move all --stop` | Stop all bots |

> Parkour jumps, block breaking/placing available via pathfinding config.

---

### `/fpp freeze`
**Aliases:** None &nbsp;|&nbsp; **Permission:** `fpp.freeze`

```
/fpp freeze <bot|all> [on|off]
```

| Example | Result |
|---------|--------|
| `/fpp freeze Bot1` | Toggle freeze |
| `/fpp freeze Bot1 on` | Freeze |
| `/fpp freeze Bot1 off` | Unfreeze |
| `/fpp freeze all on` | Freeze all bots |

---

### `/fpp rename`
**Aliases:** None &nbsp;|&nbsp; **Permission:** `fpp.rename` · `fpp.rename.own`

```
/fpp rename <oldname> <newname>
```

Renames a bot (preserves inventory, XP, chat settings, LP group). Uses delete+respawn internally.

> `fpp.rename.own` — can only rename bots you personally spawned.

---

### `/fpp ping`
**Aliases:** None &nbsp;|&nbsp; **Permission:** `fpp.ping`

```
/fpp ping <bot> --ping <ms>
/fpp ping <bot> --random
/fpp ping --count <n> --ping <ms>
/fpp ping --count <n> --random
```

Sets simulated network latency (tab-list bars + NMS latency field).

| Example | Result |
|---------|--------|
| `/fpp ping Bot1 --ping 50` | Fixed 50 ms ping |
| `/fpp ping Bot1 --random` | Weighted random (20–300 ms) |
| `/fpp ping --count 5 --ping 30` | 5 bots at 30 ms |
| `/fpp ping --count 10 --random` | 10 bots with random latency |

**Random distribution:** 60 % low (20–100 ms), 25 % medium (100–200 ms), 15 % high (200–300 ms).

---

## Bot Inventory & Actions

### `/fpp inventory` · `/fpp inv`
**Aliases:** `inv` &nbsp;|&nbsp; **Permission:** `fpp.inventory`

```
/fpp inventory <bot>
/fpp inv <bot>
```

Opens a 54-slot GUI (main, hotbar, armor, offhand). Right-clicking the bot entity also opens this without a command.

---

### `/fpp cmd`
**Aliases:** None &nbsp;|&nbsp; **Permission:** `fpp.cmd`

```
/fpp cmd <bot> <command...>
/fpp cmd <bot> --bind <command...>
/fpp cmd <bot> --clear
/fpp cmd <bot> --show
```

| Example | Result |
|---------|--------|
| `/fpp cmd Bot1 say Hello!` | Execute command as bot now |
| `/fpp cmd Bot1 --bind warp shop` | Bind `/warp shop` to right-click |
| `/fpp cmd Bot1 --clear` | Remove right-click binding |
| `/fpp cmd Bot1 --show` | Display bound command |

> Without `--bind`, the command is executed immediately. The old `--add` flag is still accepted as an alias for `--bind`.

---

### `/fpp xp`
**Aliases:** None &nbsp;|&nbsp; **Permission:** `fpp.xp`

```
/fpp xp <bot>
```

Transfers all XP from a bot to you. Clears bot's level/progress. Imposes 30-second pickup cooldown.

---

### `/fpp mine`
**Aliases:** None &nbsp;|&nbsp; **Permission:** `fpp.mine`

```
/fpp mine <bot>
/fpp mine <bot> --once
/fpp mine <bot> --stop
/fpp mine --stop
```

Area-selection mode (when enabled):
```
/fpp mine <bot> --pos1
/fpp mine <bot> --pos2
/fpp mine <bot> --start
/fpp mine <bot> --status
/fpp mine <bot> --clear
```

| Example | Result |
|---------|--------|
| `/fpp mine Bot1` | Continuous mining |
| `/fpp mine Bot1 --once` | Mine one block |
| `/fpp mine Bot1 --stop` | Stop this bot |
| `/fpp mine --stop` | Stop all mining bots |

> Area mode auto-deposits to registered `storage` containers when inventory fills.  
> Legacy `once` and `stop` positional forms still accepted.

---

### `/fpp use`
**Aliases:** None &nbsp;|&nbsp; **Permission:** `fpp.useitem`

```
/fpp use <bot>
/fpp use <bot> --once
/fpp use <bot> --stop
/fpp use --stop
```

Walks bot to your position then continuously right-clicks what it's looking at.

| Example | Result |
|---------|--------|
| `/fpp use Bot1` | Continuous right-click loop |
| `/fpp use Bot1 --once` | Single right-click |
| `/fpp use Bot1 --stop` | Stop |
| `/fpp use --stop` | Stop all |

> Legacy `once` and `stop` positional forms still accepted.

---

### `/fpp place`
**Aliases:** None &nbsp;|&nbsp; **Permission:** `fpp.place`

```
/fpp place <bot>
/fpp place <bot> --once
/fpp place <bot> --stop
/fpp place --stop
```

Bot continuously places blocks it faces — mirror of `/fpp mine` but placing.

| Example | Result |
|---------|--------|
| `/fpp place Bot1` | Continuous placement |
| `/fpp place Bot1 --once` | Place one block |
| `/fpp place Bot1 --stop` | Stop |
| `/fpp place --stop` | Stop all |

> Fetches blocks from registered `storage` containers when inventory empties.  
> Legacy `once` and `stop` positional forms still accepted.

---

### `/fpp storage`
**Aliases:** None &nbsp;|&nbsp; **Permission:** `fpp.storage`

```
/fpp storage <bot> <name>
/fpp storage <bot> --list
/fpp storage <bot> --remove <name>
/fpp storage <bot> --clear
```

Registers supply containers (chest, barrel, hopper, shulker) used by `/fpp mine` and `/fpp place`.

| Example | Result |
|---------|--------|
| `/fpp storage Bot1 chest1` | Register container at crosshair |
| `/fpp storage Bot1 --list` | List registered containers |
| `/fpp storage Bot1 --remove chest1` | Remove one |
| `/fpp storage Bot1 --clear` | Remove all |

---

### `/fpp waypoint` · `/fpp wp`
**Aliases:** `wp` &nbsp;|&nbsp; **Permission:** `fpp.waypoint`

```
/fpp waypoint add <route>
/fpp waypoint create <route>
/fpp waypoint remove <route> <index>
/fpp waypoint delete <route>
/fpp waypoint clear <route>
/fpp waypoint list [route]
```

Manages named patrol routes used by `/fpp move --wp`.

| Example | Result |
|---------|--------|
| `/fpp wp add patrol` | Add your position to route |
| `/fpp wp create tour` | Create empty route |
| `/fpp wp list` | All routes |
| `/fpp wp list patrol` | Waypoints in a route |
| `/fpp wp remove patrol 2` | Remove waypoint #2 |
| `/fpp wp delete patrol` | Delete entire route |
| `/fpp wp clear patrol` | Remove all waypoints from route |

---

## Chat & AI

### `/fpp chat`
**Aliases:** None &nbsp;|&nbsp; **Permission:** `fpp.chat`

```
/fpp chat [on|off|status]
/fpp chat <bot> [on|off|info]
/fpp chat <bot> --mute [<seconds>]
/fpp chat <bot> --tier <level>
/fpp chat <bot> --say <message...>
/fpp chat all <on|off|tier <level>|mute [seconds]>
```

Controls global and per-bot auto-chat. Supports activity tiers, timed mutes, and manual messages.

| Example | Result |
|---------|--------|
| `/fpp chat` | Toggle global on/off |
| `/fpp chat on` | Enable globally |
| `/fpp chat status` | Show global status |
| `/fpp chat Bot1 off` | Silence permanently |
| `/fpp chat Bot1 --mute 60` | Mute 60 seconds |
| `/fpp chat Bot1 --tier chatty` | Increase chat frequency |
| `/fpp chat Bot1 --say Hello!` | Force message now |
| `/fpp chat Bot1 info` | Show per-bot settings |
| `/fpp chat all on` | Enable all bots |
| `/fpp chat all --tier normal` | Set all to normal tier |

**Tiers:** `quiet` (15 %) → `passive` (25 %) → `normal` (30 %) → `active` (18 %) → `chatty` (12 %)

---

### `/fpp personality` · `/fpp persona`
**Aliases:** `persona` &nbsp;|&nbsp; **Permission:** `fpp.personality`

```
/fpp personality list
/fpp personality reload
/fpp personality <bot> set <name>
/fpp personality <bot> reset
/fpp personality <bot> show
```

Assigns AI personality `.txt` files from `plugins/FakePlayerPlugin/personalities/`.

| Example | Result |
|---------|--------|
| `/fpp personality list` | Available personalities |
| `/fpp persona Bot1 set friendly` | Assign personality |
| `/fpp persona Bot1 reset` | Revert to config default |
| `/fpp persona Bot1 show` | Show current |

> Requires `ai-conversations.enabled: true` and a valid API key in `secrets.yml`.

---

### `/fpp badword`
**Aliases:** None &nbsp;|&nbsp; **Permission:** `fpp.badword`

```
/fpp badword check
/fpp badword update
/fpp badword status
```

| Sub-command | Effect |
|-------------|--------|
| `check` | List flagged bots (read-only) |
| `update` | Auto-rename flagged bots with clean names |
| `status` | Filter config, word counts, scan results |

---

## LuckPerms Integration

### `/fpp rank`
**Aliases:** None &nbsp;|&nbsp; **Permission:** `fpp.rank`

```
/fpp rank <bot> --set <group>
/fpp rank <bot> --clear
/fpp rank --random --count <n> --set <group>
/fpp rank list
```

| Example | Result |
|---------|--------|
| `/fpp rank Bot1 --set vip` | Assign VIP group |
| `/fpp rank Bot1 vip` | *(legacy positional, still works)* |
| `/fpp rank Bot1 --clear` | Remove override (use LP default) |
| `/fpp rank --random --count 5 --set vip` | VIP to 5 random bots |
| `/fpp rank list` | List all LP groups |

> Groups persist across restarts. Requires LuckPerms.

---

### `/fpp lpinfo`
**Aliases:** None &nbsp;|&nbsp; **Permission:** `fpp.lpinfo`

```
/fpp lpinfo
```

Displays LP availability, config settings, loaded groups, and active bot group assignments.

---

## Network & Proxy

### `/fpp swap`
**Aliases:** None &nbsp;|&nbsp; **Permission:** `fpp.swap`

```
/fpp swap [on|off|status|now <bot>|list|info <bot>]
```

Controls bot session rotation (disconnect + rejoin pattern).

| Example | Result |
|---------|--------|
| `/fpp swap` | Toggle on/off |
| `/fpp swap now Bot1` | Force immediate swap |
| `/fpp swap list` | Swapped-out bots |
| `/fpp swap info Bot1` | Swap history |

---

### `/fpp peaks`
**Aliases:** None &nbsp;|&nbsp; **Permission:** `fpp.peaks`

```
/fpp peaks [on|off|status|next|force|list]
/fpp peaks wake [name]
/fpp peaks sleep <name>
```

Time-window bot pool scheduler — auto-adjusts online count by time of day.

| Example | Result |
|---------|--------|
| `/fpp peaks status` | Current window + target fraction |
| `/fpp peaks next` | Next window change |
| `/fpp peaks force` | Immediate check |
| `/fpp peaks wake` | Wake all sleeping bots |
| `/fpp peaks sleep Bot1` | Manually sleep a bot |

> Requires `swap.enabled: true`. Configure windows in `peak-hours.schedule`.

---

### `/fpp alert`
**Aliases:** None &nbsp;|&nbsp; **Permission:** `fpp.alert`

```
/fpp alert <message...>
```

Broadcasts a message to all servers in the proxy network (NETWORK mode only).

---

### `/fpp sync`
**Aliases:** None &nbsp;|&nbsp; **Permission:** `fpp.sync`

```
/fpp sync <push|pull|status|check> [file]
```

Syncs config files to/from MySQL across proxy nodes.

| Example | Result |
|---------|--------|
| `/fpp sync push` | Push all configs to DB |
| `/fpp sync pull` | Pull all configs from DB |
| `/fpp sync push config.yml` | Push single file |

**Modes:** `DISABLED` · `MANUAL` · `AUTO_PULL` · `AUTO_PUSH`

> `database.server-id`, `database.mysql.*`, and `debug` are never synced.

---

## Quick Reference

| Command | Aliases | Permission | Description |
|---------|---------|------------|-------------|
| `help` | — | `fpp.help` | Show command help menu |
| `spawn` | — | `fpp.spawn` | Spawn fake player bots |
| `despawn` | — | `fpp.delete` | Remove bots |
| `list` | — | `fpp.list` | List active bots |
| `info` | — | `fpp.info` | Query bot session history |
| `reload` | — | `fpp.reload` | Reload configuration |
| `stats` | — | `fpp.stats` | Display live statistics |
| `settings` | — | `fpp.settings` | Open settings GUI |
| `migrate` | — | `fpp.migrate` | Manage migrations & backups |
| `tp` | — | `fpp.tp` | Teleport to bot |
| `tph` | — | `fpp.tph` | Teleport bot to you |
| `move` | — | `fpp.move` | Navigate bot to player/waypoint |
| `freeze` | — | `fpp.freeze` | Freeze bot in place |
| `rename` | — | `fpp.rename` | Rename active bot |
| `ping` | — | `fpp.ping` | Set bot ping / latency |
| `inventory` | **inv** | `fpp.inventory` | Open bot inventory GUI |
| `cmd` | — | `fpp.cmd` | Execute command as bot / set bind |
| `xp` | — | `fpp.xp` | Collect XP from bot |
| `mine` | — | `fpp.mine` | Bot mining automation |
| `use` | — | `fpp.useitem` | Bot right-click automation |
| `place` | — | `fpp.place` | Bot block placement |
| `storage` | — | `fpp.storage` | Manage bot storage targets |
| `waypoint` | **wp** | `fpp.waypoint` | Manage patrol routes |
| `chat` | — | `fpp.chat` | Control bot auto-chat |
| `personality` | **persona** | `fpp.personality` | Manage AI personalities |
| `badword` | — | `fpp.badword` | Scan / fix bot names |
| `rank` | — | `fpp.rank` | Assign LuckPerms groups |
| `lpinfo` | — | `fpp.lpinfo` | Show LuckPerms status |
| `swap` | — | `fpp.swap` | Toggle bot rotation |
| `peaks` | — | `fpp.peaks` | Manage peak-hours scheduling |
| `alert` | — | `fpp.alert` | Network-wide broadcast |
| `sync` | — | `fpp.sync` | Sync configs across network |

---

## Permission Groups

### Admin (`fpp.op`)
Inherits all commands: unlimited spawning, mass despawn (`fpp.delete.all`), all info, reload, migrate, rank, network, settings.

### User (`fpp.use`)
Basic commands: spawn (1 bot limit by default), tph, xp, user-info.

### Bot Limits
`fpp.spawn.limit.<number>` — e.g. `fpp.spawn.limit.5` allows 5 bots.  
Highest assigned limit wins. Falls back to `userBotLimit` in config when no node matches.

---

## Special Notes

### Right-Click Interactions
- **Left-click / right-click** → opens inventory GUI (default)
- **Shift + right-click** → opens bot settings GUI
- **Right-click + stored bind** → executes bound command (set via `/fpp cmd <bot> --bind`)

### Console vs Player
Player-only commands (require in-game sender):
`settings`, `tph`, `waypoint add/create`, `/fpp use`, `/fpp mine --pos1/--pos2`

Console alternatives:
`help` → text pagination · `spawn` → requires explicit coordinates

### Persistence
Bot state saved on shutdown and restored on startup (`persist-on-restart: true`):
inventory · XP · chat settings · personality · frozen · LP group · right-click bind · per-bot overrides (pickup, head-AI, swim-AI, nav flags, chunk radius) · active tasks (mine, use, place, patrol)

---

## Backward Compatibility

Legacy syntax is still accepted and will continue to work:

| Old syntax | New preferred syntax | Status |
|-----------|---------------------|--------|
| `/fpp despawn --num 5` | `/fpp despawn --count 5` | ⚠ Deprecated warning |
| `/fpp mine Bot1 once` | `/fpp mine Bot1 --once` | ✅ Both work |
| `/fpp mine Bot1 stop` | `/fpp mine Bot1 --stop` | ✅ Both work |
| `/fpp use Bot1 once` | `/fpp use Bot1 --once` | ✅ Both work |
| `/fpp use Bot1 stop` | `/fpp use Bot1 --stop` | ✅ Both work |
| `/fpp place Bot1 once` | `/fpp place Bot1 --once` | ✅ Both work |
| `/fpp place Bot1 stop` | `/fpp place Bot1 --stop` | ✅ Both work |
| `/fpp move Bot1 Steve` | `/fpp move Bot1 --to Steve` | ✅ Both work |
| `/fpp cmd Bot1 --add cmd` | `/fpp cmd Bot1 --bind cmd` | ✅ Both work |
| `/fpp rank Bot1 vip` | `/fpp rank Bot1 --set vip` | ✅ Both work |

---

## Version History

**v1.6.5** (Current)
- Added `/fpp ping` with `--ping`/`--random`/`--count` flags
- Unified `--stop`/`--once` flags across `mine`, `use`, `place`
- Added `--to` flag for `/fpp move`; kept bare-player backward compat
- `--count` replaces `--num` in `/fpp despawn` (deprecated warning kept)
- Added `--bind` alias for `--add` in `/fpp cmd`
- New `FlagParser` utility — shared duplicate/conflict detection
- Fully rewritten `COMMANDS.md`

**v1.6.4**  
Added NameTag plugin support · improved skin system

**v1.6.3**  
Task persistence (mine/use/place/patrol) · bot identity caching · entity ID index

**v1.6.2**  
`/fpp place` · storage system · per-bot navigation overrides

---

**See also:**  
`Configuration.md` · `Permissions.md` · `Database.md` · `Bot-Behaviour.md`


**Version:** 1.6.5  
**Last Updated:** April 16, 2026

---

## Table of Contents

- [Core Commands](#core-commands)
- [Bot Control & Movement](#bot-control--movement)
- [Bot Inventory & Actions](#bot-inventory--actions)
- [Chat & AI](#chat--ai)
- [LuckPerms Integration](#luckperms-integration)
- [Network & Proxy](#network--proxy)
- [Quick Reference Table](#quick-reference-table)

---

## Core Commands

### `/fpp help`
**Aliases:** None  
**Permission:** `fpp.help`  
**Usage:** `/fpp help [page]`

**Description:**  
Opens an interactive GUI (for players) or shows paginated text list (for console) of all available commands. Automatically filters commands based on sender's permissions.

**Use Cases:**
- `/fpp help` — Opens GUI (players) or shows page 1 (console)
- `/fpp help 2` — Opens page 2 of command list
- New players discovering available commands
- Quick permission check (only shows commands you can use)

---

### `/fpp spawn`
**Aliases:** None  
**Permission:** `fpp.spawn` (admin) or `fpp.spawn.user` (user)  
**Usage:** `/fpp spawn [amount] [world [x y z]] [--name <name>]`

**Description:**  
Spawns one or more fake player bots at your location or specified coordinates.

**Use Cases:**
- `/fpp spawn` — Spawn 1 bot at your location
- `/fpp spawn 5` — Spawn 5 bots at your location
- `/fpp spawn 10 world 100 64 200` — Spawn 10 bots at coordinates
- `/fpp spawn --name CustomBot` — Spawn bot with specific name (requires `fpp.spawn.name`)
- `/fpp spawn 3 --name TestBot` — Spawn 3 bots, first one named "TestBot"
- Populate empty servers to attract players
- Stress-test server performance
- Fill minigames with AI players

---

### `/fpp despawn`
**Aliases:** None  
**Permission:** `fpp.delete`  
**Usage:** `/fpp despawn <name|all|--random [num]|--num <num>>`

**Description:**  
Removes active bots by name, randomly, or all at once.

**Use Cases:**
- `/fpp despawn BotName` — Remove specific bot
- `/fpp despawn all` — Remove all bots (requires `fpp.delete.all`)
- `/fpp despawn --random 5` — Remove 5 random bots
- `/fpp despawn --num 10` — Remove 10 oldest bots
- Clear server after stress test
- Remove inactive bots during low-traffic periods
- Cleanup before maintenance

**Note:** Bulk operations (`all`, `--random`, `--num`) are blocked during bot restoration on startup.

---

### `/fpp list`
**Aliases:** None  
**Permission:** `fpp.list`  
**Usage:** `/fpp list [page]`

**Description:**  
Lists all currently active bots (local + remote in NETWORK mode) with pagination.

**Use Cases:**
- `/fpp list` — Show page 1 of active bots
- `/fpp list 3` — Show page 3
- Check which bots are online
- Verify bot names before using other commands
- Monitor bot distribution across proxy network

---

### `/fpp info`
**Aliases:** None  
**Permission:** `fpp.info` (admin) or `fpp.info.user` (user)  
**Usage:** `/fpp info [bot|spawner] <name>`

**Description:**  
Query bot session history from the database. Shows spawn time, location, spawner, uptime, and more.

**Use Cases:**
- `/fpp info BotName` — Show session history for a bot
- `/fpp info spawner PlayerName` — Show all bots spawned by a player (admin only)
- Track bot activity and patterns
- Audit who spawned which bots
- Debug bot persistence issues

---

### `/fpp reload`
**Aliases:** None  
**Permission:** `fpp.reload`  
**Usage:** `/fpp reload [all|config|lang|chat|ai|skins|secrets|swap]`

**Description:**  
Reloads plugin configuration and subsystems without restarting the server.

**Use Cases:**
- `/fpp reload` — Full reload (all subsystems)
- `/fpp reload config` — Reload config.yml only
- `/fpp reload lang` — Reload language files
- `/fpp reload chat` — Reload bot chat messages and settings
- `/fpp reload ai` — Reload AI conversation system and secrets
- `/fpp reload skins` — Reload skin repository
- `/fpp reload swap` — Reload bot swap system
- Apply config changes instantly
- Test new chat messages without restart

---

### `/fpp stats`
**Aliases:** None  
**Permission:** `fpp.stats`  
**Usage:** `/fpp stats`

**Description:**  
Display live plugin statistics including active bots, frozen count, uptime, database metrics, and system info.

**Use Cases:**
- `/fpp stats` — View current plugin state
- Monitor server performance impact
- Check database query statistics
- Verify bot limits and capacity

---

### `/fpp settings`
**Aliases:** None  
**Permission:** `fpp.settings`  
**Usage:** `/fpp settings`

**Description:**  
Opens an interactive in-game settings GUI with 7 categories (General, Body, Chat, Swap, Peak Hours, PvP, Pathfinding). Toggle settings, adjust numbers, and reset to defaults all from the GUI.

**Use Cases:**
- `/fpp settings` — Open settings GUI
- Quick config adjustments without editing YAML
- Test different configurations on-the-fly
- Reset individual categories to defaults

---

### `/fpp migrate`
**Aliases:** None  
**Permission:** `fpp.migrate`  
**Usage:** `/fpp migrate <backup|status|config|lang|names|messages|db>`

**Description:**  
Manages config/data migration, backups, and database operations.

**Use Cases:**
- `/fpp migrate backup` — Create manual backup
- `/fpp migrate status` — Check config version, file sync health, DB stats
- `/fpp migrate config` — Re-run config migration chain
- `/fpp migrate lang` — Force-sync language file from JAR
- `/fpp migrate names` — Force-sync bot-names.yml
- `/fpp migrate messages` — Force-sync bot-messages.yml
- `/fpp migrate db export` — Export database to YAML
- `/fpp migrate db merge <file>` — Import YAML sessions into DB
- `/fpp migrate db tomysql` — Convert SQLite to MySQL
- Recover from config corruption
- Migrate between database backends
- Sync new lang keys to existing files

---

## Bot Control & Movement

### `/fpp tp`
**Aliases:** None  
**Permission:** `fpp.tp`  
**Usage:** `/fpp tp [botname]`

**Description:**  
Teleports you to a bot's current location.

**Use Cases:**
- `/fpp tp BotName` — Teleport to specific bot
- Find lost bots
- Inspect bot's environment
- Travel to bot patrol waypoints

---

### `/fpp tph`
**Aliases:** None  
**Permission:** `fpp.tph`  
**Usage:** `/fpp tph [botname]`

**Description:**  
Teleports your bot(s) to your current location.

**Use Cases:**
- `/fpp tph BotName` — Bring specific bot to you
- `/fpp tph` — Bring all your bots to you (user mode)
- Gather bots for reorganization
- Rescue stuck bots
- Quick relocation

---

### `/fpp move`
**Aliases:** None  
**Permission:** `fpp.move`  
**Usage:** `/fpp move <bot|all> <player>  |  <bot|all> --wp <route> [--random]  |  <bot|all> --stop`

**Description:**  
Navigate a bot (or all bots) to a player or patrol a named waypoint route in a loop using server-side A* pathfinding.

**Use Cases:**
- `/fpp move Bot1 PlayerName` — Bot follows player
- `/fpp move all PlayerName` — All bots follow player
- `/fpp move Bot1 --wp spawn_tour` — Bot patrols waypoint route
- `/fpp move Bot1 --wp patrol --random` — Random waypoint order
- `/fpp move Bot1 --stop` — Stop navigation
- Create NPC guides
- Simulate player traffic patterns
- Automated security patrols
- Living world ambience

**Note:** Supports parkour jumps, block breaking/placing (if enabled in pathfinding config).

---

### `/fpp freeze`
**Aliases:** None  
**Permission:** `fpp.freeze`  
**Usage:** `/fpp freeze <bot|all> [on|off]`

**Description:**  
Freeze or unfreeze a bot in place (prevents all movement).

**Use Cases:**
- `/fpp freeze BotName` — Toggle freeze state
- `/fpp freeze BotName on` — Freeze bot
- `/fpp freeze BotName off` — Unfreeze bot
- `/fpp freeze all on` — Freeze all bots
- Create static NPCs
- Prevent bots from wandering
- Pause patrol routes temporarily

---

### `/fpp rename`
**Aliases:** None  
**Permission:** `fpp.rename` (admin) or `fpp.rename.own` (user)  
**Usage:** `/fpp rename <oldname> <newname>`

**Description:**  
Rename an active bot (preserves all data: inventory, XP, chat settings, frozen state, LuckPerms group). Uses delete+respawn to update the GameProfile name.

**Use Cases:**
- `/fpp rename OldBot NewBot` — Rename bot
- Fix typos in bot names
- Rebrand bots for different themes
- Comply with naming policies

**Note:** Users with `fpp.rename.own` can only rename bots they spawned.

---

### `/fpp ping`
**Aliases:** None  
**Permission:** `fpp.ping`  
**Usage:** `/fpp ping [<bot>|--count <n>] [--ping <ms>|--random]`

**Description:**  
Set a simulated ping (latency) for one or more bots. Updates both tab-list display and NMS latency field.

**Use Cases:**
- `/fpp ping Bot1 --ping 50` — Set bot's ping to 50ms
- `/fpp ping Bot1 --random` — Assign random ping (20-300ms)
- `/fpp ping --count 5 --ping 30` — Set ping for 5 bots
- `/fpp ping --count 10 --random` — Random ping for 10 bots
- Simulate realistic player latency
- Test lag-compensation features
- Create regional player distribution illusion

**Ping Distribution (Random Mode):**
- 60% low (20-100ms) — local players
- 25% medium (100-200ms) — regional
- 15% high (200-300ms) — intercontinental

---

## Bot Inventory & Actions

### `/fpp inventory` **[inv]**
**Aliases:** `inv`  
**Permission:** `fpp.inventory`  
**Usage:** `/fpp inventory <bot>`  
**Shorthand:** `/fpp inv <bot>`

**Description:**  
Opens a 54-slot double-chest GUI displaying the bot's full inventory (main, hotbar, armor, offhand). Directly edit equipment and items.

**Use Cases:**
- `/fpp inventory BotName` — Open inventory GUI
- `/fpp inv Bot1` — Shorthand version
- Right-click bot entity — Opens GUI without command
- Equip bots with tools and armor
- Transfer items to/from bots
- Inspect bot inventory contents

---

### `/fpp cmd`
**Aliases:** None  
**Permission:** `fpp.cmd`  
**Usage:** `/fpp cmd <bot> <command...>  |  <bot> --add <command...>  |  <bot> --clear  |  <bot> --show`

**Description:**  
Execute a command as a bot, or bind one to right-click interaction.

**Use Cases:**
- `/fpp cmd Bot1 help` — Execute `/help` as Bot1
- `/fpp cmd Bot1 --add warp shop` — Bind `/warp shop` to right-click
- `/fpp cmd Bot1 --clear` — Remove right-click command
- `/fpp cmd Bot1 --show` — Display stored command
- Create interactive NPCs
- Simulate player activity (chat, commands)
- Right-click teleport bots

**Note:** Right-click opens inventory by default; stored command overrides this.

---

### `/fpp xp`
**Aliases:** None  
**Permission:** `fpp.xp`  
**Usage:** `/fpp xp <bot>`

**Description:**  
Transfers all XP from a bot to you (clears bot's level, progress, total XP). Imposes 30-second cooldown on bot's XP pickup.

**Use Cases:**
- `/fpp xp BotName` — Collect bot's XP
- Harvest XP from AFK bots
- Transfer mob farm XP to players
- Reset bot experience levels

---

### `/fpp mine`
**Aliases:** None  
**Permission:** `fpp.mine`  
**Usage:** `/fpp mine <bot> [once|stop|--pos1|--pos2|start]  |  stop`

**Description:**  
Bot mining automation: mine one block, mine continuously, or clear a selected cuboid area with automatic storage offloading.

**Use Cases:**
- `/fpp mine Bot1` — Continuous mining (whatever bot looks at)
- `/fpp mine Bot1 once` — Mine one block
- `/fpp mine Bot1 stop` — Stop mining
- `/fpp mine stop` — Stop all mining bots
- `/fpp mine Bot1 --pos1` — Set corner 1 (area mode)
- `/fpp mine Bot1 --pos2` — Set corner 2 (area mode)
- `/fpp mine Bot1 start` — Begin area mining
- Automated resource gathering
- Clear large areas
- Simulate mining activity

**Note:** Area mode deposits to registered storage containers when inventory fills.

---

### `/fpp use`
**Aliases:** None  
**Permission:** `fpp.useitem`  
**Usage:** `/fpp use <bot> [once|stop]  |  stop`

**Description:**  
Walks bot to your position, then continuously right-clicks what it's looking at (blocks, entities, usable items).

**Use Cases:**
- `/fpp use Bot1` — Continuous right-click
- `/fpp use Bot1 once` — Single right-click
- `/fpp use Bot1 stop` — Stop using
- `/fpp use stop` — Stop all use tasks
- Automated farming (crop harvesting)
- Entity interaction loops
- Button/lever automation

---

### `/fpp place`
**Aliases:** None  
**Permission:** `fpp.place`  
**Usage:** `/fpp place <bot> [once|stop]  |  stop`

**Description:**  
Bot continuously places blocks it is looking at (like `/fpp mine` but placing instead of breaking).

**Use Cases:**
- `/fpp place Bot1` — Continuous placement
- `/fpp place Bot1 once` — Place one block
- `/fpp place Bot1 stop` — Stop placing
- `/fpp place stop` — Stop all placement tasks
- Automated building
- Block fill operations
- Construction simulation

**Note:** Fetches blocks from registered storage containers when bot's inventory empties.

---

### `/fpp storage`
**Aliases:** None  
**Permission:** `fpp.storage`  
**Usage:** `/fpp storage <bot> [name|--list|--remove <name>|--clear]`

**Description:**  
Set or manage storage targets for a bot (chest, barrel, hopper, shulker, etc.). Used by `/fpp mine` and `/fpp place` for automatic restocking/depositing.

**Use Cases:**
- `/fpp storage Bot1 main_chest` — Register storage at crosshair
- `/fpp storage Bot1 --list` — List registered storages
- `/fpp storage Bot1 --remove main_chest` — Remove storage
- `/fpp storage Bot1 --clear` — Clear all storages
- Set up supply chains
- Automate resource collection
- Configure mining bot workflows

---

### `/fpp waypoint` **[wp]**
**Aliases:** `wp`  
**Permission:** `fpp.waypoint`  
**Usage:** `/fpp waypoint add <route> | create <route> | remove <route> <index> | delete <route> | clear <route> | list [route]`  
**Shorthand:** `/fpp wp ...`

**Description:**  
Manage named waypoint patrol routes for bots. Routes persist across restarts.

**Use Cases:**
- `/fpp waypoint add spawn_tour` — Add current position to route
- `/fpp wp create new_route` — Explicitly create empty route
- `/fpp waypoint list` — List all routes
- `/fpp waypoint list spawn_tour` — List waypoints in route
- `/fpp waypoint remove spawn_tour 2` — Remove waypoint #2
- `/fpp waypoint delete spawn_tour` — Delete entire route
- `/fpp waypoint clear spawn_tour` — Remove all waypoints from route
- Create guard patrols
- Design NPC tours
- Automate movement patterns

**Note:** Use with `/fpp move <bot> --wp <route>` to start patrol.

---

## Chat & AI

### `/fpp chat`
**Aliases:** None  
**Permission:** `fpp.chat`  
**Usage:** `/fpp chat [on|off|status|all] | <bot> [on|off|status|info|mute [sec]|say <msg>|tier <tier>]`

**Description:**  
Toggles bot auto-chat globally or per-bot. Supports activity tiers (quiet, passive, normal, active, chatty), timed mutes, and manual messages.

**Use Cases:**
- `/fpp chat` — Toggle global chat on/off
- `/fpp chat on` — Enable global chat
- `/fpp chat status` — Show chat status
- `/fpp chat Bot1 off` — Permanently silence bot
- `/fpp chat Bot1 mute 60` — Mute bot for 60 seconds
- `/fpp chat Bot1 tier chatty` — Make bot very active
- `/fpp chat Bot1 tier quiet` — Reduce bot's chattiness
- `/fpp chat Bot1 say Hello!` — Force bot to say message
- `/fpp chat Bot1 info` — Show bot's chat settings
- `/fpp chat all on` — Enable chat for all bots
- `/fpp chat all tier normal` — Set all bots to normal tier
- Control server atmosphere
- Simulate active community
- Test chat filters/plugins

**Activity Tiers:**
- `quiet` — 15% chance, low frequency
- `passive` — 25% chance, below-average
- `normal` — 30% chance, baseline
- `active` — 18% chance, above-average
- `chatty` — 12% chance, high frequency

---

### `/fpp personality` **[persona]**
**Aliases:** `persona`  
**Permission:** `fpp.personality`  
**Usage:** `/fpp personality <list|reload> | <bot> <set <name>|reset|show>`  
**Shorthand:** `/fpp persona ...`

**Description:**  
Manage AI personalities for bots. Personalities are `.txt` files in `plugins/FakePlayerPlugin/personalities/` that define bot behavior in AI conversations.

**Use Cases:**
- `/fpp personality list` — List available personalities
- `/fpp personality reload` — Reload personality files
- `/fpp personality Bot1 set friendly` — Assign personality
- `/fpp persona Bot1 set grumpy` — Shorthand version
- `/fpp personality Bot1 reset` — Use config default
- `/fpp personality Bot1 show` — Display current personality
- Customize bot AI responses
- Create themed NPC characters
- Test different AI behaviors

**Note:** Requires AI conversation system enabled (`ai-conversations.enabled: true`) and valid API key.

---

### `/fpp badword`
**Aliases:** None  
**Permission:** `fpp.badword`  
**Usage:** `/fpp badword <check|update|status>`

**Description:**  
Scan and fix bot names flagged by the badword filter. Uses global CMU list + local words + regex patterns.

**Use Cases:**
- `/fpp badword check` — List bots with inappropriate names (read-only)
- `/fpp badword update` — Auto-rename flagged bots with clean names
- `/fpp badword status` — Show filter config and scan results
- Enforce naming policies
- Comply with community standards
- Clean up after bulk spawns

**Filter Sources:**
1. Remote global baseline (CMU list)
2. `badword-filter.words` in `config.yml`
3. `plugins/FakePlayerPlugin/bad-words.yml` (local + regex patterns)

**Detection Modes:**
- Raw substring (case-insensitive)
- Leet-speak normalization (`0→o 1→i 3→e`)
- Aggressive (duplicate-char collapse)
- Custom regex patterns

---

## LuckPerms Integration

### `/fpp rank`
**Aliases:** None  
**Permission:** `fpp.rank`  
**Usage:** `/fpp rank <bot> <group|clear> | random <group> [num] | list`

**Description:**  
Assign LuckPerms groups to one bot or random bots. Updates bot prefix/suffix in tab-list and chat.

**Use Cases:**
- `/fpp rank Bot1 vip` — Assign VIP group
- `/fpp rank Bot1 clear` — Remove group (use default)
- `/fpp rank random vip 5` — Assign VIP to 5 random bots
- `/fpp rank list` — List all LP groups
- Simulate ranked player distribution
- Test permission plugins
- Create diverse bot populations

**Note:** Requires LuckPerms installed. Groups persist across restarts.

---

### `/fpp lpinfo`
**Aliases:** None  
**Permission:** `fpp.lpinfo`  
**Usage:** `/fpp lpinfo`

**Description:**  
Show LuckPerms integration status for bots. Displays LP availability, config settings, loaded groups, and active bot assignments.

**Use Cases:**
- `/fpp lpinfo` — Check LP integration status
- Debug LP prefix/suffix issues
- Verify group assignments
- Confirm LP is detecting bots

---

## Network & Proxy

### `/fpp swap`
**Aliases:** None  
**Permission:** `fpp.swap`  
**Usage:** `/fpp swap [on|off|status|now <bot>|list|info <bot>]`

**Description:**  
Toggle bot session rotation (swap in/out). Bots temporarily disconnect and rejoin to simulate real player join/leave patterns.

**Use Cases:**
- `/fpp swap` — Toggle swap system on/off
- `/fpp swap on` — Enable swap
- `/fpp swap off` — Disable swap
- `/fpp swap status` — Show swap configuration
- `/fpp swap now Bot1` — Force immediate swap
- `/fpp swap list` — List swapped-out bots
- `/fpp swap info Bot1` — Show bot's swap history
- Simulate player turnover
- Reduce memory usage during low traffic
- Test join/leave event handlers

**Configuration:**
- `swap.min-online` — Minimum bots to keep online
- `swap.retry-rejoin` — Auto-retry failed rejoins
- `swap.retry-delay` — Retry delay (seconds)

---

### `/fpp peaks`
**Aliases:** None  
**Permission:** `fpp.peaks`  
**Usage:** `/fpp peaks [on|off|status|next|force|list|wake [name]|sleep <name>]`

**Description:**  
Manage peak-hours bot pool scheduling. Automatically adjusts online bot count based on time windows.

**Use Cases:**
- `/fpp peaks` — Toggle peak-hours on/off
- `/fpp peaks on` — Enable scheduling
- `/fpp peaks status` — Show current window and target
- `/fpp peaks next` — Show next window change
- `/fpp peaks force` — Force immediate check
- `/fpp peaks list` — List sleeping bots
- `/fpp peaks wake` — Wake all sleeping bots
- `/fpp peaks wake Bot1` — Wake specific bot
- `/fpp peaks sleep Bot1` — Put bot to sleep manually
- Match bot count to player traffic
- Reduce server load during off-hours
- Automate population scaling

**Note:** Requires `swap.enabled: true`. Configure time windows in `peak-hours.schedule`.

---

### `/fpp alert`
**Aliases:** None  
**Permission:** `fpp.alert`  
**Usage:** `/fpp alert <message>`

**Description:**  
Broadcast alert to all servers in the proxy network (NETWORK mode only).

**Use Cases:**
- `/fpp alert Server restarting in 5 minutes!` — Network-wide announcement
- `/fpp alert Maintenance mode enabled` — Notify all servers
- Coordinate maintenance across network
- Send emergency notifications
- Test proxy messaging

---

### `/fpp sync`
**Aliases:** None  
**Permission:** `fpp.sync`  
**Usage:** `/fpp sync <push|pull|status|check> [file]`

**Description:**  
Sync configs across network (NETWORK mode only). Push/pull config files to/from MySQL.

**Use Cases:**
- `/fpp sync push` — Push all configs to database
- `/fpp sync pull` — Pull all configs from database
- `/fpp sync status` — Show sync mode and file list
- `/fpp sync push config.yml` — Push single file
- `/fpp sync pull bot-names.yml` — Pull single file
- Maintain consistent config across proxy
- Propagate changes to all servers
- Centralize configuration management

**Sync Modes:**
- `DISABLED` — No sync
- `MANUAL` — Only via command
- `AUTO_PULL` — Pull on startup
- `AUTO_PUSH` — Push on change

**Note:** Server-specific keys (`database.server-id`, `database.mysql.*`, `debug`) are never synced.

---

## Quick Reference Table

| Command | Aliases | Permission | Description |
|---------|---------|------------|-------------|
| `help` | — | `fpp.help` | Show command help menu |
| `spawn` | — | `fpp.spawn` | Spawn fake player bots |
| `despawn` | — | `fpp.delete` | Remove bots |
| `list` | — | `fpp.list` | List active bots |
| `info` | — | `fpp.info` | Query bot session history |
| `reload` | — | `fpp.reload` | Reload configuration |
| `stats` | — | `fpp.stats` | Display live statistics |
| `settings` | — | `fpp.settings` | Open settings GUI |
| `migrate` | — | `fpp.migrate` | Manage migrations & backups |
| `tp` | — | `fpp.tp` | Teleport to bot |
| `tph` | — | `fpp.tph` | Teleport bot to you |
| `move` | — | `fpp.move` | Navigate bot to player/waypoint |
| `freeze` | — | `fpp.freeze` | Freeze bot in place |
| `rename` | — | `fpp.rename` | Rename active bot |
| `ping` | — | `fpp.ping` | Set bot ping/latency |
| `inventory` | **inv** | `fpp.inventory` | Open bot inventory GUI |
| `cmd` | — | `fpp.cmd` | Execute command as bot |
| `xp` | — | `fpp.xp` | Collect XP from bot |
| `mine` | — | `fpp.mine` | Bot mining automation |
| `use` | — | `fpp.useitem` | Bot right-click automation |
| `place` | — | `fpp.place` | Bot block placement |
| `storage` | — | `fpp.storage` | Manage bot storage targets |
| `waypoint` | **wp** | `fpp.waypoint` | Manage patrol routes |
| `chat` | — | `fpp.chat` | Control bot auto-chat |
| `personality` | **persona** | `fpp.personality` | Manage AI personalities |
| `badword` | — | `fpp.badword` | Scan/fix bot names |
| `rank` | — | `fpp.rank` | Assign LuckPerms groups |
| `lpinfo` | — | `fpp.lpinfo` | Show LuckPerms status |
| `swap` | — | `fpp.swap` | Toggle bot rotation |
| `peaks` | — | `fpp.peaks` | Manage peak-hours scheduling |
| `alert` | — | `fpp.alert` | Network-wide broadcast |
| `sync` | — | `fpp.sync` | Sync configs across network |

---

## Command Format Convention

All commands follow the format: `/fpp <command> [arguments]`

**Notation:**
- `<required>` — Required argument
- `[optional]` — Optional argument
- `|` — OR (choose one option)
- `...` — Variable number of arguments

**Example:**
- `/fpp spawn [amount]` — Amount is optional
- `/fpp chat <bot> [on|off]` — Bot is required, on/off is optional
- `/fpp cmd <bot> <command...>` — Bot is required, command can be multiple words

---

## Permission Groups

### Admin Permissions (`fpp.op`)
Grants all commands:
- Full spawn control (unlimited count, custom names, coordinates)
- Delete all bots (`fpp.delete.all`)
- Access all info (`fpp.info`)
- Reload & migrate operations
- Rank management
- Network sync & alerts
- Settings GUI

### User Permissions (`fpp.use`)
Basic player commands:
- Spawn bots (with limit: `fpp.spawn.limit.1` by default)
- Tph own bots
- XP collection
- User info (`fpp.info.user`)

### Bot Limits
Per-user spawn limits: `fpp.spawn.limit.<number>`
- Example: `fpp.spawn.limit.5` allows 5 bots
- Highest limit wins (if user has multiple)
- No limit = falls back to `Config.userBotLimit()`

---

## Special Notes

### Right-Click Interactions
- Right-click bot → Opens inventory GUI (default)
- Shift + right-click bot → Opens bot settings GUI (requires `Config.isBotShiftRightClickSettingsEnabled()`)
- Right-click bot with stored command → Executes command (set via `/fpp cmd <bot> --add`)

### Console vs Player Commands
Some commands are player-only:
- `/fpp settings` — Requires player to open GUI
- `/fpp tph` — Requires player location
- `/fpp waypoint add` — Requires player position

Console alternatives:
- `/fpp help` — Shows text pagination instead of GUI
- `/fpp spawn` — Requires coordinates when used from console

### Persistence
The following bot properties persist across restarts (when `database.persist-on-restart: true`):
- Inventory (main, armor, offhand)
- XP (level, progress, total)
- Chat settings (enabled, tier, personality)
- Frozen state
- LuckPerms group
- Right-click command
- Per-bot overrides (pickup, head-AI, swim-AI, nav settings, chunk radius)
- Active tasks (mine, use, place, patrol)

---

## Version History

**v1.6.5** (Current)
- Added `/fpp ping` command
- Enhanced help menu with all command icons
- Updated COMMANDS.md documentation

**v1.6.4**
- Added NameTag plugin support
- Improved skin system

**v1.6.3**
- Added task persistence (mine/use/place/patrol)
- Bot identity caching
- Entity ID index optimization

**v1.6.2**
- Added `/fpp place` command
- Storage system for supply containers
- Per-bot navigation overrides

---

**For more information, see:**
- `Configuration.md` — Full config reference
- `Permissions.md` — Permission node details
- `Database.md` — Database schema and operations
- `Bot-Behaviour.md` — AI, chat, swap, and peak-hours systems

**Support:** https://discord.gg/RfjEJDG2TM

