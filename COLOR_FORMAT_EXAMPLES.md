# FakePlayerPlugin - Supported Color Formats

This document demonstrates all supported color formats for LuckPerms prefixes and bot names.

## ✅ Fully Supported Formats

### 1. MiniMessage Rainbow
```
<rainbow>Rainbow Text</rainbow>
```
Example prefix: `<rainbow>VIP</rainbow> `

### 2. MiniMessage Gradient
```
<gradient:#FF0000:#0000FF>Gradient Text</gradient>
```
Example prefix: `<gradient:#FF6B6B:#4ECDC4>ADMIN</gradient> `

### 3. MiniMessage Hex Colors
```
<#9782ff>Hex Color Text</#9782ff>
```
or (closing tag optional):
```
<#9782ff>Hex Color Text
```
Example prefix: `<#FFD700>GOLD</#FFD700> `

### 4. LuckPerms Gradient Shorthand
```
{#FFFFFF>}Gradient Start{#000000<}
```
Example prefix: `{#FF0000>}[OWNER]{#000000<} `

### 5. Mixed Legacy + MiniMessage
```
&7[<#9782ff>Phantom</#9782ff>&7]
```
Example prefix: `&8[<rainbow>GOD</rainbow>&8] `

### 6. Legacy Color Codes
```
&cRed Text
§cRed Text
&l&cBold Red
```
Example prefix: `&c&lADMIN &r`

### 7. Named Colors
```
<red>Red Text</red>
<blue>Blue Text</blue>
<gray>Gray Text</gray>
```
Example prefix: `<gold>PREMIUM</gold> `

### 8. Decorations
```
<bold>Bold Text</bold>
<italic>Italic Text</italic>
<underlined>Underlined Text</underlined>
<strikethrough>Strikethrough Text</strikethrough>
<obfuscated>Obfuscated Text</obfuscated>
```
Example prefix: `<bold><red>VIP</red></bold> `

## 🎨 Advanced Examples

### Multi-color Gradient with Rainbow
```
&7[<rainbow>LEGEND</rainbow>&7] <gradient:#FFD700:#FF6B6B>
```

### Nested Gradients (via LuckPerms shorthand)
```
{#FF0000>}{#00FF00>}Multi{#0000FF<}{#FFFF00<}
```

### Complex Mixed Format
```
&8[{#9782ff>}Phantom{#ffffff<}&8] &7
```

### Rainbow with Brackets
```
&7[<rainbow>✦ ADMIN ✦</rainbow>&7]
```

## 📝 Usage in LuckPerms

Set a prefix with any of these formats:
```
/lp group admin meta setprefix 100 "<rainbow>ADMIN</rainbow> "
/lp group vip meta setprefix 90 "&7[<#9782ff>VIP</#9782ff>&7] "
/lp group owner meta setprefix 1000 "{#FF0000>}OWNER{#000000<} "
```

## 🔧 Testing

1. Set a LuckPerms prefix with gradient/rainbow
2. Configure FakePlayerPlugin with `luckperms.use-prefix: true`
3. Spawn a bot with `/fpp spawn`
4. Check the bot's nametag and tab-list display

All formats will be automatically converted and displayed correctly!

## 📚 Technical Details

- **Conversion:** All formats are converted to MiniMessage internally via `TextUtil.legacyToMiniMessage()`
- **Rendering:** Final display uses Adventure's `MiniMessage.deserialize()`
- **Compatibility:** Works with any LuckPerms prefix format (legacy, modern, or mixed)
- **Caching:** LP data is cached for 60 seconds to minimize API calls

---

**Note:** The plugin automatically detects and converts all formats. You don't need to do anything special - just set your LuckPerms prefix in any format you prefer!

