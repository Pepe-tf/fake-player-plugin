# Bot Behaviour

> **How FPP bots behave in the world, in chat, and across restarts**

---

## Overview

FPP bots are built around a real **NMS `ServerPlayer`** entity when physical bodies are enabled.

That means a bot can have:
- tab-list presence
- server-list / player-count presence
- chat presence
- a real in-world player body
- inventory, armor, offhand, XP, and position
- pathfinding and action tasks

This is not a mannequin stack or NPC armor-stand trick — it is a real player-like server entity.

---

## Bot Architecture

```text
FakePlayer
├── name / uuid / display name
├── chat state
├── LP group / display state
├── inventory / armor / offhand / XP
├── bodyless or physical body state
├── optional ServerPlayer body
├── per-bot overrides
│   ├── respawn-on-death
│   ├── auto-eat
│   ├── auto-place-bed
│   ├── auto-milk
│   ├── prevent-bad-omen
│   ├── nav avoid water / lava
│   ├── default water path avoidance
│   ├── simulated ping
│   └── share control
├── metadata map (addon key-value storage)
└── behavior systems
    ├── head AI
    ├── swim AI
    ├── collision physics
    ├── fake chat
    ├── AI conversations
    ├── follow-target AI
    ├── PvE smart attack AI
    ├── swap / peak-hours scheduling
    └── shared pathfinding + actions
```

---

## Physical Body

When `body.enabled: true`, each bot spawns as a full `ServerPlayer` body.

### What that means

- proper hitbox
- visible player model
- skin support
- item / XP pickup support
- damage / death behavior
- world position and chunk-loading behavior

Relevant config:

```yaml
body:
  enabled: true
  pushable: true
  damageable: true
  pick-up-items: true
  pick-up-xp: true
  drop-items-on-despawn: true
```

---

## In-World Interaction Shortcuts

### Right-click bot

Normal right-click does one of two things:

1. opens the bot inventory GUI, or
2. runs the bot's stored right-click command if one has been configured via `/fpp cmd --add`

### Shift + right-click bot

If enabled, shift-right-click opens the **per-bot settings GUI** (`BotSettingGui`).

Config:

```yaml
bot-interaction:
  right-click-enabled: true
  shift-right-click-settings: true
```

---

## Per-Bot Settings GUI (`BotSettingGui`)

This is different from the global `/fpp settings` GUI. Opened by **shift + right-clicking** a bot entity.

### Categories

1. Gear **General** — frozen toggle, respawn-on-death toggle, head-AI toggle, swim-AI toggle, chunk-load-radius (numeric prompt), pick-up-items toggle, pick-up-xp toggle, auto-milk toggle, prevent-bad-omen toggle, rename action, share-control (grant/revoke controller access to other players)
2. Speech Bubble **Chat** — chat enabled/disabled, tier, AI personality
3. Sword **PvE** — smart-attack mode (cycles OFF, ON_NO_MOVE, ON_MOVE), mob type selector (visual GUI), detect range (numeric prompt), target priority (nearest / lowest-health)
4. Compass **Pathfinding** — follow-player toggle, parkour, break-blocks, place-blocks, avoid-water, avoid-lava
5. Warning **Danger** — reset-all-settings (requires double-click to confirm), delete bot (requires double-click to confirm)

It is designed for quick per-bot tuning without command spam.

---

## Command Blocking and Protection

FPP blocks bots from behaving like real players in places where that would break other systems.

### Command blocking

Bots are protected from normal command execution paths used by other plugins.

Important nuance:
- normal command execution by bots is blocked
- `/fpp cmd` intentionally uses a safe dispatch path so admins can still trigger bot actions on purpose

### Spawn protection

Bots receive a short spawn grace period so lobby/spawn plugins do not immediately teleport them away from the intended spawn location.

---

## Head AI

Bots can look at nearby players.

```yaml
head-ai:
  enabled: true
  look-range: 8.0
  turn-speed: 0.3
  tick-rate: 3
```

What it does:
- scans for nearby players
- picks a target in range
- rotates smoothly rather than snapping instantly

This is disabled while certain action locks are active so the bot does not turn away during mining / placing / using.

---

## Swim AI

```yaml
swim-ai:
  enabled: true
```

