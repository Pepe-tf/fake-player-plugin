package me.bill.fakePlayerPlugin.util;

import me.bill.fakePlayerPlugin.FakePlayerPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.util.Arrays;

/**
 * Handles automatic, non-destructive upgrades of {@code config.yml} from any
 * previous plugin version to the current one.
 *
 * <h3>How it works</h3>
 * <ol>
 *   <li>On every startup, {@link #migrateIfNeeded(FakePlayerPlugin)} is called
 *       <em>before</em> {@code Config.init()} so migrations run on the raw YAML.</li>
 *   <li>The key {@code config-version} in {@code config.yml} records which
 *       format version was last written. If it is missing the config is treated
 *       as version&nbsp;0 (very old / first run without this system).</li>
 *   <li>Each numbered migration step is applied in sequence — keys are added,
 *       renamed, or restructured as needed while existing user values are
 *       preserved.</li>
 *   <li>A full backup is created via {@link BackupManager} before any changes
 *       are written to disk.</li>
 *   <li>After all steps run the file is saved and {@code config-version} is
 *       set to {@link #CURRENT_VERSION}.</li>
 *   <li>Bukkit's own {@code copyDefaults} mechanism fills in any remaining
 *       missing keys when {@code Config.init()} runs immediately afterwards.</li>
 * </ol>
 *
 * <h3>Adding a new migration</h3>
 * <ol>
 *   <li>Increment {@link #CURRENT_VERSION}.</li>
 *   <li>Add a new {@code migrateVnToVm()} private method.</li>
 *   <li>Add a call to it in {@link #migrateIfNeeded} inside the version chain.</li>
 *   <li>Update {@code config.yml} in resources with the new keys / comments.</li>
 * </ol>
 */
public final class ConfigMigrator {

    /**
     * The config-version value written by this build.
     * <b>Increment this whenever config.yml structure changes.</b>
     */
    public static final int CURRENT_VERSION = 17;

    /**
     * Mirrors the {@code debug} flag read directly from the raw YAML during migration.
     * Used by {@link #log} so it never touches {@code Config.cfg} (which is null
     * during migration — {@code Config.init()} runs after {@code migrateIfNeeded}).
     */
    private static boolean rawDebug = false;

    private ConfigMigrator() {}

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Runs the full migration chain if the stored {@code config-version} is
     * below {@link #CURRENT_VERSION}.
     *
     * @param plugin Plugin instance (used to locate the data folder).
     * @return {@code true} if at least one migration step was applied and the
     *         file was saved; {@code false} if the config was already current.
     */
    public static boolean migrateIfNeeded(FakePlayerPlugin plugin) {
        File configFile = new File(plugin.getDataFolder(), "config.yml");

        // First run — Bukkit hasn't saved defaults yet; nothing to migrate.
        if (!configFile.exists()) {
            // Config.cfg is null here — use FppLogger.info which has no Config dependency
            FppLogger.info("ConfigMigrator: no existing config.yml — skipping migration.");
            return false;
        }

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(configFile);

        // Read debug flag directly from raw YAML — Config.cfg is still null at this point.
        rawDebug = cfg.getBoolean("debug", false);

        int stored = cfg.getInt("config-version", 0);

        if (stored >= CURRENT_VERSION) {
            log("config is current (v" + stored + "). No migration needed.");
            return false;
        }

        // ── Pre-migration backup ───────────────────────────────────────────────
        String fromLabel = stored == 0 ? "legacy" : "v" + stored;
        FppLogger.section("Config Migration");
        FppLogger.info("Upgrading config from " + fromLabel + " → v" + CURRENT_VERSION + "…");
        FppLogger.info("A backup will be created before any changes are written.");
        BackupManager.createFullBackup(plugin, "pre-migration-" + fromLabel);

        // ── Apply migrations in order ──────────────────────────────────────────
        boolean anyChange = false;
        if (stored < 2)  anyChange |= v1to2(cfg);
        if (stored < 3)  anyChange |= v2to3(cfg);
        if (stored < 4)  anyChange |= v3to4(cfg);
        if (stored < 5)  anyChange |= v4to5(cfg);
        if (stored < 6)  anyChange |= v5to6(cfg);
        if (stored < 7)  anyChange |= v6to7(cfg);
        if (stored < 8)  anyChange |= v7to8(cfg);
        if (stored < 9)  anyChange |= v8to9(cfg);
        if (stored < 10) anyChange |= v9to10(cfg);
        if (stored < 11) anyChange |= v10to11(cfg);
        if (stored < 12) anyChange |= v11to12(cfg);
        if (stored < 13) anyChange |= v12to13(cfg);
        if (stored < 14) anyChange |= v13to14(cfg);
        if (stored < 15) anyChange |= v14to15(cfg);
        if (stored < 16) anyChange |= v15to16(cfg);
        if (stored < 17) anyChange |= v16to17(cfg);

        // ── Fill any remaining missing keys from jar defaults ──────────────────
        fillDefaults(plugin, cfg);

        // ── Stamp the version and save ─────────────────────────────────────────
        cfg.set("config-version", CURRENT_VERSION);

        try {
            cfg.save(configFile);
            if (anyChange) {
                FppLogger.success("Config migrated to v" + CURRENT_VERSION + " successfully.");
            } else {
                FppLogger.success("Config stamped as v" + CURRENT_VERSION
                        + " (no structural changes needed; defaults filled).");
            }
        } catch (IOException e) {
            FppLogger.error("ConfigMigrator: failed to save migrated config: " + e.getMessage());
            return false;
        }

        return true;
    }

    /**
     * Forces a re-run of the migration chain from version 0, regardless of the
     * stored version. Also fills any missing keys from the jar defaults.
     * Useful as part of {@code /fpp migrate config}.
     */
    public static void forceMigrate(FakePlayerPlugin plugin) {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) return;

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(configFile);
        cfg.set("config-version", 0);   // reset so the chain runs fully
        try { cfg.save(configFile); } catch (IOException ignored) {}

        migrateIfNeeded(plugin);
    }

