# Ping Spoofing System Implementation

**Version:** 1.6.5  
**Date:** April 16, 2026  
**Feature:** Realistic bot ping/latency simulation

## Overview

Implemented a comprehensive ping spoofing system that simulates realistic network latency for bots. The system provides both visual (tab-list latency bar) and functional (NMS server-side latency field) ping simulation.

## Command Syntax

```
/fpp ping [target] [options]
```

### Options

- `--ping <ms>` - Sets a fixed ping value (0-9999ms)
- `--random` - Assigns a random ping value
- `--count <n>` - Applies ping to N bots (when no target specified)

### Examples

```bash
# Single bot with fixed ping
/fpp ping bot1 --ping 50

# Single bot with random ping
/fpp ping bot1 --random

# Multiple bots with fixed ping
/fpp ping --count 5 --ping 30

# Multiple bots with random ping
/fpp ping --count 10 --random
```

## Implementation Details

### 1. Core Components

#### FakePlayer.java
- Added `private int ping = -1` field
- `getPing()` / `setPing(int)` / `hasCustomPing()` methods
- Ping value capped at 9999ms, -1 = no custom ping

#### PacketHelper.java
- Added `sendTabListLatencyUpdate(Player, FakePlayer)` method
- Modified `sendTabListAdd()` to include latency in entry packet
- Added `buildEntryWithLatency()` for packet construction with ping
- Updated `mapEntryArgs()` to accept latency parameter

#### NmsPlayerSpawner.java
- Added `setPing(Player, int)` method
- Reflects latency into NMS `ServerPlayer.latency` field
- Uses reflection to find latency field by name/type

#### FakePlayerManager.java
- Added `applyPing(FakePlayer, int)` public method
- Updates 20-tick refresh loop to send latency packets for bots with custom ping
- Updates `syncToPlayer()` to include latency for new joiners

#### PingCommand.java
- Implements full command logic with option parsing
- Validates conflicting/duplicate options
- Random ping distribution: 60% (20-100ms), 25% (100-200ms), 15% (200-300ms)
- Tab-completion for bot names and numeric suggestions

### 2. Permission System

**Permission:** `fpp.ping` (default: op)

Added to:
- `Perm.java` constant
- `plugin.yml` permission entry
- `fpp.op` children list

### 3. Language Keys

Added 13 lang keys to `language/en.yml`:
- `ping-usage`, `ping-set`, `ping-set-multiple`
- `ping-bot-not-found`, `ping-no-bots`
- `ping-conflict-options`, `ping-duplicate-option`, `ping-missing-value`
- `ping-out-of-range`, `ping-invalid-number`, `ping-count-invalid`
- `ping-target-count-conflict`, `ping-unknown-option`

### 4. Packet Flow

```
User Command
    ↓
PingCommand.execute()
    ↓
FakePlayerManager.applyPing()
    ├→ FakePlayer.setPing(ping)
    ├→ NmsPlayerSpawner.setPing(player, ping)  [NMS latency field]
    └→ PacketHelper.sendTabListLatencyUpdate() [Tab-list packet]
        ↓
    All online players receive UPDATE_LATENCY packet
        ↓
    Tab-list latency bar updates
```

### 5. Persistence

**Not persisted** - Ping values reset on:
- Bot despawn/respawn
- Server restart
- Plugin reload

This is intentional for realistic behavior (bots would naturally have varying ping on each session).

## Testing Checklist

- [x] Build succeeds without errors
- [x] Command registered in `FakePlayerPlugin.onEnable()`
- [x] Permission added to `plugin.yml`
- [x] Lang keys added to `en.yml`
- [x] Tab-list latency visual updates
- [x] NMS latency field updates
- [x] Random ping distribution realistic
- [x] Option validation (no conflicts)
- [x] Tab-completion works

## Known Limitations

1. **Visual Only for Some Clients** - Some clients may not display the latency bar color changes accurately
2. **No Functional Latency** - Does not actually delay packet processing (cosmetic + NMS field only)
3. **No Persistence** - Ping resets on bot respawn (by design)

## Future Enhancements

- [ ] Optional persistence via database
- [ ] Per-bot default ping in config
- [ ] Ping variance/jitter simulation
- [ ] Geographical ping presets (e.g., `--region EU`)

## Files Modified

1. `FakePlayer.java` - ping field + getters/setters
2. `PacketHelper.java` - latency packet support
3. `NmsPlayerSpawner.java` - NMS latency field reflection
4. `FakePlayerManager.java` - applyPing() + refresh loop
5. `Perm.java` - PING permission constant
6. `FakePlayerPlugin.java` - command registration
7. `plugin.yml` - permission entry
8. `language/en.yml` - 13 lang keys

## Files Created

1. `command/PingCommand.java` - full command implementation

---

**Status:** ✅ Complete and functional  
**Build:** 1.6.5 (obfuscated jar created successfully)

