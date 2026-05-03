package me.bill.fakePlayerPlugin.fakeplayer;

import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import me.bill.fakePlayerPlugin.FakePlayerPlugin;
import me.bill.fakePlayerPlugin.api.event.FppBotDespawnEvent;
import me.bill.fakePlayerPlugin.api.event.FppBotSpawnEvent;
import me.bill.fakePlayerPlugin.api.impl.FppBotImpl;
import me.bill.fakePlayerPlugin.config.Config;
import me.bill.fakePlayerPlugin.util.FppLogger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class FakePlayerManager {

  private final FakePlayerPlugin plugin;
  private final Map<UUID, FakePlayer> byUuid = new ConcurrentHashMap<>();
  private final Map<String, FakePlayer> byName = new ConcurrentHashMap<>();
  private final Map<UUID, String> pendingSpawnNames = new ConcurrentHashMap<>();
  private final Map<UUID, String> pendingDespawnNames = new ConcurrentHashMap<>();
  private int physicsTaskId = -1;

  public FakePlayerManager(FakePlayerPlugin plugin) {
    this.plugin = plugin;
    this.physicsTaskId =
        Bukkit.getScheduler()
            .runTaskTimer(plugin, this::tickBots, 1L, 1L)
            .getTaskId();
  }

  public Collection<FakePlayer> getActivePlayers() {
    return byUuid.values();
  }

  public FakePlayer getByUuid(UUID uuid) {
    return byUuid.get(uuid);
  }

  public FakePlayer getByName(String name) {
    return name == null ? null : byName.get(name.toLowerCase(Locale.ROOT));
  }

  public boolean isBot(Player player) {
    return player != null && byUuid.containsKey(player.getUniqueId());
  }

  public int spawn(Location location, int count, Player spawner, String customName) {
    return spawnBots(location, count, spawner, customName).size();
  }

  public List<FakePlayer> spawnBots(Location location, int count, Player spawner, String customName) {
    List<FakePlayer> spawned = new ArrayList<>();
    if (location == null || location.getWorld() == null) return spawned;
    for (int i = 0; i < count; i++) {
      String name = uniqueName(customName != null && count == 1 ? customName : Config.randomName());
      UUID uuid = UUID.nameUUIDFromBytes(("fpp:" + name + ":" + System.nanoTime()).getBytes());
      pendingSpawnNames.put(uuid, name);
      Player entity =
          NmsPlayerSpawner.spawnFakePlayer(
              uuid, name, location.getWorld(), location.getX(), location.getY(), location.getZ());
      pendingSpawnNames.remove(uuid);
      if (entity == null) {
        FppLogger.warn("Could not spawn fake player '" + name + "'.");
        continue;
      }
      FakePlayer fp = new FakePlayer(uuid, name);
      fp.setPlayer(entity);
      fp.setSpawnLocation(location.clone());
      if (spawner != null) {
        fp.setSpawnedBy(spawner.getName());
        fp.setSpawnedByUuid(spawner.getUniqueId());
      }
      byUuid.put(uuid, fp);
      byName.put(name.toLowerCase(Locale.ROOT), fp);
      Bukkit.getPluginManager().callEvent(new FppBotSpawnEvent(new FppBotImpl(fp), false));
      spawned.add(fp);
    }
    return spawned;
  }

  public boolean delete(String name) {
    FakePlayer fp = getByName(name);
    if (fp == null) return false;
    return despawn(fp, fp.getPlayer());
  }

  public boolean despawn(FakePlayer fp, Player entity) {
    if (fp == null) return false;
    Bukkit.getPluginManager().callEvent(new FppBotDespawnEvent(new FppBotImpl(fp)));
    Player target = entity != null ? entity : fp.getPlayer();
    pendingDespawnNames.put(fp.getUuid(), fp.getDisplayName());
    NmsPlayerSpawner.removeFakePlayerFast(target);
    pendingDespawnNames.remove(fp.getUuid());
    byName.remove(fp.getName().toLowerCase(Locale.ROOT));
    byUuid.remove(fp.getUuid());
    fp.setPlayer(null);
    fp.setAlive(false);
    return true;
  }

  public int removeAll() {
    int count = byUuid.size();
    for (FakePlayer fp : byUuid.values()) {
      Bukkit.getPluginManager().callEvent(new FppBotDespawnEvent(new FppBotImpl(fp)));
      NmsPlayerSpawner.removeFakePlayerFast(fp.getPlayer());
    }
    byUuid.clear();
    byName.clear();
    if (physicsTaskId != -1) {
      Bukkit.getScheduler().cancelTask(physicsTaskId);
      physicsTaskId = -1;
    }
    return count;
  }

  public boolean isBotOrPending(Player player) {
    return player != null
        && (byUuid.containsKey(player.getUniqueId())
            || pendingSpawnNames.containsKey(player.getUniqueId())
            || pendingDespawnNames.containsKey(player.getUniqueId()));
  }

  public String getPendingSpawnName(UUID uuid) {
    return pendingSpawnNames.get(uuid);
  }

  public String getPendingDespawnName(UUID uuid) {
    return pendingDespawnNames.get(uuid);
  }

  private void tickBots() {
    for (FakePlayer fp : byUuid.values()) {
      Player bot = fp.getPlayer();
      if (bot == null || !bot.isOnline()) continue;
      if (!fp.isFrozen()) {
        NmsPlayerSpawner.tickPhysics(bot);
      }
      if (plugin.getFppApiImpl() != null) {
        plugin.getFppApiImpl().fireTickHandlers(fp, bot);
      }
    }
  }

  private String uniqueName(String base) {
    String cleaned = sanitizeName(base);
    String candidate = cleaned;
    int suffix = 1;
    while (byName.containsKey(candidate.toLowerCase(Locale.ROOT))
        || Bukkit.getPlayerExact(candidate) != null) {
      String tail = String.valueOf(suffix++);
      candidate = cleaned.substring(0, Math.min(cleaned.length(), 16 - tail.length())) + tail;
    }
    return candidate;
  }

  private static String sanitizeName(String raw) {
    String cleaned = raw == null ? "FakePlayer" : raw.replaceAll("[^A-Za-z0-9_]", "");
    if (cleaned.isBlank()) cleaned = "FakePlayer";
    return cleaned.substring(0, Math.min(cleaned.length(), 16));
  }
}
