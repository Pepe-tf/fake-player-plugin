package me.bill.fakePlayerPlugin.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Plugin(
        id = "fpp-velocity",
        name = "FPP Velocity",
        version = "1.6.6",
        description = "Makes FakePlayerPlugin bots count in the Velocity server list",
        authors = {"bill"})
public class FppVelocityPlugin {

    private static final MinecraftChannelIdentifier FPP_PROXY_CHANNEL =
            MinecraftChannelIdentifier.create("fpp", "proxy");

    private static final String BOT_SPAWN = "BOT_SPAWN";
    private static final String BOT_DESPAWN = "BOT_DESPAWN";
    private static final String SERVER_OFFLINE = "SERVER_OFFLINE";

    // ANSI colours (Velocity supports them on most terminals)
    private static final String RESET  = "\u001B[0m";
    private static final String BOLD   = "\u001B[1m";
    private static final String BLUE   = "\u001B[38;2;0;121;255m";
    private static final String WHITE  = "\u001B[97m";
    private static final String GREEN  = "\u001B[92m";
    private static final String GRAY   = "\u001B[90m";
    private static final String DARK   = "\u001B[38;5;240m";
    private static final String CYAN   = "\u001B[96m";
    private static final String TAG    = BOLD + BLUE + "[ꜰᴘᴘ-ᴠᴇʟᴏᴄɪᴛʏ]" + RESET;

    private static final int RULE_WIDTH = 50;
    private static final int KEY_WIDTH  = 18;

    private final ProxyServer proxy;
    private final Logger logger;
    private final long startTime = System.currentTimeMillis();

    /** Per-bot registry populated by plugin messages — provides hover-list names. */
    private final ConcurrentHashMap<UUID, BotEntry> botRegistry = new ConcurrentHashMap<>();

    /**
     * Cached total players (real + bots) from backend pings, refreshed every 5 s.
     * Bots are real NMS entities so they appear in backend player counts naturally.
     */
    private final AtomicInteger cachedBackendTotal = new AtomicInteger(0);

    @Inject
    public FppVelocityPlugin(ProxyServer proxy, Logger logger) {
        this.proxy = proxy;
        this.logger = logger;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        proxy.getChannelRegistrar().register(FPP_PROXY_CHANNEL);

        proxy.getScheduler()
                .buildTask(this, this::refreshBackendCounts)
                .repeat(5, TimeUnit.SECONDS)
                .delay(3, TimeUnit.SECONDS)
                .schedule();

        long ms = System.currentTimeMillis() - startTime;
        printStartupBanner(ms);
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        printShutdownBanner();
    }

    // ── Startup / shutdown banners ────────────────────────────────────────────

    private void printStartupBanner(long startupMs) {
        boldRule();
        log("  " + BOLD + BLUE + "FPP Velocity" + RESET + WHITE + " v1.6.6" + RESET);
        rule();
        section("Status");
        statusRow(true,  "Channel",      "fpp:proxy");
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
        String line   = BOLD + RED + "█".repeat(RULE_WIDTH) + RESET;
        logger.warn(line);
        logger.warn(TAG + " " + BOLD + YELLOW + "  ⚠  IMPORTANT — PLEASE READ  ⚠" + RESET);
        logger.warn(line);
        logger.warn(TAG + " " + WHITE + "  FakePlayerPlugin and FPP Velocity are " + BOLD + GREEN + "100% FREE" + RESET + WHITE + " and open-source." + RESET);
        logger.warn(TAG + " " + WHITE + "  They are always available at no cost on Modrinth, Hangar," + RESET);
        logger.warn(TAG + " " + WHITE + "  SpigotMC, and GitHub — officially, by the original author." + RESET);
        logger.warn(TAG + "");
        logger.warn(TAG + " " + BOLD + RED + "  If you or your server paid money for this plugin," + RESET);
        logger.warn(TAG + " " + BOLD + RED + "  you have been SCAMMED by a reseller!" + RESET);
        logger.warn(TAG + "");
        logger.warn(TAG + " " + CYAN + "  ➤ Free download:  " + RESET + WHITE + "https://modrinth.com/plugin/fake-player-plugin-(fpp)" + RESET);
        logger.warn(TAG + " " + CYAN + "  ➤ Source code:    " + RESET + WHITE + "https://github.com/Pepe-tf/fake-player-plugin" + RESET);
        logger.warn(TAG + " " + CYAN + "  ➤ Support:        " + RESET + WHITE + "https://discord.gg/QSN7f67nkJ" + RESET);
        logger.warn(TAG + "");
        logger.warn(TAG + " " + GRAY + "  Report scam listings so other server owners aren't tricked." + RESET);
        logger.warn(line);
    }

