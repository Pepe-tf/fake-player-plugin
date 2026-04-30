# ⚔ Attack Command

> **Bot attack automation — classic, mob-targeting, and hunt modes — v1.6.6.8**

`/fpp attack` makes a bot attack entities. It supports three distinct modes: classic (walk to sender and attack), mob mode (stationary auto-targeting), and hunt mode (autonomous roaming mob hunt).

---

## Command Syntax

```
/fpp attack <bot|--all> [--once|--stop|--mob [--range <n>] [--type <mob>] [--priority <mode>] [--move]]  |  --hunt [<mob>] [--range <n>] [--priority <mode>]]  |  --stop
```

## Modes

### Classic Mode (default)

Walks the bot to the command sender, locks it in place with `lockForAction()`, then continuously attacks entities in the bot's look direction using `nms.attack()`.

| Flag | Description |
|---|---|
| `--once` | Single hit, then stop |
| `--stop` | Stop attacking |

Respects 1.9+ weapon cooldown system with per-item cooldown timers (swords 12t, axes 20-25t, trident 22t, mace 33t, etc.).

### Mob Mode (`--mob`)

Stationary auto-targeting. The bot stays in place, scans for nearby hostile mobs, smoothly rotates toward the best target, and attacks with weapon cooldowns.

| Flag | Default | Description |
|---|---|---|
| `--range <n>` | 8.0 | Scan radius in blocks (1–64) |
| `--type <mob>` | (all hostile) | Specific EntityType to target (e.g. `ZOMBIE`, `SKELETON`) |
| `--priority <mode>` | `nearest` | `nearest` or `lowest-health` |
| `--move` | off | Enables pursuit — bot chases the target when out of melee range via PathfindingService |

When `--move` is used, the bot maps to `PveSmartAttackMode.ON_MOVE` — it navigates to the target, stops and attacks when in range.

**Target defaults (when `--type` is empty):** Monster, Slime, Shulker, Phantom, EnderDragon, Ghast, Hoglin. Never targets players.

### Hunt Mode (`--hunt`)

Autonomous roaming mob hunt. The bot is **not locked** at a position — it freely roams and hunts mobs within a larger range.

| Flag | Default | Description |
|---|---|---|
| `[<mob>]` | (all hostile) | Optional mob type after `--hunt` |
| `--range <n>` | 32 | Scan radius in blocks (1–64) |
| `--priority <mode>` | `nearest` | `nearest` or `lowest-health` |

Two concurrent tasks:
1. **1-tick combat task** — rotation smoothing + melee attacks with weapon cooldowns
2. **20-tick scan task** — finds the next target, navigates via PathfindingService

## Per-Bot PvE Smart Attack Mode

The PvE attack behaviour is also configurable per-bot via BotSettingGui (🗡 PvE tab) without using the `/fpp attack` command:

| Mode | Behaviour |
|---|---|
| `OFF` | No auto-attack |
| `ON_NO_MOVE` | Stationary auto-targeting (like `--mob` without `--move`) |
| `ON_MOVE` | Pursues mob targets (like `--mob --move`) |

`pveEnabled` is a convenience accessor that maps to `pveSmartAttackMode.isEnabled()`.

The BotSettingGui PvE tab also exposes: mob type selector (visual paginated GUI), detect range (1–64), and target priority (nearest / lowest-health).

## Permissions

| Permission | Description | Default |
|---|---|---|
| `fpp.attack` | All attack modes (classic, mob, hunt) | op (child of `fpp.op`) |
| `fpp.attack.hunt` | Roaming hunt mode (`--hunt`) | op (child of `fpp.op`) |

## Persistence

- Classic mode: lock location persisted for resume after restart
- Mob mode: resumed via `startMobModeFromSettings()` from BotSettingGui
- Hunt mode: not persisted across restarts (intentionally — roaming position is unpredictable)

## Examples

```
/fpp attack SteveBot                  # Classic: walk to me and attack
/fpp attack SteveBot --once          # Single hit
/fpp attack SteveBot --mob           # Stationary auto-target mobs
/fpp attack SteveBot --mob --move    # Pursue mob targets
/fpp attack SteveBot --hunt          # Autonomous roaming hunt
/fpp attack SteveBot --hunt ZOMBIE --range 48  # Hunt zombies in 48-block range
/fpp attack --stop                    # Stop all attacking bots
```

---

> **See also:** [Bot Behaviour](Bot-Behaviour.md) · [Permissions](Permissions.md) · [Find Command](Find-Command.md)