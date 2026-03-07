package me.bill.fakePlayerPlugin.listener;

import me.bill.fakePlayerPlugin.FakePlayerPlugin;
import me.bill.fakePlayerPlugin.config.Config;
import me.bill.fakePlayerPlugin.lang.Lang;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayer;
import me.bill.fakePlayerPlugin.fakeplayer.ChunkLoader;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayerBody;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayerManager;
import me.bill.fakePlayerPlugin.fakeplayer.PacketHelper;
import me.bill.fakePlayerPlugin.fakeplayer.SkinFetcher;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.persistence.PersistentDataType;

public class FakePlayerEntityListener implements Listener {

    private final FakePlayerPlugin  plugin;
    private final FakePlayerManager manager;
    private final ChunkLoader       chunkLoader;

    public FakePlayerEntityListener(FakePlayerPlugin plugin, FakePlayerManager manager, ChunkLoader chunkLoader) {
        this.plugin       = plugin;
        this.manager      = manager;
        this.chunkLoader  = chunkLoader;

        // Reset air supply every tick so the NMS drown-tick never fires.
        // WATER_BREATHING potion alone isn't enough — the zombie's internal air
        // counter still counts down and triggers drowning before Bukkit can cancel it.
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (FakePlayer fp : manager.getActivePlayers()) {
                Entity body = fp.getPhysicsEntity();
                if (!(body instanceof Zombie zombie) || !zombie.isValid()) continue;

                zombie.setRemainingAir(zombie.getMaximumAir());
                zombie.setConversionTime(-1);

                // Broadcast rotation only when spawn-body is enabled
                if (Config.spawnBody()) {
                    Location loc = zombie.getLocation();
                    float yaw   = loc.getYaw();
                    float pitch = loc.getPitch();
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        PacketHelper.sendRotation(online, fp, yaw, pitch, yaw);
                    }
                }
            }
        }, 1L, 1L);
    }

    // ── Fire / burn prevention ────────────────────────────────────────────

    /** Cancel ALL fire ignition on fake player bodies. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityCombust(EntityCombustEvent event) {
        if (isFakePlayerBody(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    /**
     * Cancel ALL entity transformations on fake player bodies.
     * Covers: Zombie → Drowned (water), Zombie → Villager (cure), etc.
     * This is the ONLY reliable way to stop drowned conversion at runtime.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityTransform(EntityTransformEvent event) {
        if (isFakePlayerBody(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    /**
     * Prevent fake player bodies from targeting any entity.
     * Without this, the zombie AI will try to pathfind and attack nearby players
     * which causes server crashes when the speed=0 attribute conflicts with pathfinding.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityTarget(EntityTargetEvent event) {
        if (isFakePlayerBody(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    // ── Damage filter ─────────────────────────────────────────────────────

    /**
     * Allow only real combat damage (melee, projectile, explosion, magic from
     * a player source). Cancel every environmental cause so the bot behaves
     * like a player: no drowning, no burning, no suffocation, no void death, etc.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!isFakePlayerBody(event.getEntity())) return;

        EntityDamageEvent.DamageCause cause = event.getCause();

        switch (cause) {
            // ── Environmental causes to BLOCK ──────────────────────────────
            case FIRE, FIRE_TICK,          // sunlight / fire blocks
                 LAVA, HOT_FLOOR,          // lava / magma
                 DROWNING, DRYOUT,         // water / out-of-water (fish etc.)
                 SUFFOCATION,              // inside blocks
                 STARVATION,              // hunger
                 VOID,                    // below world
                 FREEZE,                  // powder snow
                 WITHER,                  // wither effect
                 POISON,                  // poison effect
                 MAGIC,                   // lingering potions w/o attacker
                 LIGHTNING,               // lightning bolts
                 FALL,                    // fall damage (bots don't fall-die)
                 FLY_INTO_WALL,           // elytra wall
                 MELTING,                 // snowman in warm biome
                 CRAMMING,               // entity cramming
                 SONIC_BOOM               // warden sonic boom
                    -> {
                event.setCancelled(true);
                return;
            }
            default -> {} // ENTITY_ATTACK, PROJECTILE, BLOCK_EXPLOSION,
                           // ENTITY_EXPLOSION — let through as real combat
        }

        // Send hurt animation for surviving combat hits
        FakePlayer fp = manager.getByEntity(event.getEntity());
        if (fp != null) {
            Location loc = event.getEntity().getLocation();
            for (Player p : Bukkit.getOnlinePlayers()) {
                PacketHelper.sendHurtAnimation(p, fp);
                if (Config.hurtSound()) {
                    p.playSound(loc, Sound.ENTITY_PLAYER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!isFakePlayerBody(event.getEntity())) return;

        if (Config.suppressDrops()) {
            event.getDrops().clear();
            event.setDroppedExp(0);
        }

        FakePlayer fp = manager.getByEntity(event.getEntity());
        if (fp == null) return;

        // ── Kill message ──────────────────────────────────────────────────────
        Player killer = event.getEntity().getKiller();
        if (killer != null && Config.killMessage()) {
            Component killMsg = Lang.get("bot-kill", "killer", killer.getName(), "name", fp.getName());
            for (Player p : Bukkit.getOnlinePlayers()) p.sendMessage(killMsg);
            Bukkit.getConsoleSender().sendMessage(killMsg);
        }

        final Location respawnLoc = fp.getSpawnLocation() != null
                ? fp.getSpawnLocation().clone()
                : event.getEntity().getLocation().clone();
        final String name = fp.getName();

        if (Config.respawnOnDeath()) {
            int delay = Math.max(1, Config.respawnDelay());
            Config.debug(name + " died — respawning in " + delay + " ticks at " + respawnLoc);

            // Release chunk tickets immediately — ChunkLoader will re-acquire after respawn
            chunkLoader.releaseForBot(fp);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (Player p : Bukkit.getOnlinePlayers()) PacketHelper.sendTabListRemove(p, fp);

                Runnable doRespawn = () -> {
                    // Spawn body only if enabled
                    if (Config.spawnBody()) {
                        Entity newBody = FakePlayerBody.spawn(fp, respawnLoc);
                        if (newBody == null) {
                            broadcastLeave(name);
                            manager.removeByName(name);
                            return;
                        }
                        fp.setPhysicsEntity(newBody);
                        fp.setBodyUUID(newBody.getUniqueId());
                    }
                    fp.setSpawnLocation(respawnLoc);

                    for (Player p : Bukkit.getOnlinePlayers()) PacketHelper.sendTabListAdd(p, fp);

                    if (Config.spawnBody()) {
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            for (Player p : Bukkit.getOnlinePlayers()) {
                                PacketHelper.spawnFakePlayer(p, fp, respawnLoc);
                                PacketHelper.sendRotation(p, fp, respawnLoc.getYaw(), respawnLoc.getPitch(), respawnLoc.getYaw());
                            }
                        }, 3L);
                    }

                    Config.debug(name + " respawned successfully.");
                };

                // Only fetch skin when both flags are on
                if (Config.fetchSkin() && Config.showSkin()) {
                    SkinFetcher.fetchAsync(name, (value, signature) ->
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            if (value != null) fp.setSkin(value, signature);
                            doRespawn.run();
                        })
                    );
                } else {
                    fp.setSkin(null, null); // clear any stale skin data
                    Bukkit.getScheduler().runTask(plugin, doRespawn);
                }
            }, delay);

        } else {
            chunkLoader.releaseForBot(fp);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (Player p : Bukkit.getOnlinePlayers()) PacketHelper.sendTabListRemove(p, fp);
                broadcastLeave(name);
                manager.removeByName(name);
            }, 1L);
        }
    }

    private void broadcastLeave(String name) {
        if (!Config.leaveMessage()) return;
        Component msg = Lang.get("bot-leave", "name", name);
        for (Player p : Bukkit.getOnlinePlayers()) p.sendMessage(msg);
        Bukkit.getConsoleSender().sendMessage(msg);
    }

    private boolean isFakePlayerBody(Entity entity) {
        if (!(entity instanceof Zombie)) return false;
        if (FakePlayerManager.FAKE_PLAYER_KEY == null) return false;
        return entity.getPersistentDataContainer()
                .has(FakePlayerManager.FAKE_PLAYER_KEY, PersistentDataType.STRING);
    }
}
