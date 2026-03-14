package me.bill.fakePlayerPlugin.util;

import me.bill.fakePlayerPlugin.config.Config;

import java.util.logging.Logger;

/**
 * Enhanced coloured console logger for ꜰᴀᴋᴇ ᴘʟᴀʏᴇʀ ᴘʟᴜɢɪɴ.
 *
 * <p>Uses ANSI escape codes supported by Paper's JLine-backed console.
 * Falls back gracefully on terminals that strip ANSI (the text is still readable).
 *
 * <h3>Log levels</h3>
 * <ul>
 *   <li>{@link #info}    — plain white  — general information</li>
 *   <li>{@link #success} — bright green — positive confirmation</li>
 *   <li>{@link #warn}    — gold/amber   — non-fatal warnings</li>
 *   <li>{@link #error}   — bright red   — errors that need attention</li>
 *   <li>{@link #debug}   — yellow+grey  — verbose, only when {@code debug: true}</li>
 *   <li>{@link #highlight} — cyan bold  — important state changes (enable/disable)</li>
 * </ul>
 *
 * <h3>Formatting helpers</h3>
 * <ul>
 *   <li>{@link #section}  — labelled separator line</li>
 *   <li>{@link #rule}     — plain separator line</li>
 *   <li>{@link #kv}       — "  key ....... value" row</li>
 *   <li>{@link #statusRow} — "  [✔/✘] label : value" row</li>
 *   <li>{@link #blank}    — empty line</li>
 * </ul>
 */
public final class FppLogger {

    // ── ANSI codes ────────────────────────────────────────────────────────────
    private static final String RESET    = "\u001B[0m";
    private static final String BOLD     = "\u001B[1m";

    // Main accent: #0079FF
    private static final String BLUE     = "\u001B[38;2;0;121;255m";
    // Bright white for normal info
    private static final String WHITE    = "\u001B[97m";
    // Yellow for debug
    private static final String YELLOW   = "\u001B[93m";
    // Green for success / OK
    private static final String GREEN    = "\u001B[92m";
    // Gold/amber for warn
    private static final String GOLD     = "\u001B[33m";
    // Red for error / FAIL
    private static final String RED      = "\u001B[91m";
    // Grey for decoration / muted text
    private static final String GRAY     = "\u001B[90m";
    // Cyan for highlight
    private static final String CYAN     = "\u001B[96m";
    // Dark grey for rule lines
    private static final String DARK     = "\u001B[38;5;240m";

    /** FPP tag shown at the start of every line. */
    private static final String TAG      = BOLD + BLUE + "[ꜰᴘᴘ]" + RESET;

    /** Width of the separator rule (printable characters). */
    private static final int RULE_WIDTH  = 50;
    /** Width of the key column in kv() rows. */
    private static final int KEY_WIDTH   = 18;

    private static Logger logger;

    private FppLogger() {}

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    public static void init(Logger javaLogger) {
        logger = javaLogger;
    }

    // ── Core log methods ──────────────────────────────────────────────────────

    /** General info — white. */
    public static void info(String message) {
        logger.info(TAG + " " + WHITE + message + RESET);
    }

    /** Positive confirmation — bright green. */
    public static void success(String message) {
        logger.info(TAG + " " + GREEN + message + RESET);
    }

    /** Warning — gold/amber. Appears in console as a WARNING-level entry. */
    public static void warn(String message) {
        logger.warning(TAG + " " + GOLD + message + RESET);
    }

    /** Error — bright red. Appears in console as a SEVERE-level entry. */
    public static void error(String message) {
        logger.severe(TAG + " " + RED + message + RESET);
    }

    /**
     * Debug — yellow, only emitted when {@code debug: true} in config.
     * Prefixed with a grey [DEBUG] badge.
     */
    public static void debug(String message) {
        if (Config.isDebug()) {
            logger.info(TAG + " " + GRAY + "[" + YELLOW + "DEBUG" + GRAY + "] " + YELLOW + message + RESET);
        }
    }

