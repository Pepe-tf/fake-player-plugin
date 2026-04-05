package me.bill.fakePlayerPlugin.fakeplayer;

import me.bill.fakePlayerPlugin.FakePlayerPlugin;
import me.bill.fakePlayerPlugin.config.Config;
import me.bill.fakePlayerPlugin.database.DatabaseManager;
import me.bill.fakePlayerPlugin.util.FppLogger;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persistent identity registry that maps a bot name (scoped to this server) to a
 * stable UUID.
 *
 * <p>When a bot with a given name is first spawned, a random UUID is generated and
 * stored.  On every subsequent spawn with the same name on the same server, the same
 * UUID is returned.  This keeps LuckPerms group assignments, inventory data, and
 * database session history all linked to one consistent identity — bots never look
 * like "new players" on server restart.
 *
 * <h3>Storage priority</h3>
 * <ol>
 *   <li><b>In-memory cache</b> — populated at startup and on every first-use; always
 *       consulted first so lookups are O(1) and non-blocking.</li>
 *   <li><b>Database table {@code fpp_bot_identities}</b> — primary persistent store
 *       when the database is enabled.  Synchronous reads are used only during spawns,
 *       which are infrequent enough that the latency is negligible.</li>
 *   <li><b>YAML file {@code data/bot-identities.yml}</b> — fallback when the database
 *       is disabled; auto-created on first use and updated whenever a new mapping is
 *       created.</li>
 * </ol>
 *
 * <h3>Identity scope</h3>
 * Each mapping is scoped to the current {@code database.server-id} (read from
 * {@link Config#serverId()}).  The same bot name on two different servers produces
 * two independent UUIDs — correct behaviour for proxy setups.  The YAML fallback is
 * per-installation (one file per server folder) so it is naturally scoped.
 *
 * <h3>Thread safety</h3>
 * The in-memory cache is a {@link ConcurrentHashMap}.  The YAML file is written only
 * from the main thread.  Database reads are synchronous main-thread calls; writes are
 * enqueued to the DB write thread via {@link DatabaseManager#registerBotUuid}.
 */
public final class BotIdentityCache {

    private static final String YAML_FILE = "bot-identities.yml";

    private final DatabaseManager  db;         // may be null
    private final File             yamlFile;

    /**
     * In-memory cache keyed by lower-case bot name.
     * Server scope is implicit — one cache instance per running server.
     */
    private final Map<String, UUID> cache = new ConcurrentHashMap<>();

    /** Lazily-loaded YAML config used when {@link #db} is {@code null}. */
    private YamlConfiguration yamlConfig = null;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /**
     * @param plugin the FPP plugin instance
     * @param db     the active {@link DatabaseManager}, or {@code null} if the
     *               database is disabled — the YAML fallback will be used instead
     */
    public BotIdentityCache(FakePlayerPlugin plugin, DatabaseManager db) {
        this.db       = db;
        this.yamlFile = new File(new File(plugin.getDataFolder(), "data"), YAML_FILE);

        if (db == null) {
            // Pre-load the YAML now so lookups don't require disk I/O later
            loadYaml();
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Returns the stored UUID for {@code botName} on the current server.
     * If no mapping exists yet, a new random UUID is generated, persisted, and returned.
     *
     * <p><b>Must be called on the main thread</b> — the DB read path is synchronous.
     *
     * @param botName the bot's Minecraft username (exact case as used in the
     *                {@link com.destroystokyo.paper.profile.PlayerProfile})
     * @return the stable UUID for this bot name + server combination
     */
    public UUID lookupOrCreate(String botName) {
        String key = botName.toLowerCase();

        // 1. Hot path — in-memory cache
        UUID cached = cache.get(key);
        if (cached != null) return cached;

        // 2. Persistent store
        UUID resolved;
        if (db != null) {
            resolved = lookupOrCreateDb(botName);
        } else {
            resolved = lookupOrCreateYaml(botName, key);
        }

        cache.put(key, resolved);
        return resolved;
    }

    /**
     * Pre-populates the in-memory cache with a known mapping.
     *
     * <p>Called from {@link FakePlayerManager#spawnRestored} so that bots restored
     * from persistence (which already have a stored UUID) are remembered for the
     * lifetime of this session without hitting the DB a second time.
     * Also called when a fresh spawn registers a new UUID so that subsequent
     * lookups for the same name are instant.
     *
     * @param botName the bot's exact Minecraft username
     * @param uuid    the UUID to associate with that name on this server
     */
    public void prime(String botName, UUID uuid) {
        if (botName == null || uuid == null) return;
        cache.put(botName.toLowerCase(), uuid);
    }

    // ── Database path ─────────────────────────────────────────────────────────

    private UUID lookupOrCreateDb(String botName) {
        String serverId = Config.serverId();

        // Synchronous DB read — fast primary-key lookup, safe on main thread for spawns
        UUID fromDb = db.lookupBotUuid(botName, serverId);
        if (fromDb != null) {
            Config.debugDatabase("BotIdentityCache: DB hit for '" + botName + "' → " + fromDb);
            return fromDb;
        }

        // Not found — create, persist, return
        UUID fresh = UUID.randomUUID();
        db.registerBotUuid(botName, fresh, serverId);
        Config.debugDatabase("BotIdentityCache: new identity for '" + botName + "' → " + fresh);
        return fresh;
    }

    // ── YAML fallback path ────────────────────────────────────────────────────

    /**
     * Loads {@code data/bot-identities.yml} into memory.
     * Called once at construction time when the DB is disabled.
     */
    private void loadYaml() {
        if (!yamlFile.exists()) {
            yamlConfig = new YamlConfiguration();
            Config.debugDatabase("BotIdentityCache: no YAML file yet — will create on first spawn.");
            return;
        }
        yamlConfig = YamlConfiguration.loadConfiguration(yamlFile);
        int loaded = 0;
        for (String key : yamlConfig.getKeys(false)) {
            String val = yamlConfig.getString(key);
            if (val == null || val.isBlank()) continue;
            try {
                cache.put(key, UUID.fromString(val));
                loaded++;
            } catch (IllegalArgumentException e) {
                FppLogger.warn("BotIdentityCache: skipping malformed entry '" + key + "': " + val);
            }
        }
        if (loaded > 0) {
            FppLogger.info("BotIdentityCache: loaded " + loaded + " name→UUID mapping(s) from "
                    + YAML_FILE + ".");
        }
    }

    private UUID lookupOrCreateYaml(String botName, String cacheKey) {
        if (yamlConfig == null) yamlConfig = new YamlConfiguration();

        String stored = yamlConfig.getString(cacheKey);
        if (stored != null && !stored.isBlank()) {
            try {
                UUID fromYaml = UUID.fromString(stored);
                Config.debugDatabase("BotIdentityCache: YAML hit for '" + botName + "' → " + fromYaml);
                return fromYaml;
            } catch (IllegalArgumentException e) {
                FppLogger.warn("BotIdentityCache: malformed YAML entry for '" + botName + "' — regenerating UUID.");
            }
        }

        UUID fresh = UUID.randomUUID();
        yamlConfig.set(cacheKey, fresh.toString());
        saveYaml();
        Config.debugDatabase("BotIdentityCache: new YAML identity for '" + botName + "' → " + fresh);
        return fresh;
    }

    private void saveYaml() {
        try {
            // Ensure parent directory exists
            File parent = yamlFile.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            yamlConfig.save(yamlFile);
        } catch (IOException e) {
            FppLogger.warn("BotIdentityCache: failed to save " + YAML_FILE + ": " + e.getMessage());
        }
    }
}





