# 🔐 Permissions

> **Complete Permission System Guide**  
> **Master Access Control for FPP**

---

## 🎯 Permission Overview

FPP uses a **hierarchical permission system** with granular control over every feature. Permissions are organized into logical groups for easy management.

### 📋 **Permission Hierarchy**

```
fpp.*                           # 🎖️ Full access (superuser)
├── fpp.admin.*                 # 👑 All admin commands
│   ├── fpp.admin.spawn         # Spawn unlimited admin bots
│   ├── fpp.admin.delete        # Delete any bot
│   ├── fpp.admin.chat          # Control fake chat system
│   ├── fpp.admin.swap          # Control swap system
│   ├── fpp.admin.freeze        # Freeze any bot
│   ├── fpp.admin.tp            # Teleport to bots
│   ├── fpp.admin.tph           # Teleport any bot
│   ├── fpp.admin.rank          # Manage bot LuckPerms groups
│   ├── fpp.admin.reload        # Hot-reload configurations
│   ├── fpp.admin.migrate       # Data migration utilities
│   ├── fpp.admin.stats         # Server statistics
│   └── fpp.admin.lpinfo        # LuckPerms diagnostics
├── fpp.user.*                  # 👤 User-level commands
│   ├── fpp.user.spawn          # Spawn personal bots (limited)
│   ├── fpp.user.delete         # Delete own bots
│   ├── fpp.user.list           # List all bots
│   ├── fpp.user.info           # View bot information
│   └── fpp.user.tph            # Teleport own bots
├── fpp.bypass.*                # 🚀 Bypass restrictions
│   ├── fpp.bypass.cooldown     # No spawn cooldown
│   └── fpp.bypass.limit        # Ignore global bot limits
├── fpp.bot.*                   # 🤖 Bot quantity limits
│   ├── fpp.bot.1               # Max 1 personal bot
│   ├── fpp.bot.5               # Max 5 personal bots
│   ├── fpp.bot.10              # Max 10 personal bots
│   ├── fpp.bot.25              # Max 25 personal bots
│   └── fpp.bot.100             # Max 100 personal bots
└── fpp.help                    # 📖 Access help command (default: everyone)
```

---

## 👑 **Admin Permissions**

### 🛠️ **Core Admin Commands**

| Permission | Command | Description |
|------------|---------|-------------|
| `fpp.admin.spawn` | `/fpp spawn` | Spawn unlimited admin bots (no personal limits) |
| `fpp.admin.delete` | `/fpp despawn` | Delete any bot on the server |
| `fpp.admin.reload` | `/fpp reload` | Hot-reload all configuration files |
| `fpp.admin.migrate` | `/fpp migrate` | Access data migration and backup tools |
| `fpp.admin.stats` | `/fpp stats` | View server performance statistics |

### 🎮 **System Control**

| Permission | Command | Description |
|------------|---------|-------------|
| `fpp.admin.chat` | `/fpp chat` | Enable/disable fake chat system globally |
| `fpp.admin.swap` | `/fpp swap` | Control bot swap system and force swaps |
| `fpp.admin.freeze` | `/fpp freeze` | Freeze/unfreeze any bot's movement and AI |

### 🚀 **Advanced Management**

| Permission | Command | Description |
|------------|---------|-------------|
| `fpp.admin.tp` | `/fpp tp` | Teleport to any bot's location |
| `fpp.admin.tph` | `/fpp tph` | Teleport any bot to your location |
| `fpp.admin.rank` | `/fpp rank` | Assign LuckPerms groups to bots |
| `fpp.admin.lpinfo` | `/fpp lpinfo` | LuckPerms integration diagnostics |

---

## 👤 **User Permissions**

### 🎭 **Personal Bot Management**

| Permission | Command | Description |
|------------|---------|-------------|
| `fpp.user.spawn` | `/fpp spawn` | Spawn personal bots (limited by `fpp.bot.*`) |
| `fpp.user.delete` | `/fpp despawn` | Delete only your own bots |
| `fpp.user.tph` | `/fpp tph` | Teleport your bots to your location |

### 📊 **Information Access**

| Permission | Command | Description |
|------------|---------|-------------|
| `fpp.user.list` | `/fpp list` | View all active bots on the server |
| `fpp.user.info` | `/fpp info` | View detailed information about any bot |

### 📖 **Help Access**

