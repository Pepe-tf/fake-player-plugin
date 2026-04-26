# FakePlayerPlugin — Permission Reference

**Version:** 1.6.5  
**Last Updated:** April 16, 2026

---

## Permission Design

All FPP permissions follow a single predictable pattern:

```
fpp.<command>[.<action>][.<scope>]
```

| Segment | Purpose | Examples |
|---------|---------|---------|
| `command` | Main sub-command | `spawn`, `ping`, `move`, `mine` |
| `action` | Specific behaviour | `set`, `start`, `once`, `stop`, `bulk` |
| `scope` | Ownership tier | `own`, `others`, `bulk`, `all` |

---

## Access Tiers

| Node | Tier | Default | Description |
|------|------|---------|-------------|
| `fpp.admin` | **Admin** | `op` | Full access — new preferred name |
| `fpp.op` | **Admin** | `op` | Full access — legacy alias (identical to `fpp.admin`) |
| `fpp.use` | **User** | `true` (everyone) | User commands: spawn(1), tph, xp, info(own) |
| *(individual nodes)* | **Custom** | `op`/`false` | Fine-grained per-feature control |

> `fpp.admin` and `fpp.op` are **completely identical** in effect. Use whichever you prefer.  
> Granting `fpp.admin` automatically grants `fpp.op` (and vice versa via children).

---

## Quick Setup

```bash
# Full admin access
/lp group admin permission set fpp.admin true

# Standard user access (already default for everyone via fpp.use)
/lp group member permission set fpp.use true

# Operator who can control chat but nothing else
/lp group chatmod permission set fpp.chat true

# VIP — up to 5 bots, bypass cooldown
/lp group vip permission set fpp.spawn.limit.5 true
/lp group vip permission set fpp.bypass.cooldown true

# Completely hide /fpp from guests
/lp group guest permission set fpp.command false

# Allow staff to receive update notifications
/lp group staff permission set fpp.notify true
```

---

## Recommended Role Setups

### Admin / Owner
```
fpp.admin  (or fpp.op)
```
Inherits everything.

---

### Moderator
```
fpp.list
fpp.tp
fpp.freeze
fpp.chat
fpp.rename.own
fpp.badword
fpp.stats
fpp.inventory
fpp.ping
fpp.notify
```

---

### Builder
```
fpp.spawn (or fpp.spawn.limit.10)
fpp.despawn
fpp.move
fpp.mine
fpp.place
fpp.storage
fpp.waypoint
fpp.freeze
fpp.inventory
fpp.rename.own
```

---

### VIP Player
```
fpp.use                  (already default)
fpp.spawn.limit.5        (5 bots)
fpp.bypass.cooldown
fpp.rename.own
```

---

### Regular Player (default — no setup needed)
`fpp.use` is granted to **everyone** by default and includes:
- `fpp.spawn.user` — spawn up to limit
- `fpp.spawn.limit.1` — 1 bot default
- `fpp.tph` — bring bot to you
- `fpp.xp` — collect XP from own bot
- `fpp.info.user` — view own bot info

---

## Full Permission List

### Wildcard Tiers

| Node | Default | Description |
|------|---------|-------------|
| `fpp.admin` | `op` | Full access — new preferred admin wildcard |
| `fpp.op` | `op` | Full access — legacy admin wildcard (identical to `fpp.admin`) |
| `fpp.use` | `true` | User-tier wildcard (everyone by default) |
| `fpp.command` | `true` | Makes `/fpp` visible and tab-completable — negate to hide |
| `fpp.plugininfo` | `op` | Shows full info panel on bare `/fpp` |

---

### Spawn

