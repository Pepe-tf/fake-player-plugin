package me.bill.fakePlayerPlugin.api;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The main entry point for FakePlayerPlugin addons.
 *
 * <p>Obtain the singleton instance from your plugin's {@code onEnable}:
 * <pre>{@code
 * Plugin fpp = Bukkit.getPluginManager().getPlugin("FakePlayerPlugin");
 * if (fpp instanceof FakePlayerPlugin fp) {
 *     FppApi api = fp.getFppApi();
 * }
 * }</pre>
 */
public interface FppApi {

  // ── Bot queries ───────────────────────────────────────────────────────────

  /**
   * Returns all currently active bots on this server.
   * The returned collection is a snapshot — it will not reflect bots that
   * spawn or despawn after this call.
   */
  @NotNull Collection<FppBot> getBots();

  /**
   * Looks up a bot by its internal Minecraft username (case-insensitive).
   */
  @NotNull Optional<FppBot> getBot(@NotNull String name);

  /**
   * Looks up a bot by its stable UUID.
   */
  @NotNull Optional<FppBot> getBot(@NotNull UUID uuid);

  /**
   * Returns true if the given {@link Player} entity is a FPP bot.
   */
  boolean isBot(@NotNull Player player);

  /**
   * If the given {@link Player} is a bot, returns its {@link FppBot} view;
   * otherwise returns empty.
   */
  @NotNull Optional<FppBot> asBot(@NotNull Player player);

  /** The current number of active bots on this server. */
  int getBotCount();

  // ── Spawn / despawn ───────────────────────────────────────────────────────

  /**
   * Spawns a bot at the given location.
   *
   * @param location   spawn location
   * @param spawner    the player credited as the owner, or {@code null} for CONSOLE
   * @param name       custom bot name, or {@code null} to use a random name from the pool
   * @return the newly created {@link FppBot}, or empty if spawn failed
   *         (e.g. global limit reached, name taken)
   */
  @NotNull Optional<FppBot> spawnBot(
      @NotNull Location location,
      @Nullable Player spawner,
      @Nullable String name);

  /**
   * Despawns the bot with the given name.
   *
   * @return true if the bot existed and was despawned
   */
  boolean despawnBot(@NotNull String name);

  /**
   * Despawns the given bot.
   *
   * @return true if the bot existed and was despawned
   */
  boolean despawnBot(@NotNull FppBot bot);

  // ── Command registration ──────────────────────────────────────────────────

  /**
   * Registers an addon-contributed sub-command into the {@code /fpp} command tree.
   * The command will appear in {@code /fpp help} and respond to {@code /fpp <name>}.
   *
   * <p>Call this during your plugin's {@code onEnable}.
   * Duplicate names (case-insensitive) are silently ignored.
   *
   * @param command the command to register
   */
  void registerCommand(@NotNull FppAddonCommand command);

  // ── Tick hooks ────────────────────────────────────────────────────────────

  /**
   * Registers a per-tick AI handler that runs for every active, non-frozen,
   * bodied bot on the main thread before physics fires.
   *
   * @param handler the tick handler to register
   */
  void registerTickHandler(@NotNull FppBotTickHandler handler);

  /**
   * Removes a previously registered tick handler.
   */
  void unregisterTickHandler(@NotNull FppBotTickHandler handler);

  // ── Navigation ────────────────────────────────────────────────────────────

  /**
   * Instructs a bot to navigate to the given destination.
   * Uses FPP's built-in A* pathfinder.
   *
   * @param bot         the bot to move
   * @param destination target location
   * @param onArrive    optional callback fired when the bot arrives (main thread)
   */
  void navigateTo(
      @NotNull FppBot bot,
      @NotNull Location destination,
      @Nullable Runnable onArrive);

  /**
   * Cancels any active navigation for the given bot.
   */
  void cancelNavigation(@NotNull FppBot bot);

  /** Returns true if the bot is currently navigating. */
  boolean isNavigating(@NotNull FppBot bot);

  // ── Plugin info ───────────────────────────────────────────────────────────

  /** The currently running FPP version string. */
  @NotNull String getVersion();
}
