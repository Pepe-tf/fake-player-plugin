# Placeholders (PlaceholderAPI)

This page covers all PlaceholderAPI placeholders provided by FPP.

FPP provides **18 placeholders** organized into three categories: **Server-Wide**, **Per-World**, and **Player-Relative**.

📚 **For complete documentation, see [PLACEHOLDERAPI.md](../PLACEHOLDERAPI.md)**

---

## Server-Wide Placeholders

| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%fpp_count%` | Number of active fake players (all worlds). Also supports per-world variant — see [Per-World Placeholders](#per-world-placeholders) | `3` |
| `%fpp_max%` | Global bot limit (0 = unlimited) | `50` or `∞` |
| `%fpp_real%` | Number of real players online | `12` |
| `%fpp_total%` | Real players + fake players | `15` |
| `%fpp_online%` | Alias for `%fpp_total%` | `15` |
| `%fpp_frozen%` | Number of frozen bots | `1` |
| `%fpp_names%` | Comma-separated bot names | `Steve, Alex, Notch` |
| `%fpp_version%` | Plugin version | `1.4.28` |

---

## Per-World Placeholders

Dynamic placeholders for specific worlds (case-insensitive):

| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%fpp_count_<world>%` | Bots in specific world | `%fpp_count_overworld%` → `2` |
| `%fpp_real_<world>%` | Real players in world | `%fpp_real_nether%` → `1` |
| `%fpp_total_<world>%` | Total players in world | `%fpp_total_end%` → `3` |

**Examples:**
- `%fpp_count_overworld%` — Bots in Overworld
- `%fpp_real_nether%` — Real players in Nether  
- `%fpp_total_end%` — All players in The End

---

## Player-Relative Placeholders

These vary based on the player viewing them:

| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%fpp_user_count%` | Bots owned by this player | `2` |
| `%fpp_user_max%` | Personal bot limit | `10` |
| `%fpp_user_names%` | Names of player's bots | `MyBot1, MyBot2` |

---

## Feature Status Placeholders

| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%fpp_chat%` | Fake chat enabled | `true` |
| `%fpp_swap%` | Swap system enabled | `false` |
| `%fpp_skin%` | Skin system enabled | `true` |
| `%fpp_body%` | Physical bodies enabled | `true` |
| `%fpp_tab%` | Tab list enabled | `true` |

---

## Usage Examples

### Scoreboard Sidebar
```
§7Online: §f%fpp_real%
§7Bots: §f%fpp_count%
§7Total: §f%fpp_total%
```

### Tab List Header
```
§6Server Name
§7Players: §f%fpp_real%§7/§f50 §8| §7Bots: §f%fpp_count%
```

### Per-World Display
```
§7Overworld: §f%fpp_total_overworld%
§7Nether: §f%fpp_total_nether%  
§7End: §f%fpp_total_end%
```

---

## Technical Notes

- **Bot World Detection:** Uses live Mannequin position first, then `spawnLocation` for bodyless bots
- **Real Players:** Uses `Bukkit.getOnlinePlayers().size()` (bots are not Bukkit `Player` objects)
- **Update Frequency:** All placeholders update in real-time when bots spawn/despawn
- **Performance:** Optimized for frequent requests (no expensive operations)

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Placeholder shows `%fpp_count%` literally | PlaceholderAPI not installed or placeholder not registered |
| Count is wrong | Check if bots are spawned correctly with `/fpp list` |
| Per-world not working | Verify world name is exact (case-insensitive) |
| User placeholders empty | Player has no bots or insufficient permissions |

---

## Requirements

- **PlaceholderAPI** 2.11.6+
- **FPP** 1.4.28+  
- **Paper** 1.21+

**Installation:** PlaceholderAPI auto-detects FPP - no additional setup required.