| Node | Default | Description |
|------|---------|-------------|
| `fpp.spawn` | `op` | Admin spawn — no personal limit, all flags |
| `fpp.spawn.user` | `true` | User spawn — respects personal limit |
| `fpp.spawn.multiple` | `op` | Spawn `> 1` bot at a time |
| `fpp.spawn.mass` | `op` | Alias for `fpp.spawn.multiple` |
| `fpp.spawn.name` | `op` | Use `--name` for custom bot name |
| `fpp.spawn.coords` | `op` | Spawn at explicit world/coordinates |
| `fpp.spawn.limit.1` | `false`¹ | Personal bot limit — 1 |
| `fpp.spawn.limit.2-100` | `false` | Personal bot limits — 2 to 100 |
| `fpp.bypass.max` | `op` | Bypass global max-bots cap |
| `fpp.bypass.cooldown` | `op` | Bypass spawn cooldown |

¹ *Granted automatically via `fpp.use` — all players start with 1-bot limit.*

---

### Despawn

| Node | Default | Description |
|------|---------|-------------|
| `fpp.despawn` | `op` | Despawn bots — new preferred node |
| `fpp.delete` | `op` | Legacy alias for `fpp.despawn` (still works) |
| `fpp.despawn.bulk` | `op` | Mass despawn (`--count`, `--random`) |
| `fpp.delete.all` | `op` | Legacy alias for `fpp.despawn.bulk` |
| `fpp.despawn.own` | `false` | Despawn only own bots |

---

### Info & Navigation

| Node | Default | Description |
|------|---------|-------------|
| `fpp.help` | `true` | View `/fpp help` |
| `fpp.list` | `op` | List all active bots |
| `fpp.stats` | `op` | Live plugin statistics |
| `fpp.info` | `op` | Full database query (all bots, all history) |
| `fpp.info.user` | `true` | Own-bot info only (world, coords, uptime) |
| `fpp.tp` | `op` | Teleport to any bot |
| `fpp.tph` | `true` | Teleport own bots to you |
| `fpp.xp` | `true` | Collect XP from own bot |

---

### Chat

| Node | Default | Description |
|------|---------|-------------|
| `fpp.chat` | `op` | Full chat control (grants all sub-nodes) |
| `fpp.chat.global` | `op` | Toggle global auto-chat on/off |
| `fpp.chat.tier` | `op` | Change per-bot activity tier |
| `fpp.chat.mute` | `op` | Mute bot temporarily or permanently |
| `fpp.chat.say` | `op` | Force bot to say a message |

---

### Move

| Node | Default | Description |
|------|---------|-------------|
| `fpp.move` | `op` | Navigate bots (grants all sub-nodes) |
| `fpp.move.to` | `op` | Follow a player (`--to <player>`) |
| `fpp.move.waypoint` | `op` | Patrol a waypoint route (`--wp <route>`) |
| `fpp.move.stop` | `op` | Stop navigation (`--stop`) |

---

### Bot Actions

| Node | Default | Description |
|------|---------|-------------|
| `fpp.freeze` | `op` | Freeze / unfreeze bots |
| `fpp.rename` | `op` | Rename any bot (admin) |
| `fpp.rename.own` | `false` | Rename only own bots |
| `fpp.inventory` | `op` | Open and edit bot inventory GUI |
| `fpp.cmd` | `op` | Execute command as bot / bind right-click command |

---

### Ping

| Node | Default | Description |
|------|---------|-------------|
| `fpp.ping` | `op` | Full ping control (grants all sub-nodes) |
| `fpp.ping.set` | `op` | Set fixed ping (`--ping <ms>`) |
| `fpp.ping.random` | `op` | Random ping distribution (`--random`) |
| `fpp.ping.bulk` | `op` | Target multiple bots (`--count <n>`) |

---

### Mining / Placing / Using

