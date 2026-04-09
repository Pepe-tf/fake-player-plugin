# Peak Hours

The peak-hours system automatically adjusts how many bots are online based on real-world time windows, mimicking natural player activity patterns - busy evenings, quiet nights, weekend surges, and so on.

---

## Overview

When enabled, the plugin evaluates the current server time every **60 seconds** against a configurable schedule.  Each time window defines a **fraction** of the total bot pool that should be online during that period.

- `fraction: 1.0` → all bots online
- `fraction: 0.5` → half the bots online
- `fraction: 0.0` → no bots online (all sleeping)

Bots that exceed the target fraction are quietly put to **sleep** (removed); sleeping bots are gradually **woken** (respawned) when the fraction rises.  Transitions are staggered so joins and leaves look natural.

> **Requires:** `swap.enabled: true` - peak-hours uses the swap system to sleep and wake bots naturally.

---

## Enabling Peak Hours

```yaml
peak-hours:
  enabled: false
```

Set to `true` in `config.yml`, or control live with:

```
/fpp peaks              ← bare command toggles on/off
/fpp peaks on
/fpp peaks off
/fpp peaks status       ← current window, fraction, pool counts
/fpp peaks next         ← time until the next window change
/fpp peaks force        ← trigger an immediate evaluation now
/fpp peaks list         ← list all sleeping bots and their locations
/fpp peaks wake [name]  ← wake a specific bot (or all sleeping bots)
/fpp peaks sleep <name> ← manually put an active bot to sleep
```

The `on`/`off` commands write directly to `config.yml` and survive restarts.

**Required permission:** `fpp.peaks`

---

## Configuration

```yaml
peak-hours:
  enabled: false
  timezone: "UTC"            # Any java.time.ZoneId, e.g. "America/New_York"
  stagger-seconds: 30        # Spread bot joins/leaves over this many seconds

  # Absolute minimum bots that must stay online regardless of fraction
  min-online: 0

  # Broadcast window transitions to online admins with fpp.peaks
  notify-transitions: false

  # ── Daily schedule ──────────────────────────────────────────────────────
  schedule:
    - start: "06:00"
      end:   "09:00"
      fraction: 0.30       # Early morning - server waking up

    - start: "09:00"
      end:   "18:00"
      fraction: 0.75       # Daytime - moderate activity

    - start: "18:00"
      end:   "22:00"
      fraction: 1.00       # Peak evening - all bots online

    - start: "22:00"
      end:   "06:00"
      fraction: 0.05       # Night - almost all bots sleeping

  # ── Day-of-week overrides ───────────────────────────────────────────────
  day-overrides:
    SATURDAY:
      - start: "10:00"
        end:   "23:00"
        fraction: 1.00     # Weekend peak - full server all day
      - start: "23:00"
        end:   "10:00"
        fraction: 0.10     # Saturday night wind-down
    SUNDAY:
      - start: "10:00"
        end:   "21:00"
        fraction: 0.90
      - start: "21:00"
        end:   "10:00"
        fraction: 0.05
```

---

## Settings Reference

### Timezone

```yaml
timezone: "UTC"
```

