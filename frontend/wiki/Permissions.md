# 🔐 Permissions
> **Complete Permission Reference - v1.6.0**  
> FPP uses a two-tier system: **`fpp.op`** for admins and **`fpp.use`** for regular users.  
> All permission nodes are declared in `plugin.yml` for LuckPerms tab-completion support.
---
## 🏗️ Permission Architecture
```
fpp.op                              # Admin wildcard (default: op)
fpp.use                             # User wildcard (default: true)
fpp.spawn.limit.<N>                 # Personal bot limits (N = 1-100)
fpp.bypass.maxbots                  # Ignore global bot cap
fpp.bypass.cooldown                 # No spawn cooldown
```
See the full tree in the sections below.
---
## 👑 Admin Permissions (fpp.op)
`fpp.op` is the admin wildcard (default: op). Grants all admin commands automatically.
### Core Commands
| Permission | Command | Description |
|------------|---------|-------------|
| `fpp.spawn` | `/fpp spawn` | Spawn unlimited bots (no personal limits) |
| `fpp.delete` | `/fpp despawn` | Remove any bot on the server |
| `fpp.list` | `/fpp list` | View all active bots |
| `fpp.freeze` | `/fpp freeze` | Freeze/unfreeze any bot or all bots |
| `fpp.tp` | `/fpp tp` | Teleport to a bot |
| `fpp.tph` | `/fpp tph` | Teleport any bot to you |
| `fpp.stats` | `/fpp stats` | Server performance statistics |
| `fpp.info` | `/fpp info` | Query session database |
| `fpp.reload` | `/fpp reload` | Hot-reload all configuration |
| `fpp.migrate` | `/fpp migrate` | Migration, backup, and export |
### Bot Systems
| Permission | Command | Description |
|------------|---------|-------------|
| `fpp.chat` | `/fpp chat` | Control fake chat system |
| `fpp.swap` | `/fpp swap` | Control bot swap rotation |
| `fpp.peaks` | `/fpp peaks` | Peak-hours bot pool scheduler |
| `fpp.rank` | `/fpp rank` | Assign LuckPerms groups |
| `fpp.lpinfo` | `/fpp lpinfo` | LuckPerms diagnostics |
| `fpp.settings` | `/fpp settings` | In-game settings GUI |
### Interaction - New in v1.6.0
| Permission | Command | Description |
|------------|---------|-------------|
| `fpp.inventory` | `/fpp inventory` | Open any bot's 54-slot inventory GUI |
| `fpp.move` | `/fpp move` | Navigate bots with A* pathfinding |
| `fpp.cmd` | `/fpp cmd` | Execute or store commands on bots |
| `fpp.mine` | `/fpp mine` | Enable/stop bot block mining |
### Network & Config
| Permission | Command | Description |
|------------|---------|-------------|
| `fpp.alert` | `/fpp alert` | Broadcast network-wide admin alert |
| `fpp.sync` | `/fpp sync` | Push/pull config across proxy |
### Bypass Permissions
| Permission | Description |
|------------|-------------|
| `fpp.bypass.maxbots` | Ignore the global bot cap (`limits.max-bots`) |
| `fpp.bypass.cooldown` | Skip the per-player spawn cooldown |
---
## 👤 User Permissions (fpp.use)
`fpp.use` is the user wildcard (default: `true` for all players).
| Permission | Command | Description |
|------------|---------|-------------|
| `fpp.use` | - | Grants all user-tier permissions |
| `fpp.user.spawn` | `/fpp spawn` | Spawn personal bots (limited by `fpp.spawn.limit.*`) |
| `fpp.user.tph` | `/fpp tph` | Teleport your own bots to you |
| `fpp.user.xp` | `/fpp xp` | Transfer bot XP to yourself (v1.6.0) |
| `fpp.info.user` | `/fpp info` | View your own bot info |
| `fpp.spawn.limit.1` | - | Default 1-bot personal limit (included in `fpp.use`) |
---
## 🤖 Bot Quantity Limits
Grant `fpp.spawn.limit.<N>` to set the maximum personal bots. FPP picks the **highest** node the player has.
| Node | Cap |
|------|-----|
| `fpp.spawn.limit.1` | 1 bot (included in `fpp.use`) |
| `fpp.spawn.limit.2` | 2 bots |
| `fpp.spawn.limit.3` | 3 bots |
| `fpp.spawn.limit.5` | 5 bots |
| `fpp.spawn.limit.10` | 10 bots |
| `fpp.spawn.limit.15` | 15 bots |
| `fpp.spawn.limit.20` | 20 bots |
| `fpp.spawn.limit.50` | 50 bots |
| `fpp.spawn.limit.100` | 100 bots |
---
## 🛠️ LuckPerms Setup Examples
### Give VIP Players 5 Bots
```
/lp group vip permission set fpp.use true
/lp group vip permission set fpp.spawn.limit.5 true
```
### Give Staff All Admin Commands
```
/lp group staff permission set fpp.op true
```
### Give Mods Selected Admin Commands
```
/lp group mod permission set fpp.spawn true
/lp group mod permission set fpp.delete true
/lp group mod permission set fpp.list true
/lp group mod permission set fpp.freeze true
/lp group mod permission set fpp.inventory true
/lp group mod permission set fpp.move true
```
### Disable User Spawning
```
/lp group default permission set fpp.use false
```
---
## 🆘 Troubleshooting
**"You don't have permission"**
- Check `fpp.op` for admin commands, `fpp.use` for user commands
- Use `/lp user <name> listpermissions` to inspect
**"You have reached your bot limit"**
- Check `fpp.spawn.limit.*` nodes for the player
- FPP picks the highest limit node held
**LP tab-completion missing `fpp.` permissions?**
- All nodes are declared in `plugin.yml` - restart the server after updating FPP
---
**For commands, see [Commands](Commands.md).**