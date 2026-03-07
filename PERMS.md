# ꜰᴀᴋᴇ ᴘʟᴀʏᴇʀ ᴘʟᴜɢɪɴ — Permission Reference

All permission nodes for **FPP v0.1.5**.  
Compatible with **LuckPerms**, **PermissionsEx**, **GroupManager**, and vanilla Bukkit super-permissions.

---

## Quick-start with LuckPerms

> **Regular players get `/fpp spawn` (1 bot), `/fpp tph`, and `/fpp info` (own bots) automatically** — `fpp.user.*` is `default: true` so no LuckPerms setup is needed for basic user access.

```bash
# ── Owner / full admin ───────────────────────────────────────────────────────
/lp group owner permission set fpp.* true

# ── Admin (everything except bypass cap) ─────────────────────────────────────
/lp group admin permission set fpp.* true
/lp group admin permission set fpp.bypass.maxbots false

# ── Moderator (spawn + delete individual + list + info) ──────────────────────
/lp group moderator permission set fpp.spawn true
/lp group moderator permission set fpp.delete true
/lp group moderator permission set fpp.delete.all false   # no mass-wipe
/lp group moderator permission set fpp.list true
/lp group moderator permission set fpp.tp true
/lp group moderator permission set fpp.info true

# ── Regular users — NO SETUP NEEDED ─────────────────────────────────────────
# fpp.user.* is default: true — all players automatically get:
#   - /fpp spawn     (1 bot limit via fpp.bot.1)
#   - /fpp tph       (teleport own bot to self)
#   - /fpp info      (view own bots: world, coords, uptime)
# To DISABLE user access for a specific group:
/lp group guest permission set fpp.user.* false

# ── VIP users (3 bots each, user-tier) ───────────────────────────────────────
# fpp.user.* already active — just raise the bot limit
/lp group vip permission set fpp.bot.3 true     # overrides fpp.bot.1

# ── Premium users (10 bots + tp to their bots) ───────────────────────────────
/lp group premium permission set fpp.bot.10 true
/lp group premium permission set fpp.tp true
```

---

## Full Permission Table

### Admin tier (`fpp.*`)