    /**
     * Highlight — bold cyan, for important state transitions such as
     * plugin enable/disable. Use sparingly so it stands out.
     */
    public static void highlight(String message) {
        logger.info(TAG + " " + BOLD + CYAN + message + RESET);
    }

    // ── Formatting helpers ────────────────────────────────────────────────────

    /**
     * Prints a horizontal rule (separator line).
     * Example: {@code ── ── ── ── ── ── ── ── ── ── ── ── ── ── ── ─}
     */
    public static void rule() {
        logger.info(TAG + " " + DARK + "─".repeat(RULE_WIDTH) + RESET);
    }

    /**
     * Prints a bold rule — used for the very top and bottom of banners.
     */
    public static void boldRule() {
        logger.info(TAG + " " + GRAY + BOLD + "═".repeat(RULE_WIDTH) + RESET);
    }

    /**
     * Prints a labelled section header inside a banner.
     * Example: {@code ── Config ───────────────────────────────────────}
     *
     * @param label the section label (short, plain text)
     */
    public static void section(String label) {
        String dashes = "─".repeat(Math.max(0, RULE_WIDTH - label.length() - 4));
        logger.info(TAG + " " + DARK + "── " + RESET + BOLD + WHITE + label
                + " " + DARK + dashes + RESET);
    }

    /**
     * Prints a key→value row with dot-padding between key and value.
     * <pre>
     *   Language ............ en
     *   Debug ............... false
     * </pre>
     *
     * @param key   the label (left side)
     * @param value the value (right side, rendered in accent blue)
     */
    public static void kv(String key, Object value) {
        int dots = Math.max(1, KEY_WIDTH - key.length());
        String dotStr = DARK + ".".repeat(dots) + RESET;
        logger.info(TAG + " " + GRAY + "  " + WHITE + key + " " + dotStr + " " + BLUE + value + RESET);
    }

    /**
     * Prints a status row showing an OK (✔) or FAIL (✘) badge.
     * <pre>
     *   [✔] Database ......... SQLite (local)
     *   [✘] MySQL ........... disabled
     * </pre>
     *
     * @param ok    whether the subsystem is healthy / enabled
     * @param label short label
     * @param detail extra detail string shown after a colon
     */
    public static void statusRow(boolean ok, String label, String detail) {
        String badge  = ok ? GREEN + "[✔]" + RESET : RED + "[✘]" + RESET;
        int dots      = Math.max(1, KEY_WIDTH - label.length());
        String dotStr = DARK + ".".repeat(dots) + RESET;
        String valueColor = ok ? GREEN : GRAY;
        logger.info(TAG + " " + GRAY + "  " + badge + " " + WHITE + label
                + " " + dotStr + " " + valueColor + detail + RESET);
    }

    /**
     * Prints an empty (blank) separator line — useful between sections.
     */
    @SuppressWarnings("unused") // Public API — available for callers outside this class
    public static void blank() {
        logger.info("");
    }

    // ── Banner helpers ────────────────────────────────────────────────────────

