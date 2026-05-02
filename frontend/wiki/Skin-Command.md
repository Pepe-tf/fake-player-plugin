# 🎨 Skin Command

> **Apply or reset per-bot skins — v1.6.6.8**

`/fpp skin` lets you change any active bot's skin to a specific Minecraft username's skin, a URL-based skin (e.g. from Mineskin or Crafatar), or reset it back to the default resolution.

---

## Command Syntax

```
/fpp skin <bot> <username|url|reset>
```

| Argument | Description |
|---|---|
| `<bot>` | Name of the active bot |
| `<username>` | Apply a real Mojang player's skin |
| `<url>` | Apply a skin from a URL (`https://` or `data:image` prefix) |
| `reset` | Reset the bot to its default skin (respects `skin.mode` config) |

## How It Works

1. **Mojang username** — resolves the skin from Mojang's session server and applies it to the bot's GameProfile
2. **URL skin** — resolves the skin texture from the provided URL asynchronously, then applies it
3. **reset** — clears any per-bot skin override and returns to the default skin for the bot's name/mode

**NameTag guard:** If the NameTag plugin is installed and the bot currently has a NameTag-assigned skin, the command is blocked with a `skin-no-nametag` message. Use NameTag's own `/nick` command instead.

## Tab Completion

- Arg 1: all active bot names
- Arg 2: `reset` + online non-bot player names

## Permission

| Permission | Description | Default |
|---|---|---|
| `fpp.skin` | Run `/fpp skin` | op (child of `fpp.op`) |

## Examples

```
/fpp skin SteveBot Notch           # Apply Notch's skin
/fpp skin SteveBot https://...     # Apply a URL-based skin
/fpp skin SteveBot reset           # Reset to default
```

---

> **See also:** [Skin System](Skin-System.md) · [Permissions](Permissions.md)