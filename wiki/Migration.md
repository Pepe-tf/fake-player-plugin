> [Home](Home.md) · [Getting Started](Getting-Started.md) · [Commands](Commands.md) · [Permissions](Permissions.md) · [Configuration](Configuration.md) · [Migration & Backups](Migration.md) · [Database](Database.md) · [Skin System](Skin-System.md) · [FAQ](FAQ.md)

---

# ᴍɪɢʀᴀᴛɪᴏɴ & ʙᴀᴄᴋᴜᴘꜱ

> **Version:** 1.1.4 · **Platform:** Paper 1.21+

This page covers the **automatic config migration system**, **manual backup tools**, and **database migration utilities** built into FPP since v1.1.4. These features ensure you never lose your config or bot session data when updating the plugin.

---

## Table of Contents

1. [Overview](#overview)
2. [Automatic Config Migration](#automatic-config-migration)
3. [Backup System](#backup-system)
4. [The `/fpp migrate` Command](#the-fpp-migrate-command)
5. [Merging an Old Database](#merging-an-old-database)
6. [Exporting Data to CSV](#exporting-data-to-csv)
7. [SQLite → MySQL Migration](#sqlite--mysql-migration)
8. [Manual Rollback](#manual-rollback)
9. [Config Version Reference](#config-version-reference)

---

## Overview

Every time you update FPP, three things may have changed:

| What changed | How FPP handles it |
|---|---|
| New `config.yml` keys added | **Auto-filled** from defaults on startup |
| Old keys renamed or restructured | **Auto-migrated** via the version chain |
| Database schema updated | **Auto-applied** via incremental SQL migrations |

The migration system runs **automatically on every startup** — you don't need to do anything manually for routine updates. Manual tools (`/fpp migrate`) exist for advanced cases like merging databases, exporting data, or switching from SQLite to MySQL.

---

## Automatic Config Migration

When FPP starts, it checks the `config-version` key in your `config.yml`. If it is lower than the current version, the migration chain runs automatically:

```
[FPP] ══════════════════════════════════════════════════════
[FPP] ── Config Migration ──────────────────────────────────
[FPP] Upgrading config from v5 → v10…
[FPP] A backup will be created before any changes are written.
[FPP] Backup created → backups/2026-03-14_12-00-00_pre-migration-v5/
[FPP] Config migrated to v10 successfully.
```

### What gets migrated automatically

- **New sections** are added with sensible defaults (e.g. `swap:`, `fake-chat:`, `database:`)
- **Renamed keys** are moved to their new paths (e.g. `max-bots` → `limits.max-bots`)
- **Restructured values** are converted (e.g. `skin.enabled: true` → `skin.mode: auto`)
- **Any missing keys** are filled from the jar's bundled default config

### What is preserved

**All your existing values are preserved.** The migrator only:
- Adds new keys that don't exist yet
- Renames old keys to their new names
- Converts value types where the format changed

It **never** overwrites a key you've already set.

---

## Backup System

FPP automatically creates a backup before any migration. Backups are also triggered by database merges.

### Backup location

```
plugins/FakePlayerPlugin/
└── backups/
    ├── 2026-03-14_12-00-00_pre-migration-v5/
    │   ├── config.yml
    │   ├── bot-names.yml
    │   ├── bot-messages.yml
    │   ├── language/
    │   │   └── en.yml
    │   ├── active-bots.yml
    │   ├── fpp.db
    │   └── MANIFEST.txt
    └── 2026-03-10_09-30-00_manual/
        └── ...
```

### What is backed up

| File | Description |
|---|---|
| `config.yml` | Main plugin config |
| `bot-names.yml` | Bot name pool |
| `bot-messages.yml` | Fake chat messages |
| `language/en.yml` | Language file |
| `active-bots.yml` | Last known bot positions (YAML persistence) |
| `fpp.db` | SQLite database (with WAL/SHM files if present) |
| `MANIFEST.txt` | Backup reason, plugin version, timestamp |

### Retention policy

FPP keeps the **10 most recent** backup sets. Older ones are pruned automatically to save disk space.

### Manual backup

```
/fpp migrate backup
```

Create a backup at any time — useful before making large config changes or switching database backends.

---

## The `/fpp migrate` Command

**Permission:** `fpp.admin.migrate` (default: op, included in `fpp.*`)

```
/fpp migrate <subcommand>
```

| Subcommand | Description |
|---|---|
| `backup` | Create a manual backup of all plugin files now |
| `backups` | List all stored backup directories with ages |
| `status` | Show config version, database stats, backup count |
| `config` | Re-run the full config migration chain |
| `db merge [file]` | Merge records from an old `fpp.db` into the current database |
| `db export` | Export all session history to a CSV file |
| `db tomysql` | Migrate all data from local SQLite into configured MySQL |

### `/fpp migrate status` output

```
[FPP] ᴍɪɢʀᴀᴛɪᴏɴ ꜱᴛᴀᴛᴜꜱ
[FPP]   Config version  : 10 / 10  ✔ current
[FPP]   DB backend      : SQLite
[FPP]   Sessions        : 1,482 total, 23 active
[FPP]   Unique bots     : 340
[FPP]   Unique spawners : 7
[FPP]   Combined uptime : 14d 6h 22m
[FPP]   Backups stored  : 3 (max 10 kept)
[FPP]   Latest backup   : 2026-03-14_12-00-00_pre-migration-v5
[FPP]   Backup storage  : 48.3 MB
```

---

## Merging an Old Database

If you deleted and reinstalled FPP (or moved servers), you may have an old `fpp.db` you want to merge back in.

### Steps

1. Copy your old `fpp.db` file into:
   ```
   plugins/FakePlayerPlugin/data/fpp_old.db
   ```

2. Run:
   ```
   /fpp migrate db merge fpp_old.db
   ```

3. FPP will:
   - Create a backup first
   - Read all rows from the old database
   - Insert them into the current database using `INSERT OR IGNORE` — existing rows are **never overwritten**
   - Report how many rows were merged

### Notes

- You can specify just the filename (relative to `data/`) or a full absolute path
- The merge is safe to run while the server is live — it never deletes or updates existing records
- If a bot UUID already exists in the current database, that row is silently skipped (no duplicate inserts)

---

## Exporting Data to CSV

Export all session history to a CSV file for external analysis in Excel, Google Sheets, etc.

```
/fpp migrate db export
```

The file is saved to:
```
plugins/FakePlayerPlugin/exports/sessions_<timestamp>.csv
```

### CSV columns

```
id, bot_name, bot_uuid, spawned_by, spawned_by_uuid,
world_name, spawn_x, spawn_y, spawn_z,
last_world, last_x, last_y, last_z,
spawned_at_ms, removed_at_ms, remove_reason
```

---

## SQLite → MySQL Migration

When you're ready to move from a local SQLite database to a shared MySQL instance:

### Steps

1. **Set up MySQL** — create a database and user.

2. **Update `config.yml`:**
   ```yaml
   database:
     mysql-enabled: true
     mysql:
       host: "your-mysql-host"
       port: 3306
       database: "fpp"
       username: "fppuser"
       password: "yourpassword"
   ```

3. **Reload FPP** so it connects to MySQL and creates tables:
   ```
   /fpp reload
   ```

4. **Verify** the connection in `/fpp migrate status` — it should show `DB backend: MySQL`.

5. **Migrate the data:**
   ```
   /fpp migrate db tomysql
   ```

6. FPP reads the local `data/fpp.db`, merges all rows into MySQL, and reports the count.

> **Note:** The SQLite file is not deleted automatically. Once you've verified the MySQL data is correct, you can delete `data/fpp.db` manually.

---

## Manual Rollback

If something goes wrong after an update, you can restore from a backup:

1. Stop the server.

2. Navigate to `plugins/FakePlayerPlugin/backups/` and find the backup you want.

3. Copy `config.yml` back to `plugins/FakePlayerPlugin/config.yml`.

4. Copy `fpp.db` back to `plugins/FakePlayerPlugin/data/fpp.db`.

5. Reset the `config-version` in `config.yml` to match the old plugin version (or set it to `0` to force a full re-migration).

6. Start the server.

> **Tip:** Keep at least one manual backup before every major version update:
> ```
> /fpp migrate backup
> ```

---

## Config Version Reference

| Config Version | Plugin Version | What changed |
|---|---|---|
| 1 | 1.0.0 | Initial config |
| 2 | 1.0.1 | Added `update-checker` section |
| 3 | 1.0.3 | Added `luckperms` section |
| 4 | 1.0.5 | `skin-enabled` (bool) → `skin.mode` (string); skin pool restructured |
| 5 | 1.0.7 | Added `body` section; `spawn-body` deprecated |
| 6 | 1.0.9 | Added `chunk-loading` section |
| 7 | 1.0.11 | Added `head-ai` section |
| 8 | 1.0.13 | Added `collision` section |
| 9 | 1.0.15 | Added `swap` and `fake-chat` sections |
| **10** | **1.1.4** | Added `limits`, `bot-name`, `persistence`, `death`, `database` sections; normalised join/leave-delay keys |

> **Current version:** `10`

---

## Troubleshooting

### Config migration ran but my values were reset

The migrator only adds missing keys — it should never overwrite existing values. If your values changed, check:
1. Whether you had the key under an old name that was migrated to a new path (e.g. `max-bots` → `limits.max-bots`)
2. The backup in `backups/` for your original values

### "File not found" when running `db merge`

Make sure the file is in `plugins/FakePlayerPlugin/data/` and you're using just the filename:
```
/fpp migrate db merge fpp_old.db
```

### "Database is offline" error

The DatabaseManager failed to initialise. Check console logs for SQLite/MySQL connection errors and verify your `database:` config block.

---

← [Database](Database.md) · [← Previous](Database.md) · [Next → FAQ](FAQ.md)