| Node | Default | Description |
|------|---------|-------------|
| `fpp.mine` | `op` | Bot mining (grants all sub-nodes) |
| `fpp.mine.start` | `op` | Continuous mining |
| `fpp.mine.once` | `op` | Mine one block (`--once`) |
| `fpp.mine.stop` | `op` | Stop mining (`--stop`) |
| `fpp.mine.area` | `op` | Area-selection mining (`--pos1/--pos2/--start`) |
| `fpp.place` | `op` | Bot placement (grants all sub-nodes) |
| `fpp.place.start` | `op` | Continuous placement |
| `fpp.place.once` | `op` | Place one block (`--once`) |
| `fpp.place.stop` | `op` | Stop placing (`--stop`) |
| `fpp.useitem` | `op` | Bot right-click legacy node (grants sub-nodes) |
| `fpp.use.cmd` | `op` | New alias for `fpp.useitem` |
| `fpp.useitem.start` | `op` | Continuous right-click |
| `fpp.useitem.once` | `op` | Single right-click (`--once`) |
| `fpp.useitem.stop` | `op` | Stop right-clicking (`--stop`) |
| `fpp.storage` | `op` | Manage bot supply containers |
| `fpp.waypoint` | `op` | Manage waypoint patrol routes |

---

### LuckPerms / AI / Filter

| Node | Default | Description |
|------|---------|-------------|
| `fpp.rank` | `op` | Assign LP groups (grants all sub-nodes) |
| `fpp.rank.set` | `op` | Set group (`--set <group>`) |
| `fpp.rank.clear` | `op` | Remove group override (`--clear`) |
| `fpp.rank.bulk` | `op` | Bulk random assignment |
| `fpp.lpinfo` | `op` | LP diagnostics for a bot |
| `fpp.personality` | `op` | Manage AI personalities |
| `fpp.badword` | `op` | Scan / fix bot names |

---

### System / Network

| Node | Default | Description |
|------|---------|-------------|
| `fpp.reload` | `op` | Hot-reload plugin config |
| `fpp.migrate` | `op` | Backups, migrations, DB operations |
| `fpp.settings` | `op` | In-game settings GUI |
| `fpp.swap` | `op` | Bot session rotation |
| `fpp.peaks` | `op` | Peak-hours bot scheduling |
| `fpp.alert` | `op` | Network-wide broadcast (NETWORK mode) |
| `fpp.sync` | `op` | Config sync across proxy (NETWORK mode) |
| `fpp.notify` | `op` | Update notifications on join |

---

## Backward Compatibility

All legacy nodes continue to work exactly as before. No migration required.

| Legacy Node | New Preferred Node | Status |
|------------|-------------------|--------|
| `fpp.op` | `fpp.admin` | ✅ Both work — identical |
| `fpp.delete` | `fpp.despawn` | ✅ Both work — legacy is child of new |
| `fpp.delete.all` | `fpp.despawn.bulk` | ✅ Both work |
| `fpp.useitem` | `fpp.use.cmd` | ✅ Both work — new is alias of legacy |
| `fpp.spawn.multiple` | `fpp.spawn.mass` | ✅ Both work |

---

## Permission Inheritance Tree

```
fpp.admin
└── fpp.op
    ├── fpp.command
    ├── fpp.plugininfo
    ├── fpp.help
    ├── fpp.use
    │   ├── fpp.command
    │   ├── fpp.help
    │   ├── fpp.spawn.user
    │   ├── fpp.tph
    │   ├── fpp.xp
    │   ├── fpp.info.user
    │   └── fpp.spawn.limit.1
    ├── fpp.spawn
    │   ├── fpp.spawn.multiple / fpp.spawn.mass
    │   ├── fpp.spawn.name
    │   └── fpp.spawn.coords
    ├── fpp.despawn
    │   ├── fpp.delete → fpp.delete.all
    │   ├── fpp.despawn.bulk → fpp.delete.all
    │   └── fpp.despawn.own
    ├── fpp.chat
    │   ├── fpp.chat.global
    │   ├── fpp.chat.tier
    │   ├── fpp.chat.mute
    │   └── fpp.chat.say
    ├── fpp.move
    │   ├── fpp.move.to
    │   ├── fpp.move.waypoint
    │   └── fpp.move.stop
    ├── fpp.ping
    │   ├── fpp.ping.set
    │   ├── fpp.ping.random
    │   └── fpp.ping.bulk
    ├── fpp.mine
    │   ├── fpp.mine.start
    │   ├── fpp.mine.once
    │   ├── fpp.mine.stop
    │   └── fpp.mine.area
    ├── fpp.place
    │   ├── fpp.place.start
    │   ├── fpp.place.once
    │   └── fpp.place.stop
    ├── fpp.useitem (/ fpp.use.cmd)
    │   ├── fpp.useitem.start
    │   ├── fpp.useitem.once
    │   └── fpp.useitem.stop
    ├── fpp.rank
    │   ├── fpp.rank.set
    │   ├── fpp.rank.clear
    │   └── fpp.rank.bulk
    └── fpp.rename
        └── fpp.rename.own
```