When enabled, bots automatically swim upward in water or lava like a player holding jump.

Per-bot override: each bot has its own `swimAiEnabled` toggle (initialised from the global config at spawn). Toggle it in `BotSettingGui` General tab or programmatically via `fp.setSwimAiEnabled(boolean)`.

---

## Chunk Loading

```yaml
chunk-loading:
  enabled: true
  radius: "auto"
  update-interval: 20
```

Per-bot override: each bot has its own `chunkLoadRadius` field:
- `-1` = follow global `chunk-loading.radius`
- `0` = disable chunk loading for this bot
- `1-N` = fixed chunk radius (capped at global max)

Set it in `BotSettingGui` General tab (numeric chat prompt) or programmatically via `fp.setChunkLoadRadius(int)`.

---

## Collision and Physics

Relevant config:

```yaml
collision:
  walk-radius: 0.85
  walk-strength: 0.22
  hit-strength: 0.45
  hit-max-horizontal-speed: 0.80
  bot-radius: 0.90
  bot-strength: 0.14
  max-horizontal-speed: 0.30
```

Behavior includes:
- player pushing a bot by walking into it
- attack knockback
- bot-vs-bot separation to avoid clustering

If `body.pushable: false`, bots become effectively immovable.

---

## Damage, Death, and Respawn

Relevant config:

```yaml
combat:
  max-health: 20.0
  hurt-sound: true

death:
  respawn-on-death: false
  respawn-delay: 15
  suppress-drops: false
```

Behavior:
- bots can take environmental and combat damage when `body.damageable: true`
- they can die like a player
- they can optionally respawn after a delay
- item/XP death drops can be suppressed

#### Per-bot respawn-on-death

Each bot has its own `respawnOnDeath` flag (initialized from `death.respawn-on-death`). When enabled, the bot auto-respawns after death instead of being removed from the world. Toggle it in `BotSettingGui` General tab or programmatically via `fp.setRespawnOnDeath(boolean)`.

WorldGuard integration can also prevent player-sourced damage in no-PvP regions.

---

## Item / XP Pickup

Global defaults come from config:

```yaml
body:
  pick-up-items: true
  pick-up-xp: true
```

Per-bot overrides are available in `BotSettingGui`.

Important behavior:
- turning item pickup off for a specific bot can drop its held inventory to the ground immediately
- turning XP pickup off for a specific bot can drop stored XP to the ground immediately
- XP collection also interacts with `/fpp xp` cooldown logic

---

## PvE Smart Attack Mode

Each bot has a tri-state `PveSmartAttackMode` setting that controls automatic hostile-mob targeting:

| Mode | Behavior |
|---|---|
| `OFF` | No auto-attacking |
| `ON_NO_MOVE` | Stationary auto-targeting — bot scans for mobs within range and attacks them, but does not move |
| `ON_MOVE` | Pursues mob targets via `PathfindingService` — bot navigates to out-of-range targets and attacks on arrival |

The legacy boolean `pveEnabled` is a convenience accessor that maps to `pveSmartAttackMode.isEnabled()`.

Configurable per-bot via `BotSettingGui` PvE tab (cycles through the three states).

Additional per-bot PvE fields:
- `pveRange` — detect/scan radius (default from `attack-mob.default-range`)
- `pvePriority` — `nearest` or `lowest-health` (default from `attack-mob.default-priority`)
- `pveMobTypes` — filtered entity type set; empty = all hostile mobs

---

## Per-Bot Automation

### Auto-eat

Each bot has an `autoEatEnabled` toggle (initialized from `automation.auto-eat`). When enabled, bots automatically eat food from their inventory when hunger prevents sprinting.

### Auto-place-bed

Each bot has an `autoPlaceBedEnabled` toggle (initialized from `automation.auto-place-bed`). When enabled, bots may place a bed from inventory for auto-sleep and break it after waking.

### Auto-Milk and Bad Omen Prevention

Each bot has two per-bot toggles that control harmful effect handling:

- **`autoMilkEnabled`** (initialized from `automation.auto-milk`, default `true`) — when enabled, the bot automatically removes all `HARMFUL` potion effects each tick.
- **`preventBadOmen`** (initialized from `automation.prevent-bad-omen`, default `true`) — when enabled, the bot prevents `BAD_OMEN`, `RAID_OMEN`, and `TRIAL_OMEN` effects from being applied.

