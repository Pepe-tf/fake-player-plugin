# ꜰᴀᴋᴇ ᴘʟᴀʏᴇʀ ᴘʟᴜɢɪɴ (FPP)

> Spawn realistic fake players on your Paper server — complete with tab list, server list count, join/leave/kill messages, staggered join/leave delays, in-world physics bodies, skin support, and full hot-reload configuration.

![Version](https://img.shields.io/badge/version-0.1.5-0079FF?style=flat-square)
![MC](https://img.shields.io/badge/Minecraft-1.21.x-0079FF?style=flat-square)
![Platform](https://img.shields.io/badge/platform-Paper-0079FF?style=flat-square)
![Java](https://img.shields.io/badge/Java-21-0079FF?style=flat-square)

---

## ✦ Features

| Feature | Description |
|---|---|
| **Tab list** | Fake players appear in the tab list for all online and future players |
| **Server list count** | Fake players increment the online player count shown on the server list |
| **Join / leave messages** | Vanilla-style join and leave messages broadcast automatically |
| **Kill messages** | Broadcast when a real player kills a bot (toggleable) |
| **In-world physics body** | Bots spawn as visible zombie entities — pushable, damageable, solid (toggleable) |
| **Body rotation** | Bot body and head face the correct direction and stay in sync every tick |
| **Staggered join / leave** | Each bot joins and leaves with a random delay so it looks like real players |
| **Skin support** | Optionally fetches real Mojang skins per bot name (toggleable, cached, rate-limited) |
| **Default skin** | When skins are disabled, bots receive a random Steve/Alex skin from Minecraft |
| **Death & respawn** | Bots can respawn at their spawn location after dying, or leave permanently |
| **Combat** | Bots take damage with player hurt sounds; cannot target or attack players |
| **Chunk loading** | Bots keep chunks loaded around them like a real player (toggleable) |
| **Dynamic help** | Help command auto-discovers all registered sub-commands — no manual updates needed |
| **Clickable pagination** | Help pages have clickable prev/next buttons |
| **Hex colour + small-caps** | Styled with `#0079FF` accent and Unicode small-caps throughout |
| **Fully translatable** | All messages in `language/en.yml` |
| **Hot reload** | `/fpp reload` reloads config, language, and bot name pool instantly |
| **Bot name pool** | Names loaded from `bot-names.yml` — falls back to `BotXXXX` when pool is exhausted |

---

## ✦ Requirements

- **Paper** 1.21.x (tested on 1.21.11)
- **Java** 21+
- No external runtime dependencies

---

## ✦ Installation

1. Download `fpp.jar` from the releases page.
2. Place it in your server's `plugins/` folder.
3. Restart the server.
4. Edit `plugins/FakePlayerPlugin/config.yml`, `language/en.yml`, and `bot-names.yml` as desired.
5. Run `/fpp reload` to apply any changes without restarting.

---

## ✦ Commands

All sub-commands are under `/fpp`.

| Command | Permission | Description |
|---|---|---|
| `/fpp help [page]` | — | Paginated help menu with clickable navigation |
| `/fpp spawn [amount]` | `fpp.spawn` | Spawns fake player(s) at your location (default: 1) |
| `/fpp delete <name\|all>` | `fpp.delete` | Deletes a bot by name, or all bots at once |
| `/fpp reload` | `fpp.reload` | Reloads config, language file, and bot name pool |

### Examples
```
/fpp spawn          — spawns 1 fake player at your position
/fpp spawn 10       — spawns 10 fake players with staggered join delays
/fpp delete Steve   — removes the bot named Steve with a leave message
/fpp delete all     — removes all bots with staggered leave delays
/fpp help           — shows page 1 of the help menu
/fpp help 2         — shows page 2
/fpp reload         — hot-reloads all configuration
```

---

## ✦ Permissions

| Permission | Default | Description |
|---|---|---|
| `fpp.spawn` | `op` | Allows spawning fake players |
| `fpp.delete` | `op` | Allows deleting fake players |
| `fpp.reload` | `op` | Allows reloading the plugin configuration |

---

## ✦ Configuration

Located at `plugins/FakePlayerPlugin/config.yml`. Run `/fpp reload` to apply changes.

```yaml
language: en

debug:
  enabled: false

fake-player:
  max-bots: 1000          # 0 = unlimited
  fetch-skin: false       # fetch real Mojang skins by bot name
  show-skin: false        # apply fetched skin (requires fetch-skin: true)
  spawn-body: true        # spawn a physical zombie entity in the world

  join-delay:
    min: 0                # ticks (20 = 1 second)
    max: 5

  leave-delay:
    min: 0
    max: 5

  combat:
    max-health: 20.0
    hurt-sound: true      # play player hurt sound when bot takes damage

  death:
    respawn-on-death: true  # false = bot leaves permanently on death
    respawn-delay: 60       # ticks before respawn
    suppress-drops: true

  messages:
    join-message: true
    leave-message: true
    kill-message: true    # broadcast when a player kills a bot

  chunk-loading:
    enabled: true
    radius: 6             # chunk radius kept loaded around each bot
```

---

## ✦ Bot Names

Bot names are loaded from `plugins/FakePlayerPlugin/bot-names.yml`.

```yaml
names:
  - Steve
  - Alex
  - Notch
  # ... add as many as you like (max 16 chars each)
```

- Names are picked **randomly** without repeating active names.
- If a name matches a real Mojang account and `fetch-skin: true`, the bot uses that player's skin.
- When the name pool is exhausted, bots fall back to `BotXXXX` generated names.
- Run `/fpp reload` to reload the name pool without restarting.

---

## ✦ Language / Translations

All player-facing text lives in `plugins/FakePlayerPlugin/language/en.yml`.

To add a new language:
1. Copy `en.yml` to e.g. `fr.yml` in the same folder.
2. Translate the values.
3. Set `language: fr` in `config.yml`.
4. Run `/fpp reload`.

### Key message keys

| Key | Placeholders | Description |
|---|---|---|
| `prefix` | — | The `[FPP]` prefix used in all messages |
| `no-permission` | — | Shown when a player lacks permission |
| `player-only` | — | Shown when console runs a player-only command |
| `help-header / footer` | — | Borders of the help menu |
| `help-entry` | `{cmd}` `{args}` `{desc}` | Format for each command line |
| `reload-success` | `{ms}` | Shown after a successful reload |
| `spawn-success` | `{count}` `{total}` | Shown after bots are spawned |
| `spawn-max-reached` | `{max}` | Shown when the bot limit is hit |
| `delete-success` | `{name}` | Shown after a single bot is deleted |
| `delete-all` | `{count}` | Shown after all bots are deleted |
| `delete-not-found` | `{name}` | Shown when the bot name is not found |
| `bot-join` | `{name}` | Vanilla-style join broadcast |
| `bot-leave` | `{name}` | Vanilla-style leave broadcast |
| `bot-kill` | `{killer}` `{name}` | Kill broadcast when a player kills a bot |

---

## ✦ How It Works

FPP uses **pure reflection** — zero compile-time NMS imports. At runtime it:

1. Resolves the NMS classloader from online players.
2. Constructs a `GameProfile` with a random UUID and bot name.
3. Sends `ClientboundPlayerInfoUpdatePacket` (ADD_PLAYER + UPDATE_LISTED + UPDATE_LATENCY + UPDATE_GAME_MODE + UPDATE_DISPLAY_NAME) to all online players — this adds the bot to the tab list and increments the server list count.
4. Spawns a **zombie entity** as the physics body (invisible name tag, no AI targeting, zero movement speed, fire/drown/conversion immune).
5. Sends `ClientboundAddEntityPacket` to render the body as a player skin.
6. Broadcasts rotation packets every tick so the body faces the correct direction.
7. On player join, syncs all existing fake players to the new client automatically.

### Skin system
When `fetch-skin: true` and `show-skin: true`:
- All skins are fetched **in parallel** before the visual spawn chain starts (Mojang API, rate-limited to 200 ms between requests).
- Results are cached per name for the server session — the same name is never fetched twice.
- `/fpp reload` clears the skin cache.

When skins are disabled, the empty `GameProfile` causes the Minecraft client to assign a random default Steve/Alex skin based on the UUID.

---

## ✦ Project Structure

```
src/main/java/me/bill/fakePlayerPlugin/
├── FakePlayerPlugin.java              — Main plugin class (onEnable / onDisable)
├── command/
│   ├── CommandManager.java            — Routes /fpp sub-commands, tab-completion
│   ├── FppCommand.java                — Sub-command interface
│   ├── HelpCommand.java               — Dynamic paginated help with click events
│   ├── ReloadCommand.java             — Config / lang / name pool hot-reload
│   ├── SpawnCommand.java              — Spawn fake players
│   └── DeleteCommand.java             — Delete one or all bots
├── config/
│   ├── Config.java                    — config.yml accessor
│   └── BotNameConfig.java             — bot-names.yml accessor
├── fakeplayer/
│   ├── FakePlayer.java                — Fake player data model
│   ├── FakePlayerManager.java         — Spawn / delete lifecycle, stagger chains
│   ├── FakePlayerBody.java            — Zombie entity spawn / configuration
│   ├── ChunkLoader.java               — Per-bot chunk ticket management
│   ├── SkinFetcher.java               — Async Mojang skin fetch with cache + rate limit
│   ├── NmsHelper.java                 — NMS classloader discovery
│   └── PacketHelper.java              — All NMS packets via reflection
├── lang/
│   └── Lang.java                      — Language file loader with placeholder support
├── listener/
│   ├── PlayerJoinListener.java        — Syncs fake players to joining real players
│   └── FakePlayerEntityListener.java  — Combat, fire, drown, conversion, death, respawn
└── util/
    ├── FppLogger.java                 — Styled console logger
    └── TextUtil.java                  — Hex colour + legacy colour code parser
```

---

## ✦ Building

Open in IntelliJ IDEA and use **Build → Build Project** (no terminal required).

The output JAR will be at `target/fpp-0.0.1.jar`.

**Requirements:**
- JDK 21
- Maven (bundled with IntelliJ)
- `libs/paper-1.21.11-mojang-mapped.jar` (included in repo)
- `libs/authlib-4.0.43.jar` (included in repo)

---

## ✦ Changelog

### 0.1.5 — 2026-03-07
- Real packet-based fake player system via pure reflection (no compile-time NMS)
- Dynamic paginated help with clickable prev/next navigation
- `/fpp spawn [amount]` with staggered join delays
- `/fpp delete <name|all>` with staggered leave delays — body, tab list, and message all fire together
- `/fpp reload` hot-reloads config, language, and bot name pool
- Kill message when a player kills a bot (toggleable)
- Physical zombie body with player-skin packets, rotation sync every tick
- `spawn-body` toggle — body-less mode for tab list / server list population only
- Skin system: parallel fetch, per-name cache, Mojang rate-limit compliance (200 ms gap)
- `show-skin` / `fetch-skin` flags — disabled by default for instant spawn
- Death & respawn system with configurable delay
- Combat: player hurt sound, zero attack damage, zero follow range, no targeting
- Fire, sunlight, drowning, and drowned-conversion immunity
- Chunk loading per bot with configurable radius
- Bot name pool from `bot-names.yml` with `BotXXXX` fallback when exhausted
- Hex colour `#0079FF` + Unicode small-caps styling throughout
- Full `language/en.yml` translation support with named placeholders
- Startup and shutdown log messages with styled banner

---

## ✦ License

MIT — free to use, modify, and distribute.

---

## ✦ Author

**El_Pepes** — Built with ❤️ for the Paper ecosystem.
