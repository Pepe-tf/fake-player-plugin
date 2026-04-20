package me.bill.fakePlayerPlugin.bungee;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class FppBungeePlugin extends Plugin implements Listener {

    private static final String FPP_PROXY_CHANNEL = "fpp:proxy";

    private static final String BOT_SPAWN      = "BOT_SPAWN";
    private static final String BOT_DESPAWN    = "BOT_DESPAWN";
    private static final String SERVER_OFFLINE = "SERVER_OFFLINE";

    // ANSI colours — supported on Waterfall and most modern terminals
    private static final String RESET = "\u001B[0m";
    private static final String BOLD  = "\u001B[1m";
    private static final String BLUE  = "\u001B[38;2;0;121;255m";
    private static final String WHITE = "\u001B[97m";
    private static final String GREEN = "\u001B[92m";
    private static final String GRAY  = "\u001B[90m";
    private static final String DARK  = "\u001B[38;5;240m";
    private static final String CYAN  = "\u001B[96m";
    private static final String TAG   = BOLD + BLUE + "[ꜰᴘᴘ-ʙᴜɴɢᴇᴇ]" + RESET;

    private static final int RULE_WIDTH = 50;
    private static final int KEY_WIDTH  = 18;

    private final long startTime = System.currentTimeMillis();

    /** Per-bot registry populated by plugin messages — provides hover-list names. */
    private final ConcurrentHashMap<UUID, BotEntry> botRegistry = new ConcurrentHashMap<>();

    /**
     * Cached total players (real + bots) from backend pings, refreshed every 5 s.
     * Bots are real NMS entities so they appear in backend player counts naturally.
     */
    private final AtomicInteger cachedBackendTotal = new AtomicInteger(0);

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void onEnable() {
        getProxy().registerChannel(FPP_PROXY_CHANNEL);
        getProxy().getPluginManager().registerListener(this, this);

        getProxy().getScheduler().schedule(
                this, this::refreshBackendCounts,
                3, 5, TimeUnit.SECONDS);

        long ms = System.currentTimeMillis() - startTime;
        printStartupBanner(ms);
    }

    @Override
    public void onDisable() {
        printShutdownBanner();
        getProxy().unregisterChannel(FPP_PROXY_CHANNEL);
    }

    // ── Startup / shutdown banners ────────────────────────────────────────────

    private void printStartupBanner(long startupMs) {
        boldRule();
        log("  " + BOLD + BLUE + "FPP Bungee" + RESET + WHITE + " v" + getDescription().getVersion() + RESET);
        rule();
        section("Status");
        statusRow(true,  "Channel",      FPP_PROXY_CHANNEL);
        statusRow(true,  "Backend ping", "every 5 s");
        statusRow(false, "Bot registry", "empty  (waiting for bots)");
        kv("Startup time", startupMs + "ms");
        rule();
        logSuccess("  Ready — proxy server list will include FPP bots.");
        boldRule();
        printAntiScamWarning();
    }

    private void printAntiScamWarning() {
        String YELLOW = "\u001B[93m";
        String RED    = "\u001B[91m";
        String line   = BOLD + RED + "═".repeat(RULE_WIDTH) + RESET;
        Logger log = getLogger();
        log.warning(line);
        log.warning(TAG + " " + BOLD + YELLOW + "  ⚠  IMPORTANT — PLEASE READ  ⚠" + RESET);
        log.warning(line);
        log.warning(TAG + " " + WHITE + "  FakePlayerPlugin and FPP Bungee are " + BOLD + GREEN + "100% FREE" + RESET + WHITE + " and open-source." + RESET);
        log.warning(TAG + " " + WHITE + "  They are always available at no cost on Modrinth, Hangar," + RESET);
        log.warning(TAG + " " + WHITE + "  SpigotMC, and GitHub — officially, by the original author." + RESET);
        log.warning(TAG + "");
        log.warning(TAG + " " + BOLD + RED + "  If you or your server paid money for this plugin," + RESET);
        log.warning(TAG + " " + BOLD + RED + "  you have been SCAMMED by a reseller!" + RESET);
        log.warning(TAG + "");
        log.warning(TAG + " " + CYAN + "  ➤ Free download:  " + RESET + WHITE + "https://modrinth.com/plugin/fake-player-plugin-(fpp)" + RESET);
        log.warning(TAG + " " + CYAN + "  ➤ Source code:    " + RESET + WHITE + "https://github.com/Pepe-tf/fake-player-plugin" + RESET);
        log.warning(TAG + " " + CYAN + "  ➤ Support:        " + RESET + WHITE + "https://discord.gg/QSN7f67nkJ" + RESET);
        log.warning(TAG + "");
        log.warning(TAG + " " + GRAY + "  Report scam listings so other server owners aren't tricked." + RESET);
        log.warning(line);
    }

    private void printShutdownBanner() {
        long uptime = System.currentTimeMillis() - startTime;
        boldRule();
        logHighlight("  ꜰᴘᴘ ʙᴜɴɢᴇᴇ  -  shutting down");
        rule();
        kv("Bots tracked",   botRegistry.size());
        kv("Last count",     cachedBackendTotal.get());
        kv("Session uptime", formatUptime(uptime));
        boldRule();
        log("  Goodbye! FPP Bungee has been disabled.");
        boldRule();
    }

    // ── Backend ping ──────────────────────────────────────────────────────────

    private void refreshBackendCounts() {
        Set<String> serverNames = getProxy().getServers().keySet();
        if (serverNames.isEmpty()) {
            cachedBackendTotal.set(0);
            return;
        }

        AtomicInteger pending = new AtomicInteger(serverNames.size());
        AtomicInteger total   = new AtomicInteger(0);

        for (String name : serverNames) {
            net.md_5.bungee.api.config.ServerInfo info = getProxy().getServerInfo(name);
            if (info == null) {
                if (pending.decrementAndGet() == 0) cachedBackendTotal.set(total.get());
                continue;
            }
            info.ping((result, error) -> {
                if (result != null && result.getPlayers() != null) {
                    total.addAndGet(result.getPlayers().getOnline());
                }
                if (pending.decrementAndGet() == 0) {
                    cachedBackendTotal.set(total.get());
                }
            });
        }
    }

    // ── Plugin messages ───────────────────────────────────────────────────────

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if (!(event.getSender() instanceof Server)) return;
        if (!event.getTag().equals(FPP_PROXY_CHANNEL)) return;

        event.setCancelled(true);
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(event.getData()));
            String subchannel = in.readUTF();
            switch (subchannel) {
                case BOT_SPAWN      -> handleBotSpawn(in);
                case BOT_DESPAWN    -> handleBotDespawn(in);
                case SERVER_OFFLINE -> handleServerOffline(in);
            }
        } catch (Exception e) {
            getLogger().warning("[FPP-Bungee] Failed to parse plugin message: " + e.getMessage());
        }
    }

    private void handleBotSpawn(DataInputStream in) throws Exception {
        in.readUTF(); // msgId
        String serverId    = in.readUTF();
        UUID   uuid        = UUID.fromString(in.readUTF());
        String name        = in.readUTF();
        String displayName = in.readUTF();
        botRegistry.put(uuid, new BotEntry(uuid, name, displayName, serverId));
    }

    private void handleBotDespawn(DataInputStream in) throws Exception {
        in.readUTF(); // msgId
        in.readUTF(); // serverId
        UUID uuid = UUID.fromString(in.readUTF());
        botRegistry.remove(uuid);
    }

    private void handleServerOffline(DataInputStream in) throws Exception {
        in.readUTF(); // msgId
        String serverId = in.readUTF();
        botRegistry.entrySet().removeIf(e -> serverId.equals(e.getValue().serverId()));
    }

    // ── ProxyPingEvent ────────────────────────────────────────────────────────

    @EventHandler
    public void onProxyPing(ProxyPingEvent event) {
        ServerPing ping = event.getResponse();
        if (ping == null) return;

        ServerPing.Players players = ping.getPlayers();
        if (players == null) return;

        int backendTotal   = cachedBackendTotal.get();
        int bungeeCount    = players.getOnline();
        int displayCount   = backendTotal > bungeeCount
                ? backendTotal
                : bungeeCount + botRegistry.size();

        if (displayCount <= bungeeCount && botRegistry.isEmpty()) return;

        players.setOnline(displayCount);

        // Add bot names to the hover sample list (capped at 12 total entries)
        List<ServerPing.PlayerInfo> sample = new ArrayList<>(
                players.getSample() != null
                        ? Arrays.asList(players.getSample())
                        : List.of());

        for (BotEntry bot : botRegistry.values()) {
            if (sample.size() >= 12) break;
            String dn = bot.displayName() != null && !bot.displayName().isBlank()
                    ? bot.displayName()
                    : bot.name();
            sample.add(new ServerPing.PlayerInfo(dn, bot.uuid()));
        }

        players.setSample(sample.toArray(new ServerPing.PlayerInfo[0]));
    }

    // ── Banner helpers ────────────────────────────────────────────────────────

    private void log(String msg)          { getLogger().info(TAG + " " + WHITE + msg + RESET); }
    private void logSuccess(String msg)   { getLogger().info(TAG + " " + GREEN + msg + RESET); }
    private void logHighlight(String msg) { getLogger().info(TAG + " " + BOLD + CYAN + msg + RESET); }

    private void rule()     { getLogger().info(TAG + " " + DARK + "─".repeat(RULE_WIDTH) + RESET); }
    private void boldRule() { getLogger().info(TAG + " " + GRAY + BOLD + "═".repeat(RULE_WIDTH) + RESET); }

    private void section(String label) {
        String dashes = "─".repeat(Math.max(0, RULE_WIDTH - label.length() - 4));
        getLogger().info(TAG + " " + DARK + "── " + RESET + BOLD + WHITE + label + " " + DARK + dashes + RESET);
    }

    private void kv(String key, Object value) {
        int dots = Math.max(1, KEY_WIDTH - key.length());
        String dotStr = DARK + ".".repeat(dots) + RESET;
        getLogger().info(TAG + " " + GRAY + "  " + WHITE + key + " " + dotStr + " " + BLUE + value + RESET);
    }

    private void statusRow(boolean ok, String label, String detail) {
        String badge      = ok ? GREEN + "[+]" + RESET : GRAY + "[-]" + RESET;
        String valueColor = ok ? GREEN : GRAY;
        int dots = Math.max(1, KEY_WIDTH - label.length());
        String dotStr = DARK + ".".repeat(dots) + RESET;
        getLogger().info(TAG + " " + GRAY + "  " + badge + " " + WHITE + label + " " + dotStr + " " + valueColor + detail + RESET);
    }

    private static String formatUptime(long ms) {
        long s = ms / 1000, h = s / 3600, m = (s % 3600) / 60;
        s %= 60;
        if (h > 0) return h + "h " + m + "m " + s + "s";
        if (m > 0) return m + "m " + s + "s";
        return s + "s";
    }

    public record BotEntry(UUID uuid, String name, String displayName, String serverId) {}
}

