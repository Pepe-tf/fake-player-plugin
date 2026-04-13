package me.bill.fakePlayerPlugin.util;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import me.bill.fakePlayerPlugin.FakePlayerPlugin;
import me.bill.fakePlayerPlugin.config.Config;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayer;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayerManager;
import me.bill.fakePlayerPlugin.fakeplayer.RemoteBotEntry;
import me.bill.fakePlayerPlugin.permission.Perm;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class FppPlaceholderExpansion extends PlaceholderExpansion {

  private final FakePlayerPlugin plugin;
  private final FakePlayerManager manager;

  public FppPlaceholderExpansion(FakePlayerPlugin plugin, FakePlayerManager manager) {
    this.plugin = plugin;
    this.manager = manager;
  }

  @Override
  public @NotNull String getIdentifier() {
    return "fpp";
  }

  @Override
  public @NotNull String getAuthor() {
    return String.join(", ", plugin.getPluginMeta().getAuthors());
  }

  @Override
  public @NotNull String getVersion() {
    return plugin.getPluginMeta().getVersion();
  }

  @Override
  public boolean persist() {
    return true;
  }

  @Override
  public String onRequest(OfflinePlayer player, @NotNull String params) {
    int localBots = manager.getCount();

    Collection<RemoteBotEntry> remoteEntries =
        Config.isNetworkMode() ? plugin.getRemoteBotCache().getAll() : List.of();
    int remoteBots = remoteEntries.size();
    int bots = localBots + remoteBots;

    int real = Math.max(0, Bukkit.getOnlinePlayers().size() - localBots);

    return switch (params.toLowerCase()) {
      case "count" -> String.valueOf(bots);
      case "local_count" -> String.valueOf(localBots);
      case "network_count" -> String.valueOf(remoteBots);
      case "max" -> Config.maxBots() == 0 ? "∞" : String.valueOf(Config.maxBots());
      case "real" -> String.valueOf(real);
      case "total" -> String.valueOf(real + bots);
      case "online" -> String.valueOf(real + bots);

      case "frozen" ->
          String.valueOf(manager.getActivePlayers().stream().filter(FakePlayer::isFrozen).count());
      case "names" -> {
        Stream<String> localNames =
            manager.getActivePlayers().stream().map(FakePlayer::getDisplayName);
        if (Config.isNetworkMode()) {
          Stream<String> remoteNames = remoteEntries.stream().map(RemoteBotEntry::displayName);
          yield Stream.concat(localNames, remoteNames).collect(Collectors.joining(", "));
        }
        yield localNames.collect(Collectors.joining(", "));
      }
      case "network_names" ->
          remoteEntries.stream().map(RemoteBotEntry::displayName).collect(Collectors.joining(", "));
      case "chat" -> Config.fakeChatEnabled() ? "on" : "off";

      case "skin" -> Config.skinMode();
      case "body" -> Config.spawnBody() ? "on" : "off";
      case "pushable" -> Config.bodyPushable() ? "on" : "off";
      case "damageable" -> Config.bodyDamageable() ? "on" : "off";
      case "tab" -> Config.tabListEnabled() ? "on" : "off";
      case "max_health" -> String.valueOf(Config.maxHealth());
      case "version" -> plugin.getPluginMeta().getVersion();

      case "network" -> Config.isNetworkMode() ? "on" : "off";
      case "server_id" -> Config.serverId();
      case "persistence" -> Config.persistOnRestart() ? "on" : "off";
      case "spawn_cooldown" -> String.valueOf(Config.spawnCooldown());

      case "user_count" -> {
        if (player == null) yield "0";
        yield String.valueOf(manager.getBotsOwnedBy(player.getUniqueId()).size());
      }
      case "user_max" -> {
        if (player == null) yield String.valueOf(Config.userBotLimit());
        Player online = player.getPlayer();
        if (online == null) yield String.valueOf(Config.userBotLimit());
        int personal = Perm.resolveUserBotLimit(online);
        yield personal < 0 ? String.valueOf(Config.userBotLimit()) : String.valueOf(personal);
      }
      case "user_names" -> {
        if (player == null) yield "";
        List<FakePlayer> owned = manager.getBotsOwnedBy(player.getUniqueId());
        yield owned.stream().map(FakePlayer::getDisplayName).collect(Collectors.joining(", "));
      }

      default -> {
        if (params.startsWith("count_")) {
          String w = params.substring(6);
          yield String.valueOf(countBotsInWorld(w));
        }
        if (params.startsWith("real_")) {
          String w = params.substring(5);
          yield String.valueOf(countRealInWorld(w));
        }
        if (params.startsWith("total_")) {
          String w = params.substring(6);
          yield String.valueOf(countBotsInWorld(w) + countRealInWorld(w));
        }
        yield null;
      }
    };
  }

  private int countBotsInWorld(String worldName) {
    return (int)
        manager.getActivePlayers().stream()
            .filter(fp -> worldName.equalsIgnoreCase(getBotWorldName(fp)))
            .count();
  }

  private static String getBotWorldName(FakePlayer fp) {
    Entity body = fp.getPhysicsEntity();
    if (body != null && body.isValid()) {
      World w = body.getLocation().getWorld();
      if (w != null) return w.getName();
    }
    Location sl = fp.getSpawnLocation();
    if (sl != null && sl.getWorld() != null) return sl.getWorld().getName();
    return "";
  }

  private int countRealInWorld(String worldName) {
    World world = Bukkit.getWorld(worldName);
    if (world == null) {
      world =
          Bukkit.getWorlds().stream()
              .filter(w -> w.getName().equalsIgnoreCase(worldName))
              .findFirst()
              .orElse(null);
    }
    if (world == null) return 0;

    int botsInWorld = countBotsInWorld(worldName);
    return Math.max(0, world.getPlayers().size() - botsInWorld);
  }
}
