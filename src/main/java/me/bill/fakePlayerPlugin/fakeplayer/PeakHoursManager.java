package me.bill.fakePlayerPlugin.fakeplayer;

import me.bill.fakePlayerPlugin.FakePlayerPlugin;
import me.bill.fakePlayerPlugin.config.Config;
import me.bill.fakePlayerPlugin.database.DatabaseManager;
import me.bill.fakePlayerPlugin.lang.Lang;
import me.bill.fakePlayerPlugin.permission.Perm;
import me.bill.fakePlayerPlugin.util.FppLogger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.time.*;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages the peak-hours bot pool scheduling system.
 *
 * <h3>How it works</h3>
 * <p>A 60-second repeating tick reads the current server clock, resolves which
 * configured time window is active, computes a target fraction of the total bot
 * pool that should be online, and then gradually <em>sleeps</em> (quietly
 * removes) or <em>wakes</em> (respawns) bots to match that fraction.
 *
 * <p>Transitions are staggered over {@code peak-hours.stagger-seconds} so bots
 * join and leave naturally instead of all at once.
 *
 * <h3>Dynamic swap integration</h3>
 * <ul>
 *   <li>The total pool always includes bots temporarily offline via
 *       {@link BotSwapAI} session rotation, so short swap absences don't
 *       confuse the peak-hours math.</li>
 *   <li>{@code swap.enabled} must be {@code true} before peak-hours can be
 *       activated.  If swap is disabled while peaks is running, the tick
 *       silently pauses and wakes any sleeping bots so none are permanently lost.</li>
 * </ul>
 *
 * <h3>Config keys (peak-hours.*)</h3>
 * <ul>
 *   <li>{@code enabled}            — master toggle (requires swap.enabled: true)</li>
 *   <li>{@code timezone}           — java.time.ZoneId string</li>
 *   <li>{@code stagger-seconds}    — spread joins/leaves across this many seconds</li>
 *   <li>{@code schedule}           — list of {@code {start, end, fraction}} maps</li>
 *   <li>{@code day-overrides}      — per-DayOfWeek schedule overrides</li>
 *   <li>{@code min-online}         — absolute floor: never fewer than this many bots online</li>
 *   <li>{@code notify-transitions} — broadcast window changes to admins with fpp.peaks</li>
 * </ul>
 */
public final class PeakHoursManager {

    private final FakePlayerPlugin  plugin;
    private final FakePlayerManager manager;

    /** Optional database manager for crash-safe sleeping-bot persistence. May be {@code null}. */
    private DatabaseManager db = null;

    /** Bots put to sleep by peak-hours — FIFO so the oldest sleep first. */
    private final Deque<SleepingBot> sleepingBots = new ArrayDeque<>();

    /** Bukkit task ID of the periodic tick, or {@code -1} when not running. */
    private int tickTaskId = -1;

    /**
     * Prevents stacked adjustments: {@code true} while staggered actions are pending.
     * Cleared by the last staggered action's completion callback.
     */
    private volatile boolean adjusting = false;

    /**
     * Last fraction applied during a tick. Used to detect window transitions.
     * {@code -1.0} = no tick has run yet (forces a "transition" on first tick).
     */
    private double lastFraction = -1.0;

    // ── Constructor ───────────────────────────────────────────────────────────

    public PeakHoursManager(FakePlayerPlugin plugin, FakePlayerManager manager) {
        this.plugin  = plugin;
        this.manager = manager;
    }

    /**
     * Injects the database manager so sleeping-bot state is persisted crash-safely.
     * Call immediately after construction, before {@link #start()}.
     */
    public void setDatabaseManager(DatabaseManager db) { this.db = db; }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /**
     * Starts the 60-second periodic tick.
     * Safe to call multiple times — does nothing if already started.
     */
    public void start() {
        if (tickTaskId != -1) return;
        tickTaskId = Bukkit.getScheduler()
                .runTaskTimer(plugin, this::tick, 40L, 1200L)
                .getTaskId();
        Config.debugChat("[PeakHours] Scheduler started (60-second tick).");
    }

