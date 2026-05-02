# 🔍 Find Command

> **Bot block-finding and progressive mining — v1.6.6.8**

The `/fpp find` command sends a bot to scan nearby chunks for a specific target block and mine it progressively. It is useful for automated resource gathering without pre-defining a cuboid region.

---

## ⌨️ Command Syntax

```text
/fpp find <bot> <material> [--range <blocks>] [--count <n>] [--prefer-visible]
/fpp find <bot> --stop
/fpp find --stop
```

| Argument | Description |
|----------|-------------|
| `<bot>` | Target bot name |
| `<material>` | Block type to search for (e.g. `DIAMOND_ORE`, `OAK_LOG`) |
| `--range <blocks>` | Search radius in blocks (default: 32, max: 128) |
| `--count <n>` | Maximum blocks to mine before stopping (default: unlimited) |
| `--prefer-visible` | Use ray-tracing to prioritise blocks the bot can directly see |
| `--stop` | Cancel the active find task (single bot or all bots) |

---

## 🎯 How It Works

1. The bot scans loaded chunks within the specified range (spherical, default 32 blocks, max 128).
2. It finds the nearest matching block (or the most visible one if `--prefer-visible` is set).
3. It pathfinds to the block using `PathfindingService` and mines it with `MineCommand`.
4. `FppBotBlockBreakEvent` is fired before each break (cancellable by addons).
5. It repeats the scan → pathfind → mine loop until:
   - no more target blocks are found in range
   - the `--count` limit is reached
   - the task is cancelled (`/fpp find --stop` or `/fpp stop`)

---

## 📝 Examples

```text
/fpp find MinerBot DIAMOND_ORE
```
Mine all diamond ore within 32 blocks.

```text
/fpp find LumberBot OAK_LOG --range 64 --count 10
```
Mine up to 10 oak logs within 64 blocks.

```text
/fpp find ScoutBot IRON_ORE --range 128 --prefer-visible
```
Mine iron ore within 128 blocks, prioritising visible blocks.

```text
/fpp find MinerBot --stop
```
Cancel the active find task for MinerBot.

```text
/fpp find --stop
```
Cancel all active find tasks.

---

## ⚙️ Pathfinding Integration

Find respects the global `pathfinding` config:

- `pathfinding.max-range` caps how far the bot will walk
- `pathfinding.max-nodes` limits A* complexity
- `pathfinding.break-blocks` must be enabled for the bot to break obstructing blocks

---

## 🔐 Permission

| Permission | Description |
|------------|-------------|
| `fpp.find` | Use `/fpp find` |

---

## 🔗 Related Pages

- [Commands](Commands)
- [Configuration](Configuration)
- [Stop-Command](Stop-Command)