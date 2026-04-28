package me.bill.fakePlayerPlugin.fakeplayer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
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
  private static final String USER_AGENT = "FakePlayerPlugin/1.6.0";
  private static final int MOJANG_CONNECT_TIMEOUT_MS = 1000;
  private static final int MOJANG_READ_TIMEOUT_MS = 1000;
  private static final long LOOKUP_FAIL_COOLDOWN_MS = 60_000L;
  private static final long LOOKUP_RATE_LIMIT_COOLDOWN_MS = 300_000L;

  private static volatile long skipMojangLookupUntilMs = 0L;

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
    }
  }

  public UUID lookupOrCreate(String botName) {
    String key = botName.toLowerCase();

    UUID cached = cache.get(key);
    if (cached != null) return cached;

    UUID resolved;
    if (db != null) {
      resolved = lookupOrCreateDb(botName);
    } else {
      resolved = lookupOrCreateYaml(botName, key);
    }

    cache.put(key, resolved);
    return resolved;
  }

  public void prime(String botName, UUID uuid) {
    if (botName == null || uuid == null) return;
    cache.put(botName.toLowerCase(), uuid);
  }

  private UUID lookupOrCreateDb(String botName) {
    String serverId = Config.serverId();

    UUID fromDb = db.lookupBotUuid(botName, serverId);
    if (fromDb != null) {
      Config.debugDatabase("BotIdentityCache: DB hit for '" + botName + "' → " + fromDb);
      return fromDb;
    }

    UUID fresh = resolvePreferredUuid(botName);
    db.registerBotUuid(botName, fresh, serverId);
    Config.debugDatabase("BotIdentityCache: new identity for '" + botName + "' → " + fresh);
    return fresh;
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
        cache.put(key, UUID.fromString(val));
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

    String stored = yamlConfig.getString(ROOT + "." + cacheKey);
    if (stored != null && !stored.isBlank()) {
      try {
        UUID fromYaml = UUID.fromString(stored);
        Config.debugDatabase("BotIdentityCache: YAML hit for '" + botName + "' → " + fromYaml);
        return fromYaml;
      } catch (IllegalArgumentException e) {
        FppLogger.warn(
            "BotIdentityCache: malformed YAML entry for '" + botName + "' - regenerating UUID.");
      }
    }

    UUID fresh = resolvePreferredUuid(botName);
    yamlConfig.set(ROOT + "." + cacheKey, fresh.toString());
    saveYaml();
    Config.debugDatabase("BotIdentityCache: new YAML identity for '" + botName + "' → " + fresh);
    return fresh;
  }

  private UUID resolvePreferredUuid(String botName) {
    UUID premium = fetchPremiumUuid(botName);
    if (premium != null) {
      Config.debugDatabase("BotIdentityCache: premium UUID for '" + botName + "' → " + premium);
      return premium;
    }
    UUID fallback = deterministicOfflineUuid(botName);
    Config.debugDatabase(
        "BotIdentityCache: premium UUID unavailable for '" + botName + "', using deterministic fallback → "
            + fallback);
    return fallback;
  }

  public static UUID deterministicOfflineUuid(String botName) {
    return UUID.nameUUIDFromBytes(("OfflinePlayer:" + botName).getBytes(StandardCharsets.UTF_8));
  }

  private UUID fetchPremiumUuid(String botName) {
    long now = System.currentTimeMillis();
    if (now < skipMojangLookupUntilMs) return null;

    HttpURLConnection connection = null;
    try {
      URI uri = URI.create("https://api.mojang.com/users/profiles/minecraft/" + botName);
      connection = (HttpURLConnection) uri.toURL().openConnection();
      connection.setRequestMethod("GET");
      connection.setConnectTimeout(MOJANG_CONNECT_TIMEOUT_MS);
      connection.setReadTimeout(MOJANG_READ_TIMEOUT_MS);
      connection.setRequestProperty("User-Agent", USER_AGENT);
      connection.setRequestProperty("Accept", "application/json");

      int code = connection.getResponseCode();
      if (code == HttpURLConnection.HTTP_NOT_FOUND || code == HttpURLConnection.HTTP_NO_CONTENT) {
        return null;
      }
      if (code == 429) {
        skipMojangLookupUntilMs = System.currentTimeMillis() + LOOKUP_RATE_LIMIT_COOLDOWN_MS;
        return null;
      }
      if (code != HttpURLConnection.HTTP_OK) {
        skipMojangLookupUntilMs = System.currentTimeMillis() + LOOKUP_FAIL_COOLDOWN_MS;
        Config.debugDatabase(
            "BotIdentityCache: Mojang UUID lookup for '" + botName + "' returned HTTP " + code);
        return null;
      }

      try (Reader reader =
          new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)) {
        JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
        if (!root.has("id")) return null;
        String raw = root.get("id").getAsString();
        return parseMojangUuid(raw);
      }
    } catch (Exception e) {
      Config.debugDatabase(
          "BotIdentityCache: Mojang UUID lookup failed for '" + botName + "': " + e.getMessage());
      skipMojangLookupUntilMs = System.currentTimeMillis() + LOOKUP_FAIL_COOLDOWN_MS;
      return null;
    } finally {
      if (connection != null) connection.disconnect();
    }
  }

  private UUID parseMojangUuid(String raw) {
    if (raw == null) return null;
    String hex = raw.replace("-", "").trim();
    if (hex.length() != 32) return null;
    String dashed =
        hex.substring(0, 8)
            + "-"
            + hex.substring(8, 12)
            + "-"
            + hex.substring(12, 16)
            + "-"
            + hex.substring(16, 20)
            + "-"
            + hex.substring(20);
    try {
      return UUID.fromString(dashed);
    } catch (IllegalArgumentException ignored) {
      return null;
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
}
