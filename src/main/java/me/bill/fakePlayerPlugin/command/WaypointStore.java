package me.bill.fakePlayerPlugin.command;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class WaypointStore {

  private final JavaPlugin plugin;

  private final Map<String, List<Location>> routes = new ConcurrentHashMap<>();
  private File file;

  public WaypointStore(JavaPlugin plugin) {
    this.plugin = plugin;
  }

  public void load() {
    file = new File(plugin.getDataFolder(), "data/waypoints.yml");
    if (!file.exists()) return;

    YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
    int routeCount = 0, posCount = 0;

    for (String routeName : yaml.getKeys(false)) {
      ConfigurationSection routeSection = yaml.getConfigurationSection(routeName);
      if (routeSection == null) continue;

      List<Location> positions = new ArrayList<>();

      int i = 0;
      while (routeSection.contains(String.valueOf(i))) {
        ConfigurationSection pos = routeSection.getConfigurationSection(String.valueOf(i));
        if (pos != null) {
          String worldName = pos.getString("world", "world");
          double x = pos.getDouble("x");
          double y = pos.getDouble("y");
          double z = pos.getDouble("z");
          float yaw = (float) pos.getDouble("yaw");
          float pitch = (float) pos.getDouble("pitch");
          World w = Bukkit.getWorld(worldName != null ? worldName : "world");
          if (w != null) {
            positions.add(new Location(w, x, y, z, yaw, pitch));
            posCount++;
          }
        }
        i++;
      }
      if (!positions.isEmpty()) {
        routes.put(routeName.toLowerCase(), positions);
        routeCount++;
      }
    }
    if (routeCount > 0) {
      plugin
          .getLogger()
          .info(
              "[FPP] Loaded "
                  + routeCount
                  + " waypoint route(s) with "
                  + posCount
                  + " total position(s).");
    }
  }

  public void save() {
    if (file == null) {
      file = new File(plugin.getDataFolder(), "data/waypoints.yml");
    }
    file.getParentFile().mkdirs();

    YamlConfiguration yaml = new YamlConfiguration();
    for (Map.Entry<String, List<Location>> e : routes.entrySet()) {
      String routeName = e.getKey();
      List<Location> positions = e.getValue();
      for (int i = 0; i < positions.size(); i++) {
        Location loc = positions.get(i);
        String prefix = routeName + "." + i + ".";
        yaml.set(prefix + "world", loc.getWorld() != null ? loc.getWorld().getName() : "world");
        yaml.set(prefix + "x", loc.getX());
        yaml.set(prefix + "y", loc.getY());
        yaml.set(prefix + "z", loc.getZ());
        yaml.set(prefix + "yaw", (double) loc.getYaw());
        yaml.set(prefix + "pitch", (double) loc.getPitch());
      }
    }
    try {
      yaml.save(file);
    } catch (IOException ex) {
      plugin.getLogger().warning("[FPP] Could not save waypoints.yml: " + ex.getMessage());
    }
  }

  public boolean createRoute(String name) {
    String key = name.toLowerCase();
    if (routes.containsKey(key)) return false;
    routes.put(key, new ArrayList<>());

    return true;
  }

  public int addPos(String name, Location loc) {
    List<Location> positions = routes.computeIfAbsent(name.toLowerCase(), k -> new ArrayList<>());
    positions.add(loc.clone());
    save();
    return positions.size();
  }

  public boolean removePos(String name, int index) {
    List<Location> positions = routes.get(name.toLowerCase());
    if (positions == null || index < 0 || index >= positions.size()) return false;
    positions.remove(index);
    if (positions.isEmpty()) routes.remove(name.toLowerCase());
    save();
    return true;
  }

  public boolean delete(String name) {
    boolean removed = routes.remove(name.toLowerCase()) != null;
    if (removed) save();
    return removed;
  }

  public boolean clear(String name) {
    List<Location> positions = routes.remove(name.toLowerCase());
    boolean had = positions != null && !positions.isEmpty();
    if (had) save();
    return had;
  }

  public List<Location> getRoute(String name) {
    List<Location> positions = routes.get(name.toLowerCase());
    if (positions == null || positions.isEmpty()) return null;
    return Collections.unmodifiableList(new ArrayList<>(positions));
  }

  public boolean exists(String name) {
    List<Location> positions = routes.get(name.toLowerCase());
    return positions != null && !positions.isEmpty();
  }

  public boolean hasRoute(String name) {
    return routes.containsKey(name.toLowerCase());
  }

  public Set<String> getNames() {
    return Collections.unmodifiableSet(new TreeSet<>(routes.keySet()));
  }

  public int size() {
    return routes.size();
  }

  public int getPositionCount(String name) {
    List<Location> positions = routes.get(name.toLowerCase());
    return positions != null ? positions.size() : 0;
  }
}
