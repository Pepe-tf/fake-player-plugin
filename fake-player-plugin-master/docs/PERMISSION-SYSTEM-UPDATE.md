# Permission System Update — Full Restructure

**Date:** April 8, 2026  
**Version:** 1.5.18  
**Breaking Change:** Yes — permission nodes have been completely restructured

---

## Overview

The permission system has been completely restructured to use `fpp.op` and `fpp.use` as the primary wildcards, replacing the old `fpp.*` and `fpp.user.*` structure. Every command now has its own explicit permission node.

---

## What Changed

### Wildcard Permissions

| Old Node      | New Node  | Description                                    | Default |
|---------------|-----------|------------------------------------------------|---------|
| `fpp.*`       | `fpp.op`  | Full admin access — all commands               | `op`    |
| `fpp.user.*`  | `fpp.use` | User-tier access — spawn, tph, xp, info (own)  | `true`  |

### Bot Limit Permissions

| Old Node           | New Node                | Description                     |
|--------------------|-------------------------|---------------------------------|
| `fpp.bot.1`        | `fpp.spawn.limit.1`     | Personal bot limit — 1 bot      |
| `fpp.bot.2`        | `fpp.spawn.limit.2`     | Personal bot limit — 2 bots     |
| `fpp.bot.3`        | `fpp.spawn.limit.3`     | Personal bot limit — 3 bots     |
| `fpp.bot.5`        | `fpp.spawn.limit.5`     | Personal bot limit — 5 bots     |
| `fpp.bot.10`       | `fpp.spawn.limit.10`    | Personal bot limit — 10 bots    |
| `fpp.bot.15`       | `fpp.spawn.limit.15`    | Personal bot limit — 15 bots    |
| `fpp.bot.20`       | `fpp.spawn.limit.20`    | Personal bot limit — 20 bots    |
| `fpp.bot.50`       | `fpp.spawn.limit.50`    | Personal bot limit — 50 bots    |
| `fpp.bot.100`      | `fpp.spawn.limit.100`   | Personal bot limit — 100 bots   |

### User-Tier Permissions

| Old Node          | New Node          | Command                              | Default      |
|-------------------|-------------------|--------------------------------------|--------------|
| `fpp.user.spawn`  | `fpp.spawn.user`  | `/fpp spawn` (user-tier, 1-bot max)  | `true`       |
| `fpp.user.tph`    | `fpp.tph`         | `/fpp tph [bot]`                     | `true`       |
| `fpp.user.xp`     | `fpp.xp`          | `/fpp xp <bot>`                      | `true`       |
| `fpp.user.info`   | `fpp.info.user`   | `/fpp info [bot]` (own bots only)    | `true`       |

### Bypass Permissions

| Old Node               | New Node               | Description                           |
|------------------------|------------------------|---------------------------------------|
| `fpp.bypass.maxbots`   | `fpp.bypass.max`       | Bypass global max-bots cap            |
| `fpp.bypass.cooldown`  | `fpp.bypass.cooldown`  | Bypass spawn cooldown (no change)     |

### Admin-Tier Permissions

| Old Node             | New Node        | Command                                  | Default |
|----------------------|-----------------|------------------------------------------|---------|
| `fpp.admin.migrate`  | `fpp.migrate`   | `/fpp migrate` (config/DB migrations)    | `op`    |

All other admin permissions (spawn, delete, list, chat, reload, info, freeze, stats, lpinfo, rank, alert, sync, swap, peaks, settings, move, inventory, cmd) remain unchanged.

---

## Migration Guide for Server Owners

### LuckPerms Auto-Migration (Recommended)

If you're using LuckPerms, existing permissions will continue to work because:
- `fpp.*` is still recognized (but internally maps to `fpp.op`)
- `fpp.user.*` is still recognized (but internally maps to `fpp.use`)
- Old individual nodes (e.g., `fpp.user.spawn`) still work

**However**, we recommend updating to the new structure for clarity:

```bash
# Grant full admin access
/lp group admin permission unset fpp.*
/lp group admin permission set fpp.op true

# Grant user access
/lp group default permission unset fpp.user.*
/lp group default permission set fpp.use true

# Update bot limits
/lp user PlayerName permission unset fpp.bot.5
/lp user PlayerName permission set fpp.spawn.limit.5 true

# Update bypass permissions
/lp user VIP permission unset fpp.bypass.maxbots
/lp user VIP permission set fpp.bypass.max true
```

### Manual Migration (Vanilla Bukkit Permissions)

If you're using `permissions.yml` or another permission plugin:

1. Replace all `fpp.*` grants with `fpp.op`
2. Replace all `fpp.user.*` grants with `fpp.use`
3. Replace all `fpp.bot.<n>` grants with `fpp.spawn.limit.<n>`
4. Replace `fpp.bypass.maxbots` with `fpp.bypass.max`
5. Replace `fpp.admin.migrate` with `fpp.migrate`
6. Replace user-tier nodes:
   - `fpp.user.spawn` → `fpp.spawn.user`
   - `fpp.user.tph` → `fpp.tph`
   - `fpp.user.xp` → `fpp.xp`
   - `fpp.user.info` → `fpp.info.user`

---

## Complete Permission List

### Wildcards

- **`fpp.op`** — Full admin access (all commands)  
  *Default: op*
- **`fpp.use`** — User-tier access (spawn, tph, xp, info-own)  
  *Default: true*

### User Commands

- **`fpp.spawn.user`** — Spawn bots (limited by `fpp.spawn.limit.<n>`)  
  *Included in: fpp.use*
- **`fpp.tph`** — Teleport your own bot(s) to you  
  *Included in: fpp.use*
