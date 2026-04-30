# 👥 Bot Select Command

> **Paginated GUI of your manageable bots — v1.6.6.8**

`/fpp bots` opens a paginated chest GUI listing every bot you can administer. Click any bot head to instantly open its per-bot settings GUI (`BotSettingGui`).

---

## Command Syntax

```
/fpp bots [bot]
```

**Aliases:** `/fpp mybots`, `/fpp botmenu`

| Argument | Description |
|---|---|
| `[bot]` | Optional — if provided, opens that bot's `BotSettingGui` directly |

## How It Works

- **No bot name provided** — opens a 54-slot paginated chest GUI titled `FPP Bot Control {page}/{maxPage}`
  - Lists all bots where `BotAccess.canAdminister(player, bot)` returns `true`
  - Each bot is shown as a player head with lore: access level (Admin / Owner / Shared), owner name, "Click to open settings"
  - Navigation arrows on slots 45 (prev), 49 (page info), 53 (next)
  - 45 content slots per page
  - If no manageable bots exist: barrier item at slot 22 — "No manageable bots"
- **Bot name provided** — resolves the bot and opens `BotSettingGui.open(player, fp)` directly

## Permission

| Permission | Description | Default |
|---|---|---|
| `fpp.settings` | Open the bot selection GUI and per-bot settings | op (child of `fpp.op`) |

## Example

```
/fpp bots           # Open the paginated bot list
/fpp bots SteveBot  # Open SteveBot's settings directly
/fpp mybots          # Same as /fpp bots
```

---

> **See also:** [Permissions](Permissions.md) · [Bot Behaviour](Bot-Behaviour.md)