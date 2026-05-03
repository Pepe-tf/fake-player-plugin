package me.bill.fakePlayerPlugin.listener;

import me.bill.fakePlayerPlugin.FakePlayerPlugin;
import me.bill.fakePlayerPlugin.api.event.FppBotDeathEvent;
import me.bill.fakePlayerPlugin.api.impl.FppBotImpl;
import me.bill.fakePlayerPlugin.config.Config;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayer;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayerManager;
import me.bill.fakePlayerPlugin.util.AttributeCompat;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public final class FakePlayerDeathListener implements Listener {

  private final FakePlayerPlugin plugin;
  private final FakePlayerManager manager;

  public FakePlayerDeathListener(FakePlayerPlugin plugin, FakePlayerManager manager) {
    this.plugin = plugin;
    this.manager = manager;
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onDeathMessage(PlayerDeathEvent event) {
    if (!manager.isBot(event.getEntity())) return;
    if (!Config.deathMessage()) event.deathMessage(null);
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onBotDeath(PlayerDeathEvent event) {
    Player dead = event.getEntity();
    FakePlayer fp = manager.getByUuid(dead.getUniqueId());
    if (fp == null) return;

    if (Config.suppressDrops()) {
      event.getDrops().clear();
      event.setDroppedExp(0);
    }

    fp.incrementDeathCount();
    fp.setAlive(false);
    Bukkit.getPluginManager().callEvent(new FppBotDeathEvent(new FppBotImpl(fp), dead.getKiller()));

    if (fp.isRespawnOnDeath()) {
      respawnLater(fp, dead);
    } else {
      despawnLater(fp, dead);
    }
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onBotRespawn(PlayerRespawnEvent event) {
    FakePlayer fp = manager.getByUuid(event.getPlayer().getUniqueId());
    if (fp == null || !fp.isRespawning()) return;
    Location spawn = fp.getSpawnLocation();
    if (spawn != null && spawn.getWorld() != null) event.setRespawnLocation(spawn);
  }

  private void respawnLater(FakePlayer fp, Player dead) {
    fp.setRespawning(true);
    int delay = Math.max(1, Config.respawnDelay());
    Bukkit.getScheduler()
        .runTaskLater(
            plugin,
            () -> {
              if (!dead.isOnline()) {
                fp.setRespawning(false);
                manager.despawn(fp, dead);
                return;
              }
              dead.spigot().respawn();
              Bukkit.getScheduler()
                  .runTaskLater(
                      plugin,
                      () -> {
                        fp.setPlayer(dead);
                        fp.setAlive(true);
                        fp.setRespawning(false);
                        heal(dead);
                      },
                      2L);
            },
            delay);
  }

  private void despawnLater(FakePlayer fp, Player dead) {
    fp.setPlayer(null);
    int delay = Math.max(1, Config.deathDespawnDelay());
    Bukkit.getScheduler().runTaskLater(plugin, () -> manager.despawn(fp, dead), delay);
  }

  private void heal(Player player) {
    try {
      var attr = player.getAttribute(AttributeCompat.maxHealth());
      double max = attr == null ? 20.0 : attr.getValue();
      player.setHealth(Math.max(1.0, max));
      player.setFoodLevel(20);
      player.setFireTicks(0);
    } catch (Exception ignored) {
    }
  }
}
