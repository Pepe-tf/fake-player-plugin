package me.bill.fakePlayerPlugin.command;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class StorageStore {

  private final JavaPlugin plugin;

  private final Map<String, LinkedHashMap<String, StoragePoint>> storages =
      new ConcurrentHashMap<>();
  private File file;

  public StorageStore(JavaPlugin plugin) {
    this.plugin = plugin;
  }

  public void load() {
    file = new File(plugin.getDataFolder(), "data/storages.yml");
    if (!file.exists()) return;

    YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
    for (String botKey : yaml.getKeys(false)) {
      ConfigurationSection botSec = yaml.getConfigurationSection(botKey);
      if (botSec == null) continue;

      LinkedHashMap<String, StoragePoint> botMap = new LinkedHashMap<>();
      for (String storageKey : botSec.getKeys(false)) {
        ConfigurationSection sec = botSec.getConfigurationSection(storageKey);
        if (sec == null) continue;
        String worldName = sec.getString("world");
        World world = worldName != null ? Bukkit.getWorld(worldName) : null;
        if (world == null) continue;
        Location loc =
            new Location(
                world,
                sec.getDouble("x"),
                sec.getDouble("y"),
                sec.getDouble("z"),
                (float) sec.getDouble("yaw"),
                (float) sec.getDouble("pitch"));
        String displayName = sec.getString("display-name", storageKey);
        botMap.put(storageKey.toLowerCase(Locale.ROOT), new StoragePoint(displayName, loc));
      }
      if (!botMap.isEmpty()) {
        storages.put(botKey.toLowerCase(Locale.ROOT), botMap);
      }
    }
  }

  public void save() {
    if (file == null) file = new File(plugin.getDataFolder(), "data/storages.yml");
    if (file.getParentFile() != null) file.getParentFile().mkdirs();

    YamlConfiguration yaml = new YamlConfiguration();
    for (Map.Entry<String, LinkedHashMap<String, StoragePoint>> botEntry : storages.entrySet()) {
      String botKey = botEntry.getKey();
      for (Map.Entry<String, StoragePoint> storageEntry : botEntry.getValue().entrySet()) {
        String key = botKey + "." + storageEntry.getKey() + ".";
        StoragePoint point = storageEntry.getValue();
        Location loc = point.location();
        yaml.set(key + "display-name", point.name());
        yaml.set(key + "world", loc.getWorld() != null ? loc.getWorld().getName() : null);
        yaml.set(key + "x", loc.getX());
        yaml.set(key + "y", loc.getY());
        yaml.set(key + "z", loc.getZ());
        yaml.set(key + "yaw", (double) loc.getYaw());
        yaml.set(key + "pitch", (double) loc.getPitch());
      }
    }

    try {
      yaml.save(file);
    } catch (IOException ex) {
      plugin.getLogger().warning("[FPP] Could not save storages.yml: " + ex.getMessage());
    }
  }

  public void setStorage(String botName, String storageName, Location loc) {
    String botKey = botName.toLowerCase(Locale.ROOT);
    String storageKey = storageName.toLowerCase(Locale.ROOT);
    LinkedHashMap<String, StoragePoint> botMap =
        storages.computeIfAbsent(botKey, k -> new LinkedHashMap<>());
    botMap.put(storageKey, new StoragePoint(storageName, loc.clone()));
    save();
  }

  public List<StoragePoint> getStorages(String botName) {
    LinkedHashMap<String, StoragePoint> botMap = storages.get(botName.toLowerCase(Locale.ROOT));
    if (botMap == null || botMap.isEmpty()) return List.of();
    List<StoragePoint> out = new ArrayList<>(botMap.values());
    out.sort(
        Comparator.comparingInt((StoragePoint p) -> numericName(p.name()))
            .thenComparing(p -> p.name().toLowerCase(Locale.ROOT)));
    return Collections.unmodifiableList(out);
  }

  public Set<String> getStorageNames(String botName) {
    LinkedHashMap<String, StoragePoint> botMap = storages.get(botName.toLowerCase(Locale.ROOT));
    if (botMap == null || botMap.isEmpty()) return Set.of();
    Set<String> out = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    for (StoragePoint point : botMap.values()) out.add(point.name());
    return Collections.unmodifiableSet(out);
  }

  public boolean removeStorage(String botName, String storageName) {
    LinkedHashMap<String, StoragePoint> botMap = storages.get(botName.toLowerCase(Locale.ROOT));
    if (botMap == null) return false;
    boolean removed = botMap.remove(storageName.toLowerCase(Locale.ROOT)) != null;
    if (removed) {
      if (botMap.isEmpty()) storages.remove(botName.toLowerCase(Locale.ROOT));
      save();
    }
    return removed;
  }

  public int clearStorages(String botName) {
    LinkedHashMap<String, StoragePoint> botMap = storages.remove(botName.toLowerCase(Locale.ROOT));
    int count = botMap != null ? botMap.size() : 0;
    if (count > 0) save();
    return count;
  }

  public int getStorageCount(String botName) {
    LinkedHashMap<String, StoragePoint> botMap = storages.get(botName.toLowerCase(Locale.ROOT));
    return botMap != null ? botMap.size() : 0;
  }

  public int renameBot(String oldName, String newName) {
    String oldKey = oldName.toLowerCase(Locale.ROOT);
    String newKey = newName.toLowerCase(Locale.ROOT);
    if (oldKey.equals(newKey)) return 0;
    LinkedHashMap<String, StoragePoint> botMap = storages.remove(oldKey);
    if (botMap == null || botMap.isEmpty()) return 0;

    storages.merge(
        newKey,
        botMap,
        (existing, incoming) -> {
          existing.putAll(incoming);
          return existing;
        });
    save();
    return botMap.size();
  }

  public String nextAutoName(String botName) {
    LinkedHashMap<String, StoragePoint> botMap = storages.get(botName.toLowerCase(Locale.ROOT));
    int next = 1;
    if (botMap != null) {
      while (botMap.containsKey(String.valueOf(next))) next++;
    }
    return String.valueOf(next);
  }

  private static int numericName(String name) {
    try {
      return Integer.parseInt(name);
    } catch (NumberFormatException ignored) {
      return Integer.MAX_VALUE;
    }
  }

  public record StoragePoint(String name, Location location) {}
}
