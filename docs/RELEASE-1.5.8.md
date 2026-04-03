# FPP v1.5.8 Release Notes

**Release date:** 2026-04-03  
**Config version:** 37 (no structural changes — version stamp only)  
**Build:** `mvn -DskipTests clean package`  
**Jar:** `target/fpp-1.5.8-obfuscated.jar`

---

## Summary

v1.5.8 is a stability and accuracy release focused on four key areas:

1. **Ghost player elimination** — Fixed the "Anonymous User" / UUID 0 ghost entry appearing in the tab list
2. **Placeholder accuracy** — `%fpp_real%` and `%fpp_total%` now correctly account for bots in `Bukkit.getOnlinePlayers()`
3. **NETWORK mode `/fpp list` improvements** — Proxy-aware bot listing with remote bots
4. **LuckPerms safety** — `NoClassDefFoundError` crash eliminated on LP-free servers

---

## Changes

### 🐛 Bug Fixes

#### Ghost "Anonymous User" Fix (`FakeConnection` subclass)
- **Problem:** When bots were spawned, a phantom "Anonymous User" entry with UUID `0` appeared in the tab list. Additionally, `NullPointerException` and `ClassCastException` spam appeared in logs.
- **Root cause:** The old approach used reflection to set `Connection` internals; the resulting connection object was not a proper `Connection` subclass and caused the Minecraft server to register a null/malformed player alongside the bot.
- **Fix:** `FakeConnection` is now a proper `Connection` subclass. Its `send()` overrides are clean no-ops. Injected into `ServerPlayer.connection` immediately after `placeNewPlayer()` so `awaitingPositionFromClient` stays `null`.

#### `%fpp_real%` / `%fpp_real_<world>%` Accuracy
- **Problem:** `%fpp_real%` returned the same value as `Bukkit.getOnlinePlayers().size()` — which includes bots since they go through `placeNewPlayer()`.
- **Fix:** `%fpp_real%` now subtracts `FakePlayerManager.getActivePlayers().size()` from `Bukkit.getOnlinePlayers().size()`. Per-world variant similarly filters out bots from its world count.

#### `%fpp_total%` Double-Counting Fix
- **Problem:** In NETWORK mode, `%fpp_total%` could double-count remote bots if they were included in both the local count and remote count.
- **Fix:** `%fpp_total%` = `fpp_real` + `fpp_local_count` + `fpp_network_count` — no overlap.

---

### ✨ New Features

#### NETWORK Mode `/fpp list` Improvements
- Local bots now display `[server-id]` tags: `🎭 Steve [survival] │ Owner: Admin │ ...`
- Remote bots from other proxy servers are listed in a dedicated **"Remote bots"** section showing:
  - Server ID
  - Bot name / display name
  - Skin status (`skin: auto` / `no skin`)
- Total bot count in the header includes both local and remote bots

#### New Proxy Placeholders
| Placeholder | Description |
|-------------|-------------|
| `%fpp_local_count%` | Bots on this server only |
| `%fpp_network_count%` | Bots on other proxy servers (0 in LOCAL mode) |
| `%fpp_network_names%` | Comma-separated display names from remote servers only |

In NETWORK mode, `%fpp_count%` = `local_count + network_count` and `%fpp_names%` includes both local and remote bot names.

---

### 🔒 Stability

#### LuckPerms ClassLoader Guard
- **Problem:** On servers without LuckPerms installed, calling LP API classes caused `NoClassDefFoundError: net/luckperms/api/node/Node` crash during bot spawn or LP-related operations.
- **Fix:** All LP-dependent code (especially group-related logic) is now wrapped in `LuckPermsHelper.isAvailable()` guards. LP API classes are never loaded unless `LuckPermsHelper.isAvailable()` returns `true`.

---

## Migration

Config version bumps from **36 → 37**. No structural key changes. Migration is a version-stamp only.

The migration runs automatically on first startup. No user action required.

---

## Compatibility

- Paper 1.21.0 – 1.21.11 ✅
- Java 21+ ✅
- PacketEvents 2.x ✅ (required)
- LuckPerms (optional) ✅
- PlaceholderAPI (optional) ✅
- Velocity / BungeeCord proxy ✅ (NETWORK mode)

---

## Files Changed

| File | Change |
|------|--------|
| `ConfigMigrator.java` | `CURRENT_VERSION` 36 → 37; added `v36to37()` |
| `config.yml` (resources) | `config-version: 37`; header updated to v1.5.8 |
| `README.md` | v1.5.8 changelog; 29+ placeholders; version footer |
| `README-BBCODE.txt` | v1.5.8 section; updated placeholder count; compat note |
| `AGENTS.md` | Version references updated to 1.5.8 / config-version 37 |
| `frontend/wiki/Home.md` | Version 1.5.8; 29+ placeholders; updated What's New |
| `frontend/wiki/Placeholders.md` | New proxy placeholders; updated examples |
| `frontend/wiki/Database.md` | Expanded: NETWORK mode, `fpp_active_bots` schema, flush interval, disable DB docs |
| `frontend/wiki/Proxy-Support.md` | Fixed `server-id` key path; new Remote Bot List feature |
| `frontend/wiki/Commands.md` | `/fpp list` NETWORK mode display format |
| `wiki/Proxy-Support.md` | Fixed `server-id` key path; added proxy placeholder rows |