    public static int getCurrentVersion() { return CURRENT_VERSION; }

    // ── Migration steps ────────────────────────────────────────────────────────
    // Each method returns true if it changed anything.

    /** v1 → v2: Added update-checker section. */
    private static boolean v1to2(YamlConfiguration cfg) {
        if (cfg.contains("update-checker")) return false;
        cfg.set("update-checker.enabled", true);
        log("v1→v2", "added update-checker section");
        return true;
    }

    /** v2 → v3: Added luckperms section. */
    private static boolean v2to3(YamlConfiguration cfg) {
        if (cfg.contains("luckperms")) return false;
        cfg.set("luckperms.use-prefix", true);
        log("v2→v3", "added luckperms section");
        return true;
    }

    /**
     * v3 → v4: Skin system overhaul.
     * <ul>
     *   <li>{@code skin-enabled} (boolean) → {@code skin.mode} (string)</li>
     *   <li>{@code skin.enabled} (boolean) → {@code skin.mode} (string)</li>
     *   <li>{@code skin.custom.pool}    → {@code skin.pool}</li>
     *   <li>{@code skin.custom.by-name} → {@code skin.overrides}</li>
     * </ul>
     */
    private static boolean v3to4(YamlConfiguration cfg) {
        boolean changed = false;

        // Top-level boolean flag (very old format)
        if (cfg.contains("skin-enabled")) {
            boolean was = cfg.getBoolean("skin-enabled", true);
            cfg.set("skin.mode", was ? "auto" : "off");
            cfg.set("skin-enabled", null);
            log("v3→v4", "skin-enabled → skin.mode=" + (was ? "auto" : "off"));
            changed = true;
        }

        // skin.enabled (intermediate format)
        if (cfg.contains("skin.enabled")) {
            boolean was = cfg.getBoolean("skin.enabled", true);
            if (!cfg.contains("skin.mode")) cfg.set("skin.mode", was ? "auto" : "off");
            cfg.set("skin.enabled", null);
            log("v3→v4", "skin.enabled → skin.mode");
            changed = true;
        }

        // Ensure skin.mode always exists
        if (!cfg.contains("skin.mode")) {
            cfg.set("skin.mode", "auto");
            changed = true;
        }

        // skin.custom.pool → skin.pool
        if (cfg.contains("skin.custom.pool") && !cfg.contains("skin.pool")) {
            cfg.set("skin.pool", cfg.get("skin.custom.pool"));
            log("v3→v4", "skin.custom.pool → skin.pool");
            changed = true;
        }

        // skin.custom.by-name → skin.overrides
        if (cfg.contains("skin.custom.by-name") && !cfg.contains("skin.overrides")) {
            cfg.set("skin.overrides", cfg.get("skin.custom.by-name"));
            log("v3→v4", "skin.custom.by-name → skin.overrides");
            changed = true;
        }

        // Remove deprecated nested section if now empty
        if (!cfg.contains("skin.custom.pool") && !cfg.contains("skin.custom.by-name")) {
            cfg.set("skin.custom", null);
        }

        return changed;
    }

