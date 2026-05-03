package me.bill.fakePlayerPlugin.listener;

import java.util.Collection;
import me.bill.fakePlayerPlugin.FakePlayerPlugin;
import me.bill.fakePlayerPlugin.config.Config;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayer;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayerManager;
import me.bill.fakePlayerPlugin.fakeplayer.NmsPlayerSpawner;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

public final class BotCollisionListener implements Listener {

  private static final double VANILLA_ATTACK_UPWARD = 0.40D;
  private static final double PLAYER_SPRINT_BONUS = 0.28D;

  private final FakePlayerPlugin plugin;
  private final FakePlayerManager manager;
  private int separationTickCounter;

  public BotCollisionListener(FakePlayerPlugin plugin, FakePlayerManager manager) {
    this.plugin = plugin;
    this.manager = manager;
    plugin.getServer().getScheduler().runTaskTimer(plugin, this::tickBotSeparation, 1L, 1L);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
    if (!Config.bodyPushable()) return;
    if (!(event.getEntity() instanceof Player target)) return;
    if (!manager.isBot(target)) return;

    Entity attacker = resolveKnockbackSource(event.getDamager());
    if (attacker == null) return;
    if (attacker instanceof Player && !isPvpEnabled(target.getLocation())) return;
    if (event.getDamager() instanceof Player attackerPlayer && !canCollide(attackerPlayer, target)) {
      return;
    }

    Location aLoc = attacker.getLocation();
    Location bLoc = target.getLocation();
    double dx = bLoc.getX() - aLoc.getX();
    double dz = bLoc.getZ() - aLoc.getZ();
    double dist = Math.sqrt(dx * dx + dz * dz);

    Vector kb = computeHorizontalKnockback(attacker, target, dx, dz, dist, VANILLA_ATTACK_UPWARD);
    double strength = Config.collisionHitStrength();
    if (attacker instanceof Player p && p.isSprinting()) strength += PLAYER_SPRINT_BONUS;
    kb = scaleHorizontal(kb, strength);

    double max = Config.collisionHitMaxHoriz();
    double speed = Math.sqrt(kb.getX() * kb.getX() + kb.getZ() * kb.getZ());
    if (speed > max) {
      double scale = max / speed;
      kb.setX(kb.getX() * scale);
      kb.setZ(kb.getZ() * scale);
    }

    applyBotKnockback(target, kb);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onExplosionDamage(EntityDamageEvent event) {
    if (!Config.bodyPushable()) return;
    if (!(event.getEntity() instanceof Player target)) return;
    if (!manager.isBot(target)) return;
    EntityDamageEvent.DamageCause cause = event.getCause();
    if (cause != EntityDamageEvent.DamageCause.ENTITY_EXPLOSION
        && cause != EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
      return;
    }

    Vector facing = target.getLocation().getDirection();
    if (facing.lengthSquared() < 1e-8) facing = new Vector(1, 0, 0);
    Vector kb = new Vector(facing.getX(), 0.45, facing.getZ()).normalize();
    kb.multiply(Config.collisionHitStrength() * 0.7);
    applyBotKnockback(target, clampHorizontal(kb, Config.collisionHitMaxHoriz()));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerMove(PlayerMoveEvent event) {
    if (!Config.bodyPushable()) return;
    Location from = event.getFrom();
    Location to = event.getTo();
    if (to == null) return;
    if (Math.abs(to.getX() - from.getX()) < 1e-6 && Math.abs(to.getZ() - from.getZ()) < 1e-6) {
      return;
    }

    Player player = event.getPlayer();
    Location pLoc = player.getLocation();
    double radius = Config.collisionWalkRadius();
    double strengthBase = Config.collisionWalkStrength();
    double maxHoriz = Config.collisionMaxHoriz();

    for (FakePlayer fp : manager.getActivePlayers()) {
      Player body = fp.getPlayer();
      if (body == null || !body.isValid() || !body.getWorld().equals(player.getWorld())) continue;
      if (!canCollide(player, body)) continue;

      Location bLoc = body.getLocation();
      double dy = pLoc.getY() - bLoc.getY();
      if (dy > 2.2 || dy < -1.2) continue;
      double dx = bLoc.getX() - pLoc.getX();
      double dz = bLoc.getZ() - pLoc.getZ();
      double distSq = dx * dx + dz * dz;
      if (distSq > radius * radius || distSq < 1e-12) continue;

      double dist = Math.sqrt(distSq);
      double strength = strengthBase * (1.0 - (dist / radius));
      applyImpulse(body, (dx / dist) * strength, 0.0, (dz / dist) * strength, maxHoriz, 0.9);
    }
  }

  private void tickBotSeparation() {
    if (!Config.bodyPushable()) return;
    if ((++separationTickCounter & 1) != 0) return;
    Collection<FakePlayer> all = manager.getActivePlayers();
    if (all.size() < 2) return;

    FakePlayer[] bots = all.toArray(new FakePlayer[0]);
    double radius = Config.collisionBotRadius();
    double strengthBase = Config.collisionBotStrength();
    double maxHoriz = Config.collisionMaxHoriz();
    for (int i = 0; i < bots.length; i++) {
      Player a = bots[i].getPlayer();
      if (a == null || !a.isValid()) continue;
      Location aLoc = a.getLocation();
      for (int j = i + 1; j < bots.length; j++) {
        Player b = bots[j].getPlayer();
        if (b == null || !b.isValid() || !a.getWorld().equals(b.getWorld())) continue;
        if (!canCollide(a, b)) continue;
        Location bLoc = b.getLocation();
        double dy = aLoc.getY() - bLoc.getY();
        if (dy > 2.0 || dy < -2.0) continue;
        double dx = bLoc.getX() - aLoc.getX();
        double dz = bLoc.getZ() - aLoc.getZ();
        double distSq = dx * dx + dz * dz;
        if (distSq > radius * radius || distSq < 1e-12) continue;
        double dist = Math.sqrt(distSq);
        double nx = dx / dist;
        double nz = dz / dist;
        double strength = strengthBase * (1.0 - (dist / radius)) * 0.5;
        applyImpulse(b, nx * strength, 0.0, nz * strength, maxHoriz, 0.9);
        applyImpulse(a, -nx * strength, 0.0, -nz * strength, maxHoriz, 0.9);
      }
    }
  }

  private void applyBotKnockback(Player target, Vector velocity) {
    NmsPlayerSpawner.applyServerVelocity(target, velocity);
    plugin
        .getServer()
        .getScheduler()
        .runTaskLater(
            plugin,
            () -> {
              if (target.isOnline() && target.isValid() && manager.isBot(target)) {
                NmsPlayerSpawner.applyServerVelocity(target, velocity);
              }
            },
            1L);
  }

  private static Vector computeHorizontalKnockback(
      Entity attacker, Player target, double dx, double dz, double dist, double upward) {
    if (dist >= 1e-4) return new Vector(dx / dist, upward, dz / dist);
    Vector facing = attacker.getLocation().getDirection().setY(0);
    if (facing.lengthSquared() < 1e-8) facing = target.getLocation().getDirection().setY(0);
    if (facing.lengthSquared() < 1e-8) facing = new Vector(1, 0, 0);
    facing.normalize();
    return new Vector(facing.getX(), upward, facing.getZ());
  }

  private static Vector scaleHorizontal(Vector input, double multiplier) {
    double scale = Math.max(0.0, multiplier);
    return new Vector(input.getX() * scale, input.getY(), input.getZ() * scale);
  }

  private static Vector clampHorizontal(Vector velocity, double maxHoriz) {
    double speed = Math.sqrt(velocity.getX() * velocity.getX() + velocity.getZ() * velocity.getZ());
    if (speed > maxHoriz) {
      double scale = maxHoriz / speed;
      velocity.setX(velocity.getX() * scale);
      velocity.setZ(velocity.getZ() * scale);
    }
    return velocity;
  }

  private static void applyImpulse(
      Entity body, double ix, double iy, double iz, double maxHoriz, double maxUpward) {
    Vector vel = body.getVelocity();
    vel.setX(vel.getX() + ix);
    vel.setY(Math.max(-4.0, Math.min(maxUpward, vel.getY() + iy)));
    vel.setZ(vel.getZ() + iz);
    body.setVelocity(clampHorizontal(vel, maxHoriz));
  }

  private boolean canCollide(Player source, Entity other) {
    if (source == null || other == null) return false;
    if (!source.isCollidable()) return false;
    if (other instanceof Player otherPlayer && !otherPlayer.isCollidable()) return false;
    Scoreboard board = source.getScoreboard();
    if (board == null) return true;
    Team sourceTeam = board.getEntryTeam(source.getName());
    if (sourceTeam == null) return true;
    Team otherTeam = board.getEntryTeam(other.getName());
    Team.OptionStatus rule = sourceTeam.getOption(Team.Option.COLLISION_RULE);
    if (rule == Team.OptionStatus.NEVER) return false;
    if (rule == Team.OptionStatus.FOR_OWN_TEAM) return otherTeam != null && sourceTeam.equals(otherTeam);
    if (rule == Team.OptionStatus.FOR_OTHER_TEAMS) return otherTeam == null || !sourceTeam.equals(otherTeam);
    return true;
  }

  @SuppressWarnings("deprecation")
  private boolean isPvpEnabled(Location location) {
    return location != null && location.getWorld() != null && location.getWorld().getPVP();
  }

  private static Entity resolveKnockbackSource(Entity damager) {
    if (damager instanceof Projectile projectile) {
      ProjectileSource shooter = projectile.getShooter();
      if (shooter instanceof Entity shooterEntity) return shooterEntity;
    }
    return damager;
  }
}
