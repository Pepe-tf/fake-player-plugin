package me.bill.fakePlayerPlugin.listener;

import me.bill.fakePlayerPlugin.FakePlayerPlugin;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayerManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Syncs all currently active fake players to any real player who joins,
 * so they see the tab list entries and in-world entities immediately.
 */
public class PlayerJoinListener implements Listener {

    private final FakePlayerPlugin plugin;
    private final FakePlayerManager manager;

    public PlayerJoinListener(FakePlayerPlugin plugin, FakePlayerManager manager) {
        this.plugin  = plugin;
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (manager.getCount() == 0) return;
        // Small delay so the client is fully ready to receive entity packets
        event.getPlayer().getScheduler().runDelayed(
                plugin,
                task -> manager.syncToPlayer(event.getPlayer()),
                null,
                5L
        );
    }
}
