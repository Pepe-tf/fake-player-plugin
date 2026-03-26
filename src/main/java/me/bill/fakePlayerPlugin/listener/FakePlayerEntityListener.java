package me.bill.fakePlayerPlugin.listener;

import me.bill.fakePlayerPlugin.FakePlayerPlugin;
import me.bill.fakePlayerPlugin.config.Config;
import me.bill.fakePlayerPlugin.fakeplayer.BotBroadcast;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayer;
import me.bill.fakePlayerPlugin.fakeplayer.ChunkLoader;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayerBody;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayerManager;
import me.bill.fakePlayerPlugin.fakeplayer.PacketHelper;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.persistence.PersistentDataType;

public class FakePlayerEntityListener implements Listener {

    private final FakePlayerPlugin  plugin;
    private final FakePlayerManager manager;
    private final ChunkLoader       chunkLoader;

    public FakePlayerEntityListener(FakePlayerPlugin plugin, FakePlayerManager manager, ChunkLoader chunkLoader) {
        this.plugin      = plugin;
        this.manager     = manager;
        this.chunkLoader = chunkLoader;
    }

    // ── Damage filter ─────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!isFakeBotBody(event.getEntity())) return;

        // Primary damageable guard — event-level cancellation beats any entity flag.
        // Checked live from config so /fpp reload takes effect without respawn.
        if (!Config.bodyDamageable()) {
            event.setCancelled(true);
            return;
        }

        // Cancel damage types that shouldn't affect a player-like entity
        switch (event.getCause()) {
            case VOID, SUFFOCATION, CRAMMING,
                 STARVATION, FREEZE, MELTING,
                 FLY_INTO_WALL, DRYOUT,
                 POISON, WITHER, MAGIC,
                 LIGHTNING -> {
                event.setCancelled(true);
                return;
            }
            default -> {}
        }

        // On fatal hit — remove nametag before death animation plays.
        // Only act when the event is NOT already cancelled so we don't strip the
        // nametag on a hit that will produce no damage (e.g. if another plugin
        // cancels the event after us but the damage value is still non-zero).
        if (!event.isCancelled() && event.getEntity() instanceof Mannequin m) {
            double remaining = m.getHealth() - event.getFinalDamage();
            if (remaining <= 0) {
                FakePlayer fp = manager.getByEntity(m);
                if (fp != null) FakePlayerBody.removeNametag(fp);
            }
        }

        // Play player hurt sound
        if (Config.hurtSound()) {
            Location loc = event.getEntity().getLocation();
            for (Player p : Bukkit.getOnlinePlayers())
                p.playSound(loc, Sound.ENTITY_PLAYER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
        }
    }

    // ── Portal / teleport guard ───────────────────────────────────────────────

