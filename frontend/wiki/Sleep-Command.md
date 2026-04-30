# 😴 Sleep Command

> **Night auto-sleep with temporary bed placement — v1.6.6.8**

The `/fpp sleep` command tells a bot where its "sleep origin" is. When night falls and enough players are in bed, the bot will pathfind to its sleep origin, temporarily place a bed from its inventory, sleep through the night, then break the bed and pick it back up.

---

## ⌨️ Command Syntax

```text
/fpp sleep <bot|all> <x y z> <radius>
/fpp sleep <bot|all> --stop
```

| Argument | Description |
|----------|-------------|
| `<bot>` | Target bot name |
| `all` | Apply to every active bot |
| `<x y z>` | Sleep-origin coordinates — the bot paths to the nearest free bed within `radius` of this point at night |
| `<radius>` | Search radius (in blocks) for finding a nearby bed from the sleep origin |
| `--stop` | Cancel sleep mode for the target bot(s) |

The sleep origin defines where the bot searches for beds. You can set it to your own coordinates, a bed location, or any safe area.

---

## 🛏️ How It Works

1. You run `/fpp sleep MinerBot 100 64 -200 32` to set a sleep origin at those coordinates with a 32-block search radius.
2. The bot stores that origin and radius.
3. When night arrives and sleep voting starts, the bot:
   - uses `PathfindingService` to navigate to the nearest free bed within the search radius
   - places a bed from its inventory if needed (requires one bed in inventory)
   - enters the bed
   - sleeps until morning
   - breaks the bed
   - picks the bed item back up
4. The bot then resumes its previous task (if any).

---

## 📋 Requirements

- Bot must have a **bed** in its inventory.
- Sleep origin must be a valid placement spot (2 blocks of air above a solid block).
- `automation.auto-place-bed: true` is recommended so bots naturally carry beds.

---

## ⚙️ Related Config

```yaml
automation:
  auto-place-bed: true
```

When `auto-place-bed` is enabled, bots automatically place beds when they need to sleep (not just via `/fpp sleep`, but any sleep trigger).

---

## 🗑️ Clearing a Sleep Origin

```text
/fpp sleep MinerBot --stop
```

Use `--stop` to cancel sleep mode and clear the bot's sleep origin. Alternatively:

- Despawn and respawn the bot
- Or set the origin to an unreachable location (the bot will skip sleeping)

---

## 🔐 Permission

| Permission | Description |
|------------|-------------|
| `fpp.sleep` | Use `/fpp sleep` |

---

## 🔗 Related Pages

- [Commands](Commands)
- [Configuration](Configuration)
- [Automation](Configuration#automation)
