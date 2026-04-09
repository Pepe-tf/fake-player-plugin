package me.bill.fakePlayerPlugin.command;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe registry of named waypoint routes, persisted to
 * {@code plugins/FakePlayerPlugin/data/waypoints.yml}.
 *
 * <p>Each route is an ordered list of {@link Location}s that a bot can
 * patrol in a loop via {@code /fpp move <bot> --wp <name>}.
 *
 * <p>Names are stored and looked-up case-insensitively (lowercased keys).
 * Saves are synchronous and triggered on every mutation.
 *
 * <p>Lifecycle: call {@link #load()} once during plugin startup, then
 * use {@link #addPos}/{@link #removePos}/{@link #delete} as needed - each mutation auto-saves.
 */
public final class WaypointStore {

    private final JavaPlugin plugin;
    /** routeName (lowercase) → ordered list of positions */
    private final Map<String, List<Location>> routes = new ConcurrentHashMap<>();
    private File file;

    public WaypointStore(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // ── Load / Save ───────────────────────────────────────────────────────────

    /** Loads all waypoint routes from {@code data/waypoints.yml}. */
    public void load() {
        file = new File(plugin.getDataFolder(), "data/waypoints.yml");
        if (!file.exists()) return;

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        int routeCount = 0, posCount = 0;

        for (String routeName : yaml.getKeys(false)) {
            ConfigurationSection routeSection = yaml.getConfigurationSection(routeName);
            if (routeSection == null) continue;

            List<Location> positions = new ArrayList<>();
            // Positions stored as numbered entries: "0", "1", "2", ...
            int i = 0;
            while (routeSection.contains(String.valueOf(i))) {
                ConfigurationSection pos = routeSection.getConfigurationSection(String.valueOf(i));
                if (pos != null) {
                    String worldName = pos.getString("world", "world");
                    double x     = pos.getDouble("x");
                    double y     = pos.getDouble("y");
                    double z     = pos.getDouble("z");
                    float  yaw   = (float) pos.getDouble("yaw");
                    float  pitch = (float) pos.getDouble("pitch");
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
            plugin.getLogger().info("[FPP] Loaded " + routeCount + " waypoint route(s) with "
                    + posCount + " total position(s).");
        }
    }

    /** Saves all routes to disk. Called automatically on every mutation. */
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
                yaml.set(prefix + "x",     loc.getX());
                yaml.set(prefix + "y",     loc.getY());
                yaml.set(prefix + "z",     loc.getZ());
                yaml.set(prefix + "yaw",   (double) loc.getYaw());
                yaml.set(prefix + "pitch", (double) loc.getPitch());
            }
        }
        try {
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("[FPP] Could not save waypoints.yml: " + ex.getMessage());
        }
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    /**
     * Creates an empty named route. Does nothing if the route already exists.
     *
     * @return {@code true} if the route was newly created; {@code false} if it already existed
     */
    public boolean createRoute(String name) {
        String key = name.toLowerCase();
        if (routes.containsKey(key)) return false;
        routes.put(key, new ArrayList<>());
        // Empty routes are held in memory only - they are not written to disk until a
        // position is added (empty YAML sections carry no useful data across restarts).
        return true;
    }

    /**
     * Appends a position to the named route. Creates the route if it does not exist.
     *
     * @return the new total position count for this route
     */
    public int addPos(String name, Location loc) {
        List<Location> positions = routes.computeIfAbsent(name.toLowerCase(), k -> new ArrayList<>());
        positions.add(loc.clone());
        save();
        return positions.size();
    }

    /**
     * Removes the position at the given 0-based index from the named route.
     * Auto-deletes the route if it becomes empty.
     *
     * @return {@code true} if removed successfully
     */
    public boolean removePos(String name, int index) {
        List<Location> positions = routes.get(name.toLowerCase());
        if (positions == null || index < 0 || index >= positions.size()) return false;
        positions.remove(index);
        if (positions.isEmpty()) routes.remove(name.toLowerCase());
        save();
        return true;
    }

    /**
     * Deletes the entire named route (all positions).
     *
     * @return {@code true} if the route existed and was deleted
     */
    public boolean delete(String name) {
        boolean removed = routes.remove(name.toLowerCase()) != null;
        if (removed) save();
        return removed;
    }

    /**
     * Clears all positions from the route and removes it.
     *
     * @return {@code true} if the route existed and was cleared
     */
    public boolean clear(String name) {
        List<Location> positions = routes.remove(name.toLowerCase());
        boolean had = positions != null && !positions.isEmpty();
        if (had) save();
        return had;
    }

    /**
     * Returns an unmodifiable snapshot of the positions in the named route,
     * or {@code null} if the route does not exist or is empty.
     * Name lookup is case-insensitive.
     */
    public List<Location> getRoute(String name) {
        List<Location> positions = routes.get(name.toLowerCase());
        if (positions == null || positions.isEmpty()) return null;
        return Collections.unmodifiableList(new ArrayList<>(positions));
    }

    /** Returns {@code true} if a route with this name exists and has at least one position. */
    public boolean exists(String name) {
        List<Location> positions = routes.get(name.toLowerCase());
        return positions != null && !positions.isEmpty();
    }

    /** Returns {@code true} if a route with this name exists (including empty routes created via {@link #createRoute}). */
    public boolean hasRoute(String name) {
        return routes.containsKey(name.toLowerCase());
    }

    /** Returns an unmodifiable sorted set of all route names (lowercase). */
    public Set<String> getNames() {
        return Collections.unmodifiableSet(new TreeSet<>(routes.keySet()));
    }

    /** Returns the number of stored routes. */
    public int size() {
        return routes.size();
    }

    /** Returns the number of positions in the named route, or 0 if not found. */
    public int getPositionCount(String name) {
        List<Location> positions = routes.get(name.toLowerCase());
        return positions != null ? positions.size() : 0;
    }
}