    /**
     * Stops the periodic tick and immediately wakes all sleeping bots so
     * persistence can capture the full pool before the plugin shuts down.
     */
    public void shutdown() {
        stopTick();
        wakeAll();
        Config.debugChat("[PeakHours] Shutdown complete — all sleeping bots woken.");
    }

    /**
     * Hot-reload: cancels the current scheduler, wakes all sleeping bots
     * (config may have changed), resets transition state, then restarts
     * if peak-hours is still enabled in the new config.
     * Called automatically from {@code /fpp reload}.
     */
    public void reload() {
        stopTick();
        adjusting    = false;
        lastFraction = -1.0;
        if (!sleepingBots.isEmpty()) {
            int count = sleepingBots.size();
            wakeAll();
            Config.debugChat("[PeakHours] Reload: woke " + count + " sleeping bot(s) for re-evaluation.");
        }
        if (Config.peakHoursEnabled() && Config.swapEnabled()) {
            start();
            // Run an immediate tick so the new config takes effect within 1 s
            Bukkit.getScheduler().runTaskLater(plugin, this::tick, 20L);
        }
    }

    // ── Core tick ─────────────────────────────────────────────────────────────

    private void tick() {
        if (!Config.peakHoursEnabled()) return;

        // Guard: swap must be active for peaks to manage the bot pool safely
        if (!Config.swapEnabled()) {
            if (!sleepingBots.isEmpty()) {
                FppLogger.warn("[PeakHours] Swap disabled while " + sleepingBots.size()
                        + " bot(s) sleeping — waking all to prevent data loss.");
                wakeAll();
            }
            Config.debugChat("[PeakHours] Swap is off — tick paused.");
            return;
        }

        if (adjusting) {
            Config.debugChat("[PeakHours] Tick skipped — previous stagger still in-flight.");
            return;
        }

        double fraction    = computeTargetFraction();
        int    onlineAFK   = countOnlineAFKBots();
        int    sleeping    = sleepingBots.size();

        // Include bots BotSwapAI temporarily removed (they will rejoin on their own).
        // Without this, routine swap absences look like pool shrinkage.
        BotSwapAI swapAI     = manager.getBotSwapAI();
        int       swappedOut = (swapAI != null) ? swapAI.getSwappedOutCount() : 0;
        int       total      = onlineAFK + sleeping + swappedOut;

        if (total == 0) return;

        // Apply min-online floor so the server never feels completely empty
        int minOnline = Math.max(0, Config.peakHoursMinOnline());
        int target    = (int) Math.round(fraction * total);
        target = Math.max(minOnline, Math.min(total, target));

        // Detect and optionally announce window transitions
        checkAndAnnounceTransition(fraction);

        // Compare target against "effectively online" (currently online + returning-soon bots)
        int effectivelyOnline = onlineAFK + swappedOut;
        int toSleep = Math.max(0, effectivelyOnline - target);
        int toWake  = Math.max(0, target - effectivelyOnline);
        toSleep = Math.min(toSleep, onlineAFK); // can only sleep actually-online bots
        toWake  = Math.min(toWake,  sleeping);  // can only wake bots we put to sleep

        if (toSleep == 0 && toWake == 0) {
            Config.debugChat("[PeakHours] OK — fraction=" + String.format("%.0f%%", fraction * 100)
                    + " online=" + onlineAFK + " swapping=" + swappedOut
                    + " sleeping=" + sleeping + " total=" + total + " target=" + target + ".");
            return;
        }

        int stagger = Math.max(1, Config.peakHoursStaggerSeconds());
        Config.debugChat("[PeakHours] Adjusting — fraction=" + String.format("%.0f%%", fraction * 100)
                + " online=" + onlineAFK + " swapping=" + swappedOut
                + " sleeping=" + sleeping + " total=" + total
                + " target=" + target + " toSleep=" + toSleep + " toWake=" + toWake);

        adjusting = true;

        if (toSleep > 0) {
            List<FakePlayer> candidates = pickSleepCandidates(toSleep);
            List<Runnable>   actions    = new ArrayList<>(candidates.size());
            for (FakePlayer fp : candidates) {
                actions.add(() -> putToSleep(fp));
            }
            scheduleStaggered(actions, stagger, () -> adjusting = false);
        } else {
            List<Runnable> actions = new ArrayList<>(toWake);
            for (int i = 0; i < toWake; i++) {
                actions.add(this::wakeBotFromQueue);
            }
            scheduleStaggered(actions, stagger, () -> adjusting = false);
        }
    }

