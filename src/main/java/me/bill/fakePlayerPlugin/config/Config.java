package me.bill.fakePlayerPlugin.config;

import me.bill.fakePlayerPlugin.FakePlayerPlugin;
import me.bill.fakePlayerPlugin.util.FppLogger;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Central accessor for {@code config.yml}.
 * <p>
 * Call {@link #reload()} to re-read the file from disk (e.g. after {@code /fpp reload}).
 */
public final class Config {

    private static FakePlayerPlugin plugin;
    private static FileConfiguration cfg;

    private Config() {}

    // ── Lifecycle ────────────────────────────────────────────────────────────

    public static void init(FakePlayerPlugin instance) {
        plugin = instance;
        reload();
    }

    public static void reload() {
        // Save default config.yml from JAR if it doesn't exist on disk yet
        plugin.saveDefaultConfig();
        // Always re-read from disk and merge any missing keys from the JAR default
        plugin.reloadConfig();
        cfg = plugin.getConfig();
        cfg.options().copyDefaults(true);
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    /** Language file identifier, e.g. {@code "en"}. */
    public static String getLanguage() {
        return cfg.getString("language", "en");
    }

    /** Whether debug logging is enabled. */
    public static boolean isDebug() {
        return cfg.getBoolean("debug.enabled", false);
    }

    /** Logs a message to console only when debug mode is on. */
    public static void debug(String message) {
        FppLogger.debug(message);
    }
}