    private void printShutdownBanner() {
        long uptime = System.currentTimeMillis() - startTime;
        boldRule();
        logHighlight("  ꜰᴘᴘ ᴠᴇʟᴏᴄɪᴛʏ  -  shutting down");
        rule();
        kv("Bots tracked",   botRegistry.size());
        kv("Last count",     cachedBackendTotal.get());
        kv("Session uptime", formatUptime(uptime));
        boldRule();
        log("  Goodbye! FPP Velocity has been disabled.");
        boldRule();
    }

    // ── Backend ping ──────────────────────────────────────────────────────────

    private void refreshBackendCounts() {
        List<CompletableFuture<Integer>> futures = new ArrayList<>();
        for (RegisteredServer server : proxy.getAllServers()) {
            futures.add(server.ping()
                    .thenApply(ping -> ping.getPlayers().map(ServerPing.Players::getOnline).orElse(0))
                    .exceptionally(ex -> 0));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(() -> {
            int total = futures.stream().mapToInt(f -> { try { return f.join(); } catch (Exception e) { return 0; } }).sum();
            cachedBackendTotal.set(total);
        });
    }

    // ── Plugin messages ───────────────────────────────────────────────────────

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!(event.getSource() instanceof ServerConnection)) return;
        if (!event.getIdentifier().equals(FPP_PROXY_CHANNEL)) return;

        event.setResult(PluginMessageEvent.ForwardResult.handled());
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(event.getData()));
            String subchannel = in.readUTF();
            switch (subchannel) {
                case BOT_SPAWN      -> handleBotSpawn(in);
                case BOT_DESPAWN    -> handleBotDespawn(in);
                case SERVER_OFFLINE -> handleServerOffline(in);
            }
        } catch (Exception e) {
            logger.warn("[FPP-Velocity] Failed to parse plugin message: {}", e.getMessage());
        }
    }

    private void handleBotSpawn(DataInputStream in) throws Exception {
        in.readUTF(); // msgId
        String serverId = in.readUTF();
        UUID uuid = UUID.fromString(in.readUTF());
        String name = in.readUTF();
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

    @Subscribe
    public void onProxyPing(ProxyPingEvent event) {
        ServerPing ping = event.getPing();
        ServerPing.Players existingPlayers = ping.getPlayers().orElse(null);
        if (existingPlayers == null) return;

        int backendTotal = cachedBackendTotal.get();
        int velocityCount = existingPlayers.getOnline();
        int displayCount = backendTotal > velocityCount
                ? backendTotal
                : velocityCount + botRegistry.size();

        if (displayCount <= velocityCount && botRegistry.isEmpty()) return;

        List<ServerPing.SamplePlayer> sample = new ArrayList<>(existingPlayers.getSample());
        for (BotEntry bot : botRegistry.values()) {
            if (sample.size() >= 12) break;
            String dn = bot.displayName() != null && !bot.displayName().isBlank() ? bot.displayName() : bot.name();
            sample.add(new ServerPing.SamplePlayer(dn, bot.uuid()));
        }

        event.setPing(ping.asBuilder()
                .onlinePlayers(displayCount)
                .clearSamplePlayers()
                .samplePlayers(sample.toArray(new ServerPing.SamplePlayer[0]))
                .build());
    }

    // ── Banner helpers ────────────────────────────────────────────────────────

    private void log(String msg)          { logger.info(TAG + " " + WHITE + msg + RESET); }
    private void logSuccess(String msg)   { logger.info(TAG + " " + GREEN + msg + RESET); }
    private void logHighlight(String msg) { logger.info(TAG + " " + BOLD + CYAN + msg + RESET); }

    private void rule()     { logger.info(TAG + " " + DARK + "─".repeat(RULE_WIDTH) + RESET); }
    private void boldRule() { logger.info(TAG + " " + GRAY + BOLD + "═".repeat(RULE_WIDTH) + RESET); }

    private void section(String label) {
        String dashes = "─".repeat(Math.max(0, RULE_WIDTH - label.length() - 4));
        logger.info(TAG + " " + DARK + "── " + RESET + BOLD + WHITE + label + " " + DARK + dashes + RESET);
    }

    private void kv(String key, Object value) {
        int dots = Math.max(1, KEY_WIDTH - key.length());
        String dotStr = DARK + ".".repeat(dots) + RESET;
        logger.info(TAG + " " + GRAY + "  " + WHITE + key + " " + dotStr + " " + BLUE + value + RESET);
    }

    private void statusRow(boolean ok, String label, String detail) {
        String badge = ok ? GREEN + "[+]" + RESET : GRAY + "[-]" + RESET;
        String valueColor = ok ? GREEN : GRAY;
        int dots = Math.max(1, KEY_WIDTH - label.length());
        String dotStr = DARK + ".".repeat(dots) + RESET;
        logger.info(TAG + " " + GRAY + "  " + badge + " " + WHITE + label + " " + dotStr + " " + valueColor + detail + RESET);
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
