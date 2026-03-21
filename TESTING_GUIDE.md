# Complete Testing Guide

## 🧪 How to Test All Features

---

## Test 1: OP Spawn (Admin-Tier Behavior)

### Setup
1. Be OP on your server (or have `fpp.spawn` permission)
2. Make sure LuckPerms is installed

### Test Commands
```bash
# Spawn 5 bots
/fpp spawn 5
```

### Expected Results
✅ Bots spawn with proper names (TheCampingRusher, Skeppy, etc.)  
✅ Tab-list shows: `[PLAYER] TheCampingRusher` (NOT `bot-El_Pepes-1`)  
✅ Join messages show: `[PLAYER] TheCampingRusher joined the game`  
✅ No malformed gradient tags  

---

## Test 2: LuckPerms Gradient (All Formats)

### Test Rainbow
```bash
/lp group default meta setprefix 1 "<rainbow>PLAYER</rainbow> "
```

**Expected:** All bots instantly show rainbow prefix 🌈

### Test Gradient (MiniMessage)
```bash
/lp group default meta setprefix 1 "<gradient:#FF0000:#0000FF>PLAYER</gradient> "
```

**Expected:** All bots instantly show red→blue gradient

### Test Gradient (LuckPerms Shorthand)
```bash
/lp group default meta setprefix 1 "{#FFFFFF>}[PLAYER]{#000000<} "
```

**Expected:** All bots instantly show white→black gradient with brackets

### Test Mixed Format
```bash
/lp group default meta setprefix 1 "&7[<#FFD700>VIP</#FFD700>&7] "
```

**Expected:** Gray brackets with gold VIP text

### Test Hex Color
```bash
/lp group default meta setprefix 1 "<#9782ff>PREMIUM</#9782ff> "
```

**Expected:** Purple "PREMIUM" prefix

---

## Test 3: Auto-Update (Live Changes)

### Step-by-Step
```bash
# 1. Spawn bots
/fpp spawn 5

# 2. Check current prefix in tab-list
# (should show whatever prefix is set)

# 3. Change the prefix to rainbow
/lp group default meta setprefix 1 "<rainbow>RAINBOW</rainbow> "

# 4. ✨ INSTANT UPDATE! ✨
# - Check tab-list immediately
# - All 5 bots should now have rainbow prefix
# - No reload, no respawn needed

# 5. Change to gradient
/lp group default meta setprefix 1 "{#FF6B6B>}GRADIENT{#4ECDC4<} "

# 6. ✨ INSTANT UPDATE AGAIN! ✨
# - Tab-list updates immediately
# - All bots now show gradient

# 7. Change weight
/lp group default setweight 100

# 8. ✨ TAB-LIST REORDERS! ✨
# - Bots move in tab-list immediately
```

---

## Test 4: Debug Logging

### Enable Debug
Edit `plugins/FakePlayerPlugin/config.yml`:
```yaml
debug: true
```

Then `/fpp reload`

### Watch Console During Prefix Change

```bash
/lp group default meta setprefix 1 "<rainbow>TEST</rainbow> "
```

### Expected Console Output
```
[FPP] [LP-Auto-Update] Group 'default' data recalculated
[FPP] [LP-Auto-Update] Updating all bots (group default changed)
[FPP] [LP-Auto-Update] Updated bot 'TheCampingRusher' -> '<rainbow>TEST</rainbow> TheCampingRusher'
[FPP] [LP-Auto-Update] Updated bot 'Skeppy' -> '<rainbow>TEST</rainbow> Skeppy'
[FPP] [LP-Auto-Update] Updated 5 bot(s)
```

---

## Test 5: Malformed Gradient Cleanup

### Test Invalid Hex Codes

```bash
# Malformed gradient (5 digits instead of 6)
/lp group default meta setprefix 1 "{#FFFFF>}[PLAYER]{#00000<} "
#                                    ^^^^^^         ^^^^^^
#                                    5 digits       5 digits
```

### Expected Behavior
✅ Invalid tags are removed  
✅ Text content preserved: `[PLAYER]`  
✅ No broken `</#00000>` tags appear  
✅ Bots display cleanly  

### Test Empty Gradient

```bash
# Empty gradient tags
/lp group default meta setprefix 1 "{#>}[PLAYER]{#<} "
```

### Expected Behavior
✅ Empty tags removed  
✅ Text preserved: `[PLAYER]`  
✅ No rendering errors  

---

## Test 6: User-Tier Behavior (Non-OP)

### Setup
1. Remove OP status from a test player
2. Don't give them `fpp.spawn` permission
3. They should have `fpp.user.spawn` (default: true)

### Test Commands
```bash
# As non-OP user
/fpp spawn

# Can only spawn 1 bot (user limit)
# Bot uses user-format: bot-PlayerName-1
```

