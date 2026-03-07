package me.bill.fakePlayerPlugin.config;

import me.bill.fakePlayerPlugin.FakePlayerPlugin;
import me.bill.fakePlayerPlugin.util.FppLogger;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

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

    // ── General ──────────────────────────────────────────────────────────────

    /** Language file identifier, e.g. {@code "en"}. */
    public static String getLanguage() {
        return cfg.getString("language", "en");
    }

    /** Whether debug logging is enabled. */
    public static boolean isDebug() {
        return cfg.getBoolean("debug.enabled", false);
    }

    // ── Fake Player — General ────────────────────────────────────────────────

    /** Maximum bots allowed at once. 0 = unlimited. */
    public static int maxBots()         { return cfg.getInt("fake-player.max-bots", 10); }

    /** Whether to fetch real Mojang skins for bots. */
    public static boolean fetchSkin()   { return cfg.getBoolean("fake-player.fetch-skin", false); }

    /** Whether to show the fetched skin to players (requires fetch-skin: true). */
    public static boolean showSkin()    { return cfg.getBoolean("fake-player.show-skin", false); }

    /** Whether bots should spawn a physical zombie body in the world. */
    public static boolean spawnBody()   { return cfg.getBoolean("fake-player.spawn-body", true); }

    /** Name pool for random bot name selection — sourced from bot-names.yml. */
    public static List<String> namePool() {
        return BotNameConfig.getNames();
    }

    /** Minimum join delay in ticks when staggering multiple bot spawns. */
    public static int joinDelayMin() { return cfg.getInt("fake-player.join-delay.min", 0); }

    /** Maximum join delay in ticks when staggering multiple bot spawns. */
    public static int joinDelayMax() { return cfg.getInt("fake-player.join-delay.max", 20); }

    /** Minimum leave delay in ticks when staggering multiple bot removals. */
    public static int leaveDelayMin() { return cfg.getInt("fake-player.leave-delay.min", 0); }

    /** Maximum leave delay in ticks when staggering multiple bot removals. */
    public static int leaveDelayMax() { return cfg.getInt("fake-player.leave-delay.max", 20); }

    // ── Fake Player — Combat ─────────────────────────────────────────────────

    /** Base health bots spawn with. */
    public static double maxHealth()    { return cfg.getDouble("fake-player.combat.max-health", 20.0); }

    /** Whether to play the player hurt sound on damage. */
    public static boolean hurtSound()  { return cfg.getBoolean("fake-player.combat.hurt-sound", true); }

    // ── Fake Player — Death & Respawn ────────────────────────────────────────

    /** Whether bots respawn after death. */
    public static boolean respawnOnDeath()  { return cfg.getBoolean("fake-player.death.respawn-on-death", true); }

    /** Ticks to wait before respawning. */
    public static int respawnDelay()        { return cfg.getInt("fake-player.death.respawn-delay", 60); }

    /** Whether to suppress mob drops on bot death. */
    public static boolean suppressDrops()   { return cfg.getBoolean("fake-player.death.suppress-drops", true); }

    // ── Fake Player — Messages ───────────────────────────────────────────────

    /** Whether to broadcast a join message when a bot spawns. */
    public static boolean joinMessage()     { return cfg.getBoolean("fake-player.messages.join-message", true); }

    /** Whether to broadcast a leave message when a bot is removed. */
    public static boolean leaveMessage()    { return cfg.getBoolean("fake-player.messages.leave-message", true); }

    /** Whether to broadcast a kill message when a player kills a bot. */
    public static boolean killMessage()     { return cfg.getBoolean("fake-player.messages.kill-message", true); }

    // ── Fake Player — Chunk Loading ──────────────────────────────────────────

    /** Whether bots should keep chunks loaded around them like a real player. */
    public static boolean chunkLoadingEnabled() { return cfg.getBoolean("fake-player.chunk-loading.enabled", true); }

    /** Chunk radius bots keep loaded (vanilla player default ≈ 10). */
    public static int chunkLoadingRadius()      { return cfg.getInt("fake-player.chunk-loading.radius", 6); }

    // ── Utility ──────────────────────────────────────────────────────────────

    /** Logs a message to console only when debug mode is on. */
    public static void debug(String message) { FppLogger.debug(message); }
}