---

## Scope Model

FPP uses three ownership scopes for applicable commands:

| Scope | Meaning | How to Grant |
|-------|---------|-------------|
| **own** | Control only bots you spawned | `fpp.<cmd>.own` |
| **others** | Control any bot (implies own) | `fpp.<cmd>` base node |
| **bulk / all** | Target all bots at once | `fpp.<cmd>.bulk` or `fpp.delete.all` |

**Example — Despawn:**

```bash
# Can only despawn own bots
/lp user Alice permission set fpp.despawn.own true

# Can despawn any individual bot (not bulk)
/lp user Alice permission set fpp.despawn true
/lp user Alice permission set fpp.despawn.bulk false  # negate bulk

# Full despawn control
/lp user Alice permission set fpp.despawn true
```

---

## LuckPerms Tab-Completion

All nodes declared in `plugin.yml` are auto-discovered by LuckPerms for tab-completion:

```bash
/lp user Alice permission set fpp.<TAB>
```

> If a node doesn't appear in tab-complete, verify it exists in `plugin.yml` and restart
> the server (LuckPerms reads `plugin.yml` on load).

---

## Troubleshooting

**Player can't use a command despite having permission:**  
1. Check `fpp.command` is not negated for their group  
2. Run `/lp user <name> permission check fpp.<node>` to see resolved value  
3. Ensure `fpp.use` is not negated (it contains `fpp.command`)  

**Admin can't see plugin info panel (bare `/fpp`):**  
Grant `fpp.plugininfo` explicitly — it is NOT included in `fpp.use`.

**Bot limit not working:**  
Assign `fpp.spawn.limit.<n>` — the plugin scans 1–100 and picks the highest match.  
`fpp.spawn.limit.1` is included in `fpp.use` (everyone). Assign `fpp.spawn.limit.5` for 5 bots.

**`--all` / bulk despawn blocked:**  
Requires `fpp.delete.all` (legacy) or `fpp.despawn.bulk` (new). Both are included in `fpp.op`.

---

## Version History

**v1.6.5** (Current)
- Added `fpp.admin` as the new preferred admin wildcard (alias of `fpp.op`)
- Added `fpp.despawn` / `fpp.despawn.bulk` / `fpp.despawn.own` (new names for delete nodes)
- Added `fpp.use.cmd` alias for `fpp.useitem`
- Added `fpp.spawn.mass` alias for `fpp.spawn.multiple`
- Added `fpp.spawn.coords` for coordinate-based spawn
- Added granular action nodes: `fpp.chat.*`, `fpp.move.*`, `fpp.ping.*`
- Added `fpp.mine.*`, `fpp.place.*`, `fpp.useitem.*` action sub-nodes
- Added `fpp.rank.*` action sub-nodes
- All legacy nodes preserved with full backward compatibility

**v1.6.4**  
Added `fpp.ping` for the new ping command.

**v1.6.2**  
Added `fpp.place`, `fpp.storage`, `fpp.useitem`, `fpp.waypoint`.

---

**See also:**  
`COMMANDS.md` · `Configuration.md` · `Database.md`

