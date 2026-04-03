package me.bill.fakePlayerPlugin.listener;

import me.bill.fakePlayerPlugin.config.Config;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayerManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.persistence.PersistentDataType;

/**
 * Prevents fake player bots from executing any commands.
 *
 * <p>Bots are NMS ServerPlayer entities and may trigger command events
 * if external plugins or mods attempt to force-run commands on them.
 * This listener blocks ALL command execution paths including:
 * <ul>
 *   <li>Direct command typing</li>
 *   <li>Commands executed via {@code Player.performCommand()}</li>
 *   <li>Commands executed via {@code Bukkit.dispatchCommand()}</li>
 *   <li>Auto-command plugins (first join, scheduled commands, etc.)</li>
 *   <li>Command suggestions and tab-completion</li>
 * </ul>
 *
 * <p>Uses multiple event priorities and {@code ignoreCancelled = false}
 * to ensure bots cannot execute commands under any circumstances.
 */
public class BotCommandBlocker implements Listener {

    /**
     * Blocks command execution at the earliest possible stage.
     * Uses LOWEST priority to catch commands before any other plugin
     * can process them, including auto-command plugins.
     * Note: ignoreCancelled defaults to false, ensuring we catch all events.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onBotCommandEarly(PlayerCommandPreprocessEvent event) {
        if (isFppBot(event.getPlayer())) {
            event.setCancelled(true);
            Config.debugNms("BotCommandBlocker: blocked command (LOWEST) for "
                    + event.getPlayer().getName() + ": " + event.getMessage());
        }
    }

    /**
     * Blocks command execution at the highest priority as a safety net.
     * Catches any commands that somehow bypassed the LOWEST priority handler.
     * Note: ignoreCancelled defaults to false, ensuring we catch all events.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBotCommandLate(PlayerCommandPreprocessEvent event) {
        if (isFppBot(event.getPlayer())) {
            event.setCancelled(true);
            Config.debugNms("BotCommandBlocker: blocked command (HIGHEST) for "
                    + event.getPlayer().getName() + ": " + event.getMessage());
        }
    }

    /**
     * Blocks command execution at MONITOR priority as final safeguard.
     * This ensures bots cannot execute commands even if another plugin
     * un-cancels the event at a lower priority.
     * Note: ignoreCancelled defaults to false, ensuring we catch all events.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onBotCommandMonitor(PlayerCommandPreprocessEvent event) {
        if (isFppBot(event.getPlayer())) {
            if (!event.isCancelled()) {
                event.setCancelled(true);
                Config.debugNms("BotCommandBlocker: blocked command (MONITOR) for "
                        + event.getPlayer().getName() + ": " + event.getMessage());
            }
        }
    }

    /**
     * Prevents bots from receiving command suggestions and tab-completion.
     * This clears the command list so auto-command plugins cannot discover
     * or suggest commands to bots.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onBotCommandSend(PlayerCommandSendEvent event) {
        if (isFppBot(event.getPlayer())) {
            event.getCommands().clear();
            Config.debugNms("BotCommandBlocker: cleared command suggestions for "
                    + event.getPlayer().getName());
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
}

