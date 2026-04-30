# 💾 Save Command

> **Immediately checkpoint all active bot data — v1.6.6.8**

`/fpp save` writes a snapshot of all currently active bots to the persistence layer without waiting for a server shutdown. This is useful for manual checkpointing before planned restarts or maintenance.

---

## Command Syntax

```
/fpp save
```

No arguments required.

## What Gets Saved

- Bot names, UUIDs, locations, worlds
- Inventory contents and XP
- All per-bot settings (frozen, chat tier, AI personality, PvE mode, pathfinding overrides, etc.)
- Active task state (mine, use, place, patrol, follow)

## Requirements

- **Persistence must be enabled** — `persistence.enabled: true` in `config.yml` (default: `true`)
- If persistence is disabled, the command will display an error message

## Permission

| Permission | Description | Default |
|---|---|---|
| `fpp.save` | Run `/fpp save` | op (child of `fpp.op`) |

## When to Use

- Before a **planned restart** — ensures the latest state is on disk
- After **major bot reconfiguration** — checkpoint your work
- During **testing** — verify persistence state without shutting down

> **Note:** FPP automatically saves all bot data on server shutdown. `/fpp save` is an optional manual checkpoint for extra safety between restarts.

---

> **See also:** [Bot Behaviour](Bot-Behaviour.md) · [Database](Database.md) · [Permissions](Permissions.md)