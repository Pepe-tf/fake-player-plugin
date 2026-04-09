# Database

FPP logs every bot session to a database for auditing, analytics, the `/fpp info` command, and bot persistence across restarts.

> **Version:** 1.5.8 · **Default backend:** SQLite (bundled, zero-config) · **Optional backend:** MySQL

---

## Storage Backends

| Backend | When used | Location |
|---------|----------|----------|
| **SQLite** | Default - always available | `plugins/FakePlayerPlugin/data/fpp.db` |
| **MySQL** | When `database.mysql-enabled: true` and server is reachable | Remote MySQL server |

If MySQL is enabled but unreachable at startup, FPP automatically falls back to SQLite and logs a warning.

---

## SQLite (Default)

No configuration required. The database file is created automatically at:

```
plugins/FakePlayerPlugin/data/fpp.db
```

SQLite runs in **WAL mode** for improved concurrent read performance. It is ideal for single-server setups with no external dependencies.

---

## MySQL Setup

To use MySQL, edit `config.yml`:

```yaml
database:
  enabled: true
  mysql-enabled: true
  mysql:
    host: "localhost"
    port: 3306
    database: "fpp"
    username: "your_user"
    password: "your_password"
    use-ssl: false
    pool-size: 5
    connection-timeout: 30000
```

Then restart the server or run `/fpp reload`.

### Creating the MySQL Database

Run this on your MySQL server before starting FPP:

```sql
CREATE DATABASE fpp CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'fpp_user'@'%' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON fpp.* TO 'fpp_user'@'%';
FLUSH PRIVILEGES;
```

FPP creates all necessary tables automatically on first connection.

---

## NETWORK Mode (Multi-Server / Proxy)

Set `database.mode: "NETWORK"` to share one MySQL database across multiple backend servers.

```yaml
database:
  enabled: true
  mode: "NETWORK"
  server-id: "survival"    # unique per server
  mysql-enabled: true
  mysql:
    host: "mysql.example.com"
    database: "fpp_network"
    username: "fpp_user"
    password: "your_password"
```

In NETWORK mode:
- Every backend server tags its rows with `server_id` so bots stay isolated per-server
- `fpp_active_bots` is the **primary** restore source at startup (file fallback used only if DB is empty)
- Remote bot registry is pre-populated from `fpp_active_bots` at startup and updated via plugin messaging
- Server-specific keys (`database.server-id`, `database.mysql.*`, `debug`) are **never** pushed/pulled by Config Sync

---

## Schema

### `bot_sessions` table

Records every bot session for `/fpp info` queries and analytics.

| Column | Type | Description |
|--------|------|-------------|
| `id` | INTEGER (PK) | Auto-increment row ID |
| `server_id` | TEXT | Server identifier (`database.server-id`) - distinguishes sessions in NETWORK mode |
| `bot_name` | TEXT | Internal bot name (unique per session) |
| `display_name` | TEXT | Display name (may differ for user-tier bots) |
| `spawner_uuid` | TEXT | UUID of the player who spawned the bot |
| `spawner_name` | TEXT | Name of the spawner at spawn time |
| `world` | TEXT | World name where the bot was spawned |
| `spawn_x` | REAL | X coordinate at spawn |
| `spawn_y` | REAL | Y coordinate at spawn |
| `spawn_z` | REAL | Z coordinate at spawn |
| `last_x` | REAL | X coordinate at last save / despawn |
| `last_y` | REAL | Y coordinate at last save / despawn |
| `last_z` | REAL | Z coordinate at last save / despawn |
| `spawned_at` | TEXT | ISO-8601 timestamp of when the bot was spawned |
| `despawned_at` | TEXT | ISO-8601 timestamp of when the bot was removed (`null` if still active) |
| `removal_reason` | TEXT | Why removed: `COMMAND`, `DEATH`, `RESTART`, `SERVER_STOP`, `SWAP` |
| `active` | INTEGER | `1` = currently active; `0` = removed |

### `fpp_active_bots` table

Tracks bots that should be restored on next startup.

| Column | Type | Description |
|--------|------|-------------|
| `server_id` | TEXT | Server that owns this bot |
| `bot_uuid` | TEXT | Bot UUID (PK) |
| `bot_name` | TEXT | Internal name |
| `display_name` | TEXT | Display name |
| `world` | TEXT | Last known world |
| `x`, `y`, `z` | REAL | Last known coordinates |
| `yaw`, `pitch` | REAL | Last known rotation |
| `skin_name` | TEXT | Skin source name for restoration |
| `spawner_uuid` | TEXT | Original spawner UUID |
| `lp_group` | TEXT | LuckPerms group at despawn |

---

## Querying Records - `/fpp info`

### Admin tier (`fpp.info`)

```
/fpp info                        → all live bots + DB summary
/fpp info <botname>              → full session history for the bot
/fpp info bot <name>             → same as above
/fpp info spawner <playername>   → all bots ever spawned by that player
```

Shown fields: spawn time, despawn time, uptime, world, coordinates, spawner, removal reason.

### User tier (`fpp.user.info`)

```
/fpp info                → your own active bots
/fpp info <botname>      → limited view (world, coords, uptime) for a bot you own
```

---

## Persistence & the Database

When `persistence.enabled: true`, FPP:

1. On **shutdown** - marks all active bot sessions as `removal_reason: SERVER_STOP`, records their last position in `fpp_active_bots`, and flushes coordinates to `bot_sessions`.
2. On **startup** - queries `fpp_active_bots` (primary) or `data/active-bots.yml` (fallback) and respawns those bots at their last-known position.

This ensures bots rejoin exactly where they were when the server stopped.

---

## Location Flush

Bot positions are batch-flushed to the database on a configurable interval:

```yaml
database:
  location-flush-interval: 30   # Seconds between position flushes
```

Writes are serialised on a background thread to avoid blocking the main thread.

---

## `/fpp migrate` Database Commands

| Command | Description |
|---------|-------------|
| `/fpp migrate status` | Config version, file-sync health, backup count, DB schema version |
| `/fpp migrate backup` | Create a manual backup of all plugin files |
| `/fpp migrate backups` | List all stored backups |
| `/fpp migrate db export` | Export `bot_sessions` to a JSON file |
| `/fpp migrate db merge [file]` | Merge a JSON export back into the current DB |
| `/fpp migrate db tomysql` | Migrate data from SQLite to MySQL |

---

## Backup

### SQLite

Back up the database by copying the file while the server is stopped, or use `/fpp migrate backup` for a live snapshot:

```
plugins/FakePlayerPlugin/data/fpp.db
```

### MySQL

Use standard `mysqldump`:

```bash
mysqldump -u fpp_user -p fpp > fpp_backup.sql
```

---

## Disabling the Database

Set `database.enabled: false` to disable all database I/O. In this mode:
- No session history is recorded
- Bot persistence falls back to `data/active-bots.yml` only
- `/fpp info` returns in-memory data only (no historical records)
- `/fpp stats` DB totals show `-`
