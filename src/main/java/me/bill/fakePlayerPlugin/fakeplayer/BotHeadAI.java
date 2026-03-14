package me.bill.fakePlayerPlugin.fakeplayer;

import me.bill.fakePlayerPlugin.FakePlayerPlugin;
import me.bill.fakePlayerPlugin.config.Config;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;

/**
 * Gives each bot a "look at nearest player" behaviour.
 * Runs every tick — rotates the Mannequin body to face the closest real
 * player within range using smooth interpolation.
 *
 * <p>Tuning (all hot-reloadable via /fpp reload):
 * <ul>
 *   <li>{@code fake-player.head-ai.look-range}  — detection radius in blocks</li>
 *   <li>{@code fake-player.head-ai.turn-speed}  — interpolation factor (0.0–1.0)</li>
 * </ul>
 */
public final class BotHeadAI {

    private final FakePlayerManager manager;

    public BotHeadAI(FakePlayerPlugin plugin, FakePlayerManager manager) {
        this.manager = manager;
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    private void tick() {
        // Bail out early if head-AI is disabled or there's nothing to look at
        if (!Config.headAiEnabled()) return;

        double lookRange  = Config.headAiLookRange();
        if (lookRange <= 0) return;  // look-range: 0 also disables head AI

        float  turnSpeed  = Config.headAiTurnSpeed();
        double rangeSq    = lookRange * lookRange;

        for (FakePlayer fp : manager.getActivePlayers()) {
            Entity body = fp.getPhysicsEntity();
            if (!(body instanceof Mannequin m) || !m.isValid()) continue;

            Location botLoc = m.getLocation();

            // Find nearest player in range
            Player nearest     = null;
            double nearestDist = rangeSq;

            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.getWorld().equals(m.getWorld())) continue;
                double d = p.getLocation().distanceSquared(botLoc);
                if (d < nearestDist) { nearestDist = d; nearest = p; }
            }

            if (nearest == null) continue;

            Location target = nearest.getEyeLocation();
            double dx = target.getX() - botLoc.getX();
            double dy = target.getY() - (botLoc.getY() + m.getEyeHeight());
            double dz = target.getZ() - botLoc.getZ();
            double horiz = Math.sqrt(dx * dx + dz * dz);

            float targetYaw   = (float) Math.toDegrees(Math.atan2(-dx, dz));
            float targetPitch = (float) Math.toDegrees(-Math.atan2(dy, horiz));

            float newYaw   = lerpAngle(botLoc.getYaw(),   targetYaw,   turnSpeed);
            float newPitch = lerp     (botLoc.getPitch(),  targetPitch, turnSpeed);

            m.setRotation(newYaw, newPitch);
        }
    }

    private static float lerpAngle(float from, float to, float t) {
        float diff = to - from;
        while (diff >  180f) diff -= 360f;
        while (diff < -180f) diff += 360f;
        return from + diff * t;
    }

    private static float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }
}