### Expected Results
✅ Only 1 bot spawns (user limit)  
✅ Bot name: `bot-PlayerName-1` (user format)  
✅ Cannot spawn multiple bots  
✅ Cannot use `--name` flag  

---

## Test 7: Mixed Scenarios

### Scenario A: OP with Multiple Bots + Rainbow
```bash
# As OP
/fpp spawn 10

# Set rainbow prefix
/lp group default meta setprefix 1 "<rainbow>BOT</rainbow> "

# Expected:
# - 10 bots with proper names (Notch, Dream, etc.)
# - All show rainbow prefix in tab-list
# - Updates happen instantly
```

### Scenario B: Group Weight Changes
```bash
# Spawn bots from different groups
/lp user Steve parent add vip    # Steve's bots get VIP rank
/fpp spawn 5                      # as Steve

/lp user Bob parent add admin     # Bob's bots get ADMIN rank  
/fpp spawn 5                      # as Bob

# Change VIP weight
/lp group vip setweight 200

# Expected:
# - Steve's 5 bots instantly reorder in tab-list
# - Bob's bots stay in their position
# - No respawn needed
```

### Scenario C: Prefix + Weight Change Together
```bash
/fpp spawn 5

# Change both at once
/lp group default meta setprefix 1 "<gradient:#FF0000:#0000FF>PLAYER</gradient> "
/lp group default setweight 150

# Expected:
# - Bots get new gradient prefix (instant)
# - Bots reorder in tab-list (instant)
# - Both changes apply together
```

---

## 📊 Verification Checklist

After running tests, verify:

### Tab-List Display
- [ ] Bot names show correctly (not `bot-{spawner}-{num}` for OPs)
- [ ] Prefixes render with colors/gradients
- [ ] Bots order correctly by weight
- [ ] Display updates when LP data changes

### Join/Leave Messages
- [ ] No malformed tags (`</#00000>`)
- [ ] Prefixes render correctly
- [ ] Colors/gradients work
- [ ] Messages format properly

### Console Logging (debug: true)
- [ ] Shows LP listener registration
- [ ] Shows update events
- [ ] Shows bot update counts
- [ ] Shows weight calculations

### Performance
- [ ] No lag spikes during prefix changes
- [ ] Updates apply within 1 tick (~50ms)
- [ ] Multiple rapid changes don't spam
- [ ] Cache invalidation works

---

## 🐛 Known Issues & Solutions

### Issue: "Permission denied" as OP

**Cause:** LuckPerms overrides default permissions

**Solution 1:** Grant permission explicitly
```bash
/lp group admin permission set fpp.* true
```

**Solution 2:** Already fixed! Plugin now checks OP status

### Issue: Prefix not showing

**Cause:** `use-prefix: false` in config

**Solution:** Enable it
```yaml
luckperms:
  use-prefix: true
```

Then `/fpp reload`

### Issue: Updates delayed

**Cause:** Normal - updates run on next tick

**Solution:** Wait ~50ms, it's intentional for performance

---

## 🎯 Full Workflow Test

```bash
# 1. Clean start
/fpp delete all

# 2. Set a simple prefix
/lp group default meta setprefix 1 "&c[PLAYER] "

# 3. Spawn as OP
/fpp spawn 5

# 4. Verify tab-list shows: [PLAYER] BotName (red)

# 5. Change to rainbow
/lp group default meta setprefix 1 "<rainbow>PLAYER</rainbow> "

# 6. Verify INSTANT update to rainbow

# 7. Change to gradient
/lp group default meta setprefix 1 "{#FF0000>}GRADIENT{#0000FF<} "

# 8. Verify INSTANT update to red→blue gradient

# 9. Change weight
/lp group default setweight 50

# 10. Verify tab-list reorders immediately

# 11. Spawn more bots
/fpp spawn 3

# 12. New bots should have current gradient prefix

# ✅ SUCCESS! All features working!
```

---

## 📝 Notes

- **Auto-update requires LuckPerms** installed and enabled
- **Color formats work with or without** auto-update
- **OP check works** regardless of LuckPerms
- **Malformed tag cleanup** always active

---

## ✅ Expected Results Summary

| Test | Before Fix | After Fix |
|------|------------|-----------|
| OP spawns 5 bots | `bot-Player-1` in tab | `[PLAYER] BotName` in tab |
| Malformed gradient | `</#00000>` broken tag | Cleaned, text preserved |
| Change LP prefix | Need /fpp reload + respawn | ✨ Instant update |
| Rainbow effect | Not supported | ✅ Works perfectly |
| Mixed formats | Breaks MiniMessage tags | ✅ Preserves all tags |
| Weight change | Need respawn | ✨ Instant reorder |

---

**All tests should pass! If you encounter any issues, check the console with `debug: true` enabled.** 🧪✅

