package me.bill.fakePlayerPlugin.listener;

import me.bill.fakePlayerPlugin.FakePlayerPlugin;
import me.bill.fakePlayerPlugin.config.Config;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayer;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayerBody;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayerManager;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Provides three layers of physical interaction for Mannequin-based bots.
 *
 * <p>All tuning values are read from config on every event/tick so they
 * are hot-reloadable via {@code /fpp reload} with no restart needed:
 * <ul>
 *   <li>{@code fake-player.collision.walk-radius}</li>
 *   <li>{@code fake-player.collision.walk-strength}</li>
 *   <li>{@code fake-player.collision.max-horizontal-speed}</li>
 *   <li>{@code fake-player.collision.hit-strength}</li>
 *   <li>{@code fake-player.collision.bot-radius}</li>
 *   <li>{@code fake-player.collision.bot-strength}</li>
 * </ul>
 */
public class BotCollisionListener implements Listener {

    private final FakePlayerManager manager;

    public BotCollisionListener(FakePlayerPlugin plugin, FakePlayerManager manager) {
        this.manager = manager;
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::tickBotSeparation, 1L, 1L);
    }

    // ── 1. Hit knockback ──────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Mannequin target)) return;
        if (!isFakeBody(target)) return;
        if (!(event.getDamager() instanceof LivingEntity attacker)) return;

        double hitStrength = Config.collisionHitStrength();
        double maxHoriz    = Config.collisionMaxHoriz();

        Location aLoc = attacker.getLocation();
        Location bLoc = target.getLocation();
        double dx = bLoc.getX() - aLoc.getX();
        double dz = bLoc.getZ() - aLoc.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);

        Vector kb;
        if (dist < 1e-4) {
            Vector dir = aLoc.getDirection();
            kb = new Vector(dir.getX(), 0.1, dir.getZ());
        } else {
            kb = new Vector(dx / dist, 0.1, dz / dist);
        }

        kb.multiply(hitStrength);
        applyImpulse(target, kb.getX(), kb.getZ(), maxHoriz);
    }

    // ── 2. Walk-into push ─────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to   = event.getTo();
        if (to == null) return;
        if (Math.abs(to.getX() - from.getX()) < 1e-6
                && Math.abs(to.getZ() - from.getZ()) < 1e-6) return;

        double walkRadius   = Config.collisionWalkRadius();
        double walkStrength = Config.collisionWalkStrength();
        double maxHoriz     = Config.collisionMaxHoriz();

        Player player = event.getPlayer();
        Location pLoc = player.getLocation();

        for (FakePlayer fp : manager.getActivePlayers()) {
            Entity body = fp.getPhysicsEntity();
            if (body == null || !body.isValid()) continue;
            if (!body.getWorld().equals(player.getWorld())) continue;

            Location bLoc = body.getLocation();
            double dy = pLoc.getY() - bLoc.getY();
            if (dy > 2.2 || dy < -1.2) continue;

            double dx = bLoc.getX() - pLoc.getX();
            double dz = bLoc.getZ() - pLoc.getZ();
            double distSq = dx * dx + dz * dz;
            if (distSq > walkRadius * walkRadius || distSq < 1e-12) continue;

            double dist     = Math.sqrt(distSq);
            double overlap  = 1.0 - (dist / walkRadius);
            double strength = walkStrength * overlap;

            applyImpulse(body, (dx / dist) * strength, (dz / dist) * strength, maxHoriz);
        }
    }

    // ── 3. Bot-vs-bot separation ──────────────────────────────────────────────

    private void tickBotSeparation() {
        Collection<FakePlayer> all = manager.getActivePlayers();
        if (all.size() < 2) return;

        double botRadius   = Config.collisionBotRadius();
        double botStrength = Config.collisionBotStrength();
        double maxHoriz    = Config.collisionMaxHoriz();

        List<FakePlayer> bots = new ArrayList<>(all);
        for (int i = 0; i < bots.size(); i++) {
            Entity bodyA = bots.get(i).getPhysicsEntity();
            if (bodyA == null || !bodyA.isValid()) continue;
            Location locA = bodyA.getLocation();

            for (int j = i + 1; j < bots.size(); j++) {
                Entity bodyB = bots.get(j).getPhysicsEntity();
                if (bodyB == null || !bodyB.isValid()) continue;
                if (!bodyA.getWorld().equals(bodyB.getWorld())) continue;

                Location locB = bodyB.getLocation();
                double dy = locA.getY() - locB.getY();
                if (dy > 2.0 || dy < -2.0) continue;

                double dx     = locB.getX() - locA.getX();
                double dz     = locB.getZ() - locA.getZ();
                double distSq = dx * dx + dz * dz;
                if (distSq > botRadius * botRadius || distSq < 1e-12) continue;

                double dist     = Math.sqrt(distSq);
                double nx       = dx / dist;
                double nz       = dz / dist;
                double overlap  = 1.0 - (dist / botRadius);
                double strength = botStrength * overlap * 0.5;

                applyImpulse(bodyB,  nx * strength,  nz * strength, maxHoriz);
                applyImpulse(bodyA, -nx * strength, -nz * strength, maxHoriz);
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void applyImpulse(Entity body, double ix, double iz, double maxHoriz) {
        Vector vel = body.getVelocity();
        double newX = vel.getX() + ix;
        double newZ = vel.getZ() + iz;

        double speed = Math.sqrt(newX * newX + newZ * newZ);
        if (speed > maxHoriz) {
            double scale = maxHoriz / speed;
            newX *= scale;
            newZ *= scale;
        }

        vel.setX(newX);
        vel.setZ(newZ);
        body.setVelocity(vel);
    }

    private boolean isFakeBody(Entity entity) {
        if (FakePlayerManager.FAKE_PLAYER_KEY == null) return false;
        String val = entity.getPersistentDataContainer()
                .get(FakePlayerManager.FAKE_PLAYER_KEY,
                        org.bukkit.persistence.PersistentDataType.STRING);
        return val != null
                && !val.startsWith(FakePlayerBody.NAMETAG_PDC_VALUE)
                && !val.startsWith(FakePlayerBody.VISUAL_PDC_VALUE);
    }
}
