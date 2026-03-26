# Skin System

FPP v1.1.0 ships a fully reworked skin pipeline with three modes and multiple
skin sources — from automatic Mojang resolution to custom PNG files on disk.

---

## Quick Setup

```yaml
# config.yml
skin:
  mode: auto          # auto | custom | off
  clear-cache-on-reload: true
```

Run `/fpp reload` to apply changes without restarting.

---

## Modes

### `auto` *(Recommended — online-mode servers)*

Paper calls `Mannequin.setProfile(botName)` internally and the Minecraft client
resolves the skin from Mojang automatically — exactly like a real player joining.

| Feature | Detail |
|---------|--------|
| HTTP requests | **Zero** — the plugin does nothing |
| Skin accuracy | Perfect — matches the Minecraft account of that name |
| Works offline | ❌ Requires `online-mode=true` |
| Delay | None |

> **Tip:** If a bot name doesn't match any Mojang account the client falls back
> to the default Steve / Alex skin. This is expected behaviour.

#### Guaranteed Skin (auto mode)

When `guaranteed-skin: true`, the plugin ensures every bot gets a valid skin
even when the bot name doesn't exist on Mojang (generated names, user bots, etc.).

**Fallback chain:**

```
1. skins/ folder → pick random PNG file
2. skin.custom.pool → pick random entry (if any)
3. skin.fallback-pool → pick random from 20 pre-loaded popular skins
   ├─ Pre-loaded at startup (async, takes ~2-5 seconds)
   └─ If bots spawn before preload completes, fetch on-demand randomly
4. skin.fallback-name → single last-resort skin (default: Notch)
```

The `fallback-pool` **ensures skin diversity** even during rapid bot spawning
at server startup. If the prewarm hasn't completed yet, the plugin will:
- Randomly pick a name from the config list
- Fetch it on-demand (fast, ~50-200ms)
- Cache it for future bots

This prevents the "all bots spawn as Notch" issue when many bots spawn quickly.

**Config example:**

```yaml
skin:
  mode: auto
  guaranteed-skin: true
  fallback-name: Notch
  fallback-pool:
    - Notch              # Minecraft creator
    - jeb_               # Lead developer
    - Dinnerbone         # Developer (upside-down)
    - Grumm              # Developer (upside-down)
    - MHF_Steve          # Default male player skin
    - MHF_Alex           # Default female player skin
    - MHF_Herobrine      # Herobrine skin
    - MHF_Zombie         # Zombie mob skin
    - MHF_Creeper        # Creeper mob skin
    # ... 18 more MHF_* system accounts (default config includes 27 total)
```

The default pool includes:
- **Mojang developers** (Notch, jeb_, Dinnerbone, Grumm)
- **MHF_* system accounts** — Official Minecraft map marker accounts with mob/player skins

> **Performance note:** The prewarm fetches all 27 skins asynchronously at
> startup. This adds ~2-5 seconds to skin availability but doesn't block
> server startup. Bots spawned during this window still get diverse skins
### `custom` *(Full control — works online & offline)*

FPP runs a **5-step resolution pipeline** to find the best skin for each bot:

```
1. Exact-name override  (skin.custom.by-name)
2. File named <botname>.png  (plugins/FakePlayerPlugin/skins/)
3. Random PNG file from the skins/ folder
4. Random entry from skin.custom.pool  (player names or URLs)
5. Mojang API fallback  (fetched by bot's own Minecraft name)
```

The first step that finds a valid skin wins. All Mojang fetches are cached
in-memory for the session and can be cleared with `/fpp reload`.

#### Config pool

Add Minecraft player names or Mojang CDN URLs to the pool:

```yaml
skin:
  mode: custom
  custom:
    pool:
      - Notch
      - Technoblade
      - Dream
      - https://textures.minecraft.net/texture/abc123def456...
```

Bots without an exact-name match randomly pick from this list.

#### Per-bot name overrides

Force a specific bot name to always use a particular player's skin (or a URL):

```yaml
skin:
  mode: custom
  custom:
    by-name:
      Herobrine: Notch          # bot "Herobrine" gets Notch's skin
      CoolBot: Technoblade      # bot "CoolBot" gets Technoblade's skin
      RedBot: "https://textures.minecraft.net/texture/abc..."
```