| Permission | Command | Description |
|------------|---------|-------------|
| `fpp.help` | `/fpp help` | Access the help menu (usually granted to everyone) |

---

## 🤖 **Bot Limit System**

### 📈 **How Bot Limits Work**

FPP uses a **dual-limit system**:

1. **Personal Limits:** `fpp.bot.<number>` permissions
2. **Global Limit:** `limits.max-bots` in config.yml

**Resolution Order:**
```
1. Check fpp.bot.* permissions (highest number wins)
2. Fall back to config.yml limits.user-bot-limit
3. Apply limits.max-bots as absolute maximum
```

### 🎯 **Bot Limit Permissions**

| Permission | Personal Limit | Typical Use Case |
|------------|----------------|------------------|
| `fpp.bot.1` | 1 bot | Basic users, trial members |
| `fpp.bot.5` | 5 bots | Regular players, default users |
| `fpp.bot.10` | 10 bots | VIP members, supporters |
| `fpp.bot.25` | 25 bots | Staff members, moderators |
| `fpp.bot.50` | 50 bots | Senior staff, event managers |
| `fpp.bot.100` | 100 bots | Administrators, server owners |

**Examples:**
```yaml
# LuckPerms Examples
/lp group default permission set fpp.bot.5
/lp group vip permission set fpp.bot.10
/lp group staff permission set fpp.bot.25
/lp user Notch permission set fpp.bot.100
```

### ⚠️ **Important Notes**

- **Highest wins:** If a user has both `fpp.bot.5` and `fpp.bot.10`, they get 10 bots
- **Admin bypass:** `fpp.admin.spawn` ignores personal limits entirely  
- **Global enforcement:** `limits.max-bots` applies to total server bots
- **Real-time updates:** Limit changes apply immediately (no restart needed)

---

## 🚀 **Bypass Permissions**

### ⏱️ **Cooldown Bypass**

| Permission | Effect |
|------------|--------|
| `fpp.bypass.cooldown` | Ignore spawn cooldown timer |

**Use Cases:**
- Staff members who need immediate bot access
- Event managers during live events
- Testing and development scenarios

### 📊 **Limit Bypass**

| Permission | Effect |
|------------|--------|
| `fpp.bypass.limit` | Ignore global bot limits |

**Use Cases:**
- Emergency server population
- Special events requiring many bots
- Administrative testing

**⚠️ Warning:** Use limit bypass carefully to avoid server performance issues.

---

## 🛠️ **Permission Setup Examples**

### 🏢 **Server Staff Structure**

#### **Default Players**
```yaml
permissions:
  - fpp.help
  - fpp.user.spawn
  - fpp.user.delete
  - fpp.user.list
  - fpp.user.info
  - fpp.user.tph
  - fpp.bot.5           # 5 personal bots
```

#### **VIP Members**
```yaml
permissions:
  - fpp.help
  - fpp.user.*          # All user commands
  - fpp.bot.10          # 10 personal bots
  - fpp.bypass.cooldown # No spawn delays
```

#### **Moderators**
```yaml
permissions:
  - fpp.user.*          # All user commands
  - fpp.admin.freeze    # Freeze problematic bots
  - fpp.admin.tp        # Teleport to investigate
  - fpp.admin.stats     # Monitor server impact
  - fpp.bot.15          # 15 personal bots
  - fpp.bypass.cooldown
```

#### **Administrators**
```yaml
permissions:
  - fpp.admin.*         # All admin commands
  - fpp.bypass.*        # All bypasses
  - fpp.bot.50          # 50 personal bots
```

#### **Server Owner**
```yaml
permissions:
  - fpp.*               # Everything
```

### 🎮 **Minigame Server Setup**

#### **Players**
```yaml
permissions:
  - fpp.help
  - fpp.user.list       # See available bots for teams
  - fpp.bot.2           # 2 bots for practice
```

#### **Game Masters**
```yaml
permissions:
  - fpp.admin.spawn     # Create bots for events
  - fpp.admin.delete    # Clean up after events
  - fpp.admin.freeze    # Control bot behavior
  - fpp.admin.tph       # Position bots for games
  - fpp.bypass.*        # No restrictions during events
```

### 🏰 **Roleplay Server Setup**

#### **Citizens**
```yaml
permissions:
  - fpp.help
  - fpp.user.spawn
  - fpp.user.delete
  - fpp.user.tph
  - fpp.bot.3           # Small entourage
```