| Node | Default | Description |
|---|---|---|
| [`fpp.*`](#fpp) | `op` | Wildcard — grants every FPP permission |
| [`fpp.help`](#fpphelp) | `true` | View `/fpp help` |
| [`fpp.spawn`](#fppspawn) | `op` | Admin spawn — no personal limit |
| [`fpp.spawn.multiple`](#fppspawnmultiple) | `op` | Spawn more than 1 bot at a time |
| [`fpp.spawn.name`](#fppspawnname) | `op` | Use `--name` flag for custom bot names |
| [`fpp.delete`](#fppdelete) | `op` | Delete a bot by name |
| [`fpp.delete.all`](#fppdeleteall) | `op` | Delete ALL bots at once |
| [`fpp.list`](#fpplist) | `op` | List all active bots |
| [`fpp.chat`](#fppchat) | `op` | Toggle bot fake-chat on/off |
| [`fpp.swap`](#fppswap) | `op` | Toggle bot swap/rotation on/off |
| [`fpp.reload`](#fppreload) | `op` | Hot-reload all plugin config |
| [`fpp.info`](#fppinfo) | `op` | Full database query via `/fpp info` |
| [`fpp.tp`](#fpptp) | `op` | Teleport yourself to any bot |
| [`fpp.bypass.maxbots`](#fppbypassmaxbots) | `op` | Bypass the global `max-bots` cap |

### User tier (`fpp.user.*`)

> All user-tier nodes are **`default: true`** — regular players have these permissions automatically without any LuckPerms configuration.

| Node | Default | Description |
|---|---|---|
| [`fpp.user.*`](#fppuser) | **`true`** | Wildcard — grants all user-facing FPP commands to everyone |
| [`fpp.user.spawn`](#fppuserspawn) | **`true`** | Spawn bots up to personal limit (1 bot by default) |
| [`fpp.user.tph`](#fppusertph) | **`true`** | Teleport your own bot(s) to you |
| [`fpp.user.info`](#fppuserinfo) | **`true`** | View limited info for your own bots only |

### Bot limit nodes (`fpp.bot.<num>`)

| Node | Default | Description |
|---|---|---|
| `fpp.bot.1` | **`true`\*** | Personal limit: 1 bot — **auto-granted via `fpp.user.*`** |
| `fpp.bot.2` | `false` | Personal limit: 2 bots |
| `fpp.bot.3` | `false` | Personal limit: 3 bots |
| `fpp.bot.5` | `false` | Personal limit: 5 bots |
| `fpp.bot.10` | `false` | Personal limit: 10 bots |
| `fpp.bot.15` | `false` | Personal limit: 15 bots |
| `fpp.bot.20` | `false` | Personal limit: 20 bots |
| `fpp.bot.50` | `false` | Personal limit: 50 bots |
| `fpp.bot.100` | `false` | Personal limit: 100 bots |

> \* `fpp.bot.1` itself is `default: false` but is automatically included as a child of `fpp.user.*` which is `default: true` — so all players effectively have it.

> **How the limit resolves:** The plugin scans `fpp.bot.1` through `fpp.bot.100` and takes the **highest** matching node. If no node is set, the global `fake-player.user-bot-limit` from `config.yml` is used (default: `1`).

---

## Detailed Node Descriptions

### `fpp.*`
**Default:** `op`  
Grants every admin and user permission in one shot, including `fpp.user.*`. Assign to owner/admin groups only.

```bash
/lp group admin permission set fpp.* true
```

---

### `fpp.user.*`
**Default:** `true` (everyone, no setup needed)  
Grants all **user-tier** commands automatically to every player: `fpp.user.spawn`, `fpp.user.tph`, `fpp.user.info`, and `fpp.bot.1` (1-bot personal limit).  
Does NOT grant any admin commands (delete, reload, swap, chat, list, full info, tp).

```bash
# No setup needed — regular players already have this.

# To DISABLE for a specific group (e.g. guests / unverified players):
/lp group guest permission set fpp.user.* false

# To raise the bot limit for VIP (already have fpp.user.*, just add higher limit):
/lp group vip permission set fpp.bot.5 true
```

---

### `fpp.help`
**Default:** `true` (everyone)  
Allows viewing `/fpp help [page]`. All players have this unless explicitly negated.

---

### `fpp.spawn`
**Default:** `op`  
Admin-tier spawn — ignores personal bot limits. Automatically grants `fpp.spawn.multiple` and `fpp.spawn.name` as children.

```bash
/lp group moderator permission set fpp.spawn true
```

---

### `fpp.spawn.multiple`
**Default:** `op` (inherited from `fpp.spawn`)  
Controls spawning more than one bot at a time. Negate to restrict a group to single-bot spawning.

```bash
# Spawn but only one at a time
/lp group moderator permission set fpp.spawn true
/lp group moderator permission set fpp.spawn.multiple false
```

---

### `fpp.spawn.name`
**Default:** `op` (inherited from `fpp.spawn`)  
Controls using `--name` to spawn a bot with a custom name.  
**User-tier (`fpp.user.spawn`) cannot use `--name`** — admin spawn only.

---

### `fpp.user.spawn`
**Default:** `true` (included in `fpp.user.*`, everyone)  
User-tier spawn. Respects the player's personal bot limit resolved from `fpp.bot.<num>` nodes.  
Users **cannot** use `--name` or spawn multiple bots (those are admin-only sub-nodes).

**Limitations vs admin `fpp.spawn`:**
- ❌ No `--name` flag
- ❌ Cannot spawn more than 1 bot at a time
- ✅ Bot count capped by `fpp.bot.<num>` (default: 1 bot per player)

```bash
# No setup needed — all players already have this.
# To deny: /lp group guest permission set fpp.user.spawn false
```

---

### `fpp.bot.<num>`
**Default:** `false`  
Sets the player's personal active-bot cap. The plugin scans `fpp.bot.1` → `fpp.bot.100` and picks the **highest** matching node.

```bash
# Regular users: 1 bot (already granted by fpp.user.*)
/lp group default permission set fpp.user.* true

# VIP: 5 bots
/lp group vip permission set fpp.user.* true
/lp group vip permission set fpp.bot.5 true

# Premium: 20 bots
/lp group premium permission set fpp.user.* true
/lp group premium permission set fpp.bot.20 true
```

You can also set a per-player limit for a specific individual:
```bash
/lp user Steve permission set fpp.bot.10 true
```

---

### `fpp.delete`
**Default:** `op`  
Delete a single bot by name with `/fpp delete <name>`. Automatically grants `fpp.delete.all`.

---

### `fpp.delete.all`
**Default:** `op` (inherited from `fpp.delete`)  
Allows `/fpp delete all`. Negate to allow individual deletions without mass-wipe access.

```bash
/lp group moderator permission set fpp.delete true
/lp group moderator permission set fpp.delete.all false
```

---

### `fpp.list`
**Default:** `op`  
View all active bots with name, uptime, location, and spawner via `/fpp list`.

---

### `fpp.chat`
**Default:** `op`  
Toggle bot fake-chat on/off with `/fpp chat [on|off|status]`. Persists to `config.yml`.

---

### `fpp.swap`
**Default:** `op`  
Toggle the bot swap/rotation system with `/fpp swap [on|off|status]`. Persists to `config.yml`.

---

### `fpp.reload`
**Default:** `op`  
Hot-reload `config.yml`, `language/en.yml`, `bot-names.yml`, and `bot-messages.yml` with `/fpp reload`. **Restrict to trusted admins only.**

---

### `fpp.info`
**Default:** `op`  
Full admin database query: `/fpp info`, `/fpp info bot <name>`, `/fpp info spawner <player>`. Shows session history, timestamps, locations, removal reasons.

---

### `fpp.user.info`
**Default:** `true` (included in `fpp.user.*`, everyone)  
User-tier info — allows `/fpp info [botname]` but only for bots the player personally spawned.  
Shows **only**: world, coordinates, uptime. No session history, no other players' bots.

**Limitations vs admin `fpp.info`:**
- ❌ Cannot view other players' bots
- ❌ No session history or database records
- ❌ No `/fpp info bot` or `/fpp info spawner` sub-commands
- ✅ See world, coordinates, and uptime of own bots

```bash
# No setup needed — all players already have this.
# To deny: /lp group guest permission set fpp.user.info false
```

---

### `fpp.user.tph`
**Default:** `true` (included in `fpp.user.*`, everyone)  
Teleports the player's own bot(s) to the player with `/fpp tph [botname]`.
- If the player has 1 bot, `[botname]` is optional.
- If the player has multiple bots, they must specify a name.
- Players can only tph bots **they spawned**.
- Admins with `fpp.*` can tph **any** bot.

**Limitations vs admin:**
- ❌ Cannot tph bots spawned by other players
- ✅ Can tph their own bots regardless of bot count

```bash
# No setup needed — all players already have this.
# To deny: /lp group guest permission set fpp.user.tph false
```

---

### `fpp.tp`
**Default:** `op`  
Teleports the player **to** a bot with `/fpp tp [botname]`.  
Admins can teleport to any active bot. User-tier (`fpp.user.*`) cannot use this command — grant it explicitly if needed.

```bash
# Allow premium users to tp to their own bots too
/lp group premium permission set fpp.user.* true
/lp group premium permission set fpp.tp true
```

---

### `fpp.bypass.maxbots`
**Default:** `op`  
Bypasses the global `max-bots` cap in `config.yml` during `/fpp spawn`.  
**Grant with care — no upper bound when active.**

```bash
/lp group owner permission set fpp.bypass.maxbots true
```

---

## Recommended Group Setups

### Owner
```bash
/lp group owner permission set fpp.* true
```

### Admin
```bash
/lp group admin permission set fpp.* true
/lp group admin permission set fpp.bypass.maxbots false
```

### Moderator
```bash
/lp group moderator permission set fpp.spawn true
/lp group moderator permission set fpp.delete true
/lp group moderator permission set fpp.delete.all false
/lp group moderator permission set fpp.list true
/lp group moderator permission set fpp.tp true
/lp group moderator permission set fpp.info true
```

### VIP (5 bots, tph + tp)
```bash
# fpp.user.* already active by default — just raise the bot limit
/lp group vip permission set fpp.bot.5 true
/lp group vip permission set fpp.tp true
```

### Regular Player (1 bot, tph, info — own bots only)
```bash
# NO SETUP NEEDED
# fpp.user.* is default: true — all players automatically have:
#   /fpp spawn   (up to 1 bot)
#   /fpp tph     (teleport own bot to self)
#   /fpp info    (view own bots: world, coords, uptime)
```

### Guest (no FPP access at all)
```bash
/lp group guest permission set fpp.user.* false
```

### Helper (spawn 1 admin bot, no delete-all, can list)
```bash
/lp group helper permission set fpp.spawn true
/lp group helper permission set fpp.spawn.multiple false
/lp group helper permission set fpp.spawn.name false
/lp group helper permission set fpp.delete true
/lp group helper permission set fpp.delete.all false
/lp group helper permission set fpp.list true
```

---

## Permission Inheritance Tree

```
fpp.*
├── fpp.help                        (default: true — everyone)
├── fpp.spawn
│   ├── fpp.spawn.multiple
│   └── fpp.spawn.name
├── fpp.delete
│   └── fpp.delete.all
├── fpp.list
├── fpp.chat
├── fpp.swap
├── fpp.reload
├── fpp.info
├── fpp.tp
├── fpp.bypass.maxbots
└── fpp.user.*
    ├── fpp.user.spawn              (respects fpp.bot.<num> limit)
    ├── fpp.user.tph
    ├── fpp.user.info
    └── fpp.bot.1                   (default user limit)

fpp.bot.<num>   (standalone nodes — highest matching wins)
    fpp.bot.1   fpp.bot.2   fpp.bot.3   fpp.bot.5
    fpp.bot.10  fpp.bot.15  fpp.bot.20  fpp.bot.50  fpp.bot.100
```

---

## How `/fpp info` Differs by Tier

| Tier | Permission | What they see |
|---|---|---|
| **Admin** | `fpp.info` | Full stats, session history (all bots), spawner lookups |
| **User** | `fpp.user.info` | Own bots only — world, coordinates, uptime |
| **None** | — | Permission denied |

---

## How `/fpp spawn` Differs by Tier

| Tier | Permission | Behaviour |
|---|---|---|
| **Admin** | `fpp.spawn` | No personal limit, `--name` allowed, multiple allowed |
| **User** | `fpp.user.spawn` | Personal limit from `fpp.bot.<num>`, no `--name`, single only |
| **None** | — | Permission denied |

---

## Notes on LuckPerms Negation

LuckPerms supports explicit negation (`false`) to override inherited grants:

```bash
# Give fpp.spawn but block --name specifically
/lp user Steve permission set fpp.spawn true
/lp user Steve permission set fpp.spawn.name false

# VIP with fpp.user.* but strip tph ability
/lp group vip permission set fpp.user.* true
/lp group vip permission set fpp.user.tph false
```

Negated nodes always take priority over inherited `true` values from parent groups.

---

*ꜰᴀᴋᴇ ᴘʟᴀʏᴇʀ ᴘʟᴜɢɪɴ v0.1.5 · Paper 1.21.x · Java 21*