    /** v4 → v5: Added body section (replaces top-level {@code spawn-body}). */
    private static boolean v4to5(YamlConfiguration cfg) {
        if (cfg.contains("body")) return false;
        boolean prev = cfg.getBoolean("spawn-body", true);
        cfg.set("body.enabled", prev);
        cfg.set("spawn-body", null);
        log("v4→v5", "spawn-body → body.enabled");
        return true;
    }

    /** v5 → v6: Added chunk-loading section. */
    private static boolean v5to6(YamlConfiguration cfg) {
        if (cfg.contains("chunk-loading")) return false;
        cfg.set("chunk-loading.enabled", true);
        cfg.set("chunk-loading.radius", 6);
        cfg.set("chunk-loading.update-interval", 20);
        log("v5→v6", "added chunk-loading section");
        return true;
    }

    /** v6 → v7: Added head-ai section. */
    private static boolean v6to7(YamlConfiguration cfg) {
        if (cfg.contains("head-ai")) return false;
        cfg.set("head-ai.look-range", 8.0);
        cfg.set("head-ai.turn-speed", 0.3);
        log("v6→v7", "added head-ai section");
        return true;
    }

    /** v7 → v8: Added collision / push section. */
    private static boolean v7to8(YamlConfiguration cfg) {
        if (cfg.contains("collision")) return false;
        cfg.set("collision.walk-radius", 0.85);
        cfg.set("collision.walk-strength", 0.22);
        cfg.set("collision.max-horizontal-speed", 0.30);
        cfg.set("collision.hit-strength", 0.45);
        cfg.set("collision.bot-radius", 0.90);
        cfg.set("collision.bot-strength", 0.14);
        log("v7→v8", "added collision section");
        return true;
    }

    /** v8 → v9: Added swap and fake-chat sections. */
    private static boolean v8to9(YamlConfiguration cfg) {
        boolean changed = false;

        if (!cfg.contains("swap")) {
            cfg.set("swap.enabled",           false);
            cfg.set("swap.session-min",        120);
            cfg.set("swap.session-max",        600);
            cfg.set("swap.rejoin-delay-min",   5);
            cfg.set("swap.rejoin-delay-max",   45);
            cfg.set("swap.jitter",             30);
            cfg.set("swap.reconnect-chance",   0.15);
            cfg.set("swap.afk-kick-chance",    5);
            cfg.set("swap.farewell-chat",      true);
            cfg.set("swap.greeting-chat",      true);
            cfg.set("swap.time-of-day-bias",   true);
            log("v8→v9", "added swap section");
            changed = true;
        }

        if (!cfg.contains("fake-chat")) {
            cfg.set("fake-chat.enabled",                 false);
            cfg.set("fake-chat.require-player-online",   true);
            cfg.set("fake-chat.chance",                  0.75);
            cfg.set("fake-chat.interval.min",            5);
            cfg.set("fake-chat.interval.max",            10);
            log("v8→v9", "added fake-chat section");
            changed = true;
        }

        return changed;
    }