- **`fpp.xp`** — Collect XP from your own bot(s)  
  *Included in: fpp.use*
- **`fpp.info.user`** — View info for your own bot(s)  
  *Included in: fpp.use*

### Bot Limits

- **`fpp.spawn.limit.1`** — 1 bot limit *(included in fpp.use)*
- **`fpp.spawn.limit.2`** — 2 bots limit
- **`fpp.spawn.limit.3`** — 3 bots limit
- **`fpp.spawn.limit.5`** — 5 bots limit
- **`fpp.spawn.limit.10`** — 10 bots limit
- **`fpp.spawn.limit.15`** — 15 bots limit
- **`fpp.spawn.limit.20`** — 20 bots limit
- **`fpp.spawn.limit.50`** — 50 bots limit
- **`fpp.spawn.limit.100`** — 100 bots limit

### Admin Commands

- **`fpp.help`** — View `/fpp help` menu *(default: true)*
- **`fpp.spawn`** — Admin-tier spawn (unlimited, --name flag)
- **`fpp.spawn.multiple`** — Spawn multiple bots at once *(inherited from fpp.spawn)*
- **`fpp.spawn.name`** — Use --name flag for custom names *(inherited from fpp.spawn)*
- **`fpp.delete`** — Delete bot(s)
- **`fpp.delete.all`** — Delete all bots at once
- **`fpp.list`** — View active bot list
- **`fpp.chat`** — Toggle fake chat system
- **`fpp.reload`** — Reload plugin configuration
- **`fpp.info`** — View full bot info (all bots)
- **`fpp.tp`** — Teleport to a bot
- **`fpp.freeze`** — Freeze/unfreeze bot(s)
- **`fpp.stats`** — View statistics panel
- **`fpp.migrate`** — Access migration tools
- **`fpp.lpinfo`** — View LuckPerms diagnostic info
- **`fpp.rank`** — Assign LuckPerms groups to bots
- **`fpp.alert`** — Broadcast network-wide alerts
- **`fpp.sync`** — Sync configs across network
- **`fpp.swap`** — Control bot session rotation
- **`fpp.peaks`** — Control peak-hours scheduling
- **`fpp.settings`** — Open in-game settings GUI
- **`fpp.move`** — Control bot movement (WASD navigation)
- **`fpp.inventory`** — Open bot inventory GUI
- **`fpp.cmd`** — Execute commands as a bot

### Bypass

- **`fpp.bypass.max`** — Bypass global max-bots cap
- **`fpp.bypass.cooldown`** — Bypass spawn cooldown

---

## Testing Your Setup

After migrating, verify your permissions are working correctly:

### As a regular player (with `fpp.use`):

```
/fpp spawn          ✓ Should work (1-bot limit)
/fpp spawn 5        ✗ Should fail (admin-only)
/fpp tph            ✓ Should work (own bot)
/fpp xp <bot>       ✓ Should work (own bot)
/fpp info           ✓ Should work (own bot, limited info)
/fpp delete <bot>   ✗ Should fail (admin-only)
/fpp reload         ✗ Should fail (admin-only)
```

### As an admin (with `fpp.op`):

```
/fpp spawn          ✓ Should work
/fpp spawn 50       ✓ Should work
/fpp delete all     ✓ Should work
/fpp reload         ✓ Should work
/fpp settings       ✓ Should work
/fpp migrate status ✓ Should work
```

### Test bot limits:

```bash
# Grant a player 5-bot limit
/lp user PlayerName permission set fpp.spawn.limit.5 true

# Grant a player bypass (unlimited bots)
/lp user PlayerName permission set fpp.bypass.max true
```

---

## Code References

### Permission Constants (Perm.java)

All permission nodes are defined in:
```
src/main/java/me/bill/fakePlayerPlugin/permission/Perm.java
```

Key constants:
- `Perm.OP` — `fpp.op`
- `Perm.USE` — `fpp.use`
- `Perm.USER_SPAWN` — `fpp.spawn.user`
- `Perm.USER_TPH` — `fpp.tph`
- `Perm.USER_XP` — `fpp.xp`
- `Perm.USER_INFO` — `fpp.info.user`
- `Perm.BOT_LIMIT_PREFIX` — `fpp.spawn.limit.`

### Permission Declarations (plugin.yml)

All permissions are declared in:
```
src/main/resources/plugin.yml
```

LuckPerms reads this file for tab-completion, so missing entries won't appear in `/lp permission set fpp.<tab>`.

---

## Breaking Changes Summary

1. **`fpp.*` → `fpp.op`** — Full admin wildcard renamed
2. **`fpp.user.*` → `fpp.use`** — User wildcard renamed
3. **`fpp.bot.<n>` → `fpp.spawn.limit.<n>`** — Bot limit nodes renamed
4. **`fpp.user.spawn` → `fpp.spawn.user`** — User spawn permission renamed
5. **`fpp.user.tph` → `fpp.tph`** — User tph permission renamed
6. **`fpp.user.xp` → `fpp.xp`** — User xp permission renamed
7. **`fpp.user.info` → `fpp.info.user`** — User info permission renamed
8. **`fpp.bypass.maxbots` → `fpp.bypass.max`** — Bypass permission renamed
9. **`fpp.admin.migrate` → `fpp.migrate`** — Migrate permission renamed

---

## Support

If you encounter issues after updating permissions:

1. Check `/fpp info` output — it shows your active permission nodes
2. Run `/lp user <name> permission info` to see all granted permissions
3. Verify `plugin.yml` includes all new permission nodes
4. Restart your server after changing LuckPerms permissions

For further assistance, report issues on the FPP GitHub repository.

