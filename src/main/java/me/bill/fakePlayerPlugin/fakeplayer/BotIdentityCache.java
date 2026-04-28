package me.bill.fakePlayerPlugin.fakeplayer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import me.bill.fakePlayerPlugin.FakePlayerPlugin;
import me.bill.fakePlayerPlugin.config.Config;
import me.bill.fakePlayerPlugin.database.DatabaseManager;
import me.bill.fakePlayerPlugin.util.BotDataYaml;
import me.bill.fakePlayerPlugin.util.FppLogger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public final class BotIdentityCache {

  private static final String YAML_FILE = "bot-identities.yml";
  private static final String ROOT = "identities.by-name";
  private static final String UUID_NAMESPACE = "FakePlayerPlugin:bot:";

  private final FakePlayerPlugin pluginRef;
  private final DatabaseManager db;
  private final File yamlFile;

  private final Map<String, UUID> cache = new ConcurrentHashMap<>();

  private YamlConfiguration yamlConfig = null;

  public BotIdentityCache(FakePlayerPlugin plugin, DatabaseManager db) {
    this.pluginRef = plugin;
    this.db = db;
    this.yamlFile = new File(new File(plugin.getDataFolder(), "data"), YAML_FILE);

    if (db == null) {
      loadYaml();
      migrateLegacyYamlMappings();
    } else {
      migrateLegacyDbMappings();
    }
  }

  public UUID lookupOrCreate(String botName) {
    String key = normalizeKey(botName);

    UUID cached = cache.get(key);
    if (cached != null) return cached;

    UUID resolved = db != null ? lookupOrCreateDb(botName) : lookupOrCreateYaml(botName, key);
    cache.put(key, resolved);
    return resolved;
  }

  public void prime(String botName, UUID uuid) {
    if (botName == null || uuid == null) return;
    cache.put(normalizeKey(botName), uuid);
  }

  public static UUID deterministicBotUuid(String botName) {
    return UUID.nameUUIDFromBytes(
        (UUID_NAMESPACE + normalizeKey(botName)).getBytes(StandardCharsets.UTF_8));
  }

  private UUID lookupOrCreateDb(String botName) {
    String serverId = Config.serverId();
    UUID safeUuid = deterministicBotUuid(botName);

    UUID fromDb = db.lookupBotUuid(botName, serverId);
    if (fromDb != null) {
      if (!safeUuid.equals(fromDb)) {
        db.migrateBotUuid(botName, serverId, fromDb, safeUuid);
        Config.debugDatabase(
            "BotIdentityCache: migrated legacy DB UUID for '" + botName + "' → " + safeUuid);
      } else {
        Config.debugDatabase("BotIdentityCache: DB hit for '" + botName + "' → " + fromDb);
      }
      return safeUuid;
    }

    db.registerBotUuid(botName, safeUuid, serverId);
    Config.debugDatabase("BotIdentityCache: new identity for '" + botName + "' → " + safeUuid);
    return safeUuid;
  }

  private void loadYaml() {
    yamlConfig = BotDataYaml.load(pluginRef);
    ConfigurationSection root = yamlConfig.getConfigurationSection(ROOT);
    if (root == null && yamlFile.exists()) {
      YamlConfiguration legacy = YamlConfiguration.loadConfiguration(yamlFile);
      if (!legacy.getKeys(false).isEmpty()) {
        root = legacy;
        try {
          BotDataYaml.replaceSection(
              pluginRef,
              ROOT,
              section -> {
                for (String key : legacy.getKeys(false)) {
                  section.set(key, legacy.getString(key));
                }
              });
          if (yamlFile.exists()) yamlFile.delete();
          yamlConfig = BotDataYaml.load(pluginRef);
          root = yamlConfig.getConfigurationSection(ROOT);
        } catch (IOException e) {
          FppLogger.warn("BotIdentityCache: failed to migrate " + YAML_FILE + ": " + e.getMessage());
        }
      }
    }
    if (root == null) {
      yamlConfig = new YamlConfiguration();
      Config.debugDatabase("BotIdentityCache: no YAML data yet - will create on first spawn.");
      return;
    }
    int loaded = 0;
    for (String key : root.getKeys(false)) {
      String val = root.getString(key);
      if (val == null || val.isBlank()) continue;
      try {
        cache.put(normalizeKey(key), UUID.fromString(val));
        loaded++;
      } catch (IllegalArgumentException e) {
        FppLogger.warn("BotIdentityCache: skipping malformed entry '" + key + "': " + val);
      }
    }
    if (loaded > 0) {
      FppLogger.info(
          "BotIdentityCache: loaded " + loaded + " name→UUID mapping(s) from " + YAML_FILE + ".");
    }
  }

  private UUID lookupOrCreateYaml(String botName, String cacheKey) {
    if (yamlConfig == null) yamlConfig = new YamlConfiguration();

    UUID safeUuid = deterministicBotUuid(botName);
    String stored = yamlConfig.getString(ROOT + "." + cacheKey);
    if (stored != null && !stored.isBlank()) {
      try {
        UUID fromYaml = UUID.fromString(stored);
        if (!safeUuid.equals(fromYaml)) {
          yamlConfig.set(ROOT + "." + cacheKey, safeUuid.toString());
          saveYaml();
          Config.debugDatabase(
              "BotIdentityCache: migrated legacy YAML UUID for '" + botName + "' → " + safeUuid);
        } else {
          Config.debugDatabase("BotIdentityCache: YAML hit for '" + botName + "' → " + fromYaml);
        }
        return safeUuid;
      } catch (IllegalArgumentException e) {
        FppLogger.warn(
            "BotIdentityCache: malformed YAML entry for '" + botName + "' - regenerating UUID.");
      }
    }

    yamlConfig.set(ROOT + "." + cacheKey, safeUuid.toString());
    saveYaml();
    Config.debugDatabase("BotIdentityCache: new YAML identity for '" + botName + "' → " + safeUuid);
    return safeUuid;
  }

  private void migrateLegacyDbMappings() {
    if (db == null) return;
    int migrated = 0;
    for (DatabaseManager.BotIdentityRow row : db.getBotIdentityRows()) {
      if (row == null || row.botName() == null || row.botName().isBlank()) continue;
      UUID target = deterministicBotUuid(row.botName());
      UUID current;
      try {
        current = UUID.fromString(row.botUuid());
      } catch (Exception e) {
        cache.put(normalizeKey(row.botName()), target);
        FppLogger.warn(
            "BotIdentityCache: malformed DB UUID for '"
                + row.botName()
                + "' on server '"
                + row.serverId()
                + "' - using bot namespace UUID "
                + target
                + " for this runtime.");
        continue;
      }
      cache.put(normalizeKey(row.botName()), target);
      if (!target.equals(current)
          && db.migrateBotUuid(row.botName(), row.serverId(), current, target)) {
        migrated++;
      }
    }
    if (migrated > 0) {
      FppLogger.info(
          "BotIdentityCache: migrated " + migrated + " legacy bot UUID mapping(s) to the bot namespace.");
    }
  }

  private void migrateLegacyYamlMappings() {
    if (yamlConfig == null) yamlConfig = new YamlConfiguration();
    boolean changed = false;
    int migrated = 0;
    for (Map.Entry<String, UUID> entry : Map.copyOf(cache).entrySet()) {
      UUID target = deterministicBotUuid(entry.getKey());
      cache.put(entry.getKey(), target);
      if (!target.equals(entry.getValue())) {
        yamlConfig.set(ROOT + "." + entry.getKey(), target.toString());
        changed = true;
        migrated++;
      }
    }
    if (changed) {
      saveYaml();
      FppLogger.info(
          "BotIdentityCache: migrated " + migrated + " legacy YAML bot UUID mapping(s) to the bot namespace.");
    }
  }

  private void saveYaml() {
    try {
      File parent = yamlFile.getParentFile();
      if (parent != null && !parent.exists()) parent.mkdirs();
      BotDataYaml.save(pluginRef, yamlConfig);
      if (yamlFile.exists()) yamlFile.delete();
    } catch (IOException e) {
      FppLogger.warn("BotIdentityCache: failed to save " + YAML_FILE + ": " + e.getMessage());
    }
  }

  private static String normalizeKey(String botName) {
    return botName == null ? "" : botName.toLowerCase(Locale.ROOT);
  }
}
