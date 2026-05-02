package me.bill.fakePlayerPlugin.extension;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import me.bill.fakePlayerPlugin.FakePlayerPlugin;
import me.bill.fakePlayerPlugin.api.FppAddon;
import me.bill.fakePlayerPlugin.api.FppApi;
import me.bill.fakePlayerPlugin.api.FppExtension;
import me.bill.fakePlayerPlugin.util.FppLogger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ExtensionLoader {

  private static final class ExtensionContext {
    final File dataFolder;
    final URLClassLoader classLoader;
    volatile YamlConfiguration config;

    ExtensionContext(File dataFolder, URLClassLoader classLoader) {
      this.dataFolder = dataFolder;
      this.classLoader = classLoader;
    }
  }

  private static final ConcurrentHashMap<FppExtension, ExtensionContext> EXTENSIONS =
      new ConcurrentHashMap<>();

  private final FakePlayerPlugin plugin;
  private final List<URLClassLoader> classLoaders = new ArrayList<>();
  private final List<ExtensionAddonWrapper> activeWrappers = new ArrayList<>();

  public ExtensionLoader(@NotNull FakePlayerPlugin plugin) {
    this.plugin = plugin;
  }

  // ── Static helpers called by FppExtension default methods ──────────────────

  public static @Nullable File getDataFolder(@NotNull FppExtension ext) {
    ExtensionContext ctx = EXTENSIONS.get(ext);
    return ctx != null ? ctx.dataFolder : null;
  }

  public static @NotNull YamlConfiguration getConfig(@NotNull FppExtension ext) {
    ExtensionContext ctx = EXTENSIONS.get(ext);
    if (ctx == null) {
      return new YamlConfiguration();
    }
    YamlConfiguration cfg = ctx.config;
    if (cfg != null) {
      return cfg;
    }
    synchronized (ctx) {
      cfg = ctx.config;
      if (cfg == null) {
        cfg = loadExtensionConfig(ext, ctx);
        ctx.config = cfg;
      }
      return cfg;
    }
  }

  public static void saveDefaultConfig(@NotNull FppExtension ext) {
    ExtensionContext ctx = EXTENSIONS.get(ext);
    if (ctx == null) return;
    File configFile = new File(ctx.dataFolder, "config.yml");
    if (configFile.exists()) {
      syncConfigKeys(ext, ctx, configFile);
      return;
    }
    configFile.getParentFile().mkdirs();
    InputStream jarStream = getConfigStreamFromJar(ext, ctx);
    if (jarStream != null) {
      try (jarStream) {
        Files.copy(jarStream, configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        FppLogger.info(
            "[Extensions] Extracted default config for '" + ext.getName() + "'.");
      } catch (IOException e) {
        FppLogger.warn(
            "[Extensions] Failed to extract default config for '"
                + ext.getName()
                + "': "
                + e.getMessage());
      }
    } else {
      try {
        configFile.createNewFile();
        FppLogger.info(
            "[Extensions] Created empty config for '" + ext.getName() + "' (no default in JAR).");
      } catch (IOException e) {
        FppLogger.warn(
            "[Extensions] Failed to create config for '"
                + ext.getName()
                + "': "
                + e.getMessage());
      }
    }
    ctx.config = null; // force reload on next getConfig()
  }

  public static void extractResources(@NotNull FppExtension ext) {
    ExtensionContext ctx = EXTENSIONS.get(ext);
    if (ctx == null) return;
    File jarFile = getJarFileForExtension(ext, ctx);
    if (jarFile == null || !jarFile.exists()) return;

    String prefix = "extension-resources/";
    try (JarFile jf = new JarFile(jarFile)) {
      Enumeration<JarEntry> entries = jf.entries();
      int extracted = 0;
      while (entries.hasMoreElements()) {
        JarEntry entry = entries.nextElement();
        String name = entry.getName();
        if (!name.startsWith(prefix) || entry.isDirectory()) continue;

        String relativePath = name.substring(prefix.length());
        if (relativePath.isEmpty()) continue;

        File outFile = new File(ctx.dataFolder, relativePath);
        if (outFile.exists()) continue; // never overwrite user files

        outFile.getParentFile().mkdirs();
        try (InputStream in = jf.getInputStream(entry)) {
          Files.copy(in, outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
          extracted++;
        }
      }
      if (extracted > 0) {
        FppLogger.info(
            "[Extensions] Extracted "
                + extracted
                + " resource(s) for '"
                + ext.getName()
                + "'.");
      }
    } catch (IOException e) {
      FppLogger.warn(
          "[Extensions] Failed to extract resources for '"
              + ext.getName()
              + "': "
              + e.getMessage());
    }
  }

  public static @Nullable File saveResource(@NotNull FppExtension ext, @NotNull String jarPath) {
    ExtensionContext ctx = EXTENSIONS.get(ext);
    if (ctx == null) return null;

    InputStream resourceIn = ctx.classLoader.getResourceAsStream(jarPath);

    if (resourceIn == null) {
      File jarFile = getJarFileForExtension(ext, ctx);
      if (jarFile != null && jarFile.exists()) {
        try (JarFile jf = new JarFile(jarFile)) {
          JarEntry entry = jf.getJarEntry(jarPath);
          if (entry != null) {
            byte[] bytes = jf.getInputStream(entry).readAllBytes();
            resourceIn = new java.io.ByteArrayInputStream(bytes);
          }
        } catch (IOException ignored) {
        }
      }
    }
    if (resourceIn == null) {
      FppLogger.warn(
          "[Extensions] Resource '" + jarPath + "' not found for '" + ext.getName() + "'.");
      return null;
    }

    File outFile = new File(ctx.dataFolder, new File(jarPath).getName());
    outFile.getParentFile().mkdirs();
    try (InputStream in = resourceIn) {
      Files.copy(in, outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      FppLogger.warn(
          "[Extensions] Failed to save resource '"
              + jarPath
              + "' for '"
              + ext.getName()
              + "': "
              + e.getMessage());
      return null;
    }
    return outFile;
  }

  public static void reloadConfig(@NotNull FppExtension ext) {
    ExtensionContext ctx = EXTENSIONS.get(ext);
    if (ctx == null) return;
    synchronized (ctx) {
      ctx.config = loadExtensionConfig(ext, ctx);
    }
  }

  // ── Public API methods ─────────────────────────────────────────────────────

  public @Nullable File getExtensionDataFolder(@NotNull String extensionName) {
    for (var entry : EXTENSIONS.entrySet()) {
      if (entry.getKey().getName().equalsIgnoreCase(extensionName)) {
        return entry.getValue().dataFolder;
      }
    }
    return null;
  }

  public @Nullable YamlConfiguration getExtensionConfig(@NotNull String extensionName) {
    for (var entry : EXTENSIONS.entrySet()) {
      if (entry.getKey().getName().equalsIgnoreCase(extensionName)) {
        return getConfig(entry.getKey());
      }
    }
    return null;
  }

  public void saveDefaultExtensionConfig(@NotNull String extensionName) {
    for (var entry : EXTENSIONS.entrySet()) {
      if (entry.getKey().getName().equalsIgnoreCase(extensionName)) {
        saveDefaultConfig(entry.getKey());
        return;
      }
    }
  }

  public void reloadExtensionConfigs() {
    for (var entry : EXTENSIONS.entrySet()) {
      try {
        reloadConfig(entry.getKey());
        FppLogger.info(
            "[Extensions] Reloaded config for '" + entry.getKey().getName() + "'.");
      } catch (Throwable t) {
        FppLogger.warn(
            "[Extensions] Failed to reload config for '"
                + entry.getKey().getName()
                + "': "
                + t.getMessage());
      }
    }
  }

  // ── Extension loading ─────────────────────────────────────────────────────

  public void loadExtensions() {
    File extensionsDir = new File(plugin.getDataFolder(), "extensions");
    if (!extensionsDir.exists()) {
      return;
    }

    File[] jars = extensionsDir.listFiles(f -> f.isFile() && f.getName().endsWith(".jar"));
    if (jars == null || jars.length == 0) {
      return;
    }

    List<ExtensionAddonWrapper> wrappers = new ArrayList<>();

    for (File jar : jars) {
      int found = loadJar(jar, wrappers);
      if (found > 0) {
        FppLogger.info(
            "[Extensions] Scanned " + jar.getName() + " — " + found + " extension(s) found.");
      }
    }

    if (wrappers.isEmpty()) {
      return;
    }

    wrappers.sort(
        Comparator.comparingInt(FppAddon::getPriority)
            .thenComparing(a -> a.getName().toLowerCase()));

    for (ExtensionAddonWrapper wrapper : wrappers) {
      plugin.getFppApi().registerAddon(wrapper);
    }
    activeWrappers.addAll(wrappers);

    FppLogger.info("[Extensions] Loaded " + wrappers.size() + " extension(s) from jar file(s).");
  }

  public void reload() {
    for (ExtensionAddonWrapper wrapper : activeWrappers) {
      try {
        plugin.getFppApi().unregisterAddon(wrapper);
      } catch (Throwable t) {
        FppLogger.warn(
            "[Extensions] Failed to unregister extension '"
                + wrapper.getName()
                + "': "
                + t.getMessage());
      }
    }
    EXTENSIONS.clear();
    activeWrappers.clear();
    closeClassLoaders();
    loadExtensions();
  }

  private int loadJar(File jar, List<ExtensionAddonWrapper> wrappers) {
    URLClassLoader classLoader = null;
    int found = 0;

    try {
      URL[] urls = {jar.toURI().toURL()};
      classLoader = new URLClassLoader(urls, plugin.getClass().getClassLoader());

      try (JarFile jarFile = new JarFile(jar)) {
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
          JarEntry entry = entries.nextElement();
          String name = entry.getName();
          if (!name.endsWith(".class") || name.contains("$")) {
            continue;
          }

          String className = name.replace('/', '.').substring(0, name.length() - 6);
          try {
            Class<?> clazz = Class.forName(className, true, classLoader);
            if (FppExtension.class.isAssignableFrom(clazz)
                && !clazz.isInterface()
                && !Modifier.isAbstract(clazz.getModifiers())) {
              FppExtension ext = (FppExtension) clazz.getDeclaredConstructor().newInstance();
              registerContext(ext, jar, classLoader);
              wrappers.add(new ExtensionAddonWrapper(plugin, ext));
              found++;
            }
          } catch (NoClassDefFoundError ignored) {
          } catch (Throwable t) {
            FppLogger.warn(
                "[Extensions] Could not load class "
                    + className
                    + " from "
                    + jar.getName()
                    + ": "
                    + t.getMessage());
          }
        }
      }

      if (found > 0) {
        classLoaders.add(classLoader);
      }
    } catch (IOException e) {
      FppLogger.warn("[Extensions] Failed to load " + jar.getName() + ": " + e.getMessage());
    } finally {
      if (found == 0 && classLoader != null) {
        try {
          classLoader.close();
        } catch (IOException ignored) {
        }
      }
    }

    return found;
  }

  public void closeClassLoaders() {
    for (URLClassLoader cl : classLoaders) {
      try {
        cl.close();
      } catch (IOException ignored) {
      }
    }
    classLoaders.clear();
  }

  // ── Private helpers ────────────────────────────────────────────────────────

  private void registerContext(FppExtension ext, File jar, URLClassLoader cl) {
    String sanitizedName = ext.getName().replaceAll("[^a-zA-Z0-9_\\-.]", "_");
    File dataFolder = new File(plugin.getDataFolder(), "extensions" + File.separator + sanitizedName);
    dataFolder.mkdirs();
    EXTENSIONS.put(ext, new ExtensionContext(dataFolder, cl));
    FppLogger.info(
        "[Extensions] Registered data folder for '"
            + ext.getName()
            + "': "
            + dataFolder.getAbsolutePath());
  }

  private static @Nullable InputStream getConfigStreamFromJar(
      @NotNull FppExtension ext, @NotNull ExtensionContext ctx) {
    // Primary: config.yml at JAR root
    InputStream in = ctx.classLoader.getResourceAsStream("config.yml");
    if (in != null) return in;
    // Fallback: extension-resources/config.yml
    return ctx.classLoader.getResourceAsStream("extension-resources/config.yml");
  }

  private static @Nullable File getJarFileForExtension(
      @NotNull FppExtension ext, @NotNull ExtensionContext ctx) {
    try {
      URL[] urls = ctx.classLoader.getURLs();
      if (urls.length > 0) {
        return new File(urls[0].toURI());
      }
    } catch (Exception ignored) {
    }
    return null;
  }

  private static @NotNull YamlConfiguration loadExtensionConfig(
      @NotNull FppExtension ext, @NotNull ExtensionContext ctx) {
    File configFile = new File(ctx.dataFolder, "config.yml");
    if (!configFile.exists()) {
      // Extract default config first
      configFile.getParentFile().mkdirs();
      InputStream jarStream = getConfigStreamFromJar(ext, ctx);
      if (jarStream != null) {
        try (jarStream) {
          Files.copy(jarStream, configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
          FppLogger.warn(
              "[Extensions] Failed to extract default config for '"
                  + ext.getName()
                  + "': "
                  + e.getMessage());
        }
      } else {
        try {
          configFile.createNewFile();
        } catch (IOException e) {
          FppLogger.warn(
              "[Extensions] Failed to create config for '"
                  + ext.getName()
                  + "': "
                  + e.getMessage());
        }
      }
    }

    YamlConfiguration diskCfg = YamlConfiguration.loadConfiguration(configFile);

    // Set defaults from JAR config
    InputStream jarStream = getConfigStreamFromJar(ext, ctx);
    if (jarStream != null) {
      try (jarStream) {
        YamlConfiguration defaults =
            YamlConfiguration.loadConfiguration(
                new InputStreamReader(jarStream, StandardCharsets.UTF_8));
        diskCfg.setDefaults(defaults);
      } catch (IOException ignored) {
      }
    }

    return diskCfg;
  }

  private static void syncConfigKeys(
      @NotNull FppExtension ext,
      @NotNull ExtensionContext ctx,
      @NotNull File configFile) {
    InputStream jarStream = getConfigStreamFromJar(ext, ctx);
    if (jarStream == null) return;

    YamlConfiguration jarCfg;
    YamlConfiguration diskCfg;
    try (jarStream) {
      jarCfg =
          YamlConfiguration.loadConfiguration(
              new InputStreamReader(jarStream, StandardCharsets.UTF_8));
      diskCfg = YamlConfiguration.loadConfiguration(configFile);
    } catch (IOException e) {
      return;
    }

    List<String> missing = new ArrayList<>();
    for (String key : jarCfg.getKeys(true)) {
      if (jarCfg.isConfigurationSection(key)) continue;
      if (!diskCfg.contains(key)) missing.add(key);
    }

    if (missing.isEmpty()) return;

    for (String key : missing) {
      diskCfg.set(key, jarCfg.get(key));
    }

    try {
      diskCfg.save(configFile);
      FppLogger.info(
          "[Extensions] "
              + ext.getName()
              + " config.yml: added "
              + missing.size()
              + " new key(s).");
    } catch (IOException e) {
      FppLogger.warn(
          "[Extensions] Failed to save synced config for '"
              + ext.getName()
              + "': "
              + e.getMessage());
    }
  }

  private static final class ExtensionAddonWrapper implements FppAddon {

    private final FakePlayerPlugin plugin;
    private final FppExtension extension;

    ExtensionAddonWrapper(@NotNull FakePlayerPlugin plugin, @NotNull FppExtension extension) {
      this.plugin = plugin;
      this.extension = extension;
    }

    @Override
    public @NotNull String getName() {
      return extension.getName();
    }

    @Override
    public @NotNull String getVersion() {
      return extension.getVersion();
    }

    @Override
    public @NotNull Plugin getPlugin() {
      return plugin;
    }

    @Override
    public @NotNull String getDescription() {
      return extension.getDescription();
    }

    @Override
    public @NotNull List<String> getAuthors() {
      return extension.getAuthors();
    }

    @Override
    public @NotNull List<String> getDependencies() {
      return extension.getDependencies();
    }

    @Override
    public @NotNull List<String> getSoftDependencies() {
      return extension.getSoftDependencies();
    }

    @Override
    public int getPriority() {
      return extension.getPriority();
    }

    @Override
    public void onEnable(@NotNull FppApi api) {
      extension.onEnable(api);
    }

    @Override
    public void onDisable() {
      extension.onDisable();
    }
  }
}