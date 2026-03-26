# PlaceholderAPI — Fake Player Plugin

All placeholders use the identifier `fpp` and require [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) to be installed.  
FPP auto-registers the expansion on startup when PlaceholderAPI is detected — no `/papi ecloud` download needed.

---

## Server-Wide Placeholders

These return the same value regardless of which player requests them.

| Placeholder | Return type | Description |
|-------------|-------------|-------------|
| `%fpp_count%` | `integer` | Number of fake player bots currently active |
| `%fpp_max%` | `integer` / `∞` | Global bot cap (`limits.max-bots`). Returns `∞` when the cap is `0` (unlimited) |
| `%fpp_real%` | `integer` | Number of **real** (non-bot) players currently online |
| `%fpp_total%` | `integer` | Real players + fake bots combined — useful for "server population" displays |
| `%fpp_online%` | `integer` | Alias for `%fpp_total%` — real players + bots combined |
| `%fpp_frozen%` | `integer` | Number of bots currently frozen via `/fpp freeze` |
| `%fpp_names%` | `string` | Comma-separated list of all active bot display names, e.g. `Steve, Alex, Notch` |
| `%fpp_version%` | `string` | Running plugin version, e.g. `1.4.27` |

---

## Config State Placeholders

These reflect the current `config.yml` values and update immediately after `/fpp reload`.

| Placeholder | Values | Config key |
|-------------|--------|------------|
| `%fpp_chat%` | `on` / `off` | `fake-chat.enabled` |
| `%fpp_swap%` | `on` / `off` | `swap.enabled` |
| `%fpp_body%` | `on` / `off` | `body.enabled` |
| `%fpp_pushable%` | `on` / `off` | `body.pushable` |
| `%fpp_damageable%` | `on` / `off` | `body.damageable` |
| `%fpp_tab%` | `on` / `off` | `tab-list.enabled` |
| `%fpp_skin%` | `auto` / `custom` / `off` | `skin.mode` |
| `%fpp_max_health%` | `number` | `combat.max-health` |

---

## Player-Relative Placeholders

These return values specific to the player requesting the placeholder.  
They require an **online** player context — they fall back gracefully when the context player is offline or `null`.

| Placeholder | Return type | Description |
|-------------|-------------|-------------|
| `%fpp_user_count%` | `integer` | Number of bots currently spawned **by this player** |
| `%fpp_user_max%` | `integer` | This player's personal bot limit. Resolved from `fpp.bot.<num>` permission nodes; falls back to `limits.user-bot-limit` in `config.yml` |
| `%fpp_user_names%` | `string` | Comma-separated display names of bots **owned by this player**, e.g. `bot-Steve-1, bot-Steve-2`. Empty string when the player has no bots |

---

## Per-World Placeholders

Append any world name to `count_`, `real_`, or `total_` to scope the count to that specific world.  
World names are **case-insensitive**.

| Placeholder | Return type | Description |
|-------------|-------------|-------------|
| `%fpp_count_<world>%` | `integer` | Bots whose current position is in `<world>` |
| `%fpp_real_<world>%` | `integer` | Real (non-bot) players currently in `<world>` |
| `%fpp_total_<world>%` | `integer` | Bots + real players in `<world>` combined |

**Examples** — world named `world`, nether `world_nether`, end `world_the_end`:

| Placeholder | What it returns |
|-------------|----------------|
| `%fpp_count_world%` | Bots in the overworld |
| `%fpp_real_world_nether%` | Real players in the nether |
| `%fpp_total_world_the_end%` | Everyone (bots + players) in the end |
| `%fpp_count_mycustomworld%` | Bots in a world named `mycustomworld` |

> **Bot world resolution:** FPP checks the live Mannequin body position first.  
> For bodyless bots (spawned without `body.enabled: true`) it falls back to the bot's last recorded spawn location.  
> Bots with no resolvable world are excluded from all per-world counts.

---

## Notes

### `%fpp_real%` vs `%fpp_total%` vs `%fpp_online%`

`%fpp_real%` uses `Bukkit.getOnlinePlayers().size()`, which only counts **real** Bukkit `Player` objects.  
Fake bots are **not** Bukkit players, so they are never counted in `%fpp_real%`.

```
%fpp_total%  =  %fpp_real%  +  %fpp_count%
%fpp_online% =  %fpp_real%  +  %fpp_count%   (identical — use whichever reads more naturally)
```

### `%fpp_max%` and unlimited servers

When `limits.max-bots: 0` (no cap), `%fpp_max%` returns the literal string `∞`.  
If you need a numeric check in another plugin, set an explicit cap and compare against `%fpp_count%`.

### `%fpp_user_max%` resolution

The limit is resolved in this order:

1. Highest `fpp.bot.<num>` permission node the player has (scans `fpp.bot.1` → `fpp.bot.100`)
2. Falls back to `limits.user-bot-limit` from `config.yml` if no personal node is found

### Using `%fpp_*%` inside FPP's own `tab-list-format`

FPP calls `PlaceholderAPI.setPlaceholders(null, display)` (server-wide context) when building display names,
so you can embed any `%fpp_*%` or other PAPI placeholder directly in `bot-name.tab-list-format`:

```yaml
bot-name:
  tab-list-format: '{prefix}{bot_name}{suffix} <gray>(%fpp_count% bots)'
```

Because the context is server-wide (`null` player), player-relative placeholders like `%fpp_user_count%` will return their fallback values (`0` / `""`) in this context — use them only in external plugins where a real player context exists.

---

## Usage Examples

### Scoreboard / hologram showing population

```
Real players: %fpp_real%
Fake bots:    %fpp_count%
─────────────────────
Total online: %fpp_total%
```

### TAB plugin header showing bot count

```yaml
header:
  - "<gray>Players: <white>%fpp_real% <gray>| Bots: <white>%fpp_count%"
```

### Show a player's own bot stats

```
Your bots: %fpp_user_count% / %fpp_user_max%
Names: %fpp_user_names%
```

### Dynamic join message (via another plugin)

```
Welcome! There are %fpp_real% players and %fpp_count% bots online.
```

### Per-world scoreboard

```
── Overworld ──────────────
  Players: %fpp_real_world%
  Bots:    %fpp_count_world%
  Total:   %fpp_total_world%

── Nether ─────────────────
  Players: %fpp_real_world_nether%
  Bots:    %fpp_count_world_nether%
```

---

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| All `%fpp_*%` return `%fpp_..%` unparsed | PlaceholderAPI is not installed or FPP failed to register — check console on startup |
| `%fpp_user_count%` always returns `0` | The context player is offline or null — this placeholder requires an online player |
| `%fpp_max%` shows `∞` but you set a number | Make sure you saved the config and ran `/fpp reload` |
| Values are stale after `/fpp reload` | Config-state placeholders update instantly; bot-count placeholders reflect live state and need no reload |