#### **Nobles/VIPs**
```yaml
permissions:
  - fpp.user.*
  - fpp.bot.8           # Larger retinue
  - fpp.bypass.cooldown # Immediate access
```

#### **Town Staff**
```yaml
permissions:
  - fpp.admin.spawn     # Create NPCs for towns
  - fpp.admin.rank      # Assign appropriate groups
  - fpp.admin.freeze    # Create stationary NPCs
  - fpp.bot.20          # Many NPCs needed
```

---

## 🎯 **LuckPerms Integration**

### 🔗 **Setting Up with LuckPerms**

FPP integrates seamlessly with LuckPerms for advanced permission management:

#### **1. Create Permission Groups**
```bash
# Create bot-related groups
/lp creategroup fpp-basic
/lp creategroup fpp-vip  
/lp creategroup fpp-staff
/lp creategroup fpp-admin
```

#### **2. Assign Permissions**
```bash
# Basic users
/lp group fpp-basic permission set fpp.user.*
/lp group fpp-basic permission set fpp.bot.5

# VIP users
/lp group fpp-vip permission set fpp.user.*
/lp group fpp-vip permission set fpp.bot.10
/lp group fpp-vip permission set fpp.bypass.cooldown

# Staff members
/lp group fpp-staff permission set fpp.user.*
/lp group fpp-staff permission set fpp.admin.freeze
/lp group fpp-staff permission set fpp.admin.tp
/lp group fpp-staff permission set fpp.bot.20
/lp group fpp-staff permission set fpp.bypass.cooldown

# Administrators
/lp group fpp-admin permission set fpp.admin.*
/lp group fpp-admin permission set fpp.bypass.*
/lp group fpp-admin permission set fpp.bot.50
```

#### **3. Assign Users to Groups**
```bash
/lp user <username> parent add fpp-basic    # Default users
/lp user <username> parent add fpp-vip      # VIP users
/lp user <username> parent add fpp-staff    # Staff members
/lp user <username> parent add fpp-admin    # Administrators
```

### 🎨 **Bot LuckPerms Integration**

FPP treats bots as real NMS ServerPlayer entities — LuckPerms detects them as online players and applies prefixes, suffixes, and group weights automatically. No extra configuration is needed beyond setting the default group.

#### **Configuration** (config.yml)
```yaml
luckperms:
  default-group: ""   # LP group assigned to every new bot at spawn (blank = "default")
```

#### **Bot Group Commands**
```bash
# Assign specific groups to bots
/fpp rank Steve admin         # Make Steve use admin group
/fpp rank Alex moderator      # Make Alex use moderator group
/fpp rank Guard security      # Make Guard use security group
/fpp rank Steve clear         # Reset bot to default group
/fpp rank list                # Show each bot's current LP group
```

#### **Group Setup for Bots**
```bash
# Create bot-specific groups
/lp creategroup bot-admin
/lp group bot-admin meta setprefix "&c[Admin] &f"
/lp group bot-admin meta setweight 100

/lp creategroup bot-guard
/lp group bot-guard meta setprefix "&9[Guard] &f" 
/lp group bot-guard meta setweight 50

/lp creategroup bot-citizen
/lp group bot-citizen meta setprefix "&7[Citizen] &f"
/lp group bot-citizen meta setweight 10
```

---

## 🔍 **Permission Diagnostics**

### 🩺 **Checking Permissions**

#### **LuckPerms Commands**
```bash
/lp user <username> info                    # View user's permissions
/lp user <username> permission check fpp.user.spawn  # Check specific permission
/lp group <groupname> info                  # View group permissions
```

#### **FPP Diagnostic Commands**
```bash
/fpp lpinfo                    # LuckPerms integration status
/fpp lpinfo <botname>          # Bot's LP group information
/fpp stats --detailed          # Permission-related statistics
```

### 🐛 **Common Permission Issues**

#### **"Permission Denied" Errors**

**Issue:** User can't use commands
```bash
# Check user's permissions
/lp user <username> permission check fpp.user.spawn
/lp user <username> info

# Grant missing permission
/lp user <username> permission set fpp.user.spawn
```

#### **Bot Limit Issues**

**Issue:** User can't spawn more bots
```bash
# Check current bot limit
/lp user <username> permission check fpp.bot.10

# Increase limit
/lp user <username> permission unset fpp.bot.5
/lp user <username> permission set fpp.bot.10
```

