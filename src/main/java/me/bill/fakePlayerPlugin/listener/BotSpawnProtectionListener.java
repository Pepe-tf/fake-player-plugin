package me.bill.fakePlayerPlugin.listener;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import me.bill.fakePlayerPlugin.FakePlayerPlugin;
import me.bill.fakePlayerPlugin.config.Config;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayerManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.persistence.PersistentDataType;

public class BotSpawnProtectionListener implements Listener {

  private final FakePlayerPlugin plugin;
  private final Set<UUID> protectedBots = new HashSet<>();

  public BotSpawnProtectionListener(FakePlayerPlugin plugin) {
    this.plugin = plugin;
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBotJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();

    if (!isFppBot(player)) return;

    UUID botUuid = player.getUniqueId();
    protectedBots.add(botUuid);

    Config.debugNms(
        "BotSpawnProtection: protecting " + player.getName() + " from teleports for 5 ticks");

    Bukkit.getScheduler()
        .runTaskLater(
            plugin,
            () -> {
              protectedBots.remove(botUuid);
              Config.debugNms("BotSpawnProtection: removed protection for " + player.getName());
            },
            5L);
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onBotTeleport(PlayerTeleportEvent event) {
    Player player = event.getPlayer();

    if (!isFppBot(player)) return;

    if (!protectedBots.contains(player.getUniqueId())) return;

    PlayerTeleportEvent.TeleportCause cause = event.getCause();

    if (cause == PlayerTeleportEvent.TeleportCause.COMMAND) {
      return;
    }

    if (cause == PlayerTeleportEvent.TeleportCause.PLUGIN) {
      event.setCancelled(true);
      Config.debugNms(
          "BotSpawnProtection: blocked PLUGIN teleport for "
              + player.getName()
              + " from "
              + formatLoc(event.getFrom())
              + " to "
              + formatLoc(event.getTo()));
      return;
    }

    if (cause == PlayerTeleportEvent.TeleportCause.UNKNOWN) {
      event.setCancelled(true);
      Config.debugNms(
          "BotSpawnProtection: blocked UNKNOWN teleport for "
              + player.getName()
              + " from "
              + formatLoc(event.getFrom())
              + " to "
              + formatLoc(event.getTo()));
    }
  }

  private static boolean isFppBot(Player player) {
    if (FakePlayerManager.FAKE_PLAYER_KEY == null) return false;
    String marker =
        player
            .getPersistentDataContainer()
            .get(FakePlayerManager.FAKE_PLAYER_KEY, PersistentDataType.STRING);
    return marker != null && marker.startsWith("fpp-visual:");
  }

  private String formatLoc(Location loc) {
    if (loc == null) return "null";
    return String.format(
        "%s (%.1f, %.1f, %.1f)",
        loc.getWorld() != null ? loc.getWorld().getName() : "?",
        loc.getX(),
        loc.getY(),
        loc.getZ());
  }
}
