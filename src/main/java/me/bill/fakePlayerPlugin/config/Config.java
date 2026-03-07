package me.bill.fakePlayerPlugin.config;

import me.bill.fakePlayerPlugin.FakePlayerPlugin;
import me.bill.fakePlayerPlugin.util.FppLogger;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

/**
 * Central accessor for {@code config.yml}.
 * All methods read live from the cached {@link FileConfiguration} object,
 * so values are always up-to-date after a {@link #reload()}.
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
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        cfg = plugin.getConfig();
        cfg.options().copyDefaults(true);
        plugin.saveConfig();
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
    public static int maxBots()      { return cfg.getInt("fake-player.max-bots", 1000); }

    /** Default personal bot limit for players with {@code fpp.user.spawn}. */
    public static int userBotLimit() { return cfg.getInt("fake-player.user-bot-limit", 1); }

    /**
     * Count presets shown in tab-complete for admin-tier spawn ({@code fpp.spawn}).
     * Defaults to [1, 5, 10, 15, 20].
     */
    public static List<String> spawnCountPresetsAdmin() {
        List<?> raw = cfg.getList("fake-player.spawn-count-presets.admin",
                List.of(1, 5, 10, 15, 20));
        return raw.stream().map(Object::toString).toList();
    }

    // ── Skin ─────────────────────────────────────────────────────────────────

    /**
     * Skin rendering mode for bots.
     * <ul>
     *   <li>{@code "auto"}     — Mannequin.setProfile(name); Paper resolves skin automatically.</li>
     *   <li>{@code "fetch"}    — Plugin fetches texture from Mojang API via SkinFetcher.</li>
     *   <li>{@code "disabled"} — No skin; bots display the default Steve/Alex appearance.</li>
     * </ul>
     */
    public static String skinMode() {
        return cfg.getString("fake-player.skin.mode", "auto").toLowerCase();
    }

    /** Whether the skin cache is cleared on {@code /fpp reload} (fetch mode only). */
    public static boolean skinClearCacheOnReload() {
        return cfg.getBoolean("fake-player.skin.clear-cache-on-reload", true);
    }

    // ── Body & Persistence ────────────────────────────────────────────────────

    /** Whether bots spawn a physical Mannequin body in the world. */
    public static boolean spawnBody() { return cfg.getBoolean("fake-player.spawn-body", true); }

    /** Whether active bots are saved on shutdown and restored on next startup. */
    public static boolean persistOnRestart() { return cfg.getBoolean("fake-player.persist-on-restart", true); }

    /** Name pool for random bot name selection — sourced from bot-names.yml. */
    public static List<String> namePool() { return BotNameConfig.getNames(); }

    // ── Join / Leave Delays ───────────────────────────────────────────────────

    /** Minimum join delay in ticks when staggering multiple bot spawns. */
    public static int joinDelayMin() { return cfg.getInt("fake-player.join-delay.min", 0); }

    /** Maximum join delay in ticks when staggering multiple bot spawns. */
    public static int joinDelayMax() { return cfg.getInt("fake-player.join-delay.max", 5); }

    /** Minimum leave delay in ticks when staggering multiple bot removals. */
    public static int leaveDelayMin() { return cfg.getInt("fake-player.leave-delay.min", 0); }

    /** Maximum leave delay in ticks when staggering multiple bot removals. */
    public static int leaveDelayMax() { return cfg.getInt("fake-player.leave-delay.max", 5); }

    // ── Combat ────────────────────────────────────────────────────────────────

    /** Base health bots spawn with (default player health = 20.0). */
    public static double maxHealth()   { return cfg.getDouble("fake-player.combat.max-health", 20.0); }

    /** Whether to play the player hurt sound when a bot takes damage. */
    public static boolean hurtSound() { return cfg.getBoolean("fake-player.combat.hurt-sound", true); }

    // ── Death & Respawn ───────────────────────────────────────────────────────

    /** Whether bots respawn after death ({@code false} = leave permanently). */
    public static boolean respawnOnDeath() { return cfg.getBoolean("fake-player.death.respawn-on-death", false); }

    /** Ticks to wait before a bot respawns (20 ticks = 1 second). */
    public static int respawnDelay()       { return cfg.getInt("fake-player.death.respawn-delay", 60); }

    /** Whether to suppress mob drops on bot death. */
    public static boolean suppressDrops()  { return cfg.getBoolean("fake-player.death.suppress-drops", true); }

    // ── Messages ──────────────────────────────────────────────────────────────

    /** Broadcast a vanilla-style join message when a bot is spawned. */
    public static boolean joinMessage()  { return cfg.getBoolean("fake-player.messages.join-message", true); }

    /** Broadcast a vanilla-style leave message when a bot is removed. */
    public static boolean leaveMessage() { return cfg.getBoolean("fake-player.messages.leave-message", true); }

    /** Broadcast a kill message when a player kills a bot. */
    public static boolean killMessage()  { return cfg.getBoolean("fake-player.messages.kill-message", false); }

    // ── Chunk Loading ─────────────────────────────────────────────────────────

    /** Whether bots keep chunks loaded around them like a real player. */
    public static boolean chunkLoadingEnabled() { return cfg.getBoolean("fake-player.chunk-loading.enabled", true); }

    /** Chunk radius kept loaded around each bot (vanilla player default ≈ 10). */
    public static int chunkLoadingRadius()      { return cfg.getInt("fake-player.chunk-loading.radius", 6); }

    // ── Head AI ───────────────────────────────────────────────────────────────

    /** Radius (blocks) within which a bot rotates to face the nearest player. 0 = disabled. */
    public static double headAiLookRange()  { return cfg.getDouble("fake-player.head-ai.look-range", 8.0); }

    /** Turn-speed interpolation factor (0.0–1.0). 1.0 = instant snap, 0.1 = slow smooth. */
    public static float headAiTurnSpeed()   { return (float) cfg.getDouble("fake-player.head-ai.turn-speed", 0.3); }

    // ── Collision / Push ──────────────────────────────────────────────────────

    /** Radius (blocks) at which walking into a bot triggers a push impulse. */
    public static double collisionWalkRadius()   { return cfg.getDouble("fake-player.collision.walk-radius", 0.85); }

    /** Impulse strength when a player walks into a bot. */
    public static double collisionWalkStrength() { return cfg.getDouble("fake-player.collision.walk-strength", 0.22); }

    /** Maximum horizontal speed cap for any push source. */
    public static double collisionMaxHoriz()     { return cfg.getDouble("fake-player.collision.max-horizontal-speed", 0.30); }

    /** Knockback impulse when a player punches a bot. */
    public static double collisionHitStrength()  { return cfg.getDouble("fake-player.collision.hit-strength", 0.45); }

    /** Radius (blocks) at which two overlapping bots push each other apart. */
    public static double collisionBotRadius()    { return cfg.getDouble("fake-player.collision.bot-radius", 0.90); }

    /** Impulse strength for bot-vs-bot separation. */
    public static double collisionBotStrength()  { return cfg.getDouble("fake-player.collision.bot-strength", 0.14); }

    // ── Bot Swap ──────────────────────────────────────────────────────────────

    /** Whether the bot swap/rotation system is active. */
    public static boolean swapEnabled()        { return cfg.getBoolean("fake-player.swap.enabled", false); }

    /** Minimum seconds a bot stays online before swapping out. */
    public static int swapSessionMin()         { return cfg.getInt("fake-player.swap.session-min", 120); }

    /** Maximum seconds a bot stays online before swapping out. */
    public static int swapSessionMax()         { return cfg.getInt("fake-player.swap.session-max", 600); }

    /** Minimum seconds between a bot leaving and its replacement joining. */
    public static int swapRejoinDelayMin()     { return cfg.getInt("fake-player.swap.rejoin-delay-min", 5); }

    /** Maximum seconds between a bot leaving and its replacement joining. */
    public static int swapRejoinDelayMax()     { return cfg.getInt("fake-player.swap.rejoin-delay-max", 45); }

    /** Random jitter (±seconds) added to each bot's session timer. */
    public static int swapJitter()             { return cfg.getInt("fake-player.swap.jitter", 30); }

    /** Probability (0.0–1.0) that a rejoining bot keeps the same name (reconnect sim). */
    public static double swapReconnectChance() { return cfg.getDouble("fake-player.swap.reconnect-chance", 0.15); }

    /** Percent chance (0–100) that a bot's rejoin gap is extended 1–3 min (AFK-kick sim). */
    public static int swapAfkKickChance()      { return cfg.getInt("fake-player.swap.afk-kick-chance", 5); }

    /** Whether the bot sends a farewell chat message before leaving. */
    public static boolean swapFarewellChat()   { return cfg.getBoolean("fake-player.swap.farewell-chat", true); }

    /** Whether the replacement bot sends a greeting chat message after joining. */
    public static boolean swapGreetingChat()   { return cfg.getBoolean("fake-player.swap.greeting-chat", true); }

    /** Whether session lengths are biased by server local time-of-day. */
    public static boolean swapTimeOfDayBias()  { return cfg.getBoolean("fake-player.swap.time-of-day-bias", true); }

    // ── Fake Chat ─────────────────────────────────────────────────────────────

    /** Whether bots randomly send chat messages. */
    public static boolean fakeChatEnabled()       { return cfg.getBoolean("fake-chat.enabled", false); }

    /** Only send chat when at least one real player is online. */
    public static boolean fakeChatRequirePlayer() { return cfg.getBoolean("fake-chat.require-player-online", true); }

    /** Probability (0.0–1.0) that a scheduled chat interval actually fires a message. */
    public static double fakeChatChance()         { return cfg.getDouble("fake-chat.chance", 0.75); }

    /** Minimum seconds between a bot's own chat messages. */
    public static int fakeChatIntervalMin()       { return cfg.getInt("fake-chat.interval.min", 5); }

    /** Maximum seconds between a bot's own chat messages. */
    public static int fakeChatIntervalMax()       { return cfg.getInt("fake-chat.interval.max", 10); }

    /** List of messages bots can randomly send — loaded from bot-messages.yml. */
    public static List<String> fakeChatMessages() { return BotMessageConfig.getMessages(); }

    // ── Database — MySQL ──────────────────────────────────────────────────────

    public static boolean mysqlEnabled()  { return cfg.getBoolean("database.mysql.enabled", false); }
    public static String  mysqlHost()     { return cfg.getString("database.mysql.host", "localhost"); }
    public static int     mysqlPort()     { return cfg.getInt("database.mysql.port", 3306); }
    public static String  mysqlDatabase() { return cfg.getString("database.mysql.database", "fpp"); }
    public static String  mysqlUsername() { return cfg.getString("database.mysql.username", "root"); }
    public static String  mysqlPassword() { return cfg.getString("database.mysql.password", ""); }
    public static boolean mysqlUseSSL()   { return cfg.getBoolean("database.mysql.use-ssl", false); }

    // ── Utility ───────────────────────────────────────────────────────────────

    /** Logs a message to console only when debug mode is on. */
    public static void debug(String message) { FppLogger.debug(message); }
}