    /**
     * Prevents bot Mannequin bodies from traversing portals.
     *
     * <p>When an entity goes through a portal Minecraft ejects its passengers
     * (the ArmorStand nametag) at the portal entrance, orphaning it in the
     * original world.  The entity also receives a new entity-id in the target
     * world, breaking {@code entityIdIndex} lookups and causing the death
     * handler to silently skip cleanup.  Blocking the portal event prevents
     * all of these cascading issues.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityPortal(EntityPortalEvent event) {
        if (!isFakeBotBody(event.getEntity())) return;
        event.setCancelled(true);
        Config.debug("Blocked portal traversal for bot body: "
                + event.getEntity().getPersistentDataContainer()
                        .get(FakePlayerManager.FAKE_PLAYER_KEY, PersistentDataType.STRING));
    }

    /**
     * Prevents bot Mannequin bodies from being teleported to a different world
     * via commands, plugins, or other mechanics (chorus fruit, etc.).
     *
     * <p>Same root cause as the portal case: cross-world teleports eject
     * passengers and break the entity-id index.  Same-world teleports are
     * allowed (they don't change the entity id or eject riders).
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityTeleport(EntityTeleportEvent event) {
        if (!isFakeBotBody(event.getEntity())) return;
        org.bukkit.Location from = event.getFrom();
        org.bukkit.Location to   = event.getTo();
        if (to == null || from.getWorld() == null || to.getWorld() == null) return;
        if (from.getWorld().equals(to.getWorld())) return; // same world — fine
        event.setCancelled(true);
        Config.debug("Blocked cross-world teleport for bot body: "
                + event.getEntity().getPersistentDataContainer()
                        .get(FakePlayerManager.FAKE_PLAYER_KEY, PersistentDataType.STRING));
    }

    // ── Death ─────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!isFakeBotBody(event.getEntity())) return;

        if (Config.suppressDrops()) {
            event.getDrops().clear();
            event.setDroppedExp(0);
        }

        FakePlayer fp = manager.getByEntity(event.getEntity());
        if (fp == null) return;

        Player killer = event.getEntity().getKiller();
        if (killer != null) {
            BotBroadcast.broadcastKill(killer.getName(), fp.getDisplayName());
        }

        final Location respawnLoc = fp.getSpawnLocation() != null
                ? fp.getSpawnLocation().clone()
                : event.getEntity().getLocation().clone();
        final String name        = fp.getName();
        final String displayName = fp.getDisplayName();

        // Remove nametag immediately (entity is dead but ArmorStand is not).
        // If the body crossed worlds the stored ArmorStand reference may point to a
        // different world; null it first so removeNametag always falls through to the
        // full world-scan, which finds and removes it regardless of which world it's in.
        if (fp.getNametagEntity() != null
                && fp.getPhysicsEntity() != null
                && fp.getPhysicsEntity().isValid()
                && fp.getNametagEntity().getWorld() != null
                && !fp.getNametagEntity().getWorld().equals(fp.getPhysicsEntity().getWorld())) {
            // Nametag is in a different world than the body (ejected at portal entrance).
            // Clear the direct ref so removeNametag goes straight to the world-scan.
            fp.setNametagEntity(null);
        }
        FakePlayerBody.removeNametag(fp);
        // Clear physics entity reference — the Mannequin is now dead/invalid
        fp.setPhysicsEntity(null);
        // Remove from entity-id index so stale lookups don't return this fp
        manager.removeFromEntityIndex(event.getEntity().getEntityId());

        if (Config.respawnOnDeath()) {
            int delay = Math.max(1, Config.respawnDelay());
            if (chunkLoader != null) chunkLoader.releaseForBot(fp);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // Remove from tab list while dead
                for (Player p : Bukkit.getOnlinePlayers()) PacketHelper.sendTabListRemove(p, fp);

                // Double-check no orphaned nametag survived
                FakePlayerBody.removeOrphanedNametags(name);

                if (Config.spawnBody()) {
                    Entity newBody = FakePlayerBody.spawn(fp, respawnLoc);
                    if (newBody == null) {
                        broadcastLeave(displayName);
                        manager.removeByName(name);
                        return;
                    }
                    fp.setPhysicsEntity(newBody);
                    manager.registerEntityIndex(newBody.getEntityId(), fp);
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            if (!newBody.isValid()) return;
                            FakePlayerBody.applyResolvedSkin(plugin, fp, newBody);
                            fp.setNametagEntity(FakePlayerBody.spawnNametag(fp, newBody));
                            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                if (Config.tabListEnabled())
                                    for (Player p : Bukkit.getOnlinePlayers())
                                        PacketHelper.sendTabListAdd(p, fp);
                            }, 20L);
                        }, 1L);
                    } else {
                        // Body disabled but bot should "respawn" — re-add to tab list
                        fp.setNametagEntity(null);
                        if (Config.tabListEnabled())
                            for (Player p : Bukkit.getOnlinePlayers())
                                PacketHelper.sendTabListAdd(p, fp);
                    }

                fp.setSpawnLocation(respawnLoc);
            }, delay);

        } else {
            if (chunkLoader != null) chunkLoader.releaseForBot(fp);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (Player p : Bukkit.getOnlinePlayers()) PacketHelper.sendTabListRemove(p, fp);
                // World-scan cleanup — catches any entity the direct ref missed
                FakePlayerBody.removeOrphanedNametags(name);
                FakePlayerBody.removeOrphanedBodies(name);
                broadcastLeave(displayName);
                manager.removeByName(name);
            }, 1L);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void broadcastLeave(String displayName) {
        BotBroadcast.broadcastLeaveByDisplayName(displayName);
    }

    /** True only for Mannequin entities tagged as an FPP physics body. */
    private boolean isFakeBotBody(Entity entity) {
        if (!(entity instanceof Mannequin)) return false;
        if (FakePlayerManager.FAKE_PLAYER_KEY == null) return false;
        String val = entity.getPersistentDataContainer()
                .get(FakePlayerManager.FAKE_PLAYER_KEY, PersistentDataType.STRING);
        return val != null
                && !val.startsWith(FakePlayerBody.NAMETAG_PDC_VALUE)
                && !val.startsWith(FakePlayerBody.VISUAL_PDC_VALUE);
    }
}