    // ── Sleep / wake ──────────────────────────────────────────────────────────

    /** Internal: put a single bot to sleep. */
    private void putToSleep(FakePlayer fp) {
        if (fp == null) return;
        if (manager.getByUuid(fp.getUuid()) == null) {
            Config.debugChat("[PeakHours] putToSleep: '" + fp.getName() + "' already gone.");
            return;
        }
        // Cancel swap session so BotSwapAI does not independently rejoin this bot
        BotSwapAI swapAI = manager.getBotSwapAI();
        if (swapAI != null) swapAI.cancel(fp.getUuid());

        Location loc  = fp.getLiveLocation();
        String   name = fp.getName();
        sleepingBots.addLast(new SleepingBot(name, loc));
        manager.delete(name);
        persistSleepingBots();  // crash-safe: write queue updated after every sleep
        Config.debugChat("[PeakHours] '" + name + "' → sleep (pool: " + sleepingBots.size() + ").");
    }

    /**
     * Manually puts an active AFK bot to sleep by name.
     * @return {@code true} if the bot was found and queued; {@code false} otherwise.
     */
    public boolean putBotToSleepByName(String name) {
        FakePlayer fp = manager.getByName(name);
        if (fp == null || fp.getBotType() == BotType.PVP) return false;
        putToSleep(fp);
        return true;
    }

    /** Internal: pop the oldest sleeping bot and respawn it. */
    private void wakeBotFromQueue() {
        SleepingBot sb = sleepingBots.pollFirst();
        if (sb != null) {
            wakeEntry(sb);
            persistSleepingBots();  // update DB to reflect new queue state
        }
    }

    /**
     * Wakes a sleeping bot by name.
     * @return {@code true} if found and woken; {@code false} if not in the sleeping pool.
     */
    public boolean wakeBotByName(String name) {
        SleepingBot target = null;
        for (SleepingBot sb : sleepingBots) {
            if (sb.name().equalsIgnoreCase(name)) { target = sb; break; }
        }
        if (target == null) return false;
        sleepingBots.remove(target);
        wakeEntry(target);
        persistSleepingBots();  // update DB to reflect new queue state
        return true;
    }

    /** Shared respawn logic for a {@link SleepingBot} entry. */
    private void wakeEntry(SleepingBot sb) {
        if (sb.loc() == null || sb.loc().getWorld() == null) {
            FppLogger.warn("[PeakHours] Sleeping bot '" + sb.name()
                    + "' had no valid location — discarded.");
            return;
        }
        String customName = null;
        if (Config.swapSameNameOnRejoin() && !manager.isNameUsed(sb.name())) {
            customName = sb.name();
        }
        int result = manager.spawn(sb.loc(), 1, null, customName, true);
        Config.debugChat("[PeakHours] '" + sb.name()
                + "' → wake (result=" + result + ", pool: " + sleepingBots.size() + ").");
    }

    /**
     * Immediately wakes every sleeping bot.
     * Used on shutdown, reload, and when disabling peak-hours.
     */
    public void wakeAll() {
        int count = sleepingBots.size();
        // Drain the deque manually so we don't call persistSleepingBots() for every
        // individual removal (would be O(n) DB writes). One clearSleepingBots() at the end suffices.
        while (!sleepingBots.isEmpty()) {
            SleepingBot sb = sleepingBots.pollFirst();
            if (sb != null) wakeEntry(sb);
        }
        clearPersistedSleepingBots();  // single DB clear after all bots are woken
        if (count > 0) Config.debugChat("[PeakHours] wakeAll() — woke " + count + " bot(s).");
    }