    /**
     * v9 → v10: Comprehensive restructure.
     * <ul>
     *   <li>{@code max-bots}           → {@code limits.max-bots}</li>
     *   <li>{@code persist-on-restart} → {@code persistence.enabled}</li>
     *   <li>Added {@code limits} section with user-bot-limit + spawn-presets</li>
     *   <li>Added {@code bot-name} section</li>
     *   <li>Added {@code death} section</li>
     *   <li>Added {@code database} section</li>
     *   <li>Normalised join-delay / leave-delay flat keys → nested sections</li>
     * </ul>
     */
    private static boolean v9to10(YamlConfiguration cfg) {
        boolean changed = false;

        // limits section
        if (!cfg.contains("limits")) {
            int prev = cfg.getInt("max-bots", 1000);
            cfg.set("limits.max-bots",       prev);
            cfg.set("limits.user-bot-limit", 1);
            cfg.set("limits.spawn-presets",  Arrays.asList(1, 5, 10, 15, 20));
            cfg.set("max-bots", null);
            log("v9→v10", "max-bots → limits section");
            changed = true;
        } else if (cfg.contains("max-bots")) {
            // Merge leftover key
            if (!cfg.contains("limits.max-bots")) cfg.set("limits.max-bots", cfg.getInt("max-bots", 1000));
            cfg.set("max-bots", null);
            changed = true;
        }

        // bot-name section
        if (!cfg.contains("bot-name")) {
            cfg.set("bot-name.admin-format", "{bot_name}");
            cfg.set("bot-name.user-format",  "bot-{spawner}-{num}");
            log("v9→v10", "added bot-name section");
            changed = true;
        }

        // persistence section
        if (!cfg.contains("persistence")) {
            boolean prev = cfg.getBoolean("persist-on-restart", true);
            cfg.set("persistence.enabled", prev);
            cfg.set("persist-on-restart", null);
            log("v9→v10", "persist-on-restart → persistence.enabled");
            changed = true;
        }

        // death section
        if (!cfg.contains("death")) {
            cfg.set("death.respawn-on-death", false);
            cfg.set("death.respawn-delay",    60);
            cfg.set("death.suppress-drops",   true);
            log("v9→v10", "added death section");
            changed = true;
        }

        // join-delay flat keys → nested section
        if (cfg.contains("join-delay-min") && !cfg.contains("join-delay.min")) {
            cfg.set("join-delay.min", cfg.getInt("join-delay-min", 0));
            cfg.set("join-delay.max", cfg.getInt("join-delay-max", 40));
            cfg.set("join-delay-min", null);
            cfg.set("join-delay-max", null);
            log("v9→v10", "join-delay-min/max → join-delay section");
            changed = true;
        }
        if (cfg.contains("leave-delay-min") && !cfg.contains("leave-delay.min")) {
            cfg.set("leave-delay.min", cfg.getInt("leave-delay-min", 0));
            cfg.set("leave-delay.max", cfg.getInt("leave-delay-max", 40));
            cfg.set("leave-delay-min", null);
            cfg.set("leave-delay-max", null);
            log("v9→v10", "leave-delay-min/max → leave-delay section");
            changed = true;
        }

        // database section
        if (!cfg.contains("database")) {
            cfg.set("database.mysql-enabled",                   false);
            cfg.set("database.mysql.host",                      "localhost");
            cfg.set("database.mysql.port",                      3306);
            cfg.set("database.mysql.database",                  "fpp");
            cfg.set("database.mysql.username",                  "root");
            cfg.set("database.mysql.password",                  "");
            cfg.set("database.mysql.use-ssl",                   false);
            cfg.set("database.mysql.pool-size",                 5);
            cfg.set("database.mysql.connection-timeout",        30000);
            cfg.set("database.location-flush-interval",         30);
            cfg.set("database.session-history.max-rows",        20);
            log("v9→v10", "added database section");
            changed = true;
        }

        return changed;
    }

    /**
     * v10 → v11: Enhanced skin system — guaranteed skin + fallback name.
     * <ul>
     *   <li>Added {@code skin.guaranteed-skin} — always apply a skin (default: true)</li>
     *   <li>Added {@code skin.fallback-name}   — last-resort Mojang name (default: Notch)</li>
     * </ul>
     */
    private static boolean v10to11(YamlConfiguration cfg) {
        boolean changed = false;
        if (!cfg.contains("skin.guaranteed-skin")) {
            cfg.set("skin.guaranteed-skin", true);
            log("v10→v11", "added skin.guaranteed-skin = true");
            changed = true;
        }
        if (!cfg.contains("skin.fallback-name")) {
            cfg.set("skin.fallback-name", "Notch");
            log("v10→v11", "added skin.fallback-name = Notch");
            changed = true;
        }
        return changed;
    }

    /**
     * v11 → v12: Config simplification pass.
     * <ul>
     *   <li>Added {@code head-ai.enabled} — explicit on/off toggle for head AI (default: true)</li>
     * </ul>
     */
    private static boolean v11to12(YamlConfiguration cfg) {
        if (cfg.contains("head-ai.enabled")) return false;
        cfg.set("head-ai.enabled", true);
        log("v11→v12", "added head-ai.enabled = true");
        return true;
    }