Config:

```yaml
automation:
  auto-milk: true
  prevent-bad-omen: true
```

Both toggles are available in `BotSettingGui` General tab and persisted to the database (schema v22). They can also be set programmatically via `fp.setAutoMilkEnabled(boolean)` and `fp.setPreventBadOmen(boolean)`.

> **Note:** These settings exist and are persisted, but the underlying `BotEffectHandler.tickEffects()` method is not yet wired into the main bot tick loop. The toggles will have no runtime effect until the tick integration is completed in a future update.

### Default Water Path Avoidance

Each bot has a `defaultWaterPathAvoidanceEnabled` flag (initialized `true`) that controls the default value for `navAvoidWater` and `navAvoidLava` when the bot is first spawned. This is separate from the global pathfinding config — it provides a per-bot baseline that the per-bot Pathfinding tab toggles then override.

When `defaultWaterPathAvoidanceEnabled` is `true` (the default), a freshly spawned bot starts with water and lava avoidance enabled. When `false`, the bot starts with avoidance disabled.

Set programmatically via `fp.setDefaultWaterPathAvoidanceEnabled(boolean)`.

### Nav avoid water / lava

Per-bot pathfinding overrides:
- `navAvoidWater` — when `true`, the bot's pathfinding avoids water paths (default follows `defaultWaterPathAvoidanceEnabled`)
- `navAvoidLava` — when `true`, the bot's pathfinding avoids lava paths (default follows `defaultWaterPathAvoidanceEnabled`)

These can be toggled per-bot in `BotSettingGui` Pathfinding tab.

---

## Share Control

Owners and admins can grant or revoke controller access to other players for a specific bot. This is accessible via the **share-control** entry in `BotSettingGui` General tab. Controllers can perform limited operations on the bot (move, inventory, etc.) without being the the full owner.

---

## Ping Simulation

Bots can display a realistic simulated ping value in the tab list.

### Global config

```yaml
ping:
  enabled: false
  min: 25
  max: 180
  latency-effect: true
  spike-chance: 0.05
  spike-min: 250
  spike-max: 500
  join-ramp-ticks: 60
```

| Key | Default | Description |
|---|---|---|
| `enabled` | `false` | Master toggle for ping simulation (changed from `true` in config v70) |
| `min` | `25` | Minimum simulated ping (ms) |
| `max` | `180` | Maximum simulated ping (ms) |
| `latency-effect` | `true` | Whether simulated ping affects the in-game latency bars |
| `spike-chance` | `0.05` | Chance per tick of a temporary ping spike |
| `spike-min` | `250` | Minimum spike ping (ms) |
| `spike-max` | `500` | Maximum spike ping (ms) |
| `join-ramp-ticks` | `60` | Ticks over which ping ramps from 0 to its target after a bot joins (simulates a realistic connection) |

### Per-bot overrides

Each bot has its own `ping` field (default `-1`, meaning use the server-generated value). Set a specific visible ping with:

```text
/fpp ping <bot> --ping <ms>
/fpp ping <bot> --random
/fpp ping <bot>
```

- `--ping <ms>` sets an explicit value (0–9999)
- `--random` assigns a random realistic value within the configured min/max range
- No flag shows the current ping

Bulk operations:

```text
/fpp ping --ping <ms> --count <n>
```

Permissions:
- `fpp.ping` — view bot ping
- `fpp.ping.set` — set a specific value
- `fpp.ping.random` — assign random ping
- `fpp.ping.bulk` — bulk `--count` operations

Per-bot ping values are persisted to the database (`ping` and `ping_user_set` columns in `fpp_active_bots`, schema v22).

---

## Despawn Snapshots

When a bot is despawned (via `/fpp despawn`, the Danger tab in `BotSettingGui`, or any other removal path), FPP can capture a snapshot of the bot's inventory and XP before the entity is destroyed.

This data is stored in the **`fpp_despawn_snapshots`** database table (schema v17+). Each snapshot row records:

- bot UUID
- full inventory contents (main inventory, armor, offhand)
- total XP, level, and level progress
- timestamp