    // ── Database persistence (crash-safe sleeping-bot state) ──────────────────

    /**
     * Snapshots the current {@code sleepingBots} deque and enqueues a replace-all
     * write to {@code fpp_sleeping_bots}.  Call after every queue mutation
     * (sleep or individual wake) so the DB always reflects in-memory state.
     */
    private void persistSleepingBots() {
        if (db == null || !me.bill.fakePlayerPlugin.config.Config.databaseEnabled()) return;
        List<me.bill.fakePlayerPlugin.database.DatabaseManager.SleepingBotRow> rows =
                new ArrayList<>(sleepingBots.size());
        int i = 0;
        for (SleepingBot sb : sleepingBots) {
            Location loc = sb.loc();
            if (loc == null || loc.getWorld() == null) { i++; continue; }
            rows.add(new me.bill.fakePlayerPlugin.database.DatabaseManager.SleepingBotRow(
                    i++, sb.name(),
                    loc.getWorld().getName(),
                    loc.getX(), loc.getY(), loc.getZ(),
                    loc.getYaw(), loc.getPitch()
            ));
        }
        db.saveSleepingBots(rows);
    }

    /** Enqueues a delete of all sleeping-bot rows for this server. */
    private void clearPersistedSleepingBots() {
        if (db == null || !me.bill.fakePlayerPlugin.config.Config.databaseEnabled()) return;
        db.clearSleepingBots();
    }

    /**
     * Crash-recovery restore: reads {@code fpp_sleeping_bots} for this server,
     * repopulates {@link #sleepingBots}, then clears the DB rows.
     *
     * <p>Called once from {@code FakePlayerPlugin.onEnable()} after the
     * {@code DatabaseManager} is available and before the peak-hours tick starts.
     * This ensures the sleeping-bot pool survives unclean shutdowns / crashes.
     *
     * @param database the initialised {@link DatabaseManager}; ignored if {@code null}.
     */
    public void restoreSleepingBotsFromDatabase(
            me.bill.fakePlayerPlugin.database.DatabaseManager database) {
        if (database == null) return;
        List<me.bill.fakePlayerPlugin.database.DatabaseManager.SleepingBotRow> rows =
                database.loadSleepingBots();
        if (rows.isEmpty()) return;

        int restored = 0;
        for (me.bill.fakePlayerPlugin.database.DatabaseManager.SleepingBotRow row : rows) {
            World world = Bukkit.getWorld(row.world());
            if (world == null) {
                FppLogger.warn("[PeakHours] Crash-recovery: cannot restore sleeping bot '"
                        + row.botName() + "' — world '" + row.world() + "' not loaded; discarded.");
                continue;
            }
            Location loc = new Location(world, row.x(), row.y(), row.z(), row.yaw(), row.pitch());
            sleepingBots.addLast(new SleepingBot(row.botName(), loc));
            restored++;
        }
        // Clear the table — bots are now in memory.  The queue will be re-persisted if modified.
        database.clearSleepingBots();
        if (restored > 0) {
            FppLogger.info("[PeakHours] Crash-recovery: restored " + restored
                    + " sleeping bot(s) from database.");
        }
    }

    // ── Force check ───────────────────────────────────────────────────────────

    /**
     * Triggers an immediate peak-hours evaluation, bypassing the 60-second interval.
     * Also clears any stalled {@code adjusting} lock so a stuck previous cycle
     * cannot prevent the force-check from running.
     */
    public void forceCheck() {
        if (adjusting) {
            Config.debugChat("[PeakHours] forceCheck: clearing stalled adjusting flag.");
            adjusting = false;
        }
        tick();
    }

    // ── Stagger helper ────────────────────────────────────────────────────────

