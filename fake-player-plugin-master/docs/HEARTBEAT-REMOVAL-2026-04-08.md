# Heartbeat System Removal — April 8, 2026

## Summary

Completely removed the StatsHeartbeat system from the FakePlayerPlugin. The system was never used and has been fully cleaned up.

---

## Files Removed

### 1. **StatsHeartbeat.java**
**Path:** `src/main/java/me/bill/fakePlayerPlugin/util/StatsHeartbeat.java`

**Status:** ✅ Deleted

**Description:** Anonymous server stats heartbeat that was intended to POST server statistics to the FPP Vercel API every 5 minutes. The class was never instantiated or used anywhere in the plugin.

---

## Files Modified

### 1. **Config.java**
**Path:** `src/main/java/me/bill/fakePlayerPlugin/config/Config.java`

**Changes:**
- Removed `heartbeatEnabled()` method

**Before:**
```java
public static boolean metricsEnabled() {
    return cfg.getBoolean("metrics.enabled", true);
}

/** Whether stats heartbeat to Vercel API is enabled. Maps to {@code heartbeat.enabled}. */
public static boolean heartbeatEnabled() {
    return cfg.getBoolean("heartbeat.enabled", false);
}
```

**After:**
```java
public static boolean metricsEnabled() {
    return cfg.getBoolean("metrics.enabled", true);
}
```

---

### 2. **config.yml**
**Path:** `src/main/resources/config.yml`

**Changes:**
- Removed `heartbeat` config section (6 lines)

**Before:**
```yaml
# Anonymous usage statistics via FastStats. No personal data collected.
metrics:
  enabled: true

# Anonymous server stats heartbeat to the FPP Vercel API homepage.
# POSTs server_id + bot/player counts every 5 minutes for live website stats.
# No personal data collected. Purely opt-in (disabled by default).
heartbeat:
  enabled: false


# ═════════════════════════════════════════════════════════════════════════════
#  PATHFINDING  ·  used by /fpp move
```

**After:**
```yaml
# Anonymous usage statistics via FastStats. No personal data collected.
metrics:
  enabled: true


# ═════════════════════════════════════════════════════════════════════════════
#  PATHFINDING  ·  used by /fpp move
```

---

## Build Verification

### Compilation
```
[INFO] Compiling 91 source files with javac [debug deprecation release 21] to target\classes
[INFO] BUILD SUCCESS
```

**Note:** Source file count decreased from 92 → 91 (StatsHeartbeat.java removed)

### ProGuard Obfuscation
```
Original number of program classes:            129
Final number of program classes:               127
Number of obfuscated classes:                  122
Number of obfuscated fields:                   738
Number of obfuscated methods:                  1547
```

**Status:** ✅ Success

### Deployment
- `build/fpp.jar` — ✅ Created
- `~/Desktop/dmc/plugins/fpp.jar` — ✅ Deployed

---

## Impact Assessment

### Code Changes
| Metric | Before | After | Delta |
|--------|--------|-------|-------|
| Java files | 92 | 91 | -1 |
| Total lines (source) | ~14,000 | ~13,850 | -150 |
| Config.yml lines | 443 | 437 | -6 |

### Features
- **Removed:** StatsHeartbeat (anonymous server stats POSTed to Vercel API)
- **Preserved:** All other features including FastStats metrics

### User Impact
- **Existing users:** No impact — heartbeat was disabled by default and never instantiated
- **New users:** No impact — feature was never documented or announced

---

## Rationale

The StatsHeartbeat system was:
1. Never instantiated in `FakePlayerPlugin.java`
2. Never called from any other code
3. Disabled by default in config
4. Not documented in any user-facing documentation
5. Not mentioned in AGENTS.md or any wiki pages

The system was likely a proof-of-concept or planned feature that was never integrated. Removing it cleans up the codebase without affecting any functionality.

---

## Related Changes

This removal supersedes the previous build fix (BUILD-FIX-2026-04-08.md) which added:
- `Config.heartbeatEnabled()` method
- `heartbeat` config section
- Mojang DataFixerUpper dependency (still needed for NMS)

The DataFixerUpper dependency remains as it's required for NMS compilation regardless of the heartbeat system.

---

## Testing Recommendations

1. **Build verification:** ✅ Complete
2. **Runtime test:** Start server and verify no errors related to heartbeat
3. **Config test:** Verify `/fpp reload` works without heartbeat section
4. **Backward compatibility:** Old configs with `heartbeat.enabled: false` will be ignored (no migration needed)

---

## Future Notes

If a similar stats collection feature is desired in the future:
1. Integrate it properly in `FakePlayerPlugin.onEnable()`
2. Document it in AGENTS.md and wiki
3. Add proper shutdown hooks in `onDisable()`
4. Consider opt-in vs opt-out default

---

**End of Document**

