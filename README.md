# ꜰᴀᴋᴇ ᴘʟᴀʏᴇʀ ᴘʟᴜɢɪɴ (FPP)

> Spawn realistic fake players on your Paper server — with tab list presence, server list count, join/leave messages, in-world bodies, guaranteed skins, chunk loading, bot swap/rotation, fake chat, AI conversations, area mining, block placing, pathfinding, follow-target automation, per-bot settings GUI, per-bot swim AI & chunk-radius overrides, per-bot PvE attack settings, per-bot XP & item pickup control, tab-list ping simulation, NameTag plugin integration, LuckPerms integration, proxy network support, Velocity companion plugin, BungeeCord companion plugin, full Paper 1.21.x compatibility (1.21.0–1.21.11), and full hot-reload.

[![Version](https://img.shields.io/modrinth/v/fake-player-plugin-%28fpp%29?style=flat-square&label=version&color=0079FF&logo=modrinth)](https://modrinth.com/plugin/fake-player-plugin-(fpp))
![MC](https://img.shields.io/badge/Minecraft-1.21.x-0079FF?style=flat-square)
![Platform](https://img.shields.io/badge/platform-Paper-0079FF?style=flat-square)
![Java](https://img.shields.io/badge/Java-21-0079FF?style=flat-square)
[![License: MIT](https://img.shields.io/badge/License-MIT-green?style=flat-square)](https://github.com/Pepe-tf/fake-player-plugin/blob/main/LICENSE)
[![GitHub](https://img.shields.io/badge/GitHub-Open%20Source-181717?style=flat-square&logo=github)](https://github.com/Pepe-tf/fake-player-plugin)
[![Modrinth](https://img.shields.io/badge/Modrinth-FPP-00AF5C?style=flat-square&logo=modrinth)](https://modrinth.com/plugin/fake-player-plugin-(fpp))
[![Discord](https://img.shields.io/badge/Discord-Join%20Server-5865F2?style=flat-square&logo=discord&logoColor=white)](https://discord.gg/QSN7f67nkJ)
[![Wiki](https://img.shields.io/badge/Wiki-fpp.wtf-7B8EF0?style=flat-square)](https://fpp.wtf)
[![GitHub Sponsors](https://img.shields.io/badge/GitHub%20Sponsors-Sponsor-EA4AAA?style=flat-square&logo=githubsponsors&logoColor=white)](https://github.com/sponsors/Pepe-tf)
[![Patreon](https://img.shields.io/badge/Patreon-Support%20FPP-FF424D?style=flat-square&logo=patreon&logoColor=white)](https://www.patreon.com/c/F_PP?utm_medium=unknown&utm_source=join_link&utm_campaign=creatorshare_creator&utm_content=copyLink)

---

> 🎉 **FakePlayerPlugin is now Open Source!** The full source code is available on [GitHub](https://github.com/Pepe-tf/fake-player-plugin) under the **MIT License**. Contributions, bug reports, and pull requests are welcome!

---

## What It Does

FPP adds fake players to your server that look and behave like real ones:

- Show up in the **tab list** and **server list player count**
- Broadcast **join, leave, and kill messages**
- Spawn as **physical NMS ServerPlayer entities** — pushable, damageable, solid
- Always have a **real skin** (guaranteed fallback chain — never Steve/Alex unless you want it)
- **Load chunks** around them exactly like a real player
- **Rotate their head** to face nearby players
- **Swim automatically** in water and lava — mimics a real player holding spacebar
- **Send fake chat messages** from a configurable message pool (with LP prefix/suffix support, typing delays, burst messages, mention replies, and event reactions)
- **Swap in and out** automatically with fresh names and personalities
- **Persist across restarts** — they come back where they left off
- **Freeze** any bot in place with `/fpp freeze`
- **Open bot inventory** — 54-slot GUI with equipment slots; right-click any bot entity to open
- **Pathfind to players** — A* grid navigation with WALK, ASCEND, DESCEND, PARKOUR, BREAK, PLACE move types
- **Mine blocks** — continuous or one-shot block breaking; area selection with pos1/pos2 cuboid mode
- **Place blocks** — continuous block placing with per-bot supply container support
- **Right-click automation** — assign a command to any bot; right-clicking it runs the command
- **Transfer XP** — drain a bot's entire XP pool to yourself with `/fpp xp`
- **Named waypoint routes** — save patrol routes; bots walk them on a loop with `/fpp move --wp`
- **Rename bots** — rename any active bot with full state preservation (inventory, XP, LP group, tasks)
- **Per-bot settings GUI** — shift+right-click any bot to open a 6-row settings chest (General · Chat · PvE · Pathfinding · Danger) — now available to all users with `fpp.settings` permission
- **AI conversations** — bots respond to `/msg` with AI-generated replies; 7 providers (OpenAI, Groq, Anthropic, Gemini, Ollama, Copilot, Custom); per-bot personalities via `personalities/` folder
- **Badword filter** — case-insensitive with leet-speak normalization, auto-rename bad names, remote word list
- **Set bot ping** — simulate realistic tab-list latency per bot with `/fpp ping`; fixed, random, or bulk modes
- **PvE attack automation** — bots attack nearby entities, auto-target mobs (`--mob`), pursue targets (`--move`), or roam-hunt (`--hunt`) with `/fpp attack`
- **Per-bot PvE smart attack mode** — tri-state `OFF` / `ON (still)` / `ON (move)` configurable per-bot via `BotSettingGui`; mob type selector, range, and priority
- **Follow-target automation** — bots continuously follow any online player with `/fpp follow`; path recalculates as target moves, persists across restarts
- **Skin command** — apply any Mojang skin, URL skin, or reset with `/fpp skin <bot> <username|url|reset>`
- **Skin persistence** — resolved skins saved to DB and re-applied on restart without a new Mojang API round-trip
- **Per-bot pathfinding overrides** — parkour, break-blocks, place-blocks, nav-avoid-water, nav-avoid-lava configurable per-bot via `BotSettingGui`
- **Per-bot respawn-on-death** — bots auto-respawn after death instead of being removed
- **Per-bot auto-eat / auto-place-bed** — realistic survival overrides per bot
- **Bot select menu** — `/fpp bots` opens a paginated GUI of your manageable bots; click to open per-bot settings
- **Save command** — `/fpp save` immediately checkpoints all bot data to disk
- **Set owner** — `/fpp setowner <bot> <player>` transfers bot ownership and clears shared controllers
- **Roam mode** — `/fpp move <bot> --roam [x,y,z] [radius]` for autonomous random wandering
- **NameTag integration** — nick-conflict guard, bot isolation from nick cache, skin sync, auto-rename via nick
- **LuckPerms** — per-bot group assignment, weighted tab-list ordering, prefix/suffix in chat and nametags
- **Proxy/network support** — Velocity & BungeeCord cross-server chat, alerts, and shared database
- **Velocity companion** (`fpp-velocity.jar`) — drop this into your Velocity proxy's `plugins/` folder to inflate the server-list player count and hover list with FPP bots; includes an anti-scam startup warning
- **BungeeCord companion** (`fpp-bungee.jar`) — identical feature set for BungeeCord/Waterfall networks; drop into your BungeeCord `plugins/` folder; no configuration needed
- **Config sync** — push/pull configuration files across your proxy network
- **PlaceholderAPI** — 29+ placeholders including per-world bot counts, network state, spawn cooldown, and new proxy-aware counts
- **Extension / Addon API** — drop `.jar` files into `plugins/FakePlayerPlugin/extensions/` to load third-party addons with full access to commands, events, tick handlers, and settings GUI tabs
- **Random name generator** — `bot-name.mode: random` generates realistic Minecraft-style usernames on the fly
- **Find command** — bots scan nearby chunks for target blocks and mine them progressively
- **Bot groups** — personal bot groups with GUI management for bulk commands
- **WorldEdit integration** — `--wesel` flag for mine/place uses your WorldEdit selection
- **Automation** — `auto-eat` and `auto-place-bed` defaults for realistic bot survival behaviour
- **Folia support** — compatible with Folia's regionised threading model
- Fully **hot-reloadable** — no restarts needed

---

## Requirements

| Requirement | Version |
|-------------|---------|
| [Paper](https://papermc.io/downloads/paper) | 1.21.x |
| Java | 21+ |
| [LuckPerms](https://luckperms.net) | Optional — auto-detected |
| [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) | Optional — auto-detected (29+ placeholders) |
| [WorldGuard](https://dev.bukkit.org/projects/worldguard) | Optional — auto-detected (no-PvP region protection) |
| [WorldEdit](https://enginehub.org/worldedit/) | Optional — auto-detected (`--wesel` flag for mine/place) |
| [NameTag](https://lode.gg) | Optional — auto-detected (nick-conflict guard, skin sync) |

> **PlaceholderAPI Integration:** FPP provides 29+ placeholders including per-world bot counts, player-relative stats, network state, and system status. See [PLACEHOLDERAPI.md](PLACEHOLDERAPI.md) for the complete reference.

> **Compatibility:** Supports all Paper 1.21.x versions (1.21.0 through 1.21.11). Check the server console after startup for any version-specific notes.

> SQLite is bundled — no database setup required. MySQL is available for multi-server/proxy setups.

---

## Installation

1. Download the latest `fpp-*.jar` from [![Modrinth](https://img.shields.io/modrinth/v/fake-player-plugin-%28fpp%29?style=flat-square&label=Modrinth&color=00AF5C&logo=modrinth)](https://modrinth.com/plugin/fake-player-plugin-(fpp)/versions) and place it in your `plugins/` folder.
2. Restart your server — config files are created automatically.
3. Edit `plugins/FakePlayerPlugin/config.yml` to your liking.
4. Run `/fpp reload` to apply changes at any time.

> **Updating?** FPP automatically migrates your config on first start and creates a timestamped backup before changing anything.

---

## Commands

All commands are under `/fpp` (aliases: `/fakeplayer`, `/fp`).

| Command | Description |
|---------|-------------|
| `/fpp` | Plugin info — version, active bots, download links |
| `/fpp help [page]` | Interactive GUI help menu — paginated, permission-filtered, click-navigable |
| `/fpp spawn [amount] [--name <name>]` | Spawn fake player(s) at your location |
| `/fpp despawn <name\|all\|--random [n]\|--num <n>>` | Remove a bot by name, remove all, remove random N, or remove N oldest (blocked during persistence restore) |
| `/fpp list` | List all active bots with uptime and location |
| `/fpp freeze <name\|all> [on\|off]` | Freeze or unfreeze bots — frozen bots are immovable |
| `/fpp inventory <bot>` | Open the bot's full 54-slot inventory GUI (alias: `/fpp inv`) |
| `/fpp move <bot> <player>` | Navigate a bot to an online player using A* pathfinding |
| `/fpp move <bot> --coords <x> <y> <z>` | Navigate a bot to exact world coordinates (supports `~` relative offsets) |
| `/fpp move <bot> --wp <route>` | Patrol a named waypoint route on a loop |
| `/fpp move <bot> --stop` | Stop the bot's current navigation |
| `/fpp mine <bot> [once\|stop]` | Continuous or one-shot block mining |
| `/fpp mine <bot> --pos1\|--pos2\|--start\|--status\|--stop` | Area-selection cuboid mining mode |
| `/fpp place <bot> [once\|stop]` | Continuous or one-shot block placing |
| `/fpp storage <bot> [name\|--list\|--remove\|--clear]` | Register supply containers for mine/place restocking |
| `/fpp use <bot>` | Bot right-clicks / activates the block it's looking at |
| `/fpp waypoint <name> [create\|add\|remove\|list\|clear]` | Manage named patrol route waypoints (`add` auto-creates the route) |
| `/fpp xp <bot>` | Transfer all of a bot's XP to yourself |
| `/fpp cmd <bot> <command>` | Execute a command on a bot (or `--add`/`--clear`/`--show` for stored right-click command) |
| `/fpp rename <old> <new>` | Rename a bot preserving all state (inventory, XP, LP group, tasks) |
| `/fpp personality <bot> set\|reset\|show` | Assign or clear AI personality per bot |
| `/fpp personality list\|reload` | List available personality files or reload them |
| `/fpp ping [<bot>] [--ping <ms>\|--random] [--count <n>]` | Set simulated tab-list ping for one or all bots |
| `/fpp attack <bot> [--stop]` | Bot walks to sender and attacks nearby entities (PvE); `--mob` for stationary mob-targeting mode; `--mob --move` to pursue targets; `--hunt` for roaming mob hunt |
| `/fpp follow <bot\|all> <player>` | Bot continuously follows an online player; path recalculates as target moves |
| `/fpp follow <bot\|all> --stop` | Stop the bot's current follow loop |
| `/fpp sleep <bot\|all> <x y z> <radius>` | Set a sleep-origin so the bot auto-sleeps at night near that location |
| `/fpp sleep <bot\|all> --stop` | Clear the bot's sleep-origin |
| `/fpp stop [<bot>\|all]` | Cancel all active tasks for a bot (move, mine, place, use, attack, follow, sleep) |
| `/fpp find <bot> <block> [--radius <n>] [--count <n>]` | Bot scans nearby chunks for target blocks and mines them progressively |
| `/fpp groups [gui\|list\|create\|delete\|add\|remove]` | Personal bot groups with GUI management |
| `/fpp save` | Immediately save all active bot data to disk (persistence checkpoint) |
| `/fpp setowner <bot> <player>` | Transfer ownership of a bot to another player |
| `/fpp bots [bot]` | Open paginated GUI of your manageable bots; click to open settings (aliases: `mybots`, `botmenu`) |
| `/fpp skin <bot> <username\|url\|reset>` | Apply a skin to a bot from a Minecraft username, URL, or reset to default |
| `/fpp badword add\|remove\|list\|reload` | Manage the runtime badword list |
| `/fpp chat [on\|off\|status]` | Toggle the fake chat system |
| `/fpp swap [on\|off\|status\|now <bot>\|list\|info <bot>]` | Toggle / manage the bot swap/rotation system |
| `/fpp peaks [on\|off\|status\|next\|force\|list\|wake <name>\|sleep <name>]` | Time-based bot pool scheduler |
| `/fpp rank <bot> <group>` | Assign a specific bot to a LuckPerms group |
| `/fpp rank random <group> [num\|all]` | Assign random bots to a LuckPerms group |
| `/fpp rank list` | List all active bots with their current LuckPerms group |
| `/fpp lpinfo [bot-name]` | LuckPerms diagnostic info — prefix, weight, rank, ordering |
| `/fpp stats` | Live statistics panel — bots, frozen, system status, DB totals, TPS |
| `/fpp info [bot <name> \| spawner <name>]` | Query the session database |
| `/fpp tp <name>` | Teleport yourself to a bot |
| `/fpp tph [name]` | Teleport your bot to yourself |
| `/fpp settings` | Open the in-game settings GUI — toggle config values live |
| `/fpp alert <message>` | Broadcast an admin message network-wide (proxy) |
| `/fpp sync push [file]` | Upload config file(s) to the proxy network |
| `/fpp sync pull [file]` | Download config file(s) from the proxy network |
| `/fpp sync status [file]` | Show sync status and version info |
| `/fpp sync check [file]` | Check for local changes vs network version |
| `/fpp migrate` | Backup, migration, and export tools |
| `/fpp reload` | Hot-reload all config, language, skins, name/message pools |

---

## Permissions

### Admin (`fpp.op` — default: op)

| Permission | Description |
|------------|-------------|
| `fpp.op` | All admin commands (admin wildcard, default: op) |
| `fpp.spawn` | Spawn bots (unlimited, supports `--name` and multi-spawn) |
| `fpp.delete` | Remove bots |
| `fpp.list` | List all active bots |
| `fpp.freeze` | Freeze / unfreeze any bot or all bots |
| `fpp.chat` | Toggle fake chat |
| `fpp.swap` | Toggle bot swap |
| `fpp.rank` | Assign bots to LuckPerms groups |
| `fpp.lpinfo` | View LuckPerms diagnostic info for any bot |
| `fpp.stats` | View the `/fpp stats` live statistics panel |
| `fpp.info` | Query the database |
| `fpp.reload` | Reload configuration |
| `fpp.tp` | Teleport to bots |
| `fpp.tph` | Teleport your own bot to you |
| `fpp.tph.all` | Teleport all accessible bots to you at once |
| `fpp.bypass.maxbots` | Bypass the global bot cap |
| `fpp.peaks` | Manage the peak-hours bot pool scheduler |
| `fpp.settings` | Open the in-game settings GUI |
| `fpp.inventory` | Open any bot's inventory GUI |
| `fpp.move` | Navigate bots with A* pathfinding |
| `fpp.cmd` | Execute or store commands on bots |
| `fpp.mine` | Enable/stop bot block mining |
| `fpp.place` | Enable/stop bot block placing |
| `fpp.storage` | Register supply containers for bots |
| `fpp.useitem` | Bot right-click / use-item automation |
| `fpp.waypoint` | Manage named patrol route waypoints |
| `fpp.rename` | Rename any bot (with full state preservation) |
| `fpp.rename.own` | Rename only bots the sender personally spawned |
| `fpp.personality` | Assign AI personalities to bots |
| `fpp.badword` | Manage the runtime badword filter list |
| `fpp.ping` | View/set simulated tab-list ping for bots |
| `fpp.attack` | PvE attack automation (classic & mob-targeting modes) |
| `fpp.follow` | Follow-target bot automation (persistent across restarts) |
| `fpp.find` | Bot block-finding and progressive mining |
| `fpp.sleep` | Set bot sleep-origin for night auto-sleep |
| `fpp.stop` | Cancel all active tasks for one or all bots |
| `fpp.save` | Immediately save all bot data to disk |
| `fpp.setowner` | Transfer bot ownership to another player |
| `fpp.skin` | Apply or reset per-bot skins |
| `fpp.migrate` | Data migration and backup utilities |
| `fpp.alert` | Broadcast network-wide admin alerts |
| `fpp.sync` | Push/pull config across proxy network |

### User (`fpp.use` — default: true for all players)

| Permission | Description |
|------------|-------------|
| `fpp.use` | All user-tier commands (granted by default) |
| `fpp.spawn.user` | Spawn your own bot (limited by `fpp.spawn.limit.<num>`) |
| `fpp.tph` | Teleport your bot to you |
| `fpp.xp` | Transfer a bot's XP to yourself |
| `fpp.info.user` | View your bot's location and uptime |

### Bot Limits

Grant players a `fpp.spawn.limit.<num>` node to set how many bots they can spawn. FPP picks the highest one they have.

`fpp.spawn.limit.1` · `fpp.spawn.limit.2` · `fpp.spawn.limit.3` · `fpp.spawn.limit.5` · `fpp.spawn.limit.10` · `fpp.spawn.limit.15` · `fpp.spawn.limit.20` · `fpp.spawn.limit.50` · `fpp.spawn.limit.100`

> **LuckPerms example** — give VIPs 5 bots:
> ```
> /lp group vip permission set fpp.use true
> /lp group vip permission set fpp.spawn.limit.5 true
> ```

---

## Configuration Overview

Located at `plugins/FakePlayerPlugin/config.yml`. Run `/fpp reload` after any change.

| Section | What it controls |
|---------|-----------------|
| `language` | Language file to load (`language/en.yml`) |
| `debug` | Legacy master debug switch; per-subsystem toggles under `logging.debug.*` |
| `update-checker` | Enable/disable startup version check |
| `metrics` | Opt-out toggle for anonymous FastStats usage statistics |
| `limits` | Global bot cap, per-user limit, spawn tab-complete presets |
| `spawn-cooldown` | Seconds between `/fpp spawn` uses per player (`0` = off) |
| `bot-name` | Display name format for admin/user bots (`admin-format`, `user-format`) |
| `luckperms` | `default-group` — LP group assigned to every new bot at spawn |
| `skin` | Skin mode (`player` / `random` / `none`), `guaranteed-skin` toggle, pool, `skins/` folder |
| `badword-filter` | Name profanity filter — leet-speak normalization, remote word list, auto-rename |
| `bot-interaction` | Right-click / shift-right-click settings GUI toggles |
| `body` | Physical entity (`enabled`), `pushable`, `damageable`, `pick-up-items`, `pick-up-xp`, `drop-items-on-despawn` |
| `persistence` | Whether bots rejoin on server restart; task state (mine/place/patrol) also persisted |
| `join-delay` / `leave-delay` | Random delay range (ticks) for natural join/leave timing |
| `messages` | Toggle join, leave, and kill broadcast messages; admin compatibility notifications |
| `combat` | Bot HP and hurt sound |
| `death` | Respawn on death, respawn delay, item drop suppression |
| `chunk-loading` | Radius, update interval |
| `head-ai` | Enable/disable, look range, turn speed |
| `swim-ai` | Automatic swimming in water/lava (`enabled`, default `true`) |
| `collision` | Push physics — walk strength, hit strength, bot separation |
| `pathfinding` | A* options — parkour, break-blocks, place-blocks, place-material, arrival distances, node limits, max-fall |
| `fake-chat` | Enable, chance, interval, typing delays, burst messages, bot-to-bot chat, mention replies, event reactions |
| `ai-conversations` | AI DM system — provider config, personality, typing delay, conversation history |
| `swap` | Auto rotation — session length, absence duration, min-online floor, retry-on-fail, farewell/greeting chat |
| `peak-hours` | Time-based bot pool scheduler — schedule, day-overrides, stagger-seconds, min-online |
| `performance` | Position sync distance culling (`position-sync-distance`) |
| `tab-list` | Show/hide bots in the player tab list |
| `server-list` | Whether bots count in the server-list player total; `count-bots`, `include-remote-bots` |
| `config-sync` | Cross-server config push/pull mode (`DISABLED` / `MANUAL` / `AUTO_PULL` / `AUTO_PUSH`) |
| `database` | `mode` (`LOCAL` / `NETWORK`), `server-id`, SQLite (default) or MySQL |
| `automation` | `auto-eat`, `auto-place-bed` — realistic bot survival defaults (per-bot overrides available) |
| `attack-mob` | PvE auto-targeting defaults (`default-range`, `default-priority`, etc.), smart attack mode |

---

## AI Conversations

Bots can respond to `/msg`, `/tell`, and `/whisper` commands with AI-generated replies matching their personality.

**Setup:**
1. Edit `plugins/FakePlayerPlugin/secrets.yml` and add your API key
2. Set `ai-conversations.enabled: true` in `config.yml`
3. Bots will automatically respond — no restart needed

**Supported Providers** (picked in priority order — first key that works wins):

| Provider | Key in secrets.yml |
|----------|-------------------|
| OpenAI | `openai-api-key` |
| Anthropic | `anthropic-api-key` |
| Groq | `groq-api-key` |
| Google Gemini | `google-gemini-api-key` |
| Ollama | `ollama-base-url` (local, no key needed) |
| Copilot / Azure | `copilot-api-key` |
| Custom OpenAI-compatible | `custom-openai-base-url` |

**Personalities:** Drop `.txt` files into `plugins/FakePlayerPlugin/personalities/` to create custom personality prompts. Assign per-bot with `/fpp personality <bot> set <name>`.

Bundled personalities: `friendly`, `grumpy`, `noob`.

---

## Skin System

Three modes — set with `skin.mode`:

| Mode | Behaviour |
|------|-----------|
| `player` *(default)* | Fetches a real Mojang skin matching the bot's name |
| `random` | Full control — per-bot overrides, a `skins/` PNG folder, and a random pool |
| `none` | No skin — bots use the default Steve/Alex appearance |

> **Legacy aliases:** `auto` = `player`, `custom` = `random`, `off` = `none` — all still accepted.

**Skin fallback** (`skin.guaranteed-skin`, default `true`) — bots whose name has no matching Mojang account get a random skin from the built-in 1000-player fallback pool. Set to `false` to use the default Steve/Alex appearance instead.

In `random` mode the resolution pipeline is: per-bot override → `skins/<name>.png` → random PNG from `skins/` folder → random entry from `pool` → Mojang API for the bot's own name.

---

## Proxy Companions

FPP ships two optional companion plugins that inflate the **proxy-level** server-list player count to include FPP bots.

### Velocity Companion (`fpp-velocity.jar`)

A lightweight standalone Velocity plugin.

**What it does:**
- Registers the `fpp:proxy` plugin-messaging channel and listens for `BOT_SPAWN` / `BOT_DESPAWN` / `SERVER_OFFLINE` messages from backend servers
- Maintains a live bot registry; pings all backend servers every 5 seconds and caches their player counts
- Intercepts `ProxyPingEvent` to inflate the proxy-level player count and hover sample list with bot names (up to 12 shown)

**Installation:**
1. Drop `fpp-velocity.jar` into your Velocity proxy's `plugins/` folder — no config file needed
2. Restart Velocity

**Requirements:** Velocity 3.3.0+

---

### BungeeCord Companion (`fpp-bungee.jar`)

Identical feature set for BungeeCord/Waterfall networks.

**What it does:**
- Registers the `fpp:proxy` plugin-messaging channel and listens for `BOT_SPAWN` / `BOT_DESPAWN` / `SERVER_OFFLINE` messages from backend servers
- Maintains a live bot registry; pings all backend servers every 5 seconds and caches their player counts
- Intercepts `ProxyPingEvent` to inflate the proxy-level player count and hover sample list with bot names (up to 12 shown)

**Installation:**
1. Drop `fpp-bungee.jar` into your BungeeCord/Waterfall proxy's `plugins/` folder — no config file needed
2. Restart BungeeCord

**Requirements:** BungeeCord or any Waterfall fork

---

> ⚠️ **FPP and both companion plugins are 100% FREE & open-source.** If you or your server paid money for any of them, you were **scammed by a reseller**. Always download from the official sources:
> - **Modrinth:** https://modrinth.com/plugin/fake-player-plugin-(fpp)
> - **GitHub:** https://github.com/Pepe-tf/fake-player-plugin
> - **Discord:** https://discord.gg/QSN7f67nkJ

---

## Changelog

### v1.6.6.8 *(2026-05-02)*

**Bot Join/Leave Messages** — Custom `bot-join`/`bot-leave` lang keys replace vanilla messages; fully customizable MiniMessage formatting. Vanilla quit messages always nulled for bots. Death-despawn leave fires after kill message for proper ordering.

**Skin System** — Retry count 3→5; null/invalid results handled gracefully; all retry/failure logs converted to debug-level (`Config.debugSkin()`, silent by default).

**Ping System** — `ping.enabled` default changed to `false` (opt-in). Config v67→v70. DB schema v21→v22.

**Per-Bot Settings GUI** — 5 categories: General (frozen, respawn-on-death, head-AI, swim-AI, chunk-radius, pick-up-items, pick-up-xp, rename, share-control), Chat, PvE (smart-attack OFF/ON still/ON move, mob type selector, range, priority), Pathfinding (follow-player, parkour, break/place blocks), Danger (reset-all, delete).

**PvE Smart Attack** — Per-bot tri-state: OFF / ON_NO_MOVE / ON_MOVE (pursues via PathfindingService). `/fpp attack --mob --move` maps to ON_MOVE.

**Attack Hunt** — `/fpp attack <bot|all> --hunt [<mob>] [--range] [--priority]` — roaming mob hunt (range 32, not position-locked). Perm: `fpp.attack.hunt`

**New Commands** — `/fpp save`, `/fpp setowner`, `/fpp bots`, `/fpp skin`, `/fpp find`, `/fpp groups`, `/fpp sleep`, `/fpp stop`, `/fpp move --coords`, `/fpp move --roam` (autonomous wandering)

**Per-Bot Features** — respawnOnDeath, autoEat, autoPlaceBed, navAvoidWater, navAvoidLava, share control, mob type selector GUI

**Extension API** — `FppExtension` interface (drop-in JARs); `getDataFolder`, `getConfig`, `saveDefaultConfig`, etc.; 20+ API events

**Random Names** — `bot-name.mode: random` (default) generates realistic usernames; no more `Bot1234`

**WorldEdit** — `--wesel` flag for `/fpp mine` and `/fpp place`; soft-depend

**Automation** — `auto-eat: true`, `auto-place-bed: true` per-bot defaults

**Pathfinding** — Door/gate/trapdoor handling; ladder/vine/scaffolding climbing; knockback fix for 1.21.9+; organic walk wobble; sprint-jump on airborne→ground transition

**Folia** — `folia-supported: true` declared

**Config** 65→70 · **DB Schema** 18→22 · **New Perms** `fpp.save`, `fpp.setowner`, `fpp.skin`, `fpp.attack.hunt`, `fpp.find`, `fpp.sleep`, `fpp.stop`, `fpp.mine.wesel`, `fpp.place.wesel`, `fpp.tph.all`

Full changelog: [frontend/wiki/Changelog.md](frontend/wiki/Changelog.md)

---

## Support the Project

[![Ko-fi](https://img.shields.io/badge/Ko--fi-Support%20FPP-FF5E5B?style=flat-square&logo=ko-fi&logoColor=white)](https://ko-fi.com/fakeplayerplugin)
[![GitHub Sponsors](https://img.shields.io/badge/GitHub%20Sponsors-Sponsor-EA4AAA?style=flat-square&logo=githubsponsors&logoColor=white)](https://github.com/sponsors/Pepe-tf)
[![Patreon](https://img.shields.io/badge/Patreon-Support%20FPP-FF424D?style=flat-square&logo=patreon&logoColor=white)](https://www.patreon.com/c/F_PP?utm_medium=unknown&utm_source=join_link&utm_campaign=creatorshare_creator&utm_content=copyLink)

Donations are completely optional. Every contribution goes directly toward improving the plugin.

Thank you for using Fake Player Plugin. Without you, it wouldn't be where it is today.

---

## Links

- [Modrinth](https://modrinth.com/plugin/fake-player-plugin-(fpp)) — download
- [SpigotMC](https://www.spigotmc.org/resources/fake-player-plugin-fpp.133572/) — download
- [PaperMC Hangar](https://hangar.papermc.io/Pepe-tf/FakePlayerPlugin) — download
- [BuiltByBit](https://builtbybit.com/resources/fake-player-plugin.98704/) — download
- [Wiki](https://fpp.wtf) — documentation
- [Ko-fi](https://ko-fi.com/fakeplayerplugin) — support the project
- [GitHub Sponsors](https://github.com/sponsors/Pepe-tf) — support the project
- [Patreon](https://www.patreon.com/c/F_PP?utm_medium=unknown&utm_source=join_link&utm_campaign=creatorshare_creator&utm_content=copyLink) — support the project
- [Discord](https://discord.gg/QSN7f67nkJ) — support & feedback
- [GitHub](https://github.com/Pepe-tf/fake-player-plugin) — **open-source repository · source, issues & pull requests**

---

*Built for Paper 1.21.x · Java 21 · FPP v1.6.6.8 · [Modrinth](https://modrinth.com/plugin/fake-player-plugin-(fpp)) · [SpigotMC](https://www.spigotmc.org/resources/fake-player-plugin-fpp.133572/) · [PaperMC](https://hangar.papermc.io/Pepe-tf/FakePlayerPlugin) · [BuiltByBit](https://builtbybit.com/resources/fake-player-plugin.98704/) · [Wiki](https://fpp.wtf) · [GitHub](https://github.com/Pepe-tf/fake-player-plugin)*
