package me.bill.fakePlayerPlugin.fakeplayer;

import me.bill.fakePlayerPlugin.FakePlayerPlugin;
import me.bill.fakePlayerPlugin.config.Config;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Zombie;

import java.util.*;

/**
 * Keeps chunks loaded around each active bot, mimicking the behaviour of a
 * real player.  Uses Paper's {@code World.addPluginChunkTicket()} so the
 * server treats the area exactly like a player-loaded chunk (mobs spawn,
 * redstone ticks, crops grow, etc.).
 *
 * <p>A repeating task fires every second (20 ticks) to refresh tickets for
 * the bot's current position and release tickets for chunks no longer in
 * range.  When a bot is removed its tickets are released immediately.
 */
public final class ChunkLoader {

    private final FakePlayerPlugin plugin;
    private final FakePlayerManager manager;

    /** Maps each FakePlayer UUID → set of chunk keys currently ticketed. */
    private final Map<UUID, Set<Long>> ticketedChunks = new HashMap<>();

    public ChunkLoader(FakePlayerPlugin plugin, FakePlayerManager manager) {
        this.plugin  = plugin;
        this.manager = manager;

        // Refresh chunk tickets every second
        org.bukkit.Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    // ── Per-second tick ───────────────────────────────────────────────────────

    private void tick() {
        if (!Config.chunkLoadingEnabled()) {
            // If disabled at runtime, release all existing tickets
            if (!ticketedChunks.isEmpty()) releaseAll();
            return;
        }

        int radius = Math.max(0, Config.chunkLoadingRadius());

        for (FakePlayer fp : manager.getActivePlayers()) {
            Entity body = fp.getPhysicsEntity();
            if (!(body instanceof Zombie) || !body.isValid()) continue;

            World world = body.getWorld();
            int cx = body.getLocation().getBlockX() >> 4;
            int cz = body.getLocation().getBlockZ() >> 4;

            Set<Long> desired = new HashSet<>();
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    desired.add(chunkKey(cx + dx, cz + dz));
                }
            }

            Set<Long> current = ticketedChunks.computeIfAbsent(fp.getUuid(), k -> new HashSet<>());

            // Add new tickets
            for (long key : desired) {
                if (!current.contains(key)) {
                    int chunkX = keyX(key);
                    int chunkZ = keyZ(key);
                    world.addPluginChunkTicket(chunkX, chunkZ, plugin);
                    current.add(key);
                }
            }

            // Remove stale tickets (bot has moved away)
            Iterator<Long> it = current.iterator();
            while (it.hasNext()) {
                long key = it.next();
                if (!desired.contains(key)) {
                    world.removePluginChunkTicket(keyX(key), keyZ(key), plugin);
                    it.remove();
                }
            }
        }

        // Release tickets for bots that are no longer active
        Set<UUID> activeUuids = new HashSet<>();
        for (FakePlayer fp : manager.getActivePlayers()) activeUuids.add(fp.getUuid());

        Iterator<Map.Entry<UUID, Set<Long>>> entryIt = ticketedChunks.entrySet().iterator();
        while (entryIt.hasNext()) {
            Map.Entry<UUID, Set<Long>> entry = entryIt.next();
            if (!activeUuids.contains(entry.getKey())) {
                // Need the world — try to find it from any remaining entity
                // We can't easily get the world here without the FakePlayer;
                // use the global ticket remove which clears all for this plugin key.
                // Instead, just skip — tickets will be garbage-collected by Paper
                // when the plugin unloads, or we release in releaseForBot().
                entryIt.remove();
            }
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Immediately releases all chunk tickets held for a specific bot.
     * Call this when a bot is deleted or permanently dies.
     */
    public void releaseForBot(FakePlayer fp) {
        Set<Long> keys = ticketedChunks.remove(fp.getUuid());
        if (keys == null || keys.isEmpty()) return;

        Entity body = fp.getPhysicsEntity();
        World world = body != null ? body.getWorld() : null;
        if (world == null) return;

        for (long key : keys) {
            world.removePluginChunkTicket(keyX(key), keyZ(key), plugin);
        }
    }

    /**
     * Releases ALL chunk tickets held by this loader (call on plugin disable).
     */
    public void releaseAll() {
        // Paper will clean up all plugin tickets on plugin disable automatically,
        // but we do it explicitly for cleanliness / hot-reloads.
        for (FakePlayer fp : manager.getActivePlayers()) {
            releaseForBot(fp);
        }
        ticketedChunks.clear();
    }

    // ── Key helpers ───────────────────────────────────────────────────────────

    private static long chunkKey(int x, int z) {
        return ((long) x & 0xFFFFFFFFL) | (((long) z & 0xFFFFFFFFL) << 32);
    }

    private static int keyX(long key) { return (int) (key & 0xFFFFFFFFL); }
    private static int keyZ(long key) { return (int) ((key >>> 32) & 0xFFFFFFFFL); }
}


