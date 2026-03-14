package me.bill.fakePlayerPlugin.util;

import me.bill.fakePlayerPlugin.FakePlayerPlugin;
import me.bill.fakePlayerPlugin.config.Config;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayerManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 * Manages the server tab-list header and footer for all online players.
 *
 * <p>When {@code tab-list.enabled: true}, this manager periodically
 * updates the header/footer with live bot-count and player-count placeholders:
 *
 * <ul>
 *   <li>{@code {bot_count}}   — active fake-player count</li>
 *   <li>{@code {real_count}}  — real online player count</li>
 *   <li>{@code {total_count}} — real + bot count</li>
 *   <li>{@code {max_bots}}    — configured max-bots (or ∞)</li>
 * </ul>
 *
 * <p>The header/footer is updated every {@code tab-list.update-interval} ticks
 * (default 40 = 2 s). Call {@link #reload()} after a {@code /fpp reload}
 * to pick up new config values. Call {@link #shutdown()} on plugin disable
 * to cancel the task and clear the header/footer.
 */
public final class TabListManager {

    private final FakePlayerPlugin plugin;
    private final FakePlayerManager botManager;

    private BukkitTask task;

    public TabListManager(FakePlayerPlugin plugin, FakePlayerManager botManager) {
        this.plugin     = plugin;
        this.botManager = botManager;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    public void start() {
        cancelTask();
        if (!Config.tabListEnabled()) return;

        int interval = Math.max(1, Config.tabListUpdateInterval());
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::update, interval, interval);
        FppLogger.debug("TabListManager started (interval=" + interval + " ticks).");
    }

    public void reload() {
        start(); // restart with potentially new interval / toggle
    }

    public void shutdown() {
        cancelTask();
        // Clear header/footer for all players on shutdown
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendPlayerListHeaderAndFooter(Component.empty(), Component.empty());
        }
    }

    // ── Update ────────────────────────────────────────────────────────────────

    private void update() {
        if (!Config.tabListEnabled()) return;

        int botCount   = botManager.getCount();
        int realCount  = Bukkit.getOnlinePlayers().size();
        int totalCount = realCount + botCount;
        int maxBots    = Config.maxBots();

        String rawHeader = Config.tabListHeader()
                .replace("{bot_count}",   String.valueOf(botCount))
                .replace("{real_count}",  String.valueOf(realCount))
                .replace("{total_count}", String.valueOf(totalCount))
                .replace("{max_bots}",    maxBots == 0 ? "∞" : String.valueOf(maxBots));

        String rawFooter = Config.tabListFooter()
                .replace("{bot_count}",   String.valueOf(botCount))
                .replace("{real_count}",  String.valueOf(realCount))
                .replace("{total_count}", String.valueOf(totalCount))
                .replace("{max_bots}",    maxBots == 0 ? "∞" : String.valueOf(maxBots));

        Component header = rawHeader.isBlank() ? Component.empty() : TextUtil.colorize(rawHeader);
        Component footer = rawFooter.isBlank() ? Component.empty() : TextUtil.colorize(rawFooter);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendPlayerListHeaderAndFooter(header, footer);
        }
    }

    /**
     * Forces an immediate single update — called when bots spawn or despawn
     * so the header/footer reflects the new count instantly.
     */
    public void updateNow() {
        if (Config.tabListEnabled()) {
            Bukkit.getScheduler().runTask(plugin, this::update);
        }
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private void cancelTask() {
        if (task != null && !task.isCancelled()) {
            task.cancel();
            task = null;
        }
    }
}


