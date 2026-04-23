package me.bill.fakePlayerPlugin.api;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface FppApi {
  @NotNull Collection<FppBot> getBots();
  @NotNull Optional<FppBot> getBot(@NotNull String name);
  @NotNull Optional<FppBot> getBot(@NotNull UUID uuid);
  boolean isBot(@NotNull Player player);
  @NotNull Optional<FppBot> asBot(@NotNull Player player);
  int getBotCount();
  @NotNull Optional<FppBot> spawnBot(@NotNull Location location, @Nullable Player spawner, @Nullable String name);
  boolean despawnBot(@NotNull String name);
  boolean despawnBot(@NotNull FppBot bot);
  void registerCommand(@NotNull FppAddonCommand command);
  void registerTickHandler(@NotNull FppBotTickHandler handler);
  void unregisterTickHandler(@NotNull FppBotTickHandler handler);
  void navigateTo(@NotNull FppBot bot, @NotNull Location destination, @Nullable Runnable onArrive);
  void cancelNavigation(@NotNull FppBot bot);
  boolean isNavigating(@NotNull FppBot bot);
  @NotNull String getVersion();
  @Nullable Player getOnlinePlayer(@NotNull String name);
  int getOnlineCount();
}
