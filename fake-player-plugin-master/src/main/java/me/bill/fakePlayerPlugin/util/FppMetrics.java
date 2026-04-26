package me.bill.fakePlayerPlugin.util;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.Callable;
import me.bill.fakePlayerPlugin.FakePlayerPlugin;
import me.bill.fakePlayerPlugin.config.Config;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayerManager;
import org.bukkit.Bukkit;

public final class FppMetrics {

  private static final String TOKEN = "376511af6c97b56954ff2abed24dfaea";

  private URLClassLoader fsLoader;
  private Object metrics;
  private boolean initialised = false;

  public void init(FakePlayerPlugin plugin, FakePlayerManager botManager) {
    if (TOKEN.isBlank()) {
      FppLogger.warn("Metrics: TOKEN is blank - FastStats disabled.");
      return;
    }

    FppLogger.debug("Metrics: Initialising FastStats (token=" + TOKEN.substring(0, 8) + "...)");

    ClassLoader prevCtx = Thread.currentThread().getContextClassLoader();
    try {

      File libsDir = new File(plugin.getDataFolder(), ".faststats-libs");
      libsDir.mkdirs();

      File coreJar = extractResource(plugin, "faststats/faststats-core.jar", libsDir);
      File bukkitJar = extractResource(plugin, "faststats/faststats-bukkit.jar", libsDir);

      fsLoader =
          new URLClassLoader(
              new URL[] {coreJar.toURI().toURL(), bukkitJar.toURI().toURL()},
              plugin.getClass().getClassLoader());

      Thread.currentThread().setContextClassLoader(fsLoader);

      FppLogger.debug("Metrics: [1/3] Creating ErrorTracker...");
      Class<?> etClass = fsLoader.loadClass("dev.faststats.core.ErrorTracker");
      Object errorTracker = etClass.getMethod("contextAware").invoke(null);
      FppLogger.debug("Metrics:       ErrorTracker ✔");

      FppLogger.debug("Metrics: [2/3] Building BukkitMetrics...");
      Class<?> bmClass = fsLoader.loadClass("dev.faststats.bukkit.BukkitMetrics");
      Class<?> mClass = fsLoader.loadClass("dev.faststats.core.data.Metric");

      Method numberMethod = findMethod(mClass, "number", 2);
      Method stringMethod = findMethod(mClass, "string", 2);

      Object factory = bmClass.getMethod("factory").invoke(null);
      factory = chain(factory, "token", TOKEN);

      factory =
          addMetric(
              factory,
              numberMethod.invoke(
                  null,
                  "active_bots",
                  (Callable<Long>) () -> (long) (botManager == null ? 0 : botManager.getCount())));
      factory =
          addMetric(
              factory,
              numberMethod.invoke(
                  null,
                  "online_players",
                  (Callable<Long>) () -> (long) Bukkit.getOnlinePlayers().size()));
      factory =
          addMetric(
              factory, stringMethod.invoke(null, "skin_mode", (Callable<String>) Config::skinMode));
      factory =
          addMetric(
              factory,
              numberMethod.invoke(
                  null,
                  "persistence_enabled",
                  (Callable<Long>) () -> Config.persistOnRestart() ? 1L : 0L));
      factory =
          addMetric(
              factory,
              numberMethod.invoke(
                  null, "body_enabled", (Callable<Long>) () -> Config.spawnBody() ? 1L : 0L));
      factory =
          addMetric(
              factory,
              numberMethod.invoke(
                  null,
                  "fake_chat_enabled",
                  (Callable<Long>) () -> Config.fakeChatEnabled() ? 1L : 0L));
      factory =
          addMetric(
              factory,
              numberMethod.invoke(
                  null,
                  "chunk_loading_enabled",
                  (Callable<Long>) () -> Config.chunkLoadingEnabled() ? 1L : 0L));
      factory =
          addMetric(
              factory,
              stringMethod.invoke(
                  null,
                  "database_type",
                  (Callable<String>) () -> Config.mysqlEnabled() ? "mysql" : "sqlite"));
      factory =
          addMetric(
              factory,
              numberMethod.invoke(
                  null,
                  "luckperms_installed",
                  (Callable<Long>)
                      () -> Bukkit.getPluginManager().getPlugin("LuckPerms") != null ? 1L : 0L));
      factory =
          addMetric(
              factory,
              numberMethod.invoke(
                  null, "max_bots_config", (Callable<Long>) () -> (long) Config.maxBots()));

      factory = chain(factory, "errorTracker", errorTracker);
      factory = chain(factory, "debug", false);
      FppLogger.debug("Metrics:       BukkitMetrics built ✔");

      FppLogger.debug("Metrics: [3/3] Calling ready()...");
      metrics = chain(factory, "create", plugin);
      findMethod(metrics.getClass(), "ready", 0).invoke(metrics);
      initialised = true;
      FppLogger.debug("Metrics: FastStats connected and reporting ✔");

    } catch (Throwable t) {
      FppLogger.error("╔══════════════════════════════════════════════════");
      FppLogger.error("║  Metrics: FastStats init FAILED");
      FppLogger.error("║  " + t.getClass().getName() + ": " + t.getMessage());
      Throwable c = t;
      int d = 0;
      while (c.getCause() != null && d++ < 6) {
        c = c.getCause();
        FppLogger.error("║  Caused by: " + c.getClass().getName() + ": " + c.getMessage());
      }
      FppLogger.error("║  Stack (top 10):");
      StackTraceElement[] st = t.getStackTrace();
      for (int i = 0; i < Math.min(10, st.length); i++) FppLogger.error("║    at " + st[i]);
      FppLogger.error("╚══════════════════════════════════════════════════");
      metrics = null;
      closeLoader();
    } finally {
      Thread.currentThread().setContextClassLoader(prevCtx);
    }
  }

