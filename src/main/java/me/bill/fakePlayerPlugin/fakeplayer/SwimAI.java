package me.bill.fakePlayerPlugin.fakeplayer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Idle swim AI — runs every tick for non-frozen bots that are in water/lava
 * when no PathfindingService navigation is active.
 *
 * Behaviour:
 *  • Lava           → always jump (float upward)
 *  • Near surface   → stop jumping so the bot bobs naturally at the surface
 *  • Submerged      → jump to rise; pitch tilted forward to enter sprint-swim pose
 *  • Ceiling above  → jump to press against ceiling and navigate horizontally
 *  • Water exit     → fire a direct Y-velocity impulse when bot is at the waterline
 *                     and a solid block is directly ahead at foot level (climb onto shore)
 *
 * This class only controls jumping and pitch.  Forward movement / yaw are
 * left to PathfindingService (when navigating) or to the idle state.
 */
public final class SwimAI {

  /** Blocks above the bot's feet to scan for the water surface. */
  private static final int SURFACE_SCAN = 6;
  /** Blocks above the bot's feet to check for a solid ceiling. */
  private static final int CEILING_SCAN = 3;
  /**
   * If the water surface is within this many blocks above the bot's feet,
   * treat the bot as "near surface" and stop jumping so it bobs naturally.
   */
  private static final int NEAR_SURFACE_THRESHOLD = 2;

  private SwimAI() {}

  /**
   * Called from the main bot tick loop (FakePlayerManager) before tickPhysics.
   *
   * @param bot          the bot player entity
   * @param navJump      true if PathfindingService has requested a jump this tick
   * @param isNavigating true if PathfindingService currently owns this bot's movement
   */
  public static void tick(Player bot, boolean navJump, boolean isNavigating) {
    if (!bot.isInWater() && !bot.isInLava()) return;

    if (navJump) {
      // PathfindingService already requested a jump — honour it and don't interfere.
      NmsPlayerSpawner.setJumping(bot, true);
      return;
    }

    if (bot.isInLava()) {
      // Always rise in lava.
      NmsPlayerSpawner.setJumping(bot, true);
      return;
    }

    // Water logic — determine how far the surface is.
    Location loc = bot.getLocation();
    World world = bot.getWorld();
    int bx = loc.getBlockX();
    int by = loc.getBlockY();
    int bz = loc.getBlockZ();

    int distToSurface = distanceToSurface(world, bx, by, bz);
    boolean hasCeiling = hasSolidCeiling(world, bx, by, bz);

    if (distToSurface == 0) {
      // Bot is at or above the surface.
      // Check if it needs a water-exit jump — solid block at foot level ahead.
      if (!isNavigating && isAtWaterExit(world, bx, by, bz, loc.getYaw())) {
        applyExitImpulse(bot);
      }
      NmsPlayerSpawner.setJumping(bot, false);
      return;
    }

    if (distToSurface <= NEAR_SURFACE_THRESHOLD && !hasCeiling) {
      // Close to the surface.
      // distToSurface == 1 means the bot's head is already at the waterline —
      // apply an exit impulse if a solid block is ahead so it climbs onto land.
      if (distToSurface == 1 && !isNavigating) {
        applyExitImpulse(bot);
      }
      NmsPlayerSpawner.setJumping(bot, false);
      return;
    }

    // Submerged (or ceiling forcing horizontal travel) — rise.
    NmsPlayerSpawner.setJumping(bot, true);

    // When not actively navigating, tilt the bot's pitch so Minecraft sees it as
    // sprint-swimming forward rather than standing in place.
    if (!isNavigating) {
      Location current = bot.getLocation();
      // Pitch ~-20 = looking slightly upward/forward → triggers swim pose in water.
      bot.setRotation(current.getYaw(), -20f);
      NmsPlayerSpawner.setHeadYaw(bot, current.getYaw());
      // Sprint-swim requires the sprinting flag.
      bot.setSprinting(true);
      NmsPlayerSpawner.setMovementForward(bot, 1.0f);
    }
  }

  /**
   * Applies a direct upward velocity impulse to launch the bot out of the water.
   * Uses setVelocity rather than the NMS jumping field because onGround is
   * unreliable while the bot is floating at the waterline.
   * Only fires if the bot is not already moving upward.
   */
  private static void applyExitImpulse(Player bot) {
    Vector vel = bot.getVelocity();
    if (vel.getY() < 0.2) {
      vel.setY(0.42);
      bot.setVelocity(vel);
    }
  }

  /**
   * Returns true if the bot appears to be at a water-to-land edge in the direction
   * it is currently facing — i.e. the block one step ahead at feet level is solid
   * and the block above it is passable, meaning a 1-block step-up is needed.
   */
  private static boolean isAtWaterExit(World world, int bx, int by, int bz, float yaw) {
    double rad = Math.toRadians(yaw);
    int dx = (int) Math.round(-Math.sin(rad));
    int dz = (int) Math.round( Math.cos(rad));
    Material front      = world.getBlockAt(bx + dx, by,     bz + dz).getType();
    Material frontAbove = world.getBlockAt(bx + dx, by + 1, bz + dz).getType();
    return front.isSolid() && !frontAbove.isSolid();
  }

  /**
   * Returns the number of water/kelp/bubble blocks between the bot's feet and the
   * first air block above (i.e. distance to the open surface).
   * Returns 0 if the block directly above is already air (bot is at the surface).
   * Returns {@link #SURFACE_SCAN} if no air was found within scan range.
   */
  private static int distanceToSurface(World world, int x, int y, int z) {
    for (int dy = 1; dy <= SURFACE_SCAN; dy++) {
      int cy = y + dy;
      if (cy >= world.getMaxHeight()) return SURFACE_SCAN;
      Material mat = world.getBlockAt(x, cy, z).getType();
      if (mat.isAir()) {
        return dy;
      }
      if (mat != Material.WATER && mat != Material.KELP_PLANT
          && mat != Material.KELP && mat != Material.SEAGRASS
          && mat != Material.TALL_SEAGRASS && mat != Material.BUBBLE_COLUMN) {
        // Hit a non-water solid block above — treat as deep / ceiling.
        return SURFACE_SCAN;
      }
    }
    return SURFACE_SCAN;
  }

  /**
   * Returns true if there is a solid block within {@link #CEILING_SCAN} blocks
   * directly above the bot — meaning the bot cannot simply rise to the surface.
   */
  private static boolean hasSolidCeiling(World world, int x, int y, int z) {
    for (int dy = 1; dy <= CEILING_SCAN; dy++) {
      int cy = y + dy;
      if (cy >= world.getMaxHeight()) return true;
      Material mat = world.getBlockAt(x, cy, z).getType();
      if (mat.isSolid()) return true;
    }
    return false;
  }
}
