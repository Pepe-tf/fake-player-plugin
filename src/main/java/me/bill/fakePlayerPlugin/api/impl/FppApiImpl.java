package me.bill.fakePlayerPlugin.api.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import me.bill.fakePlayerPlugin.FakePlayerPlugin;
import me.bill.fakePlayerPlugin.api.FppAddonCommand;
import me.bill.fakePlayerPlugin.api.FppApi;
import me.bill.fakePlayerPlugin.api.FppBot;
import me.bill.fakePlayerPlugin.api.FppBotTickHandler;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayer;
import java.util.function.Supplier;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayerManager;
import me.bill.fakePlayerPlugin.fakeplayer.PathfindingService;
import me.bill.fakePlayerPlugin.fakeplayer.PathfindingService.NavigationRequest;
import me.bill.fakePlayerPlugin.fakeplayer.PathfindingService.Owner;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Internal implementation of {@link FppApi}.
 * Obtained via {@link FakePlayerPlugin#getFppApi()}.
 */
public final class FppApiImpl implements FppApi {

  private final FakePlayerPlugin plugin;
  private final FakePlayerManager manager;

  /** Registered addon tick handlers — thread-safe iterate, rare write. */
  private final CopyOnWriteArrayList<FppBotTickHandler> tickHandlers = new CopyOnWriteArrayList<>();

  /** Registered addon commands — iterated by CommandManager. */
  private final CopyOnWriteArrayList<FppAddonCommand> addonCommands = new CopyOnWriteArrayList<>();

  public FppApiImpl(@NotNull FakePlayerPlugin plugin, @NotNull FakePlayerManager manager) {
    this.plugin  = plugin;
    this.manager = manager;
  }

  // ── Bot queries ───────────────────────────────────────────────────────────

  @Override
  public @NotNull Collection<FppBot> getBots() {
    Collection<FakePlayer> raw = manager.getActivePlayers();
    List<FppBot> result = new ArrayList<>(raw.size());
    for (FakePlayer fp : raw) result.add(new FppBotImpl(fp));
    return result;
  }

  @Override
  public @NotNull Optional<FppBot> getBot(@NotNull String name) {
    FakePlayer fp = manager.getByName(name);
    return fp == null ? Optional.empty() : Optional.of(new FppBotImpl(fp));
  }

  @Override
  public @NotNull Optional<FppBot> getBot(@NotNull UUID uuid) {
    FakePlayer fp = manager.getByUuid(uuid);
    return fp == null ? Optional.empty() : Optional.of(new FppBotImpl(fp));
  }

  @Override
  public boolean isBot(@NotNull Player player) {
    return manager.getByUuid(player.getUniqueId()) != null;
  }

  @Override
  public @NotNull Optional<FppBot> asBot(@NotNull Player player) {
    return getBot(player.getUniqueId());
  }

  @Override
  public int getBotCount() {
    return manager.getActivePlayers().size();
  }

  // ── Spawn / despawn ───────────────────────────────────────────────────────

  @Override
  public @NotNull Optional<FppBot> spawnBot(
      @NotNull Location location,
      @Nullable Player spawner,
      @Nullable String name) {

    int result = manager.spawn(location, 1, spawner, name, /* bypassMax */ false);
    if (result <= 0) return Optional.empty();

    // If a custom name was given we can look it up immediately.
    if (name != null) {
      FakePlayer fp = manager.getByName(name);
      return fp == null ? Optional.empty() : Optional.of(new FppBotImpl(fp));
    }

    // Random name: find the most-recently added bot at this location owned by this spawner.
    String spawnerName = spawner != null ? spawner.getName() : "CONSOLE";
    long now = System.currentTimeMillis();
    FakePlayer newest = null;
    for (FakePlayer fp : manager.getActivePlayers()) {
      if (fp.getSpawnedBy().equals(spawnerName)
          && (now - fp.getSpawnTime().toEpochMilli()) < 2000) {
        if (newest == null
            || fp.getSpawnTime().isAfter(newest.getSpawnTime())) {
          newest = fp;
        }
      }
    }
    return newest == null ? Optional.empty() : Optional.of(new FppBotImpl(newest));
  }

  @Override
  public boolean despawnBot(@NotNull String name) {
    return manager.delete(name);
  }

  @Override
  public boolean despawnBot(@NotNull FppBot bot) {
    return manager.delete(bot.getName());
  }

  // ── Command registration ──────────────────────────────────────────────────

  @Override
  public void registerCommand(@NotNull FppAddonCommand command) {
    String nameLower = command.getName().toLowerCase();
    for (FppAddonCommand existing : addonCommands) {
      if (existing.getName().equalsIgnoreCase(nameLower)) return; // duplicate — ignore
    }
    addonCommands.add(command);
    // Tell CommandManager to include this command (if already initialised).
    var cmdManager = plugin.getCommandManager();
    if (cmdManager != null) cmdManager.registerAddonCommand(command);
  }

  /** Returns all registered addon commands (used by CommandManager). */
  public @NotNull List<FppAddonCommand> getAddonCommands() {
    return addonCommands;
  }

  // ── Tick hooks ────────────────────────────────────────────────────────────

  @Override
  public void registerTickHandler(@NotNull FppBotTickHandler handler) {
    tickHandlers.addIfAbsent(handler);
  }

  @Override
  public void unregisterTickHandler(@NotNull FppBotTickHandler handler) {
    tickHandlers.remove(handler);
  }

  /**
   * Called by {@link FakePlayerManager}'s tick loop for each active, non-frozen, bodied bot.
   * Runs on the main thread.
   */
  public void fireTickHandlers(@NotNull FakePlayer fp, @NotNull Player entity) {
    if (tickHandlers.isEmpty()) return;
    FppBotImpl view = new FppBotImpl(fp);
    for (FppBotTickHandler h : tickHandlers) {
      try {
        h.onTick(view, entity);
      } catch (Throwable t) {
        me.bill.fakePlayerPlugin.util.FppLogger.warn(
            "[FppApi] Tick handler threw an exception for bot '"
                + fp.getName()
                + "': "
                + t.getMessage());
      }
    }
  }

  // ── Navigation ────────────────────────────────────────────────────────────

  @Override
  public void navigateTo(
      @NotNull FppBot bot,
      @NotNull Location destination,
      @Nullable Runnable onArrive) {

    FakePlayer fp = manager.getByUuid(bot.getUuid());
    PathfindingService svc = plugin.getPathfindingService();
    if (fp == null || svc == null) return;

    final Location dest = destination.clone();
    svc.navigate(
        fp,
        new NavigationRequest(
            Owner.SYSTEM,
            () -> dest,
            /* arrivalDistance      */ 1.5,
            /* recalcDistance       */ 3.5,
            /* maxNullRecalcs       */ 5,
            /* onArrive             */ () -> { if (onArrive != null) onArrive.run(); },
            /* onCancel             */ () -> {},
            /* onPathFailure        */ () -> {}));
  }

  @Override
  public void cancelNavigation(@NotNull FppBot bot) {
    PathfindingService svc = plugin.getPathfindingService();
    if (svc != null) svc.cancel(bot.getUuid());
  }

  @Override
  public boolean isNavigating(@NotNull FppBot bot) {
    PathfindingService svc = plugin.getPathfindingService();
    return svc != null && svc.isNavigating(bot.getUuid());
  }

  // ── Plugin info ───────────────────────────────────────────────────────────

  @Override
  public @NotNull String getVersion() {
    return plugin.getDescription().getVersion();
  }

  @Override
  public @Nullable Player getOnlinePlayer(@NotNull String name) {
    return Bukkit.getPlayer(name);
  }

  @Override
  public int getOnlineCount() {
    return Bukkit.getOnlinePlayers().size();
  }
}