    /**
     * v12 → v13: Added metrics opt-out toggle.
     */
    private static boolean v12to13(YamlConfiguration cfg) {
        if (cfg.contains("metrics.enabled")) return false;
        cfg.set("metrics.enabled", true);
        log("v12→v13", "added metrics.enabled = true");
        return true;
    }

    /**
     * v13 → v14: Added spawn-cooldown and tab-list header/footer.
     * <ul>
     *   <li>Added {@code spawn-cooldown} — per-user spawn delay in seconds (default: 0)</li>
     *   <li>Added {@code tab-list.enabled} — tab-list header/footer toggle (default: false)</li>
     *   <li>Added {@code tab-list.update-interval}, {@code tab-list.header}, {@code tab-list.footer}</li>
     * </ul>
     */
    private static boolean v13to14(YamlConfiguration cfg) {
        boolean changed = false;
        if (!cfg.contains("spawn-cooldown")) {
            cfg.set("spawn-cooldown", 0);
            log("v13→v14", "added spawn-cooldown = 0");
            changed = true;
        }
        if (!cfg.contains("tab-list.enabled")) {
            cfg.set("tab-list.enabled", false);
            cfg.set("tab-list.update-interval", 40);
            cfg.set("tab-list.header", "");
            cfg.set("tab-list.footer", "");
            log("v13→v14", "added tab-list section");
            changed = true;
        }
        return changed;
    }

    /** v14 -> v15: Add database.enabled master toggle and messages.notify-admins-on-join default */
    private static boolean v14to15(YamlConfiguration cfg) {
        boolean changed = false;
        if (!cfg.contains("database.enabled")) {
            cfg.set("database.enabled", true);
            log("v14→v15", "added database.enabled = true");
            changed = true;
        }
        if (!cfg.contains("messages.notify-admins-on-join")) {
            cfg.set("messages.notify-admins-on-join", true);
            log("v14→v15", "added messages.notify-admins-on-join = true");
            changed = true;
        }
        return changed;
    }

    /** v15 -> v16: Add luckperms.weight-ordering-enabled default */
    private static boolean v15to16(YamlConfiguration cfg) {
        boolean changed = false;
        if (!cfg.contains("luckperms.weight-ordering-enabled")) {
            cfg.set("luckperms.weight-ordering-enabled", true);
            log("v15→v16", "added luckperms.weight-ordering-enabled = true");
            changed = true;
        }
        return changed;
    }

    /** v16 -> v17: Add luckperms.bot-group and luckperms.packet-prefix-char defaults */
    private static boolean v16to17(YamlConfiguration cfg) {
        boolean changed = false;
        if (!cfg.contains("luckperms.bot-group")) {
            cfg.set("luckperms.bot-group", "");
            log("v16→v17", "added luckperms.bot-group = ''");
            changed = true;
        }
        if (!cfg.contains("luckperms.packet-prefix-char")) {
            cfg.set("luckperms.packet-prefix-char", "{");
            log("v16→v17", "added luckperms.packet-prefix-char = '{'");
            changed = true;
        }
        return changed;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────
    private static void fillDefaults(FakePlayerPlugin plugin, YamlConfiguration cfg) {
        try (InputStream stream = plugin.getResource("config.yml")) {
            if (stream == null) return;
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream));
            cfg.setDefaults(defaults);
            cfg.options().copyDefaults(true);
        } catch (IOException e) {
            // Safe: does not use Config.cfg
            FppLogger.warn("ConfigMigrator.fillDefaults: " + e.getMessage());
        }
    }

    /**
     * Logs a migration step message.
     * Uses {@link #rawDebug} read from the raw YAML — never touches {@code Config.cfg}
     * because this runs before {@code Config.init()}.
     */
    private static void log(String step, String message) {
        if (rawDebug) {
            FppLogger.info("[ConfigMigrator][" + step + "] " + message);
        }
    }

    /** Single-arg variant used for non-step messages (e.g. "config is current"). */
    private static void log(String message) {
        if (rawDebug) {
            FppLogger.info("[ConfigMigrator] " + message);
        }
    }
}