Any valid [Java `ZoneId`](https://docs.oracle.com/en/java/docs/api/java.base/java/time/ZoneId.html) string.  Examples:

| Value | Region |
|-------|--------|
| `"UTC"` | Universal Coordinated Time (default) |
| `"America/New_York"` | US Eastern |
| `"Europe/London"` | UK |
| `"Asia/Tokyo"` | Japan |
| `"Australia/Sydney"` | Australia Eastern |

---

### Stagger Seconds

```yaml
stagger-seconds: 30
```

Distributes bot joins and leaves evenly across this many seconds so they don't all happen at once.  With 10 bots and `stagger-seconds: 60`, one bot wakes roughly every 6 seconds.

---

### Min Online

```yaml
min-online: 0
```

Sets a hard floor - at least this many AFK bots will always remain online, regardless of the computed fraction.  Useful for ensuring the server never appears completely empty during off-peak hours.

---

### Notify Transitions

```yaml
notify-transitions: false
```

When `true`, online players with the `fpp.peaks` permission receive a chat notification each time the server enters a new time window.

---

### Schedule

Each entry in `schedule` (and each day override) is a map with three keys:

| Key | Type | Description |
|-----|------|-------------|
| `start` | `HH:mm` | Window start time (24-hour) |
| `end`   | `HH:mm` | Window end time (24-hour) |
| `fraction` | `0.0 - 1.0` | Fraction of the total bot pool that should be online |

**Midnight-crossing windows** (e.g. `22:00 → 06:00`) are handled automatically - no special configuration is needed.

Windows are checked **top-to-bottom**; the first matching window wins.  If no window matches, the fraction defaults to `1.0` (all bots online).

---

### Day-of-Week Overrides

```yaml
day-overrides:
  SATURDAY:
    - start: "10:00"
      end:   "23:00"
      fraction: 1.00
```

Override the default daily schedule for specific days.  Keys must be uppercase Java [`DayOfWeek`](https://docs.oracle.com/en/java/docs/api/java.base/java/time/DayOfWeek.html) names:

`MONDAY` `TUESDAY` `WEDNESDAY` `THURSDAY` `FRIDAY` `SATURDAY` `SUNDAY`

When a day-override is present it completely replaces the default schedule for that day.  Remove or comment out an entry to fall back to the default schedule.

---

## How Sleeping Works

When peak-hours needs to reduce the online count:

1. A random selection of AFK bots are chosen as **sleep candidates**.
2. Each bot's pending swap session is cancelled (so `BotSwapAI` does not independently rejoin it).
3. The bot's current location is saved.
4. The bot is quietly despawned (no leave message is broadcast - it simply vanishes from the tab list).

When the fraction rises:

1. Bots are woken from the queue **FIFO** (oldest first).
2. Each bot respawns at its saved location.
3. If `swap.same-name-on-rejoin: true` and the original name is available, it rejoins with the same name.

---

## Interaction with BotSwapAI

The total bot pool counted by peak-hours **includes** bots currently offline via the normal swap rotation (`BotSwapAI.getSwappedOutCount()`).  This prevents routine swap absences from appearing as a sudden pool shrinkage that peak-hours would try to compensate for.

> If swap is disabled while peak-hours is running, the tick pauses and **all sleeping bots are immediately woken** to prevent data loss.

---

## Diagnostics

```
/fpp peaks status
```

Displays:

| Field | Meaning |
|-------|---------|
| `window` | Active time window (e.g. `18:00-22:00`) |
| `fraction` | Target fraction as a percentage |
| `target` | Target number of bots online |
| `online` | Currently online AFK bots |
| `swapping` | Bots temporarily offline via BotSwapAI |
| `sleeping` | Bots in the peak-hours sleep queue |
| `total` | Total pool (online + swapping + sleeping) |
| `tz` | Configured timezone |

```
/fpp peaks next
```

Shows the time remaining until the next window change and its fraction.

---

## Example - Realistic 24/7 Server

```yaml
swap:
  enabled: true
  session:
    min: 120
    max: 600
  absence:
    min: 15
    max: 60

peak-hours:
  enabled: true
  timezone: "America/New_York"
  stagger-seconds: 45
  min-online: 2
  notify-transitions: false

  schedule:
    - start: "07:00"
      end:   "12:00"
      fraction: 0.40   # Morning ramp-up
    - start: "12:00"
      end:   "17:00"
      fraction: 0.65   # Afternoon
    - start: "17:00"
      end:   "22:00"
      fraction: 1.00   # Prime time
    - start: "22:00"
      end:   "01:00"
      fraction: 0.50   # Late night
    - start: "01:00"
      end:   "07:00"
      fraction: 0.10   # Overnight minimum

  day-overrides:
    SATURDAY:
      - start: "10:00"
        end:   "02:00"
        fraction: 1.00
      - start: "02:00"
        end:   "10:00"
        fraction: 0.10
    SUNDAY:
      - start: "10:00"
        end:   "23:00"
        fraction: 0.85
      - start: "23:00"
        end:   "10:00"
        fraction: 0.10
```

---

## Notes

- Peak-hours **does not override** the global `limits.max-bots` cap.
- Only **AFK bots** (`BotType.AFK`) are managed by peak-hours.  PVP bots are never put to sleep.
- Sleeping bots are **not persisted** separately - on shutdown, `PeakHoursManager` wakes all sleeping bots before `BotPersistence.save()` runs, so they are included in the normal persistence file.
- `/fpp reload` wakes all sleeping bots, resets state, then immediately re-evaluates the new config.

