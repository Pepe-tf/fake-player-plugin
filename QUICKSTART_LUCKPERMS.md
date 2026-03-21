# Quick Start Guide - LuckPerms Features

## 🚀 Getting Started in 30 Seconds

Your plugin now has **automatic LuckPerms prefix updates** and **full color format support**!

---

## Step 1: Make Sure Config is Set

Open `plugins/FakePlayerPlugin/config.yml`:

```yaml
luckperms:
  use-prefix: true  # ← Make sure this is true
```

If you changed it, do `/fpp reload`

---

## Step 2: Test It Out!

### Try Rainbow Effect

```bash
/lp group default meta setprefix 1 "<rainbow>PLAYER</rainbow> "
```

**Watch your bots update INSTANTLY!** 🌈

### Try Gradient

```bash
/lp group default meta setprefix 1 "<gradient:#FF0000:#0000FF>PLAYER</gradient> "
```

**Bots change to gradient IMMEDIATELY!** ✨

### Try LuckPerms Shorthand

```bash
/lp group admin meta setprefix 100 "{#FF0000>}ADMIN{#0000FF<} "
```

**Red to blue gradient appears INSTANTLY!** 🎨

### Try Mixed Format

```bash
/lp group vip meta setprefix 90 "&7[<#FFD700>VIP</#FFD700>&7] "
```

**Gray brackets with gold text - INSTANT UPDATE!** ⚡

---

## Step 3: Change Anytime!

```bash
# Spawn some bots first
/fpp spawn
/fpp spawn
/fpp spawn

# Now change the prefix however you want
/lp group default meta setprefix 1 "<rainbow>✨ LEGEND ✨</rainbow> "

# ✨ ALL BOTS UPDATE INSTANTLY! ✨
# No restart, no reload, no respawn needed!
```

---

## 🎯 Common Use Cases

### Update All Bots to Rainbow

```bash
/lp group default meta setprefix 1 "<rainbow>PLAYER</rainbow> "
```

### Give Admin Bots a Gradient

```bash
/lp group admin meta setprefix 100 "<gradient:#FF0000:#FFD700>ADMIN</gradient> "
```

### Change Tab-List Order

```bash
# Higher weight = higher in tab-list
/lp group vip setweight 200
```

### Mix Legacy and Modern

```bash
/lp group premium meta setprefix 85 "&8[<#9782ff>PREMIUM</#9782ff>&8] "
```

---

## ✅ That's It!

No complex setup, no configuration files to edit. Just:

1. ✅ Make sure `use-prefix: true` in config
2. ✅ Change LuckPerms prefixes anytime
3. ✅ **Bots update automatically!**

---

## 🎨 More Examples

```bash
# Gold gradient
/lp group default meta setprefix 1 "{#FFD700>}PLAYER{#FF8C00<} "

# Purple hex color
/lp group default meta setprefix 1 "<#9782ff>PLAYER</#9782ff> "

# Bold red
/lp group default meta setprefix 1 "&c&lPLAYER &r"

# Rainbow with brackets
/lp group default meta setprefix 1 "&7[<rainbow>LEGEND</rainbow>&7] "

# Complex gradient
/lp group default meta setprefix 1 "<gradient:#FF6B6B:#4ECDC4:#45B7D1>PREMIUM</gradient> "
```

**ALL OF THESE UPDATE INSTANTLY!** ⚡✨

---

## 🐛 Troubleshooting

**Q: Bots not updating?**

Check these:
- `luckperms.use-prefix: true` in config.yml
- LuckPerms is installed and enabled
- Console shows: `LuckPerms: auto-update listener registered`

**Q: Want to see debug logs?**

```yaml
debug: true  # in config.yml
```

Then check console for:
```
[LP-Auto-Update] Updated 3 bot(s)
```

---

## 💡 Pro Tip

You can change prefixes **during active gameplay** with players online and bots spawned!

```bash
# Players are playing, bots are running around

/lp group default meta setprefix 1 "<rainbow>RAINBOW MODE</rainbow> "

# ✨ Everyone sees the change INSTANTLY! ✨
# No lag, no disconnect, no disruption
```

---

**Enjoy your instant LuckPerms prefix updates!** 🎉🚀✨

