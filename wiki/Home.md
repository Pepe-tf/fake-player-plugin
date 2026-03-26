# ꜰᴀᴋᴇ ᴘʟᴀʏᴇʀ ᴘʟᴜɢɪɴ — Wiki

## Welcome

**Fake Player Plugin (FPP)** is an advanced bot-spoofing plugin for Paper 1.21+ servers.  
It spawns realistic fake players that appear in the **tab list**, **server list**, **join/leave messages**, and as physical **Mannequin entities** in the world — indistinguishable from real players at a glance.

---

## Wiki Pages

| Page | Description |
|------|-------------|
| [Getting Started](Getting-Started.md) | Installation, requirements, and first launch |
| [Commands](Commands.md) | Every `/fpp` sub-command with usage and examples |
| [Permissions](Permissions.md) | Full permission node reference and LuckPerms setup |
| [Configuration](Configuration.md) | `config.yml` reference — every option explained |
| [Migration & Backups](Migration.md) | Auto-migration, backups, DB merge/export, SQLite→MySQL |
| [Language](Language.md) | `language/en.yml` — editing messages and colours |
| [Bot Names](Bot-Names.md) | `bot-names.yml` — managing the name pool |
| [Bot Messages](Bot-Messages.md) | `bot-messages.yml` — fake chat message pool |
| [Database](Database.md) | SQLite / MySQL storage setup and bot record info |
| [Skin System](Skin-System.md) | How bot skins work and the three skin modes |
| [Bot Behaviour](Bot-Behaviour.md) | Head AI, combat, death, chunk loading, and push |
| [Swap System](Swap-System.md) | Automatic bot rotation and personality system |
| [Fake Chat](Fake-Chat.md) | Bot chat AI configuration |
| [FAQ & Troubleshooting](FAQ.md) | Common issues and fixes |

---

## Quick-Start

```
1. Drop fpp.jar into your /plugins folder.
2. Restart the server — configs are generated automatically.
3. Join the server and run:  /fpp spawn 5
4. Watch five fake players appear in tab, chat, and the world.
```

---

## Feature Highlights

- ✅ Realistic tab-list entries with proper game-mode and latency display  
- ✅ Join / leave messages broadcast like real players  
- ✅ Physical **Mannequin** bodies with pushback, head-tracking, and hit-sounds  
- ✅ Automatic skin resolution (server-side, zero HTTP calls in `auto` mode)  
- ✅ Per-player bot limits via `fpp.bot.<num>` permission nodes  
- ✅ Bot **persistence** — bots leave on shutdown and rejoin after restart  
- ✅ **Swap system** — bots rotate with realistic personalities and timing  
- ✅ **Fake chat** — bots send messages from `bot-messages.yml` with a fully customisable `chat-format` (MiniMessage / `&` codes, `{bot_name}` / `{message}` placeholders)  
- ✅ **Customisable display names** — `tab-list-format` supports `{prefix}`, `{bot_name}`, `{suffix}` (LuckPerms) and any `%papi%` placeholder  
- ✅ **Toggleable body interaction** — independently disable push (`body.pushable`) or damage (`body.damageable`) hot-reloadable per config  
- ✅ **Database** — SQLite (default) or MySQL session history  
- ✅ Full **PlaceholderAPI** expansion — 15 server-wide (`%fpp_count%`, `%fpp_real%`, `%fpp_total%`, `%fpp_pushable%`, `%fpp_damageable%`, ...) + 3 player-relative (`%fpp_user_count%`, `%fpp_user_max%`, `%fpp_user_names%`)  
- ✅ Full **LuckPerms** compatibility — prefix, suffix, and weight ordering from LP groups  
- ✅ MiniMessage colour formatting throughout  
- ✅ **Auto-migration** — config upgrades automatically on update, never loses your data  
- ✅ **Backup system** — timestamped backups before every migration, 10-set rolling window  
- ✅ **DB merge/export** — merge old databases, export CSV, switch backends via `/fpp migrate`  

---

## Support & Permissions

This plugin is proprietary software.  
To request usage permissions or report issues, contact the owner on Discord: **Bill_Hub**

> See [LICENSE](../LICENSE) for the full terms.

