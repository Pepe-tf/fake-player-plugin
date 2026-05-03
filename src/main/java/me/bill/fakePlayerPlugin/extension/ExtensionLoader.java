package me.bill.fakePlayerPlugin.extension;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import me.bill.fakePlayerPlugin.FakePlayerPlugin;
import me.bill.fakePlayerPlugin.api.FppExtension;
import me.bill.fakePlayerPlugin.util.FppLogger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ExtensionLoader {

  private record ExtensionContext(File dataFolder, URLClassLoader classLoader, File jarFile) {}

  private static final Map<FppExtension, ExtensionContext> CONTEXTS = new ConcurrentHashMap<>();

  private final FakePlayerPlugin plugin;
  private final List<FppExtension> extensions = new ArrayList<>();
  private final List<URLClassLoader> classLoaders = new ArrayList<>();

  public ExtensionLoader(FakePlayerPlugin plugin) {
    this.plugin = plugin;
  }

  public void loadExtensions() {
    File dir = new File(plugin.getDataFolder(), "extensions");
    if (!dir.exists() && !dir.mkdirs()) {
      FppLogger.warn("Could not create extensions directory: " + dir);
      return;
    }

    File[] jars = dir.listFiles(file -> file.isFile() && file.getName().endsWith(".jar"));
    if (jars == null || jars.length == 0) return;

    List<FppExtension> loaded = new ArrayList<>();
    for (File jar : jars) {
      loaded.addAll(loadJar(jar));
    }
    loaded.sort(Comparator.comparingInt(FppExtension::getPriority).thenComparing(FppExtension::getName));

    for (FppExtension extension : loaded) {
      try {
        extension.onEnable(plugin.getFppApi());
        extensions.add(extension);
        FppLogger.info(
            "Enabled extension "
                + extension.getName()
                + " v"
                + extension.getVersion()
                + ".");
      } catch (Throwable t) {
        FppLogger.warn("Extension " + extension.getName() + " failed to enable: " + t.getMessage());
        CONTEXTS.remove(extension);
      }
    }
  }

  public void reloadExtensions() {
    disableExtensions();
    if (plugin.getFppApiImpl() != null) plugin.getFppApiImpl().clearExtensionRegistrations();
    closeClassLoaders();
    loadExtensions();
  }

  public void disableExtensions() {
    List<FppExtension> copy = new ArrayList<>(extensions);
    copy.sort(Comparator.comparingInt(FppExtension::getPriority).reversed());
    for (FppExtension extension : copy) {
      try {
        extension.onDisable();
      } catch (Throwable t) {
        FppLogger.warn("Extension " + extension.getName() + " failed to disable: " + t.getMessage());
      }
    }
    extensions.clear();
    CONTEXTS.clear();
    if (plugin.getFppApiImpl() != null) plugin.getFppApiImpl().clearExtensionRegistrations();
  }

  public void closeClassLoaders() {
    for (URLClassLoader classLoader : classLoaders) {
      try {
        classLoader.close();
      } catch (IOException ignored) {
      }
    }
    classLoaders.clear();
  }

  public @Nullable File getExtensionDataFolder(@NotNull String extensionName) {
    return CONTEXTS.entrySet().stream()
        .filter(entry -> entry.getKey().getName().equalsIgnoreCase(extensionName))
        .map(entry -> entry.getValue().dataFolder())
        .findFirst()
        .orElse(null);
  }

  public @Nullable YamlConfiguration getExtensionConfig(@NotNull String extensionName) {
    return CONTEXTS.entrySet().stream()
        .filter(entry -> entry.getKey().getName().equalsIgnoreCase(extensionName))
        .map(entry -> getConfig(entry.getKey()))
        .findFirst()
        .orElse(null);
  }

  public void saveDefaultExtensionConfig(@NotNull String extensionName) {
    CONTEXTS.keySet().stream()
        .filter(extension -> extension.getName().equalsIgnoreCase(extensionName))
        .findFirst()
        .ifPresent(ExtensionLoader::saveDefaultConfig);
  }

  public static @Nullable File getDataFolder(@NotNull FppExtension extension) {
    ExtensionContext context = CONTEXTS.get(extension);
    return context == null ? null : context.dataFolder();
  }

  public static @NotNull YamlConfiguration getConfig(@NotNull FppExtension extension) {
    ExtensionContext context = CONTEXTS.get(extension);
    if (context == null) return new YamlConfiguration();
    return YamlConfiguration.loadConfiguration(new File(context.dataFolder(), "config.yml"));
  }

  public static void reloadConfig(@NotNull FppExtension extension) {
    getConfig(extension);
  }

  public static void saveDefaultConfig(@NotNull FppExtension extension) {
    saveResource(extension, "config.yml");
  }

  public static void extractResources(@NotNull FppExtension extension) {
    ExtensionContext context = CONTEXTS.get(extension);
    if (context == null) return;
    try (JarFile jarFile = new JarFile(context.jarFile())) {
      Enumeration<JarEntry> entries = jarFile.entries();
      while (entries.hasMoreElements()) {
        JarEntry entry = entries.nextElement();
        String entryName = entry.getName();
        if (entry.isDirectory() || !entryName.startsWith("extension-resources/")) continue;
        String relativePath = entryName.substring("extension-resources/".length());
        if (relativePath.isBlank()) continue;
        saveResource(extension, entryName, relativePath);
      }
    } catch (IOException e) {
      FppLogger.warn("Could not extract extension resources for " + extension.getName() + ": " + e.getMessage());
    }
  }

  public static @Nullable File saveResource(@NotNull FppExtension extension, @NotNull String jarPath) {
    return saveResource(extension, jarPath, jarPath);
  }

  private static @Nullable File saveResource(
      @NotNull FppExtension extension, @NotNull String jarPath, @NotNull String outputPath) {
    ExtensionContext context = CONTEXTS.get(extension);
    if (context == null) return null;
    File out = new File(context.dataFolder(), outputPath);
    if (out.exists()) return out;
    File parent = out.getParentFile();
    if (parent != null) parent.mkdirs();
    try (InputStream in = context.classLoader().getResourceAsStream(jarPath)) {
      if (in == null) return null;
      Files.copy(in, out.toPath(), StandardCopyOption.REPLACE_EXISTING);
      return out;
    } catch (IOException e) {
      FppLogger.warn("Could not save extension resource " + jarPath + ": " + e.getMessage());
      return null;
    }
  }

  private List<FppExtension> loadJar(File jar) {
    List<FppExtension> found = new ArrayList<>();
    try {
      URLClassLoader classLoader =
          new URLClassLoader(new URL[] {jar.toURI().toURL()}, plugin.getClass().getClassLoader());
      classLoaders.add(classLoader);
      try (JarFile jarFile = new JarFile(jar)) {
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
          JarEntry entry = entries.nextElement();
          if (entry.isDirectory() || !entry.getName().endsWith(".class")) continue;
          String className =
              entry.getName().substring(0, entry.getName().length() - 6).replace('/', '.');
          try {
            Class<?> cls = Class.forName(className, false, classLoader);
            if (FppExtension.class.isAssignableFrom(cls)
                && !cls.isInterface()
                && !java.lang.reflect.Modifier.isAbstract(cls.getModifiers())) {
              FppExtension extension = (FppExtension) cls.getDeclaredConstructor().newInstance();
              File dataFolder = new File(plugin.getDataFolder(), "extensions/" + extension.getName());
              dataFolder.mkdirs();
              CONTEXTS.put(extension, new ExtensionContext(dataFolder, classLoader, jar));
              found.add(extension);
            }
          } catch (Throwable ignored) {
          }
        }
      }
    } catch (IOException e) {
      FppLogger.warn("Could not load extension jar " + jar.getName() + ": " + e.getMessage());
    }
    return found;
  }
}