    /**
     * Distributes {@code actions} evenly across {@code staggerSeconds} with
     * small random jitter on each delay so joins/leaves look organic.
     * Calls {@code onDone} after the last action fires.
     */
    private void scheduleStaggered(List<Runnable> actions, int staggerSeconds, Runnable onDone) {
        if (actions.isEmpty()) {
            adjusting = false;
            if (onDone != null) onDone.run();
            return;
        }
        int  n          = actions.size();
        long totalTicks = (long) staggerSeconds * 20L;

        for (int i = 0; i < n; i++) {
            final int     idx    = i;
            final Runnable action = actions.get(i);
            long base   = n == 1 ? 20L : (long)(((double) idx / (n - 1)) * totalTicks) + 20L;
            long jitter = ThreadLocalRandom.current().nextInt(9) - 4L; // ±4 ticks
            long delay  = Math.max(20L, base + jitter);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                action.run();
                if (idx == n - 1 && onDone != null) onDone.run();
            }, delay);
        }
    }

    // ── Transition tracking ───────────────────────────────────────────────────

    private void checkAndAnnounceTransition(double newFraction) {
        if (lastFraction >= 0.0 && Math.abs(newFraction - lastFraction) > 0.001) {
            String label = String.format("%.0f%%", newFraction * 100);
            String window = getCurrentWindowLabel();
            Config.debugChat("[PeakHours] Transition → " + window + " (" + label + ").");
            if (Config.peakHoursNotifyTransitions()) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (Perm.has(p, Perm.PEAKS)) {
                        p.sendMessage(Lang.get("peaks-transition",
                                "window",   window,
                                "fraction", label));
                    }
                }
            }
        }
        lastFraction = newFraction;
    }

    // ── Time window resolution ────────────────────────────────────────────────

    /**
     * Computes the target fraction for the current time.
     * Returns {@code 1.0} when no window matches (all bots online).
     */
    public double computeTargetFraction() {
        ZoneId zone = safeZone();
        LocalDateTime now  = LocalDateTime.now(zone);
        LocalTime     time = now.toLocalTime();
        DayOfWeek     dow  = now.getDayOfWeek();

        for (Map<?, ?> window : resolveSchedule(dow)) {
            String startStr = objectToString(window.get("start"));
            String endStr   = objectToString(window.get("end"));
            double frac;
            try { frac = Double.parseDouble(objectToString(window.get("fraction"))); }
            catch (NumberFormatException e) { continue; }
            try {
                LocalTime start = LocalTime.parse(startStr);
                LocalTime end   = LocalTime.parse(endStr);
                if (timeInWindow(time, start, end)) return Math.max(0.0, Math.min(1.0, frac));
            } catch (DateTimeParseException ignored) {}
        }
        return 1.0;
    }

    /**
     * Returns the fraction of the next scheduled window, or {@code -1.0} if none.
     */
    public double getNextWindowFraction() {
        ZoneId zone = safeZone();
        LocalDateTime now  = LocalDateTime.now(zone);
        LocalTime     time = now.toLocalTime();
        DayOfWeek     dow  = now.getDayOfWeek();

        List<Map<?, ?>> schedule = resolveSchedule(dow);
        long shortest = Long.MAX_VALUE;
        double nextFrac = -1.0;
        for (Map<?, ?> window : schedule) {
            try {
                LocalTime wStart = LocalTime.parse(objectToString(window.get("start")));
                long secs = time.until(wStart, ChronoUnit.SECONDS);
                if (secs <= 0) secs += 86400L;
                if (secs > 0 && secs < shortest) {
                    shortest = secs;
                    nextFrac = Double.parseDouble(objectToString(window.get("fraction")));
                    nextFrac = Math.max(0.0, Math.min(1.0, nextFrac));
                }
            } catch (Exception ignored) {}
        }
        return nextFrac;
    }

    private List<Map<?, ?>> resolveSchedule(DayOfWeek dow) {
        ConfigurationSection overrides = Config.peakHoursDayOverrides();
        if (overrides != null) {
            String dayKey = dow.name();
            if (overrides.contains(dayKey)) {
                List<Map<?, ?>> daySchedule = overrides.getMapList(dayKey);
                if (!daySchedule.isEmpty()) return daySchedule;
            }
        }
        return Config.peakHoursSchedule();
    }

    private static boolean timeInWindow(LocalTime time, LocalTime start, LocalTime end) {
        if (!end.isBefore(start)) {
            return !time.isBefore(start) && time.isBefore(end);
        } else {
            return !time.isBefore(start) || time.isBefore(end);
        }
    }

    // ── Bot selection ─────────────────────────────────────────────────────────

    private int countOnlineAFKBots() {
        int c = 0;
        for (FakePlayer fp : manager.getActivePlayers()) {
            if (fp.getBotType() == BotType.AFK) c++;
        }
        return c;
    }

    private List<FakePlayer> pickSleepCandidates(int limit) {
        List<FakePlayer> list = new ArrayList<>();
        for (FakePlayer fp : manager.getActivePlayers()) {
            if (fp.getBotType() == BotType.AFK) list.add(fp);
        }
        Collections.shuffle(list, ThreadLocalRandom.current());
        return list.subList(0, Math.min(limit, list.size()));
    }

    // ── Diagnostics ───────────────────────────────────────────────────────────

    public int     getSleepingCount() { return sleepingBots.size(); }
    public boolean isRunning()        { return tickTaskId != -1; }

    /** Returns online AFK + sleeping + BotSwapAI swapped-out (matches tick() denominator). */
    public int getTotalPool() {
        BotSwapAI s = manager.getBotSwapAI();
        return countOnlineAFKBots() + sleepingBots.size() + (s != null ? s.getSwappedOutCount() : 0);
    }

    public List<String> getSleepingNames() {
        List<String> names = new ArrayList<>(sleepingBots.size());
        for (SleepingBot sb : sleepingBots) names.add(sb.name());
        return Collections.unmodifiableList(names);
    }

    /** Returns an immutable snapshot of all sleeping entries (oldest first). */
    public List<SleepingBot> getSleepingEntries() { return List.copyOf(sleepingBots); }

    public boolean isSleeping(String name) {
        for (SleepingBot sb : sleepingBots) {
            if (sb.name().equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    public String getCurrentWindowLabel() {
        ZoneId zone = safeZone();
        LocalDateTime now  = LocalDateTime.now(zone);
        LocalTime     time = now.toLocalTime();
        DayOfWeek     dow  = now.getDayOfWeek();
        for (Map<?, ?> window : resolveSchedule(dow)) {
            String s = objectToString(window.get("start"));
            String e = objectToString(window.get("end"));
            try {
                if (timeInWindow(time, LocalTime.parse(s), LocalTime.parse(e))) return s + "–" + e;
            } catch (DateTimeParseException ignored) {}
        }
        return "none";
    }

    public long getSecondsToNextWindow() {
        ZoneId zone = safeZone();
        LocalDateTime now  = LocalDateTime.now(zone);
        LocalTime     time = now.toLocalTime();
        DayOfWeek     dow  = now.getDayOfWeek();
        List<Map<?, ?>> schedule = resolveSchedule(dow);
        if (schedule.isEmpty()) return -1;
        long shortest = Long.MAX_VALUE;
        for (Map<?, ?> window : schedule) {
            try {
                LocalTime wStart = LocalTime.parse(objectToString(window.get("start")));
                long secs = time.until(wStart, ChronoUnit.SECONDS);
                if (secs <= 0) secs += 86400L;
                if (secs > 0 && secs < shortest) shortest = secs;
            } catch (DateTimeParseException ignored) {}
        }
        return shortest == Long.MAX_VALUE ? -1 : shortest;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void stopTick() {
        if (tickTaskId != -1) {
            Bukkit.getScheduler().cancelTask(tickTaskId);
            tickTaskId = -1;
        }
    }

    private ZoneId safeZone() {
        try { return ZoneId.of(Config.peakHoursTimezone()); }
        catch (Exception e) { return ZoneId.of("UTC"); }
    }

    private static String objectToString(Object o) { return o == null ? "" : String.valueOf(o); }

    // ── Data record ───────────────────────────────────────────────────────────

    /** Immutable snapshot of a bot sleeping in the peak-hours pool. */
    public record SleepingBot(String name, Location loc) {}
}

