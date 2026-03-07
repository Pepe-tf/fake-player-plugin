package me.bill.fakePlayerPlugin.fakeplayer;

import me.bill.fakePlayerPlugin.util.FppLogger;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Zombie;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Spawns and configures a real Bukkit {@link Zombie} as the sole entity for a
 * fake player. Its entity ID and UUID are reused in the player-skin packet so
 * interacting with the visual directly affects this entity.
 *
 * <p>Movement is suppressed by setting MOVEMENT_SPEED to 0 — the zombie keeps
 * full physics simulation (gravity, knockback, collisions) but cannot self-propel.
 * This avoids all fragile NMS goal-selector reflection that caused chicken spawns
 * and frozen bots on Paper 1.21.
 */
public final class FakePlayerBody {

    private FakePlayerBody() {}

    public static Entity spawn(FakePlayer fp, Location loc) {
        try {
            Entity entity = loc.getWorld().spawn(loc, Zombie.class, zombie -> {
                zombie.setInvisible(true);
                zombie.setSilent(true);
                zombie.setCustomNameVisible(false);

                // Keep AI enabled — physics (gravity, knockback, collisions) requires it.
                // Movement is blocked by zeroing MOVEMENT_SPEED instead of NMS goal hacks.
                zombie.setAI(true);
                zombie.setGravity(true);
                zombie.setInvulnerable(false);
                zombie.setShouldBurnInDay(false);
                zombie.setRemoveWhenFarAway(false);
                zombie.setAdult();

                // Disable drowned conversion — -1 means "never convert"
                zombie.setConversionTime(-1);

                // Zero movement speed → bot stands still but is still physically simulated.
                // External velocity (knockback, explosions, pushes) still applies normally.
                var speed = zombie.getAttribute(Attribute.MOVEMENT_SPEED);
                if (speed != null) speed.setBaseValue(0.0);

                // Zero attack damage → bot can never hurt anyone even if AI tries
                var attack = zombie.getAttribute(Attribute.ATTACK_DAMAGE);
                if (attack != null) attack.setBaseValue(0.0);

                // Zero follow range → bot won't detect or chase any target
                var followRange = zombie.getAttribute(Attribute.FOLLOW_RANGE);
                if (followRange != null) followRange.setBaseValue(0.0);

                // Health
                var maxHp = zombie.getAttribute(Attribute.MAX_HEALTH);
                double hp = me.bill.fakePlayerPlugin.config.Config.maxHealth();
                if (maxHp != null) maxHp.setBaseValue(hp);
                zombie.setHealth(hp);

                // Permanent fire resistance
                zombie.addPotionEffect(new PotionEffect(
                        PotionEffectType.FIRE_RESISTANCE,
                        PotionEffect.INFINITE_DURATION,
                        0, false, false, false));

                // Permanent water breathing
                zombie.addPotionEffect(new PotionEffect(
                        PotionEffectType.WATER_BREATHING,
                        PotionEffect.INFINITE_DURATION,
                        0, false, false, false));

                // Tag as fake player body
                zombie.getPersistentDataContainer().set(
                        FakePlayerManager.FAKE_PLAYER_KEY,
                        org.bukkit.persistence.PersistentDataType.STRING,
                        fp.getName()
                );
            });


            me.bill.fakePlayerPlugin.config.Config.debug("Spawned body for " + fp.getName()
                    + " entityId=" + entity.getEntityId()
                    + " uuid=" + entity.getUniqueId());
            return entity;

        } catch (Exception e) {
            FppLogger.warn("FakePlayerBody.spawn failed for " + fp.getName() + ": " + e.getMessage());
            return null;
        }
    }
}