  public void shutdown() {
    if (metrics != null && initialised) {
      try {
        findMethod(metrics.getClass(), "shutdown", 0).invoke(metrics);
      } catch (Throwable ignored) {
      }
      metrics = null;
    }
    initialised = false;
    closeLoader();
  }

  public boolean isActive() {
    return initialised && metrics != null;
  }

  private static Object chain(Object obj, String name, Object... args)
      throws ReflectiveOperationException {
    Method found = null;

    Class<?> cls = obj.getClass();
    outer:
    while (cls != null) {
      for (Method m : cls.getDeclaredMethods()) {
        if (m.getName().equals(name) && m.getParameterCount() == args.length) {
          found = m;
          break outer;
        }
      }
      for (Class<?> iface : cls.getInterfaces()) {
        for (Method m : iface.getDeclaredMethods()) {
          if (m.getName().equals(name) && m.getParameterCount() == args.length) {
            found = m;
            break outer;
          }
        }
      }
      cls = cls.getSuperclass();
    }

    if (found == null) {
      for (Method m : obj.getClass().getMethods()) {
        if (m.getName().equals(name) && m.getParameterCount() == args.length) {
          found = m;
          break;
        }
      }
    }
    if (found == null)
      throw new NoSuchMethodException(
          obj.getClass().getSimpleName() + "." + name + "/" + args.length);
    found.setAccessible(true);
    return found.invoke(obj, args);
  }

  private static Method findMethod(Class<?> cls, String name, int paramCount)
      throws NoSuchMethodException {
    Class<?> c = cls;
    while (c != null) {
      for (Method m : c.getDeclaredMethods()) {
        if (m.getName().equals(name) && m.getParameterCount() == paramCount) {
          m.setAccessible(true);
          return m;
        }
      }
      c = c.getSuperclass();
    }
    for (Method m : cls.getMethods()) {
      if (m.getName().equals(name) && m.getParameterCount() == paramCount) {
        m.setAccessible(true);
        return m;
      }
    }
    throw new NoSuchMethodException(cls.getSimpleName() + "." + name + "/" + paramCount);
  }

  private static Object addMetric(Object factory, Object metric)
      throws ReflectiveOperationException {
    Class<?> cls = factory.getClass();
    while (cls != null) {
      for (Method m : cls.getDeclaredMethods()) {
        if (m.getName().equals("addMetric") && m.getParameterCount() == 1) {
          m.setAccessible(true);
          try {
            return m.invoke(factory, metric);
          } catch (IllegalArgumentException ignored) {
          }
        }
      }
      cls = cls.getSuperclass();
    }
    for (Method m : factory.getClass().getMethods()) {
      if (m.getName().equals("addMetric") && m.getParameterCount() == 1) {
        m.setAccessible(true);
        try {
          return m.invoke(factory, metric);
        } catch (IllegalArgumentException ignored) {
        }
      }
    }
    throw new NoSuchMethodException("addMetric");
  }

  private static File extractResource(FakePlayerPlugin plugin, String resourcePath, File destDir)
      throws IOException {
    String fileName = resourcePath.substring(resourcePath.lastIndexOf('/') + 1);
    File dest = new File(destDir, fileName);
    try (InputStream in = plugin.getResource(resourcePath)) {
      if (in == null) throw new IOException("Bundled resource not found: " + resourcePath);
      byte[] data = in.readAllBytes();
      if (!dest.exists() || dest.length() != data.length) {
        try (OutputStream out = new FileOutputStream(dest)) {
          out.write(data);
        }
        FppLogger.debug("Metrics: extracted " + fileName + " (" + data.length / 1024 + " KB)");
      }
    }
    return dest;
  }

  private void closeLoader() {
    if (fsLoader != null) {
      try {
        fsLoader.close();
      } catch (IOException ignored) {
      }
      fsLoader = null;
    }
  }
}
