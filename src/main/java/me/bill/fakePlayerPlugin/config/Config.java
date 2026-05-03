package me.bill.fakePlayerPlugin.config;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import me.bill.fakePlayerPlugin.FakePlayerPlugin;
import me.bill.fakePlayerPlugin.util.FppLogger;
import me.bill.fakePlayerPlugin.util.RandomNameGenerator;

public final class Config {

  private static FakePlayerPlugin plugin;

  private Config() {}

  public static void init(FakePlayerPlugin plugin) {
    Config.plugin = plugin;
    plugin.reloadConfig();
  }

  public static List<String> namePool() {
    if (plugin == null) return List.of();
    List<String> names = plugin.getConfig().getStringList("bot-names");
    if (names.isEmpty()) {
      names = plugin.getConfig().getStringList("names");
    }
    if (names.isEmpty()) {
      names = List.of("FakePlayer", "BuilderBot", "MinerBot", "SteveBot");
    }
    return new ArrayList<>(names);
  }

  public static String randomName() {
    if ("random".equalsIgnoreCase(botNameMode())) {
      return RandomNameGenerator.generate();
    }
    List<String> names = namePool();
    return names.get(ThreadLocalRandom.current().nextInt(names.size()));
  }

  public static String botNameMode() {
    if (plugin == null) return "list";
    return plugin.getConfig().getString("bot-name-mode", "list");
  }

  public static boolean bodyPushable() {
    return plugin == null || plugin.getConfig().getBoolean("body.pushable", true);
  }

  public static double collisionHitStrength() {
    return plugin == null ? 0.65 : plugin.getConfig().getDouble("body.collision.hit-strength", 0.65);
  }

  public static double collisionHitMaxHoriz() {
    return plugin == null ? 1.15 : plugin.getConfig().getDouble("body.collision.hit-max-horizontal", 1.15);
  }

  public static double collisionWalkRadius() {
    return plugin == null ? 0.85 : plugin.getConfig().getDouble("body.collision.walk-radius", 0.85);
  }

  public static double collisionWalkStrength() {
    return plugin == null ? 0.22 : plugin.getConfig().getDouble("body.collision.walk-strength", 0.22);
  }

  public static double collisionBotRadius() {
    return plugin == null ? 0.72 : plugin.getConfig().getDouble("body.collision.bot-radius", 0.72);
  }

  public static double collisionBotStrength() {
    return plugin == null ? 0.16 : plugin.getConfig().getDouble("body.collision.bot-strength", 0.16);
  }

  public static double collisionMaxHoriz() {
    return plugin == null ? 0.7 : plugin.getConfig().getDouble("body.collision.max-horizontal", 0.7);
  }

  public static boolean isBotRightClickInventoryEnabled() {
    return plugin == null || plugin.getConfig().getBoolean("bot-interaction.right-click-inventory", true);
  }

  public static boolean isBotShiftRightClickSettingsEnabled() {
    return plugin == null || plugin.getConfig().getBoolean("bot-interaction.shift-right-click-settings", true);
  }

  public static boolean debug() {
    return plugin != null && plugin.getConfig().getBoolean("debug", false);
  }

  public static void debugNms(String message) {
    if (debug()) FppLogger.debug(message);
  }

  public static boolean deathMessage() {
    return plugin == null || plugin.getConfig().getBoolean("messages.death-message", true);
  }

  public static boolean respawnOnDeath() {
    return plugin != null && plugin.getConfig().getBoolean("death.respawn-on-death", false);
  }

  public static int respawnDelay() {
    return plugin == null ? 60 : plugin.getConfig().getInt("death.respawn-delay", 60);
  }

  public static int deathDespawnDelay() {
    return plugin == null ? 20 : plugin.getConfig().getInt("death.despawn-delay", 20);
  }

  public static boolean suppressDrops() {
    return plugin != null && plugin.getConfig().getBoolean("death.suppress-drops", false);
  }
}