#### **Admin Commands Not Working**

**Issue:** Staff can't use admin commands
```bash
# Check admin permissions
/lp user <username> permission check fpp.admin.*

# Grant admin access
/lp user <username> permission set fpp.admin.*
```

---

## 📚 **Best Practices**

### ✅ **Recommended Setup**

1. **Use Groups:** Assign permissions to groups, not individual users
2. **Hierarchical Structure:** Create clear permission levels (basic → vip → staff → admin)
3. **Gradual Limits:** Increase bot limits gradually based on user trust/activity
4. **Monitor Usage:** Use `/fpp stats` to monitor bot usage and performance
5. **Document Changes:** Keep track of permission changes and rationale

### ⚠️ **Security Considerations**

1. **Limit Admin Access:** Only give `fpp.admin.*` to trusted staff
2. **Monitor Bot Limits:** High limits can impact server performance
3. **Regular Audits:** Review permissions periodically
4. **Bypass Permissions:** Use `fpp.bypass.*` sparingly and for trusted users only
5. **Global Limits:** Always set reasonable `limits.max-bots` in config

### 🎯 **Performance Guidelines**

**Bot Limits by Server Size:**
- **Small Server (10-50 players):** 5-10 bots per user, 50 global
- **Medium Server (50-100 players):** 3-8 bots per user, 100 global
- **Large Server (100+ players):** 2-5 bots per user, 150+ global

**Monitor Performance:**
```bash
/fpp stats --detailed         # Check TPS impact
/timings report               # Detailed server performance
```

---

## 🔧 **Migration from Other Permission Plugins**

### 🔄 **From PermissionsEx (PEX)**

```yaml
# PEX format
groups:
  default:
    permissions:
    - fpp.user.*
    - fpp.bot.5
```

**Convert to LuckPerms:**
```bash
/lp migration permissionsex   # Auto-migrate
# OR manually:
/lp group default permission set fpp.user.*
/lp group default permission set fpp.bot.5
```

### 🔄 **From GroupManager**

```yaml
# GroupManager format
groups:
  default:
    permissions:
    - fpp.user.*
    - fpp.bot.5
```

**Convert to LuckPerms:**
```bash
/lp migration groupmanager    # Auto-migrate if available
# OR manually recreate groups
```

---

## 🆘 **Troubleshooting**

### ❌ **Common Issues**

**"You don't have permission to use this command"**
- ✅ Check user has required permission node
- ✅ Verify permission plugin is working
- ✅ Check for permission conflicts or negations
- ✅ Ensure user is in correct group

**"You have reached your bot limit (X/Y)"**
- ✅ Check user's `fpp.bot.*` permissions
- ✅ Verify highest bot limit permission
- ✅ Check global bot limit in config
- ✅ Remove existing bots if needed

**"Bot creation failed due to server limits"**
- ✅ Check `limits.max-bots` in config.yml
- ✅ Use `/fpp list` to see current bot count
- ✅ Remove unused bots or increase global limit
- ✅ Monitor server performance before increasing

**LuckPerms integration not working**
- ✅ Verify LuckPerms is installed and loaded
- ✅ Use `/fpp lpinfo` for diagnostic information
- ✅ Restart server after major LP changes

### 🔍 **Debug Commands**

```bash
/lp user <username> info                     # Check user permissions
/fpp lpinfo                                  # LP integration status  
/fpp stats                                   # Bot counts and limits
/fpp list --owner <username>                 # User's current bots
/permissions check <username> fpp.user.spawn # Test specific permission
```

---

## 📖 **Quick Reference**

### 🎯 **Essential Permissions**

**Basic User Package:**
```
fpp.help + fpp.user.* + fpp.bot.5
```

**VIP Package:** 
```
fpp.user.* + fpp.bot.10 + fpp.bypass.cooldown
```

**Staff Package:**
```
fpp.user.* + fpp.admin.freeze + fpp.admin.tp + fpp.bot.20
```

**Admin Package:**
```
fpp.admin.* + fpp.bypass.* + fpp.bot.50
```

### 🔗 **Related Documentation**

- **[Commands](Commands.md)** — Complete command reference
- **[Configuration](Configuration.md)** — Global limits and settings
- **[LuckPerms Setup](Getting-Started.md#luckperms-integration)** — Detailed LP integration guide

---

**🔐 Master FPP permissions and create the perfect access control system for your server!**