Keys are matched **case-insensitively** against the bot's internal Minecraft name.

#### Skin folder

Place standard Minecraft PNG skin files in:

```
plugins/FakePlayerPlugin/skins/
```

**Naming rules:**

| File name | Behaviour |
|-----------|-----------|
| `anything.png` | Added to the random pool — any bot without a better match can use it |
| `<botname>.png` | Used **exclusively** for the bot named `<botname>` (exact, case-insensitive) |

Supported formats: `64×64` (modern slim/wide) and `64×32` (legacy classic) PNG files.

> **Note:** Folder skins have **no RSA signature**. They display correctly on Paper
> servers but may produce a `"profile not signed"` debug message — this is harmless.

The folder is scanned on startup and on `/fpp reload`.

---

### `off`

No skin is applied. Bots display the default Steve or Alex appearance
(determined by UUID per vanilla Minecraft rules).

---

## Full Config Reference

```yaml
skin:
  # Skin mode: auto | custom | off
  mode: auto

  # Guarantee every bot gets a skin (never spawn as Steve)
  # When enabled, falls back to folder/pool/fallback-pool/fallback-name
  # if the bot's name doesn't exist on Mojang
  guaranteed-skin: true

  # Last-resort fallback skin (single Mojang account)
  # Only used when all other fallback sources are empty/unavailable
  fallback-name: Notch

  # Random fallback pool (27 official Minecraft accounts by default)
  # Provides skin diversity for bots with non-existent names
  # Pre-loaded at startup (async); on-demand random fetch if needed during spawn
  # Includes Mojang developers + MHF_* map marker system accounts
  fallback-pool:
    - Notch
    - jeb_
    - Dinnerbone
    - Grumm
    - MHF_Steve
    - MHF_Alex
    - MHF_Herobrine
    - MHF_Zombie
    - MHF_Creeper
    - MHF_Skeleton
    # ... add more real Minecraft usernames or MHF_* system accounts

  # Clear resolved skin cache on /fpp reload
  clear-cache-on-reload: true

  # ── Custom mode ──────────────────────────────────────────────────────────
  custom:
    # Random pool — Minecraft names or Mojang CDN URLs
    pool:
    #  - Notch
    #  - Technoblade
    #  - https://textures.minecraft.net/texture/<hash>

    # Exact-name overrides: botname → player-name or URL
    by-name: {}
    #  Herobrine: Notch
    #  CoolBot: https://textures.minecraft.net/texture/<hash>

  # ── Skin folder ──────────────────────────────────────────────────────────
  # Place .png files in: plugins/FakePlayerPlugin/skins/
  # <botname>.png → exact match for that bot
  # anything.png  → random pool fallback
```

---

## How Skins Are Resolved (custom mode flow)

```
Bot "Herobrine" spawns
       │
       ▼
[1] by-name override?  ──YES──► Use "Notch" skin (fetched via Mojang API)
       │NO
       ▼
[2] skins/Herobrine.png exists?  ──YES──► Use that file
       │NO
       ▼
[3] Any PNG in skins/ folder?  ──YES──► Pick random file
       │NO
       ▼
[4] Any entries in pool?  ──YES──► Pick random entry
       │NO
       ▼
[5] Fetch "Herobrine" from Mojang API  ──OK──► Use fetched skin
                                        │FAIL
                                        ▼
                                 Fall back to auto mode
                                 (Mannequin.setProfile(name))
```

---

## Rate Limiting

When `mode: custom` is active and the plugin fetches skins from Mojang:

- Requests are queued with a **200 ms gap** between each call
- Results are **cached per session** — each name is fetched at most once
- Mojang's documented limit is ~600 requests / 10 minutes per IP
- `/fpp reload` clears the cache (if `clear-cache-on-reload: true`)

---

## Choosing a Mode

| Scenario | Recommended mode |
|----------|-----------------|
| Online-mode server, bot names match Mojang accounts | `auto` |
| Online-mode server, custom/random bot names | `custom` with pool |
| Offline-mode server | `custom` with pool or folder |
| You have a set of specific skin PNGs you want to use | `custom` with skins folder |
| You want a specific bot to always wear a specific skin | `custom` + `by-name` |
| Performance-critical, skins don't matter | `off` |

---

← [Database](Database.md) · [Bot Behaviour](Bot-Behaviour.md) →
