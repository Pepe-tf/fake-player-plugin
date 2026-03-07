package me.bill.fakePlayerPlugin.fakeplayer;

import me.bill.fakePlayerPlugin.FakePlayerPlugin;
import me.bill.fakePlayerPlugin.config.Config;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mannequin;

import java.util.*;

/**
 * Keeps chunks loaded around each active bot exactly like a real player does.
 *
 * <p>Uses Paper's {@code World.addPluginChunkTicket()} — the server counts
 * these as player-equivalent load sources so mobs spawn, redstone ticks,
 * crops grow, etc. in the surrounding area.
 *
 * <p>Position source priority (per bot, every tick):
 * <ol>
 *   <li>Live {@link Mannequin} body position — most accurate.</li>
 *   <li>{@link FakePlayer#getSpawnLocation()} — used when {@code spawn-body}
 *       is disabled or the body hasn't materialised yet.</li>
 * </ol>
 *
 * <p>Tickets are released immediately via {@link #releaseForBot(FakePlayer)}
 * when a bot is deleted/dies, and {@link #releaseAll()} cleans everything on
 * plugin disable. A per-second background task keeps the ticket set in sync
 * as the physics engine moves the body (knockback, gravity, etc.).
 */
public final class ChunkLoader {

    private final FakePlayerPlugin plugin;
    private final FakePlayerManager manager;

    /**
     * Maps each FakePlayer UUID → (worldName → set of packed chunk keys).
     * We store the world name alongside keys so we can release tickets even
     * after the body entity has been removed.
     */
    private final Map<UUID, WorldChunks> ticketedChunks = new HashMap<>();

    public ChunkLoader(FakePlayerPlugin plugin, FakePlayerManager manager) {
        this.plugin  = plugin;
        this.manager = manager;

        // Refresh chunk tickets every second (20 ticks)
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    // ── Per-second tick ───────────────────────────────────────────────────────

    private void tick() {
        if (!Config.chunkLoadingEnabled()) {
            if (!ticketedChunks.isEmpty()) releaseAll();
            return;
        }

        int radius = Math.max(0, Config.chunkLoadingRadius());

        Set<UUID> activeUuids = new HashSet<>();

        for (FakePlayer fp : manager.getActivePlayers()) {
            activeUuids.add(fp.getUuid());

            // ── Resolve current position ──────────────────────────────────────
            Location pos = null;

            Entity body = fp.getPhysicsEntity();
            if (body instanceof Mannequin m && m.isValid()) {
                pos = m.getLocation();
            } else if (fp.getSpawnLocation() != null
                    && fp.getSpawnLocation().getWorld() != null) {
                pos = fp.getSpawnLocation();
            }

            if (pos == null || pos.getWorld() == null) continue;

            World  world = pos.getWorld();
            String wName = world.getName();
            int    cx    = pos.getBlockX() >> 4;
            int    cz    = pos.getBlockZ() >> 4;

            // ── Build desired ticket set ──────────────────────────────────────
            Set<Long> desired = new HashSet<>((radius * 2 + 1) * (radius * 2 + 1));
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    desired.add(chunkKey(cx + dx, cz + dz));
                }
            }

            WorldChunks wc = ticketedChunks.computeIfAbsent(fp.getUuid(),
                    k -> new WorldChunks(wName, new HashSet<>()));

            // If the bot crossed worlds, release old tickets first
            if (!wc.worldName.equals(wName)) {
                releaseWorldChunks(wc);
                wc = new WorldChunks(wName, new HashSet<>());
                ticketedChunks.put(fp.getUuid(), wc);
            }

            // Add new tickets
            for (long key : desired) {
                if (wc.keys.add(key)) {
                    world.addPluginChunkTicket(keyX(key), keyZ(key), plugin);
                }
            }

            // Remove stale tickets (bot moved away)
            Iterator<Long> it = wc.keys.iterator();
            while (it.hasNext()) {
                long key = it.next();
                if (!desired.contains(key)) {
                    world.removePluginChunkTicket(keyX(key), keyZ(key), plugin);
                    it.remove();
                }
            }
        }

        // Release tickets for bots that are no longer active
        ticketedChunks.entrySet().removeIf(entry -> {
            if (activeUuids.contains(entry.getKey())) return false;
            releaseWorldChunks(entry.getValue());
            return true;
        });
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Immediately releases all chunk tickets held for a specific bot.
     * Safe to call even after the body entity has been removed.
     */
    public void releaseForBot(FakePlayer fp) {
        WorldChunks wc = ticketedChunks.remove(fp.getUuid());
        if (wc == null) return;
        releaseWorldChunks(wc);
    }

    /** Releases ALL chunk tickets held by this loader (call on plugin disable). */
    public void releaseAll() {
        ticketedChunks.values().forEach(this::releaseWorldChunks);
        ticketedChunks.clear();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Releases every ticket in a WorldChunks record. */
    private void releaseWorldChunks(WorldChunks wc) {
        World world = Bukkit.getWorld(wc.worldName);
        if (world == null || wc.keys.isEmpty()) return;
        for (long key : wc.keys) {
            world.removePluginChunkTicket(keyX(key), keyZ(key), plugin);
        }
        wc.keys.clear();
    }

    // ── Key helpers ───────────────────────────────────────────────────────────

    private static long chunkKey(int x, int z) {
        return (x & 0xFFFFFFFFL) | ((z & 0xFFFFFFFFL) << 32);
    }

    private static int keyX(long key) { return (int)  (key         & 0xFFFFFFFFL); }
    private static int keyZ(long key) { return (int) ((key >>> 32) & 0xFFFFFFFFL); }

    // ── Inner record ─────────────────────────────────────────────────────────

    /** Pairs a world name with the set of packed chunk keys ticketed in that world. */
    private static final class WorldChunks {
        String    worldName;
        Set<Long> keys;
        WorldChunks(String worldName, Set<Long> keys) {
            this.worldName = worldName;
            this.keys      = keys;
        }
    }
}