    /**
     * Prints the full FPP startup banner including version, author, and a
     * subsystem status table.
     *
     * <p>Call after ALL subsystems have been initialised so statuses are accurate.
     *
     * @param version        plugin version string
     * @param authors        comma-joined author list
     * @param namePoolSize   number of names in the name pool
     * @param msgPoolSize    number of chat messages in the pool
     * @param dbBackend      short label like "SQLite (local)" or "MySQL" or "none"
     * @param dbOk           whether the DB connected successfully
     * @param skinMode       value from Config.skinMode()
     * @param bodyEnabled    whether Mannequin bodies are spawned
     * @param persistEnabled whether bot persistence is on
     * @param luckPermsFound whether LuckPerms is installed
     * @param swapEnabled    whether bot swap/rotation is on
     * @param fakeChatEnable whether fake chat is on
     * @param chunkLoading   whether chunk loading is on
     * @param maxBots        global bot limit (0 = unlimited)
     * @param metricsActive  whether FastStats metrics are running
     */
    public static void printStartupBanner(
            String version,
            String authors,
            int    namePoolSize,
            int    msgPoolSize,
            String dbBackend,
            boolean dbOk,
            String skinMode,
            boolean bodyEnabled,
            boolean persistEnabled,
            boolean luckPermsFound,
            boolean swapEnabled,
            boolean fakeChatEnable,
            boolean chunkLoading,
            int    maxBots,
            boolean metricsActive) {

        boldRule();
        info("  " + BOLD + BLUE + "ꜰᴀᴋᴇ ᴘʟᴀʏᴇʀ ᴘʟᴜɢɪɴ" + RESET + WHITE + "  v" + version + RESET);
        info("  " + GRAY + "Author : " + WHITE + authors);
        info("  " + GRAY + "Modrinth → " + BLUE + "https://modrinth.com/plugin/fake-player-plugin-(fpp)");
        rule();

        section("Configuration");
        kv("Skin mode",      skinMode);
        kv("Name pool",      namePoolSize + " names");
        kv("Msg pool",       msgPoolSize  + " messages");
        kv("Max bots",       maxBots == 0 ? "unlimited" : String.valueOf(maxBots));

        section("Subsystems");
        statusRow(dbOk,           "Database",       dbBackend);
        statusRow(bodyEnabled,    "Mannequin body", bodyEnabled    ? "enabled"       : "disabled");
        statusRow(persistEnabled, "Persistence",    persistEnabled ? "enabled"       : "disabled");
        statusRow(chunkLoading,   "Chunk loading",  chunkLoading   ? "enabled"       : "disabled");
        statusRow(swapEnabled,    "Bot swap",        swapEnabled    ? "enabled"       : "disabled");
        statusRow(fakeChatEnable, "Fake chat",       fakeChatEnable ? "enabled"       : "disabled");
        statusRow(luckPermsFound, "LuckPerms",       luckPermsFound ? "detected"      : "not installed");
        statusRow(metricsActive,  "Metrics",         metricsActive  ? "reporting"     : "disabled");

        boldRule();
        success("  Plugin started successfully! Type /fpp for help.");
        boldRule();
    }

    /**
     * Prints the shutdown banner summarising what was cleaned up.
     *
     * @param botsRemoved number of bots that were cleanly removed
     * @param dbFlushed   whether DB sessions were flushed
     * @param uptimeMs    server uptime since plugin enable, in milliseconds
     */
    public static void printShutdownBanner(int botsRemoved, boolean dbFlushed, long uptimeMs) {
        boldRule();
        highlight("  ꜰᴀᴋᴇ ᴘʟᴀʏᴇʀ ᴘʟᴜɢɪɴ  —  shutting down");
        rule();
        kv("Bots removed",   botsRemoved);
        kv("DB sessions",    dbFlushed ? "flushed ✔" : "skipped (no DB)");
        kv("Session uptime", formatUptime(uptimeMs));
        boldRule();
        info("  Goodbye! ꜰᴀᴋᴇ ᴘʟᴀʏᴇʀ ᴘʟᴜɢɪɴ has been disabled.");
        boldRule();
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    /**
     * Formats a millisecond duration as {@code Xh Ym Zs}.
     * Used in the shutdown banner to show how long the plugin ran.
     */
    private static String formatUptime(long ms) {
        long totalSec = ms / 1_000;
        long hours    = totalSec / 3600;
        long minutes  = (totalSec % 3600) / 60;
        long seconds  = totalSec % 60;
        if (hours > 0) return hours + "h " + minutes + "m " + seconds + "s";
        if (minutes > 0) return minutes + "m " + seconds + "s";
        return seconds + "s";
    }
}
