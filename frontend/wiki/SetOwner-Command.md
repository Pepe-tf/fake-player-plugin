# 👤 Set Owner Command

> **Transfer bot ownership — v1.6.6.8**

`/fpp setowner` transfers full ownership of a bot to another player. The new owner gains complete control over the bot, and all previously shared controllers are removed.

---

## Command Syntax

```
/fpp setowner <bot> <player>
```

| Argument | Description |
|---|---|
| `<bot>` | Name of the active bot |
| `<player>` | Name of the new owner (online or offline) |

## What Happens

1. Bot's `spawnedBy` / `spawnedByUuid` fields are updated to the new owner
2. **All shared controllers are cleared** — anyone who previously had access via `share-control` loses it
3. If database persistence is enabled, the owner change is written to the database immediately

## Tab Completion

- Arg 1: all active bot names
- Arg 2: all online player names

## Permission

| Permission | Description | Default |
|---|---|---|
| `fpp.setowner` | Run `/fpp setowner` | op (child of `fpp.op`) |

## Example

```
/fpp setowner SteveBot Alex
```

Transfers ownership of `SteveBot` to the player `Alex`. Alex is now the sole owner.

---

> **See also:** [Permissions](Permissions.md) · [Bot Behaviour](Bot-Behaviour.md)