Snapshots are loaded on startup via `FakePlayerManager.initDespawnSnapshots()` and are available for inspection or restoration by addons. When persistence is enabled, bots that are restored on restart reload their state from `fpp_active_bots` rather than from snapshots — however, snapshots serve as a safety net for non-graceful shutdowns and for addon-driven restoral flows.

---

## Bot Metadata

Each bot carries a transient key-value metadata map intended for addon use. This map is **not persisted** across restarts — it exists only for the lifetime of the bot's session.

### API surface

```java
// Set or overwrite a value
fp.setMetadata(String key, Object value);

// Retrieve a value (null if absent)
Object value = fp.getMetadata(String key);

// Check existence
boolean has = fp.hasMetadata(String key);

// Remove a single entry
fp.removeMetadata(String key);

// Get the entire map (read-only view)
Map<String, Object> map = fp.getMetadataMap();

// Clear all metadata
fp.clearMetadata();
```

### Addon API equivalent

```java
// Via FppBot interface
bot.setMetadata(String key, Object value);
Object value = bot.getMetadata(String key);
boolean has = bot.hasMetadata(String key);
bot.removeMetadata(String key);
Map<String, Object> map = bot.getMetadataMap();
bot.clearMetadata();
```

Common uses: tracking addon-specific state (e.g., cooldowns, task progress, custom flags) without needing a separate external data store.

---

## Shared Pathfinding and Action Engine

Navigation is now centralized through **`PathfindingService`**.

Used by:
- `/fpp move`
- `/fpp mine`
- `/fpp place`
- `/fpp use`
- `/fpp follow`
- `/fpp attack`
- waypoint patrols

### Supported move types

- `WALK`
- `ASCEND`
- `DESCEND`
- `PARKOUR`
- `BREAK`
- `PLACE`
- `PILLAR`
- `SWIM`
- `CLIMB`

### Path options (per-bot overrides)

These flags are read from each bot's per-bot settings at path time:

- `parkour` — allow parkour jumps (default from `pathfinding.parkour`)
- `breakBlocks` — allow breaking blocks in the path (default from `pathfinding.break-blocks`)
- `placeBlocks` — allow placing blocks in the path (default from `pathfinding.place-blocks`)
- `avoidWater` — avoid water (per-bot `navAvoidWater`)
- `avoidLava` — avoid lava (per-bot `navLavaAvoidLava`)

### Shared helpers

- `BotNavUtil` — stand positions, facing, action-location checks, block use helpers
- `StorageInteractionHelper` — lock, open, transfer, unlock flow for storage containers

### Action lock handoff

Some navigation flows use an atomic "arrive and lock" handoff so the bot does not drift or rotate away in the tick between movement completion and action start.

---

## Movement Modes

### Continuously follow a player

```text
/fpp follow <bot|all> <player>
/fpp follow <bot|all> --stop
```

The bot continuously follows an online player using `PathfindingService` (Owner `FOLLOW`).

- Path recalculates whenever the target moves >3.5 blocks (configurable via `pathfinding.follow-recalc-distance`) or every 60 ticks
- Arrival distance: 2.0 blocks; re-navigates 5 ticks after arrival for smooth continuous following
- Respects `pathfinding.max-fall` — will not choose paths with unsafe drops
- FOLLOW task persisted in `fpp_bot_tasks` — bot resumes following after restart if the target is online
- Permission: `fpp.follow`

### Follow a player (one-shot / navigate-to)

```text
/fpp move <bot> <player>
/fpp move <bot|all> --to <player>
```

The bot navigates to the target player. `--to` is the canonical flag form; the positional syntax still works.

> **Tip:** Use `/fpp follow` when you want the bot to keep following indefinitely and survive restarts. Use `/fpp move` for one-shot navigation to a player position where the bot should stop on arrival.

### Roam mode (autonomous random wander)

```text
/fpp move <bot|all> --roam [x,y,z] [radius]
/fpp move <bot|all> --stop
```

The bot wanders continuously within a fixed radius (3-500 blocks) around a center point.

- If no coordinates are given, the bot's current position becomes the center
- Roam state persists across restarts via `data/bot-tasks.yml` (YAML-only; not stored in the DB task table)
- Respects `pathfinding.max-fall` — will not choose paths with unsafe drops

