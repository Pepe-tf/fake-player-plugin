> **🏠 Home** · [Getting Started](Getting-Started.md) · [Commands](Commands.md) · [Permissions](Permissions.md) · [Configuration](Configuration.md) · [Language](Language.md) · [Bot Names](Bot-Names.md) · [Bot Messages](Bot-Messages.md) · [Database](Database.md) · [Skin System](Skin-System.md) · [Bot Behaviour](Bot-Behaviour.md) · [Swap System](Swap-System.md) · [Fake Chat](Fake-Chat.md) · [FAQ & Troubleshooting](FAQ.md)

---

# ꜰᴀᴋᴇ ᴘʟᴀʏᴇʀ ᴘʟᴜɢɪɴ — Wiki

> **Version:** 1.0.15 · **Platform:** Paper 1.21+ · **Author:** Bill_Hub

---

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
- ✅ **Fake chat** — bots send configurable messages from `bot-messages.yml`  
- ✅ **Database** — SQLite (default) or MySQL session history  
- ✅ Full **LuckPerms** compatibility via Bukkit permission layer  
- ✅ MiniMessage colour formatting throughout  

---

## Support & Permissions

This plugin is proprietary software.  
To request usage permissions or report issues, contact the owner on Discord: **Bill_Hub**

> See [LICENSE](../LICENSE) for the full terms.

---

| ◀ *(first page)* | [🏠 Home](Home.md) | [Getting Started ▶](Getting-Started.md) |
|:---|:---:|---:|