package me.bill.fakePlayerPlugin.fakeplayer;

import com.destroystokyo.paper.profile.PlayerProfile;
import me.bill.fakePlayerPlugin.FakePlayerPlugin;
import me.bill.fakePlayerPlugin.config.Config;
import me.bill.fakePlayerPlugin.lang.Lang;
import me.bill.fakePlayerPlugin.util.FppLogger;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class FakePlayerManager {

    /** PDC key used to tag the physics-body zombie. */
    public static NamespacedKey FAKE_PLAYER_KEY;

    private final FakePlayerPlugin plugin;
    private final Map<UUID, FakePlayer> activePlayers = new ConcurrentHashMap<>();
    private final Set<String> usedNames = new HashSet<>();
    private ChunkLoader chunkLoader;

    public void setChunkLoader(ChunkLoader cl) { this.chunkLoader = cl; }


    public FakePlayerManager(FakePlayerPlugin plugin) {
        this.plugin = plugin;
        FAKE_PLAYER_KEY = new NamespacedKey(plugin, "fake_player_name");
    }

    // ── Spawn ────────────────────────────────────────────────────────────────

    /**
     * Spawns {@code count} fake players at the given location.
     * <p>
     * Strategy:
     * <ol>
     *   <li>Pre-generate and reserve all names immediately.</li>
     *   <li>Kick off ALL skin fetches in parallel right away (SkinFetcher
     *       queues them 200 ms apart so Mojang's rate limit is respected).</li>
     *   <li>Once every skin is ready, begin the visual spawn chain — each bot
     *       appears after a random join delay with no HTTP wait in between.</li>
     * </ol>
     *
     * @return number of bots queued (-1 if at limit)
     */
    public int spawn(Location location, int count) {
        int maxBots = Config.maxBots();
        if (maxBots > 0) {
            int available = maxBots - activePlayers.size();
            if (available <= 0) return -1;
            count = Math.min(count, available);
        }

        // ── Step 1: pre-generate names & FakePlayer objects ──────────────────
        List<FakePlayer> batch = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String name = generateName();
            if (name == null) break;
            UUID uuid = UUID.randomUUID();
            PlayerProfile profile = Bukkit.createProfile(uuid, name);
            FakePlayer fp = new FakePlayer(uuid, name, profile);
            fp.setSpawnLocation(location);
            activePlayers.put(uuid, fp);
            batch.add(fp);
        }
        if (batch.isEmpty()) return 0;

        int total = batch.size();
        Config.debug("Queued " + total + " bot(s) — fetching skins in parallel.");

        if (!Config.fetchSkin()) {
            // No skin fetching — start visual chain immediately
            Bukkit.getScheduler().runTask(plugin, () -> visualChain(batch, 0, location));
            return total;
        }

        // ── Step 2: fetch ALL skins in parallel right now ────────────────────
        // AtomicInteger counts how many fetches have completed.
        // When it reaches batch.size(), the visual chain starts.
        java.util.concurrent.atomic.AtomicInteger done =
                new java.util.concurrent.atomic.AtomicInteger(0);

        for (FakePlayer fp : batch) {
            String name = fp.getName();
            SkinFetcher.fetchAsync(name, (value, signature) -> {
                // Callback fires on the SkinFetcher thread — update fp safely
                if (value != null) {
                    fp.setSkin(value, signature);
                    Config.debug("Skin ready: " + name);
                }
                // When last skin completes, kick off the visual chain on main thread
                if (done.incrementAndGet() == total) {
                    Bukkit.getScheduler().runTask(plugin,
                            () -> visualChain(batch, 0, location));
                }
            });
        }

        return total;
    }

    /**
     * Visually spawns the bot at {@code index} in {@code batch}, then
     * schedules the next one after a random join delay.
     * Skins are already loaded — no HTTP happens here.
     */
    private void visualChain(List<FakePlayer> batch, int index, Location location) {
        if (index >= batch.size()) return;

        FakePlayer fp = batch.get(index);
        // Guard: bot may have been deleted while skins were loading
        if (!activePlayers.containsKey(fp.getUuid())) {
            visualChain(batch, index + 1, location);
            return;
        }

        finishSpawn(fp, fp.getName(), location);

        int delayMin = Config.joinDelayMin();
        int delayMax = Math.max(delayMin, Config.joinDelayMax());
        long delay;
        if (delayMax <= 0) {
            delay = 0;
        } else {
            int spread = delayMax - delayMin;
            delay = Math.max(1, delayMin + (spread > 0
                    ? ThreadLocalRandom.current().nextInt(spread + 1)
                    : 0));
        }

        Bukkit.getScheduler().runTaskLater(plugin,
                () -> visualChain(batch, index + 1, location), delay);
    }

    private void finishSpawn(FakePlayer fp, String botName, Location spawnLoc) {
        // Spawn physical body only if enabled in config
        if (Config.spawnBody()) {
            Entity body = FakePlayerBody.spawn(fp, spawnLoc);
            if (body != null) {
                fp.setPhysicsEntity(body);
                fp.setBodyUUID(body.getUniqueId());
            }
        }

        // Tab list entry — always added regardless of spawn-body
        for (Player online : Bukkit.getOnlinePlayers()) {
            PacketHelper.sendTabListAdd(online, fp);
        }

        // Join message
        if (Config.joinMessage()) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Component msg = Lang.get("bot-join", "name", botName);
                for (Player online : Bukkit.getOnlinePlayers()) online.sendMessage(msg);
                Bukkit.getConsoleSender().sendMessage(msg);
            }, 1L);
        }

        // Entity spawn + rotation packets — only if body was created
        if (Config.spawnBody()) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                float yaw   = spawnLoc.getYaw();
                float pitch = spawnLoc.getPitch();
                for (Player online : Bukkit.getOnlinePlayers()) {
                    PacketHelper.spawnFakePlayer(online, fp, spawnLoc);
                    PacketHelper.sendRotation(online, fp, yaw, pitch, yaw);
                }
            }, 3L);
        }
    }

    // ── Remove all ───────────────────────────────────────────────────────────

    public void removeAll() {
        if (activePlayers.isEmpty()) return;

        int delayMin = Config.leaveDelayMin();
        int delayMax = Math.max(delayMin, Config.leaveDelayMax());
        boolean stagger = delayMax > 0;

        // Snapshot and clear registry immediately — prevents double-removal
        List<FakePlayer> toRemove = new ArrayList<>(activePlayers.values());
        activePlayers.clear();
        usedNames.clear();

        for (int i = 0; i < toRemove.size(); i++) {
            FakePlayer fp = toRemove.get(i);

            // Each bot gets a random delay; index offset guarantees stagger
            long leaveDelay;
            if (!stagger) {
                leaveDelay = 0;
            } else {
                int spread = delayMax - delayMin;
                leaveDelay = i + delayMin + (spread > 0
                        ? ThreadLocalRandom.current().nextInt(spread + 1)
                        : 0);
            }

            final FakePlayer target = fp;
            Runnable doVisualRemove = () -> {
                // Kill body and release chunks at the same time as the leave message
                Entity body = target.getPhysicsEntity();
                if (body != null && body.isValid()) body.remove();
                if (chunkLoader != null) chunkLoader.releaseForBot(target);

                List<Player> snapshot = new ArrayList<>(Bukkit.getOnlinePlayers());
                for (Player online : snapshot) {
                    PacketHelper.sendTabListRemove(online, target);
                    PacketHelper.despawnFakePlayer(online, target);
                }
                if (Config.leaveMessage()) {
                    Component msg = Lang.get("bot-leave", "name", target.getName());
                    for (Player online : snapshot) online.sendMessage(msg);
                    Bukkit.getConsoleSender().sendMessage(msg);
                }
                Config.debug("Removed bot: " + target.getName());
            };

            if (leaveDelay <= 0) {
                Bukkit.getScheduler().runTask(plugin, doVisualRemove);
            } else {
                Bukkit.getScheduler().runTaskLater(plugin, doVisualRemove, leaveDelay);
            }
        }

        Config.debug("Staggered visual removal of " + toRemove.size() + " fake player(s).");
    }

    // ── Delete one ───────────────────────────────────────────────────────────

    /**
     * Deletes a single fake player by name — kills the physics body, removes
     * from tab list, despawns the visual, broadcasts leave message.
     *
     * @return true if a bot with that name was found and removed, false otherwise
     */
    public boolean delete(String name) {
        FakePlayer fp = null;
        for (FakePlayer candidate : activePlayers.values()) {
            if (candidate.getName().equalsIgnoreCase(name)) { fp = candidate; break; }
        }
        if (fp == null) return false;

        final FakePlayer target = fp;

        // Remove from registry immediately — prevents double-delete and clears tab-complete
        activePlayers.remove(target.getUuid());
        usedNames.remove(target.getName());

        // Defer body removal, tab-list, despawn and leave message together
        int delayMin = Config.leaveDelayMin();
        int delayMax = Math.max(delayMin, Config.leaveDelayMax());
        long leaveDelay;
        if (delayMax <= 0) {
            leaveDelay = 0;
        } else {
            int spread = delayMax - delayMin;
            leaveDelay = Math.max(1, delayMin + (spread > 0
                    ? ThreadLocalRandom.current().nextInt(spread + 1)
                    : 0));
        }

        Runnable doVisualRemove = () -> {
            // Kill body and release chunks at the same time as the leave message
            Entity body = target.getPhysicsEntity();
            if (body != null && body.isValid()) body.remove();
            if (chunkLoader != null) chunkLoader.releaseForBot(target);

            List<Player> snapshot = new ArrayList<>(Bukkit.getOnlinePlayers());
            for (Player online : snapshot) {
                PacketHelper.sendTabListRemove(online, target);
                PacketHelper.despawnFakePlayer(online, target);
            }
            if (Config.leaveMessage()) {
                Component leaveMsg = Lang.get("bot-leave", "name", target.getName());
                for (Player online : snapshot) online.sendMessage(leaveMsg);
                Bukkit.getConsoleSender().sendMessage(leaveMsg);
            }
            Config.debug("Deleted fake player: " + name);
        };

        if (leaveDelay <= 0) {
            Bukkit.getScheduler().runTask(plugin, doVisualRemove);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, doVisualRemove, leaveDelay);
        }

        return true;
    }

    /** Synchronous instant removal — only call from onDisable where schedulers can't run. */
    public void removeAllSync() {
        if (activePlayers.isEmpty()) return;
        List<Player> snapshot = new ArrayList<>(Bukkit.getOnlinePlayers());
        for (FakePlayer fp : activePlayers.values()) {
            Entity body = fp.getPhysicsEntity();
            if (body != null && body.isValid()) body.remove();
            if (chunkLoader != null) chunkLoader.releaseForBot(fp);
            for (Player online : snapshot) {
                PacketHelper.sendTabListRemove(online, fp);
                PacketHelper.despawnFakePlayer(online, fp);
            }
        }
        activePlayers.clear();
        usedNames.clear();
    }

    // ── Remove by name (post-death cleanup) ──────────────────────────────────

    /** Removes a fake player by name (called after permanent death). */
    public void removeByName(String name) {
        activePlayers.values().removeIf(fp -> {
            if (!fp.getName().equals(name)) return false;
            usedNames.remove(fp.getName());
            Config.debug("Removed from registry: " + name);
            return true;
        });
    }

    // ── Sync to joining player ────────────────────────────────────────────────

    /**
     * Syncs all existing fake players to a newly joined real player.
     */
    public void syncToPlayer(Player player) {
        for (FakePlayer fp : activePlayers.values()) {
            Entity body = fp.getPhysicsEntity();
            Location loc = (body != null && body.isValid())
                    ? body.getLocation()
                    : fp.getSpawnLocation() != null
                        ? fp.getSpawnLocation()
                        : player.getWorld().getSpawnLocation();

            PacketHelper.sendTabListAdd(player, fp);

            if (Config.spawnBody()) {
                final Location spawnLoc = loc;
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    PacketHelper.spawnFakePlayer(player, fp, spawnLoc);
                    PacketHelper.sendRotation(player, fp, spawnLoc.getYaw(), spawnLoc.getPitch(), spawnLoc.getYaw());
                }, 2L);
            }
        }
    }

    // ── Queries ──────────────────────────────────────────────────────────────

    /** Returns the FakePlayer whose physics body has the given entity id, or null. */
    public FakePlayer getByEntity(Entity entity) {
        for (FakePlayer fp : activePlayers.values()) {
            Entity body = fp.getPhysicsEntity();
            if (body != null && body.getEntityId() == entity.getEntityId()) return fp;
        }
        return null;
    }

    /** Returns a list of all active bot names (for tab-completion). */
    public List<String> getActiveNames() {
        return activePlayers.values().stream().map(FakePlayer::getName).collect(Collectors.toList());
    }

    public Collection<FakePlayer> getActivePlayers() {
        return Collections.unmodifiableCollection(activePlayers.values());
    }

    public int getCount() { return activePlayers.size(); }

    // ── Name generation ──────────────────────────────────────────────────────

    private String generateName() {
        List<String> pool = Config.namePool();
        if (pool.isEmpty()) return fallbackName();

        // Build a list of candidate indices that are not already in use
        List<Integer> candidates = new ArrayList<>(pool.size());
        for (int i = 0; i < pool.size(); i++) {
            String n = pool.get(i);
            // Skip if already active, pending spawn, or a real online player
            if (!usedNames.contains(n) && Bukkit.getPlayerExact(n) == null) {
                candidates.add(i);
            }
        }

        if (!candidates.isEmpty()) {
            // Pick a random candidate index — O(1) random access, no list copy needed
            int pick = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
            String name = pool.get(pick);
            usedNames.add(name);
            return name;
        }

        // Pool exhausted — fall back to generated name
        return fallbackName();
    }

    private String fallbackName() {
        String generated;
        int attempts = 0;
        do {
            generated = "Bot" + ThreadLocalRandom.current().nextInt(1000, 9999);
            if (++attempts > 200) return null; // safety cap
        } while (usedNames.contains(generated) || Bukkit.getPlayerExact(generated) != null);
        usedNames.add(generated);
        return generated;
    }
}
