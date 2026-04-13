package me.bill.fakePlayerPlugin.fakeplayer;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import me.bill.fakePlayerPlugin.config.Config;
import me.bill.fakePlayerPlugin.util.FppLogger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public final class SkinRepository {

  private static SkinRepository INSTANCE;

  public static synchronized SkinRepository get() {
    if (INSTANCE == null) INSTANCE = new SkinRepository();
    return INSTANCE;
  }

  private SkinRepository() {}

  private final List<SkinProfile> folderSkins = new CopyOnWriteArrayList<>();

  private final Map<String, SkinProfile> namedOverrides = new ConcurrentHashMap<>();

  private final List<SkinProfile> poolSkins = new CopyOnWriteArrayList<>();

  private final List<SkinProfile> fallbackPoolSkins = new CopyOnWriteArrayList<>();

  private final Map<String, SkinProfile> sessionCache = new ConcurrentHashMap<>();

  private volatile SkinProfile fallbackSkin = null;

  private Plugin plugin;

  public synchronized void init(Plugin plugin) {
    this.plugin = plugin;
    reload();
  }

  public synchronized void reload() {
    folderSkins.clear();
    namedOverrides.clear();
    poolSkins.clear();
    fallbackPoolSkins.clear();
    sessionCache.clear();
    fallbackSkin = null;

    if (Config.skinClearCacheOnReload()) {
      SkinFetcher.clearCache();
    }

    String mode = Config.skinMode();

    if ("off".equals(mode) || "disabled".equals(mode)) {
      FppLogger.debug("SkinRepository: mode=off - repository not loaded.");
      return;
    }

    if ("auto".equals(mode)) {

      if (Config.skinGuaranteed()) {
        loadFallbackPool();
        loadFallbackSkin();
        FppLogger.debug(
            "SkinRepository: mode=auto + guaranteed-skin=true - fallback pool + fallback skin"
                + " queued.");
      } else {
        FppLogger.debug("SkinRepository: mode=auto - on-demand skin resolution enabled.");
      }
      return;
    }

    if (Config.skinUseSkinFolder()) {
      loadFolderSkins();
    }
    loadConfigPool();
    if (Config.skinGuaranteed()) {
      loadFallbackSkin();
    }
    FppLogger.debug(
        "SkinRepository: loaded "
            + folderSkins.size()
            + " folder skin(s), "
            + poolSkins.size()
            + " pool skin(s), "
            + namedOverrides.size()
            + " named override(s).");
  }

  public void resolve(
      String botName,
      java.util.function.Consumer<@org.jetbrains.annotations.Nullable SkinProfile> callback) {
    if (botName == null || botName.isBlank()) {
      deliver(callback, null);
      return;
    }

    String normalizedBotName = botName.trim();
    String mode = Config.skinMode();

    if ("off".equals(mode) || "disabled".equals(mode)) {
      deliver(callback, null);
      return;
    }

    String cacheKey = buildCacheKey(mode, normalizedBotName);
    SkinProfile cached = sessionCache.get(cacheKey);
    if (cached != null) {
      deliver(callback, cached);
      return;
    }

    Consumer<SkinProfile> finish =
        profile -> {
          if (profile != null && profile.isValid()) {
            sessionCache.put(cacheKey, profile);
          }
          deliver(callback, profile);
        };

    if ("auto".equals(mode)) {
      resolveAuto(normalizedBotName, finish);
      return;
    }

    resolveCustom(normalizedBotName, finish);
  }

  private void resolveAuto(String botName, Consumer<SkinProfile> callback) {
    FppLogger.debug(
        "SkinRepository: auto-resolving signed skin for '" + botName + "' via mineskin.eu.");
    SkinFetcher.fetchAsync(
        botName,
        (value, sig) -> {
          if (value != null && !value.isBlank()) {
            callback.accept(new SkinProfile(value, sig, "auto:" + botName));
          } else if (Config.skinGuaranteed()) {
            FppLogger.debug(
                "SkinRepository: auto lookup failed for '"
                    + botName
                    + "' - using guaranteed skin.");
            getAnyValidSkin(callback);
          } else {
            callback.accept(null);
          }
        });
  }

  private void resolveCustom(String botName, Consumer<SkinProfile> callback) {

    SkinProfile override = namedOverrides.get(botName.toLowerCase());
    if (override != null && override.isValid()) {
      FppLogger.debug(
          "SkinRepository: name-override hit for '" + botName + "' → " + override.getSource());
      callback.accept(override);
      return;
    }

    SkinProfile fileExact = findExactFileMatch(botName);
    if (fileExact != null && fileExact.isValid()) {
      FppLogger.debug(
          "SkinRepository: exact-file match for '" + botName + "' → " + fileExact.getSource());
      callback.accept(fileExact);
      return;
    }

    if (!folderSkins.isEmpty()) {
      SkinProfile random = folderSkins.get(ThreadLocalRandom.current().nextInt(folderSkins.size()));
      FppLogger.debug(
          "SkinRepository: random folder skin for '" + botName + "' → " + random.getSource());
      callback.accept(random);
      return;
    }

    if (!poolSkins.isEmpty()) {
      SkinProfile random = poolSkins.get(ThreadLocalRandom.current().nextInt(poolSkins.size()));
      FppLogger.debug(
          "SkinRepository: random pool skin for '" + botName + "' → " + random.getSource());
      callback.accept(random);
      return;
    }

    FppLogger.debug("SkinRepository: falling back to skin API fetch for '" + botName + "'.");
    SkinFetcher.fetchAsync(
        botName,
        (value, sig) -> {
          if (value != null) {
            SkinProfile p = new SkinProfile(value, sig, "name:" + botName);
            callback.accept(p);
          } else if (Config.skinGuaranteed()) {

            FppLogger.debug(
                "SkinRepository: API fallback failed for '"
                    + botName
                    + "' - using guaranteed skin.");
            getAnyValidSkin(callback);
          } else {
            callback.accept(null);
          }
        });
  }

  private void loadFolderSkins() {
    if (plugin == null) return;
    File skinsDir = new File(plugin.getDataFolder(), "skins");
    if (!skinsDir.exists()) {
      boolean created = skinsDir.mkdirs();
      FppLogger.debug(
          "SkinRepository: "
              + (created ? "created" : "could not create")
              + " skins folder at "
              + skinsDir.getPath());
      return;
    }

    File[] pngFiles =
        skinsDir.listFiles(
            (dir, name) ->
                name.toLowerCase().endsWith(".png") || name.toLowerCase().endsWith(".skin"));
    if (pngFiles == null || pngFiles.length == 0) {
      FppLogger.debug("SkinRepository: skins folder is empty.");
      return;
    }

    for (File file : pngFiles) {
      try {
        byte[] bytes = Files.readAllBytes(file.toPath());
        String base64 = Base64.getEncoder().encodeToString(bytes);

        String textureUrl = "data:image/png;base64," + base64;
        String textureJson = buildTextureJson(textureUrl);
        String textureValue =
            Base64.getEncoder().encodeToString(textureJson.getBytes(StandardCharsets.UTF_8));

        String displayName = file.getName().replaceAll("\\.(png|skin)$", "");
        SkinProfile profile = new SkinProfile(textureValue, null, "file:" + file.getName());

        folderSkins.add(profile);

        namedOverrides.put(displayName.toLowerCase(), profile);

        FppLogger.debug("SkinRepository: loaded skin file '" + file.getName() + "'.");
      } catch (Exception e) {
        FppLogger.warn(
            "SkinRepository: failed to load '" + file.getName() + "': " + e.getMessage());
      }
    }
  }

  private static String buildTextureJson(String skinUrl) {
    return "{\"textures\":{\"SKIN\":{\"url\":\"" + skinUrl + "\"}}}";
  }

  private SkinProfile findExactFileMatch(String botName) {
    String target = "file:" + botName.toLowerCase() + ".png";
    String target2 = "file:" + botName.toLowerCase() + ".skin";
    for (SkinProfile p : folderSkins) {
      String src = p.getSource().toLowerCase();
      if (src.equals(target) || src.equals(target2)) return p;
    }
    return null;
  }

  private void loadConfigPool() {

    List<String> pool = Config.skinCustomPool();
    for (String entry : pool) {
      if (entry == null || entry.isBlank()) continue;
      entry = entry.trim();

      if (isUrl(entry)) {

        loadFromUrl(entry);
      } else {

        loadFromName(entry, null);
      }
    }

    Map<String, String> byName = Config.skinCustomByName();
    for (Map.Entry<String, String> e : byName.entrySet()) {
      String botName = e.getKey().toLowerCase().trim();
      String skinSrc = e.getValue() != null ? e.getValue().trim() : "";
      if (skinSrc.isEmpty()) continue;

      if (isUrl(skinSrc)) {
        loadFromUrlForName(botName, skinSrc);
      } else {
        loadFromName(skinSrc, botName);
      }
    }
  }

  private static boolean isUrl(String s) {
    return s.startsWith("http://") || s.startsWith("https://");
  }

  private void loadFromName(String playerName, String forBotName) {
    SkinFetcher.fetchAsync(
        playerName,
        (value, sig) -> {
          if (value == null) {
            FppLogger.debug("SkinRepository: no skin found for pool entry '" + playerName + "'.");
            return;
          }
          SkinProfile p = new SkinProfile(value, sig, "name:" + playerName);
          if (forBotName != null) {
            namedOverrides.put(forBotName, p);
            FppLogger.debug(
                "SkinRepository: loaded name-override '" + forBotName + "' → " + playerName + ".");
          } else {
            poolSkins.add(p);
            FppLogger.debug("SkinRepository: added pool skin from '" + playerName + "'.");
          }
        });
  }

  private void loadFromUrl(String url) {
    SkinFetcher.fetchByUrl(
        url,
        (value, sig) -> {
          if (value == null) {
            FppLogger.debug("SkinRepository: no skin from URL '" + url + "'.");
            return;
          }
          SkinProfile p = new SkinProfile(value, sig, "url:" + url);
          poolSkins.add(p);
          FppLogger.debug("SkinRepository: added pool skin from URL.");
        });
  }

  private void loadFromUrlForName(String botName, String url) {
    SkinFetcher.fetchByUrl(
        url,
        (value, sig) -> {
          if (value == null) {
            FppLogger.debug("SkinRepository: no skin from URL for '" + botName + "'.");
            return;
          }
          SkinProfile p = new SkinProfile(value, sig, "url:" + url);
          namedOverrides.put(botName, p);
          FppLogger.debug("SkinRepository: loaded URL override for '" + botName + "'.");
        });
  }

  private void loadFallbackPool() {
    List<String> names = Config.skinFallbackPool();
    if (names.isEmpty()) {
      FppLogger.debug("SkinRepository: fallback-pool is empty - will use single fallback-name.");
      return;
    }

    FppLogger.debug("SkinRepository: loading " + names.size() + " fallback-pool skins...");
    for (String name : names) {
      if (name == null || name.isBlank()) continue;
      SkinFetcher.fetchAsync(
          name,
          (value, sig) -> {
            if (value != null && !value.isBlank()) {
              SkinProfile p = new SkinProfile(value, sig, "fallback-pool:" + name);
              fallbackPoolSkins.add(p);
              FppLogger.debug(
                  "SkinRepository: fallback-pool skin '"
                      + name
                      + "' loaded ("
                      + fallbackPoolSkins.size()
                      + "/"
                      + names.size()
                      + ").");
            } else {
              FppLogger.debug(
                  "SkinRepository: fallback-pool skin '"
                      + name
                      + "' not found (name may not exist).");
            }
          });
    }
  }

  private void loadFallbackSkin() {
    String name = Config.skinFallbackName().trim();
    if (name.isBlank()) return;
    SkinFetcher.fetchAsync(
        name,
        (value, sig) -> {
          if (value != null && !value.isBlank()) {
            fallbackSkin = new SkinProfile(value, sig, "fallback:" + name);
            FppLogger.debug("SkinRepository: fallback skin '" + name + "' pre-loaded.");
          } else {
            FppLogger.warn(
                "SkinRepository: fallback skin '"
                    + name
                    + "' failed to fetch - bots using generated names may appear as Steve.");
          }
        });
  }

  public void getAnyValidSkin(Consumer<SkinProfile> callback) {

    if (!folderSkins.isEmpty()) {
      SkinProfile p = folderSkins.get(ThreadLocalRandom.current().nextInt(folderSkins.size()));
      FppLogger.debug("SkinRepository: guaranteed-skin → folder skin (" + p.getSource() + ").");
      callback.accept(p);
      return;
    }

    if (!poolSkins.isEmpty()) {
      SkinProfile p = poolSkins.get(ThreadLocalRandom.current().nextInt(poolSkins.size()));
      FppLogger.debug("SkinRepository: guaranteed-skin → pool skin (" + p.getSource() + ").");
      callback.accept(p);
      return;
    }

    if (!fallbackPoolSkins.isEmpty()) {
      SkinProfile p =
          fallbackPoolSkins.get(ThreadLocalRandom.current().nextInt(fallbackPoolSkins.size()));
      FppLogger.debug(
          "SkinRepository: guaranteed-skin → fallback-pool skin (" + p.getSource() + ").");
      callback.accept(p);
      return;
    }

    List<String> poolConfig = Config.skinFallbackPool();
    if (poolConfig != null && !poolConfig.isEmpty()) {
      String randomName = poolConfig.get(ThreadLocalRandom.current().nextInt(poolConfig.size()));
      if (randomName != null && !randomName.isBlank()) {
        FppLogger.debug(
            "SkinRepository: guaranteed-skin → on-demand random fallback-pool fetch for '"
                + randomName
                + "'.");
        SkinFetcher.fetchAsync(
            randomName,
            (value, sig) -> {
              if (value != null && !value.isBlank()) {
                SkinProfile p = new SkinProfile(value, sig, "fallback-pool-ondemand:" + randomName);

                fallbackPoolSkins.add(p);
                FppLogger.debug(
                    "SkinRepository: on-demand fallback-pool skin '"
                        + randomName
                        + "' fetched and cached.");
                callback.accept(p);
              } else {

                FppLogger.debug(
                    "SkinRepository: on-demand fallback-pool fetch failed for '"
                        + randomName
                        + "' - trying fallback-name.");
                fetchSingleFallback(callback);
              }
            });
        return;
      }
    }

    if (fallbackSkin != null && fallbackSkin.isValid()) {
      FppLogger.debug(
          "SkinRepository: guaranteed-skin → pre-loaded fallback ("
              + fallbackSkin.getSource()
              + ").");
      callback.accept(fallbackSkin);
      return;
    }

    fetchSingleFallback(callback);
  }

  private void fetchSingleFallback(Consumer<SkinProfile> callback) {
    String name = Config.skinFallbackName().trim();
    if (!name.isBlank()) {
      FppLogger.debug(
          "SkinRepository: guaranteed-skin → on-demand fallback fetch for '" + name + "'.");
      SkinFetcher.fetchAsync(
          name,
          (value, sig) -> {
            if (value != null && !value.isBlank()) {
              SkinProfile p = new SkinProfile(value, sig, "fallback:" + name);
              fallbackSkin = p;
              FppLogger.debug("SkinRepository: on-demand fallback fetched for '" + name + "'.");
              callback.accept(p);
            } else {
              FppLogger.warn(
                  "SkinRepository: on-demand fallback fetch also failed for '"
                      + name
                      + "' - bot will use Steve.");
              callback.accept(null);
            }
          });
    } else {
      callback.accept(null);
    }
  }

  public int getFolderSkinCount() {
    return folderSkins.size();
  }

  public int getPoolSkinCount() {
    return poolSkins.size();
  }

  public int getOverrideCount() {
    return namedOverrides.size();
  }

  public int getCacheSize() {
    return sessionCache.size();
  }

  public void clearSessionCache() {
    sessionCache.clear();
  }

  public @org.jetbrains.annotations.Nullable SkinProfile getSessionCached(String botName) {
    if (botName == null || botName.isBlank()) return null;
    return sessionCache.get(
        buildCacheKey(Config.skinMode(), botName.trim().toLowerCase(Locale.ROOT)));
  }

  private String buildCacheKey(String mode, String botName) {
    return mode.toLowerCase(Locale.ROOT) + ":" + botName.toLowerCase(Locale.ROOT);
  }

  private void deliver(
      Consumer<@org.jetbrains.annotations.Nullable SkinProfile> callback,
      @org.jetbrains.annotations.Nullable SkinProfile profile) {

    Plugin effectivePlugin =
        (plugin != null) ? plugin : me.bill.fakePlayerPlugin.FakePlayerPlugin.getInstance();
    if (effectivePlugin != null && effectivePlugin.isEnabled() && !Bukkit.isPrimaryThread()) {
      Bukkit.getScheduler().runTask(effectivePlugin, () -> callback.accept(profile));
      return;
    }
    callback.accept(profile);
  }
}
