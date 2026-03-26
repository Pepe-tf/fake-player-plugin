package me.bill.fakePlayerPlugin.fakeplayer;

import me.bill.fakePlayerPlugin.FakePlayerPlugin;
import me.bill.fakePlayerPlugin.config.Config;
import me.bill.fakePlayerPlugin.util.LuckPermsHelper;
import me.bill.fakePlayerPlugin.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Drives bot fake-chat. Each bot gets its OWN independent scheduler loop so
 * messages fire as fast as the interval config allows — identical pacing to
 * the join/leave stagger system.
 * <p>
 * Per-bot loop:
 * <ol>
 *   <li>Wait random delay between [min, max] seconds.</li>
 *   <li>Roll chance — skip silently if unlucky.</li>
 *   <li>Pick a message that differs from the bot's own last message.</li>
 *   <li>Resolve {name} / {random_player} placeholders.</li>
 *   <li>Broadcast and reschedule.</li>
 * </ol>
 * All config values are re-read on every fire so /fpp reload and
 * /fpp chat on|off take effect immediately without a restart.
 */
public final class BotChatAI {

    private final FakePlayerPlugin  plugin;
    private final FakePlayerManager manager;

    /** Last message each bot sent — per-bot no-repeat tracking. */
    private final Map<UUID, String> lastMessage = new ConcurrentHashMap<>();
    /** Task IDs for each bot's scheduler loop so we can cancel on disable. */
    private final Map<UUID, Integer> taskIds = new ConcurrentHashMap<>();

    public BotChatAI(FakePlayerPlugin plugin, FakePlayerManager manager) {
        this.plugin  = plugin;
        this.manager = manager;
        // Start a watcher that picks up newly spawned bots and gives each one
        // its own loop. Runs every 20 ticks (1 s) — lightweight.
        Bukkit.getScheduler().runTaskTimer(plugin, this::syncBotLoops, 20L, 20L);
    }

    // ── Loop management ───────────────────────────────────────────────────────

    /** Starts loops for new bots, cancels loops for removed bots. */
    private void syncBotLoops() {
        // Start a loop for any newly spawned bot (no Set allocation)
        for (FakePlayer fp : manager.getActivePlayers()) {
            if (!taskIds.containsKey(fp.getUuid())) {
                scheduleNext(fp.getUuid());
            }
        }
        // Cancel loops for bots that are no longer active
        taskIds.entrySet().removeIf(e -> {
            if (manager.getByUuid(e.getKey()) == null) {
                Bukkit.getScheduler().cancelTask(e.getValue());
                lastMessage.remove(e.getKey());
                return true;
            }
            return false;
        });
    }


    private void scheduleNext(UUID botUuid) {
        if (!Config.fakeChatEnabled()) {
            // Re-check in 2 s — quick reaction when chat is re-enabled
            int id = Bukkit.getScheduler().runTaskLater(plugin,
                    () -> scheduleNext(botUuid), 40L).getTaskId();
            taskIds.put(botUuid, id);
            return;
        }

        int minTicks = Config.fakeChatIntervalMin() * 20;
        int maxTicks = Config.fakeChatIntervalMax() * 20;
        if (maxTicks < minTicks) maxTicks = minTicks;

        long delay = minTicks == maxTicks
                ? Math.max(1L, minTicks)
                : Math.max(1L, minTicks + ThreadLocalRandom.current().nextInt(maxTicks - minTicks + 1));

        int id = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            fireChat(botUuid);
            // Only reschedule if bot is still active
            if (manager.getByUuid(botUuid) != null) {
                scheduleNext(botUuid);
            } else {
                taskIds.remove(botUuid);
                lastMessage.remove(botUuid);
            }
        }, delay).getTaskId();

        taskIds.put(botUuid, id);
    }

    // ── Fire ──────────────────────────────────────────────────────────────────

    private void fireChat(UUID botUuid) {
        if (!Config.fakeChatEnabled()) return;

        FakePlayer bot = manager.getByUuid(botUuid);
        if (bot == null) return;

        if (Config.fakeChatRequirePlayer() && Bukkit.getOnlinePlayers().isEmpty()) return;

        // Chance roll
        if (ThreadLocalRandom.current().nextDouble() > Config.fakeChatChance()) return;

        List<String> messages = Config.fakeChatMessages();
        if (messages.isEmpty()) return;

        // Pick message — avoid the same bot repeating its own last message
        String last = lastMessage.get(botUuid);
        String raw;
        if (messages.size() == 1) {
            raw = messages.getFirst();
        } else {
            List<String> pool = new ArrayList<>(messages);
            if (last != null) pool.remove(last);
            raw = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
        }
        lastMessage.put(botUuid, raw);

        // Resolve placeholders
        String message = raw
                .replace("{name}", bot.getName())
                .replace("{random_player}", resolveRandomPlayer(bot));

        // Build the broadcast line from the configurable chat-format
        // Resolve LP prefix/suffix (cached — no extra HTTP calls)
        String prefix = "";
        String suffix = "";
        if (Config.luckpermsUsePrefix()) {
            LuckPermsHelper.LpData lpData = LuckPermsHelper.getBotLpData(
                    bot.getLuckpermsGroup(), bot.getSpawnedByUuid());
            prefix = lpData.prefix();
            suffix = lpData.suffix();
        }

        String formatted = Config.fakeChatFormat()
                .replace("{prefix}", prefix)
                .replace("{suffix}", suffix)
                .replace("{bot_name}", bot.getDisplayName())
                .replace("{message}", message);

        // Expand PlaceholderAPI tokens if available
        if (formatted.contains("%")) {
            try {
                if (org.bukkit.Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                    formatted = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(null, formatted);
                }
            } catch (Exception ignored) {}
        }

        Component chatLine = TextUtil.colorize(formatted);

        Bukkit.getServer().broadcast(chatLine);
        Config.debug("BotChatAI: " + bot.getName() + " said: " + message);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String resolveRandomPlayer(FakePlayer self) {
        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (!online.isEmpty())
            return online.get(ThreadLocalRandom.current().nextInt(online.size())).getName();

        List<FakePlayer> others = new ArrayList<>(manager.getActivePlayers());
        others.removeIf(fp -> fp.getUuid().equals(self.getUuid()));
        if (!others.isEmpty())
            return others.get(ThreadLocalRandom.current().nextInt(others.size())).getName();

        return self.getName();
    }
}
