# FakePlayerPlugin - LuckPerms Color Support Enhancement

## 🎉 What's New

The plugin now **fully supports all LuckPerms color formats** including:

✅ **MiniMessage rainbow tags** - `<rainbow>text</rainbow>`  
✅ **MiniMessage gradients** - `<gradient:#FF0000:#0000FF>text</gradient>`  
✅ **MiniMessage hex colors** - `<#9782ff>text</#9782ff>`  
✅ **LuckPerms gradient shorthand** - `{#fffff>}[PLAYER]{#00000<}`  
✅ **Mixed legacy + MiniMessage** - `&7[<#9782ff>Phantom</#9782ff>&7]`  
✅ **Legacy color codes** - `&c`, `§c`, `&l`, etc.  

## 📝 Changes Made

### 1. Enhanced `TextUtil.legacyToMiniMessage()` 
**File:** `src/main/java/me/bill/fakePlayerPlugin/util/TextUtil.java`

- Added detection for existing MiniMessage tags to prevent double-conversion
- Implemented `convertMixedFormat()` method to handle mixed legacy + MiniMessage syntax
- Created `legacyCodeToMiniMessage()` mapper for individual legacy codes
- Now preserves all MiniMessage tags (`<rainbow>`, `<gradient>`, `<#hex>`) while converting legacy codes

**Key improvements:**
- Detects when a string contains MiniMessage tags and uses special handling
- Converts legacy `&` and `§` codes only when they're outside MiniMessage tags
- Recursively handles nested tags and complex gradient combinations

### 2. Updated `LuckPermsHelper` Documentation
**File:** `src/main/java/me/bill/fakePlayerPlugin/util/LuckPermsHelper.java`

- Added comprehensive color format documentation in class Javadoc
- Listed all supported formats with examples
- Updated to reflect full gradient and rainbow support

### 3. Updated AGENTS.md
**File:** `AGENTS.md`

- Added note about full color format support in TextUtil
- Added LuckPerms integration documentation
- Listed all supported color formats with examples

### 4. Created Examples Documentation
**File:** `COLOR_FORMAT_EXAMPLES.md`

- Comprehensive examples of all supported color formats
- Usage instructions for LuckPerms
- Testing guide
- Technical details about implementation

## 🧪 How It Works

### Processing Pipeline

1. **LuckPermsHelper extracts raw prefix** from LP group
2. **TextUtil.legacyToMiniMessage() converts**:
   - LuckPerms `{#...}` shorthand → `<gradient:...>`
   - Detects if MiniMessage tags present
   - If mixed format: converts only legacy codes outside tags
   - If pure legacy: converts all codes
   - If pure MiniMessage: returns as-is
3. **MiniMessage.deserialize()** renders final colored component

### Example Conversions

| Input (LP Prefix) | Converted To | Display |
|-------------------|--------------|---------|
| `{#FF0000>}ADMIN{#0000FF<}` | `<gradient:#FF0000:#0000FF>ADMIN</gradient>` | Red→Blue gradient |
| `&7[<#9782ff>VIP</#9782ff>&7]` | `<gray>[<#9782ff>VIP</#9782ff><gray>]` | Gray brackets, purple text |
| `<rainbow>LEGEND</rainbow>` | `<rainbow>LEGEND</rainbow>` | Rainbow animation |
| `&c&lADMIN &r` | `<red><bold>ADMIN <reset>` | Bold red text |

## 🔧 Testing

### In-Game Testing

1. **Set a gradient prefix in LuckPerms:**
   ```
   /lp group admin meta setprefix 100 "{#FF0000>}ADMIN{#0000FF<} "
   ```

2. **Configure FPP to use LP prefixes:**
   ```yaml
   luckperms:
     use-prefix: true
     bot-group: ""  # empty = use spawner's group
   ```

3. **Spawn a bot:**
   ```
   /fpp spawn
   ```

4. **Check the result:**
   - Bot nametag shows gradient prefix
   - Tab list shows bot with gradient prefix
   - All color formats render correctly

### Test Different Formats

```bash
# Rainbow
/lp group vip meta setprefix 90 "<rainbow>VIP</rainbow> "

# Mixed legacy + hex
/lp group premium meta setprefix 85 "&7[<#FFD700>PREMIUM</#FFD700>&7] "

# LuckPerms gradient shorthand
/lp group owner meta setprefix 1000 "{#FFFFFF>}[OWNER]{#000000<} "

# Complex rainbow with brackets
/lp group legend meta setprefix 95 "&8[<rainbow>LEGEND</rainbow>&8] "
```

## 📊 Backwards Compatibility

✅ All existing prefixes continue to work  
✅ Pure legacy codes (`&c`, `§c`) still supported  
✅ Existing MiniMessage tags preserved  
✅ No config changes required  
✅ Automatic detection and conversion  

## 🐛 Bug Fixes

- Fixed compilation errors in `getBotLpData()` calls (missing UUID parameter)
- Fixed mixed format handling (legacy codes breaking MiniMessage tags)
- Fixed gradient shorthand not being converted before MiniMessage parsing

## 📚 Technical Notes

- **Conversion is cached:** LP data cached for 60 seconds to minimize API calls
- **Smart detection:** Automatically detects format and uses appropriate conversion
- **Preserves nesting:** Handles nested tags and complex gradient combinations
- **No breaking changes:** All existing functionality preserved

## 🎯 Summary

The plugin now **fully supports every color format** that LuckPerms can produce:

- ✅ Modern MiniMessage tags (rainbow, gradient, hex)
- ✅ LuckPerms color shorthand (`{#...}`)
- ✅ Legacy color codes (`&`, `§`)
- ✅ Mixed formats (legacy + MiniMessage together)
- ✅ Nested and complex gradients

**No configuration needed** - just set your LuckPerms prefix in any format and the plugin will handle it automatically!

---

**Build Status:** ✅ BUILD SUCCESS  
**Files Modified:** 3 core files + 2 documentation files  
**Tests Passed:** All compilation checks passed  
**Version:** 1.4.20+gradient-support

