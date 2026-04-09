package me.bill.fakePlayerPlugin.listener;

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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Protects bots from being teleported by lobby/spawn plugins during their initial spawn.
 *
 * <p>Many lobby systems teleport players to a lobby spawn location on join.
 * Since bots are real NMS ServerPlayer entities, they trigger PlayerJoinEvent
 * and get caught by these teleport mechanics. This listener prevents that
 * by canceling teleports for bots during their spawn grace period (first 5 ticks).
 */
public class BotSpawnProtectionListener implements Listener {

    private final FakePlayerPlugin plugin;
    private final Set<UUID> protectedBots = new HashSet<>();

    public BotSpawnProtectionListener(FakePlayerPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * When a bot joins (spawns), mark it as protected from teleports for 5 ticks.
     * This gives the bot time to fully spawn at the correct location before
     * lobby plugins can interfere.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBotJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Only protect bots, not real players
        if (!isFppBot(player)) return;

        UUID botUuid = player.getUniqueId();
        protectedBots.add(botUuid);

        Config.debugNms("BotSpawnProtection: protecting " + player.getName() + " from teleports for 5 ticks");

        // Remove protection after 5 ticks (250ms) - enough time for the spawn to complete
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            protectedBots.remove(botUuid);
            Config.debugNms("BotSpawnProtection: removed protection for " + player.getName());
        }, 5L);
    }

    /**
     * Cancel any teleport attempts on protected bots during their spawn grace period.
     * This prevents lobby plugins from moving bots to spawn points.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBotTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();

        // Only protect FPP bots
        if (!isFppBot(player)) return;

        // Only cancel if this bot is in the protection period
        if (!protectedBots.contains(player.getUniqueId())) return;

        // Allow teleports caused by FPP itself (e.g., /fpp tp, /fpp tph)
        // These will have cause PLUGIN or COMMAND
        PlayerTeleportEvent.TeleportCause cause = event.getCause();

        // Don't block FPP's own teleport commands or manual admin teleports
        if (cause == PlayerTeleportEvent.TeleportCause.COMMAND) {
            return; // Allow /tp, /fpp tp, /fpp tph
        }

        // Cancel plugin-initiated teleports (lobby systems) during spawn
        if (cause == PlayerTeleportEvent.TeleportCause.PLUGIN) {
            event.setCancelled(true);
            Config.debugNms("BotSpawnProtection: blocked PLUGIN teleport for " + player.getName()
                    + " from " + formatLoc(event.getFrom()) + " to " + formatLoc(event.getTo()));
            return;
        }

        // Also block UNKNOWN cause teleports during spawn (some lobby plugins use this)
        if (cause == PlayerTeleportEvent.TeleportCause.UNKNOWN) {
            event.setCancelled(true);
            Config.debugNms("BotSpawnProtection: blocked UNKNOWN teleport for " + player.getName()
                    + " from " + formatLoc(event.getFrom()) + " to " + formatLoc(event.getTo()));
        }
    }

    /**
     * Checks if the given player is an FPP bot by looking for the PDC marker.
     */
    private static boolean isFppBot(Player player) {
        if (FakePlayerManager.FAKE_PLAYER_KEY == null) return false;
        String marker = player.getPersistentDataContainer()
                .get(FakePlayerManager.FAKE_PLAYER_KEY, PersistentDataType.STRING);
        return marker != null && marker.startsWith("fpp-visual:");
    }

    /**
     * Formats a location for debug logging.
     */
    private String formatLoc(Location loc) {
        if (loc == null) return "null";
        return String.format("%s (%.1f, %.1f, %.1f)",
                loc.getWorld() != null ? loc.getWorld().getName() : "?",
                loc.getX(), loc.getY(), loc.getZ());
    }
}



