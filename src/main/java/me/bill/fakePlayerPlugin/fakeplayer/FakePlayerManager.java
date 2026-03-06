package me.bill.fakePlayerPlugin.fakeplayer;

import com.destroystokyo.paper.profile.PlayerProfile;
import me.bill.fakePlayerPlugin.FakePlayerPlugin;
import me.bill.fakePlayerPlugin.config.Config;
import me.bill.fakePlayerPlugin.util.FppLogger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class FakePlayerManager {

    private final FakePlayerPlugin plugin;
    private final Map<UUID, FakePlayer> activePlayers = new ConcurrentHashMap<>();
    private final Set<String> usedNames = new HashSet<>();

    private static final String[] NAME_POOL = {
            "Alex", "Steve", "Notch", "Herobrine", "Jeb_",
            "Dream", "Technoblade", "xQc", "Pogchamp", "CraftKing",
            "PixelWolf", "ShadowByte", "NightCrawler", "IronFist", "CreeperBro",
            "DiamondDave", "GhostMiner", "LavaKing", "FrostByte", "StarForge"
    };

    public FakePlayerManager(FakePlayerPlugin plugin) {
        this.plugin = plugin;
    }

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Spawns {@code count} fake players at the given location.
     * @return number actually spawned
     */
    public int spawn(Location location, int count) {
        int spawned = 0;
        for (int i = 0; i < count; i++) {
            String name = generateName();
            UUID uuid   = UUID.randomUUID();
            PlayerProfile profile = Bukkit.createProfile(uuid, name);

            FakePlayer fp = new FakePlayer(uuid, name, profile);
            fp.setSpawnLocation(location);
            activePlayers.put(uuid, fp);

            // Try to create a real NMS ServerPlayer first.
            // placeNewPlayer handles tab list + server count + world entity automatically.
            Object nmsPlayer = NmsHelper.addFakePlayer(fp, location);
            if (nmsPlayer != null) {
                fp.setNmsPlayer(nmsPlayer);
                Config.debug("Spawned real NMS player: " + name);
            } else {
                // Fallback: packet-only mode
                Config.debug("NmsHelper failed, using packet fallback for: " + name);
                for (Player p : Bukkit.getOnlinePlayers()) {
                    PacketHelper.sendTabListAdd(p, fp);
                    // Delay entity packet by 2 ticks so tab entry is processed first
                    final Player receiver = p;
                    final Location spawnLoc = location;
                    Bukkit.getScheduler().runTaskLater(plugin, () ->
                            PacketHelper.spawnFakePlayer(receiver, fp, spawnLoc), 2L);
                }
                // Broadcast join message only in fallback mode
                // (real NMS players trigger it automatically via vanilla)
                final String finalName = name;
                Bukkit.getScheduler().runTaskLater(plugin, () ->
                        Bukkit.broadcast(
                                Component.translatable("multiplayer.player.joined",
                                        Component.text(finalName).color(NamedTextColor.YELLOW))
                                        .color(NamedTextColor.YELLOW)), 1L);
            }

            spawned++;
        }

        FppLogger.success("Spawned " + spawned + " fake player(s). Total active: " + activePlayers.size());
        return spawned;
    }

    /** Removes all active fake players. */
    public void removeAll() {
        if (activePlayers.isEmpty()) return;
        int count = activePlayers.size();

        for (FakePlayer fp : activePlayers.values()) {
            if (fp.getNmsPlayer() != null) {
                // Real NMS player — remove from server (handles tab list + count automatically)
                NmsHelper.removeFakePlayer(fp);
            } else {
                // Packet fallback
                for (Player p : Bukkit.getOnlinePlayers()) {
                    PacketHelper.sendTabListRemove(p, fp);
                    PacketHelper.despawnFakePlayer(p, fp);
                }
                final String name = fp.getName();
                Bukkit.getScheduler().runTaskLater(plugin, () ->
                        Bukkit.broadcast(
                                Component.translatable("multiplayer.player.left",
                                        Component.text(name).color(NamedTextColor.YELLOW))
                                        .color(NamedTextColor.YELLOW)), 1L);
            }
        }

        Config.debug("Removed " + count + " fake player(s).");
        activePlayers.clear();
        usedNames.clear();
    }

    /**
     * Syncs all existing fake players to a newly joined real player.
     * Only needed in packet-fallback mode; real NMS players handle this automatically.
     */
    public void syncToPlayer(Player player) {
        for (FakePlayer fp : activePlayers.values()) {
            if (fp.getNmsPlayer() != null) continue; // NMS handles it
            Location loc = fp.getSpawnLocation() != null
                    ? fp.getSpawnLocation()
                    : player.getWorld().getSpawnLocation();
            // Tab list first, then entity 2 ticks later
            PacketHelper.sendTabListAdd(player, fp);
            final Location spawnLoc = loc;
            Bukkit.getScheduler().runTaskLater(plugin, () ->
                    PacketHelper.spawnFakePlayer(player, fp, spawnLoc), 2L);
        }
    }

    public Collection<FakePlayer> getActivePlayers() {
        return Collections.unmodifiableCollection(activePlayers.values());
    }

    public int getCount() { return activePlayers.size(); }

    // ── Name generation ──────────────────────────────────────────────────────

    private String generateName() {
        List<String> pool = new ArrayList<>(Arrays.asList(NAME_POOL));
        Collections.shuffle(pool);
        for (String n : pool) {
            if (!usedNames.contains(n) && Bukkit.getPlayerExact(n) == null) {
                usedNames.add(n);
                return n;
            }
        }
        String generated;
        do { generated = "Player" + ThreadLocalRandom.current().nextInt(1000, 9999); }
        while (usedNames.contains(generated));
        usedNames.add(generated);
        return generated;
    }
}