### Patrol a waypoint route

```text
/fpp move <bot> --wp <route>
```

The bot walks a named route built with `/fpp waypoint`.

No prior `/fpp wp create` step needed — `/fpp wp add <route>` creates the route automatically on first use.

### Stop movement

```text
/fpp move <bot> --stop
```

---

## Mining, Placing, Using, and Storage

### Mining

Bots can mine a looked-at block continuously or in a cuboid area.

Area mining supports:
- `--pos1`
- `--pos2`
- `--start`
- `--status`
- `--stop`

### Placing

Bots can place blocks continuously or once.

### Using

Bots can right-click / activate the block they are looking at.

### Storage integration

Registered storage containers can be used for:
- depositing mined items
- fetching placement materials

---

## Task Persistence

Active tasks now survive restart.

This includes:
- `MINE`
- `USE`
- `PLACE`
- `PATROL`
- `FOLLOW` — bot resumes following the last target player if they are online after restart

Persistence source:
- DB: `fpp_bot_tasks`
- YAML fallback: `data/bot-tasks.yml`

That means a bot can restart and continue:
- a mine job
- a place job
- a use job
- a waypoint patrol
- following a specific player

---

## Fake Chat Behavior

Bots can chat autonomously using:
- random intervals
- activity tiers
- burst messages
- mention replies
- bot-to-bot conversations
- event-triggered reactions
- player-chat reactions

See [Fake-Chat](Fake-Chat).

---

## AI Conversations

Separate from fake chat, bots can reply privately to:
- `/msg`
- `/tell`
- `/whisper`

These use:
- `ai-conversations.*` config
- provider keys in `secrets.yml`
- default / custom personality files from `personalities/`

Per-bot personality assignment:

```text
/fpp personality <bot> set <name>
```

---

## Rename Behavior

Bots can be renamed live with:

```text
/fpp rename <old> <new>
```

The rename system fully preserves important state and suppresses fake join/leave spam during the rename lifecycle.

Preserved state includes:
- inventory
- XP
- LuckPerms group
- chat settings
- AI personality
- stored command
- frozen state

---

## Swap and Peak-Hours Behavior

### Swap system

Bots can rotate out after a configurable session and come back later.

Important config keys:
- `swap.min-online`
- `swap.retry-rejoin`
- `swap.retry-delay`
- `swap.farewell-chat`
- `swap.greeting-chat`

### Peak hours

Peak-hours scales the number of active AFK bots based on real-world time windows.

Important notes:
- requires `swap.enabled: true`
- only AFK bots are managed
- sleeping state is crash-safe when DB is enabled

---

## Performance Notes

For large bot counts, the biggest behavior-related cost centers are:
- chunk loading
- pathfinding
- chat/event systems
- frequent position syncs

Relevant config:

```yaml
performance:
  position-sync-distance: 128.0
```

Tips:
- reduce bot counts first
- disable systems you do not need
- keep chunk loading on `auto` unless you need a fixed radius
- use freezes for idle bots

---

## Troubleshooting

### Bot does not rotate its head

Check:
- `head-ai.enabled: true`
- `head-ai.look-range` is high enough
- the bot is not frozen or action-locked

### Shift-right-click does nothing

Check:
- `bot-interaction.right-click-enabled: true`
- `bot-interaction.shift-right-click-settings: true`
- you are actually sneaking

### Bot does not continue task after restart

Check:
- `persistence.enabled: true`
- DB is available, or YAML fallback files are writable
- task is one of the persisted task types (mine/use/place/patrol/follow)

### Bot is not reacting in DMs

Check:
- `ai-conversations.enabled: true`
- a valid provider key is in `secrets.yml`
- the personality file exists and was reloaded

### Auto-milk or Bad Omen prevention not working

The per-bot `autoMilkEnabled` and `preventBadOmen` toggles exist and are persisted, but `BotEffectHandler.tickEffects()` is not yet wired into the tick loop. These settings will take effect in a future update.

---

## Related Pages

- [Commands](Commands)
- [Configuration](Configuration)
- [Fake-Chat](Fake-Chat)
- [Swap-System](Swap-System)
- [Peak-Hours](Peak-Hours)
- [Skin-System](Skin-System)