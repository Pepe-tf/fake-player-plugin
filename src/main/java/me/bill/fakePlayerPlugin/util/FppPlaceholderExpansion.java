package me.bill.fakePlayerPlugin.util;

import me.bill.fakePlayerPlugin.FakePlayerPlugin;
import me.bill.fakePlayerPlugin.config.Config;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayerManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

/**
 * PlaceholderAPI expansion for ꜰᴀᴋᴇ ᴘʟᴀʏᴇʀ ᴘʟᴜɢɪɴ.
 *
 * <p>Register with {@link #register()} after PlaceholderAPI is detected on the server.
 * All placeholders are server-wide (player-independent).
 *
 * <h3>Available placeholders</h3>
 * <table>
 *   <tr><td>{@code %fpp_count%}</td><td>Active fake-player count</td></tr>
 *   <tr><td>{@code %fpp_max%}</td><td>Global max-bots limit (or ∞)</td></tr>
 *   <tr><td>{@code %fpp_chat%}</td><td>{@code on} / {@code off} — fake-chat state</td></tr>
 *   <tr><td>{@code %fpp_swap%}</td><td>{@code on} / {@code off} — bot-swap state</td></tr>
 *   <tr><td>{@code %fpp_skin%}</td><td>Current skin mode</td></tr>
 *   <tr><td>{@code %fpp_body%}</td><td>{@code on} / {@code off} — body state</td></tr>
 *   <tr><td>{@code %fpp_frozen%}</td><td>Count of frozen bots</td></tr>
 *   <tr><td>{@code %fpp_version%}</td><td>Plugin version string</td></tr>
 * </table>
 */
public final class FppPlaceholderExpansion extends PlaceholderExpansion {

    private final FakePlayerPlugin  plugin;
    private final FakePlayerManager manager;

    public FppPlaceholderExpansion(FakePlayerPlugin plugin, FakePlayerManager manager) {
        this.plugin  = plugin;
        this.manager = manager;
    }

    @Override public @NotNull String getIdentifier() { return "fpp"; }
    @Override public @NotNull String getAuthor()     { return String.join(", ", plugin.getPluginMeta().getAuthors()); }
    @Override public @NotNull String getVersion()    { return plugin.getPluginMeta().getVersion(); }
    @Override public          boolean persist()       { return true; } // survive /papi reload

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        return switch (params.toLowerCase()) {
            case "count"   -> String.valueOf(manager.getCount());
            case "max"     -> Config.maxBots() == 0 ? "∞" : String.valueOf(Config.maxBots());
            case "chat"    -> Config.fakeChatEnabled() ? "on" : "off";
            case "swap"    -> Config.swapEnabled()    ? "on" : "off";
            case "skin"    -> Config.skinMode();
            case "body"    -> Config.spawnBody()      ? "on" : "off";
            case "frozen"  -> {
                long frozen = manager.getActivePlayers().stream()
                        .filter(fp -> fp.isFrozen()).count();
                yield String.valueOf(frozen);
            }
            case "version" -> plugin.getPluginMeta().getVersion();
            default        -> null; // unknown placeholder — let PAPI handle it
        };
    }
}

