package me.bill.fakePlayerPlugin.util;

import me.bill.fakePlayerPlugin.config.Config;

import java.util.logging.Logger;

/**
 * Coloured console logger for FPP.
 * Uses ANSI escape codes supported by Paper's console (via JLine).
 *
 * Colours used:
 *  • Prefix tag  →  #0079FF (bright blue)
 *  • INFO        →  bright white
 *  • DEBUG       →  yellow
 *  • SUCCESS     →  green
 *  • WARN        →  gold/orange
 *  • ERROR       →  red
 */
public final class FppLogger {

    // ANSI codes
    private static final String RESET   = "\u001B[0m";
    private static final String BOLD    = "\u001B[1m";
    private static final String BLUE    = "\u001B[38;2;0;121;255m"; // #0079FF
    private static final String WHITE   = "\u001B[97m";
    private static final String YELLOW  = "\u001B[93m";
    private static final String GREEN   = "\u001B[92m";
    private static final String GOLD    = "\u001B[33m";
    private static final String RED     = "\u001B[91m";
    private static final String GRAY    = "\u001B[90m";

    private static final String TAG = BOLD + BLUE + "[FPP]" + RESET;

    private static Logger logger;

    private FppLogger() {}

    public static void init(Logger javaLogger) {
        logger = javaLogger;
    }

    // ── Public API ───────────────────────────────────────────────────────────

    /** General info — white. */
    public static void info(String message) {
        logger.info(TAG + " " + WHITE + message + RESET);
    }

    /** Positive confirmation — green. */
    public static void success(String message) {
        logger.info(TAG + " " + GREEN + message + RESET);
    }

    /** Warning — gold. */
    public static void warn(String message) {
        logger.warning(TAG + " " + GOLD + message + RESET);
    }

    /** Error — red. */
    public static void error(String message) {
        logger.severe(TAG + " " + RED + message + RESET);
    }

    /** Debug — yellow, only printed when debug mode is on. */
    public static void debug(String message) {
        if (Config.isDebug()) {
            logger.info(TAG + " " + GRAY + "[" + YELLOW + "DEBUG" + GRAY + "] " + YELLOW + message + RESET);
        }
    }
}

