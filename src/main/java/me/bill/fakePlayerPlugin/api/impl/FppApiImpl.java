package me.bill.fakePlayerPlugin.api.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import me.bill.fakePlayerPlugin.FakePlayerPlugin;
import me.bill.fakePlayerPlugin.api.FppAddon;
import me.bill.fakePlayerPlugin.api.FppAddonCommand;
import me.bill.fakePlayerPlugin.api.FppApi;
import me.bill.fakePlayerPlugin.api.FppBot;
import me.bill.fakePlayerPlugin.api.FppBotTickHandler;
import me.bill.fakePlayerPlugin.api.FppCommandExtension;
import me.bill.fakePlayerPlugin.api.FppNavigationGoal;
import me.bill.fakePlayerPlugin.api.FppSettingsTab;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayer;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayerManager;
import me.bill.fakePlayerPlugin.util.FppLogger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class FppApiImpl implements FppApi {

  private final FakePlayerPlugin plugin;
  private final FakePlayerManager manager;
  private final CopyOnWriteArrayList<FppBotTickHandler> tickHandlers = new CopyOnWriteArrayList<>();
  private final CopyOnWriteArrayList<FppSettingsTab> settingsTabs = new CopyOnWriteArrayList<>();
  private final CopyOnWriteArrayList<FppSettingsTab> botSettingsTabs = new CopyOnWriteArrayList<>();
  private final CopyOnWriteArrayList<FppAddon> addons = new CopyOnWriteArrayList<>();
  private final Map<Class<?>, Object> services = new ConcurrentHashMap<>();

  public FppApiImpl(FakePlayerPlugin plugin, FakePlayerManager manager) {
    this.plugin = plugin;
    this.manager = manager;
  }

  @Override public @NotNull Collection<FppBot> getBots() {
    List<FppBot> out = new ArrayList<>();
    for (FakePlayer fp : manager.getActivePlayers()) out.add(new FppBotImpl(fp));
    return out;
  }

  @Override public @NotNull Collection<FppBot> getBotsControllableBy(@NotNull Player player) {
    return getBots();
  }

  @Override public @NotNull Collection<FppBot> getBotsOwnedBy(@NotNull Player player) {
    return getBots().stream().filter(bot -> bot.isOwnedBy(player.getUniqueId())).toList();
  }

  @Override public @NotNull Optional<FppBot> getBot(@NotNull String name) {
    FakePlayer fp = manager.getByName(name);
    return fp == null ? Optional.empty() : Optional.of(new FppBotImpl(fp));
  }

  @Override public @NotNull Optional<FppBot> getBot(@NotNull UUID uuid) {
    FakePlayer fp = manager.getByUuid(uuid);
    return fp == null ? Optional.empty() : Optional.of(new FppBotImpl(fp));
  }

  @Override public boolean isBot(@NotNull Player player) { return manager.isBot(player); }
  @Override public @NotNull Optional<FppBot> asBot(@NotNull Player player) { return getBot(player.getUniqueId()); }
  @Override public boolean canControlBot(@NotNull Player player, @NotNull FppBot bot) { return true; }
  @Override public int getBotCount() { return manager.getActivePlayers().size(); }

  @Override public @NotNull Optional<FppBot> spawnBot(@NotNull Location location, @Nullable Player spawner, @Nullable String name) {
    List<FakePlayer> spawned = manager.spawnBots(location, 1, spawner, name);
    return spawned.isEmpty() ? Optional.empty() : Optional.of(new FppBotImpl(spawned.getFirst()));
  }

  @Override public boolean despawnBot(@NotNull String name) { return manager.delete(name); }
  @Override public boolean despawnBot(@NotNull FppBot bot) { return manager.delete(bot.getName()); }

  @Override public void registerCommand(@NotNull FppAddonCommand command) {
    if (plugin.getCommandManager() != null) plugin.getCommandManager().registerAddonCommand(command);
  }

  @Override public void unregisterCommand(@NotNull FppAddonCommand command) {
    if (plugin.getCommandManager() != null) plugin.getCommandManager().unregisterAddonCommand(command);
  }

  @Override public void registerCommandExtension(@NotNull FppCommandExtension extension) {
    if (plugin.getCommandManager() != null) plugin.getCommandManager().registerCommandExtension(extension);
  }

  @Override public void unregisterCommandExtension(@NotNull FppCommandExtension extension) {
    if (plugin.getCommandManager() != null) plugin.getCommandManager().unregisterCommandExtension(extension);
  }

  @Override public void registerTickHandler(@NotNull FppBotTickHandler handler) { tickHandlers.addIfAbsent(handler); }
  @Override public void unregisterTickHandler(@NotNull FppBotTickHandler handler) { tickHandlers.remove(handler); }
  public void fireTickHandlers(FakePlayer fp, Player entity) {
    FppBotImpl bot = new FppBotImpl(fp);
    for (FppBotTickHandler handler : tickHandlers) handler.onTick(bot, entity);
  }

  @Override public void registerSettingsTab(@NotNull FppSettingsTab tab) { settingsTabs.addIfAbsent(tab); }
  @Override public void unregisterSettingsTab(@NotNull FppSettingsTab tab) { settingsTabs.remove(tab); }
  @Override public void registerBotSettingsTab(@NotNull FppSettingsTab tab) { botSettingsTabs.addIfAbsent(tab); }
  @Override public void unregisterBotSettingsTab(@NotNull FppSettingsTab tab) { botSettingsTabs.remove(tab); }
  public @NotNull List<FppSettingsTab> getSettingsTabs() { return List.copyOf(settingsTabs); }
  public @NotNull List<FppSettingsTab> getBotSettingsTabs() { return List.copyOf(botSettingsTabs); }
  @Override public void registerAddon(@NotNull FppAddon addon) {
    if (!addons.addIfAbsent(addon)) return;
    try {
      addon.onEnable(this);
    } catch (Throwable t) {
      addons.remove(addon);
      FppLogger.warn("Addon failed to enable: " + t.getMessage());
    }
  }
  @Override public void unregisterAddon(@NotNull FppAddon addon) {
    if (!addons.remove(addon)) return;
    try {
      addon.onDisable();
    } catch (Throwable t) {
      FppLogger.warn("Addon failed to disable: " + t.getMessage());
    }
  }

  public void clearExtensionRegistrations() {
    for (FppAddon addon : List.copyOf(addons)) {
      unregisterAddon(addon);
    }
    tickHandlers.clear();
    settingsTabs.clear();
    botSettingsTabs.clear();
    services.clear();
    if (plugin.getCommandManager() != null) plugin.getCommandManager().clearAddonRegistrations();
  }
  @Override public void sayAsBot(@NotNull FppBot bot, @NotNull String message) { Bukkit.broadcastMessage("<" + bot.getName() + "> " + message); }
  @Override public void navigateTo(@NotNull FppBot bot, @NotNull Location destination, @Nullable Runnable onArrive) { if (onArrive != null) onArrive.run(); }
  @Override public void navigateTo(@NotNull FppBot bot, @NotNull Location destination, @Nullable Runnable onArrive, @Nullable Runnable onFail, @Nullable Runnable onCancel) { navigateTo(bot, destination, onArrive); }
  @Override public void navigateTo(@NotNull FppBot bot, @NotNull Location destination, @Nullable Runnable onArrive, @Nullable Runnable onFail, @Nullable Runnable onCancel, double arrivalDistance) { navigateTo(bot, destination, onArrive); }
  @Override public void cancelNavigation(@NotNull FppBot bot) {}
  @Override public boolean isNavigating(@NotNull FppBot bot) { return false; }
  @Override public void setNavigationGoal(@NotNull FppBot bot, @NotNull FppNavigationGoal goal) {}
  @Override public void clearNavigationGoal(@NotNull FppBot bot) {}
  @Override public boolean runAsBot(@NotNull FppBot bot, @NotNull String command) {
    Player entity = bot.getEntity();
    return entity != null && Bukkit.dispatchCommand(entity, command);
  }
  @Override public boolean isBotOnline(@NotNull UUID uuid) { return manager.getByUuid(uuid) != null; }
  @Override public @NotNull String getVersion() { return plugin.getPluginMeta().getVersion(); }
  @Override public @NotNull Plugin getPlugin() { return plugin; }
  @Override public @Nullable Player getOnlinePlayer(@NotNull String name) { return Bukkit.getPlayerExact(name); }
  @Override public int getOnlineCount() { return Bukkit.getOnlinePlayers().size(); }
  @Override public <T> void registerService(@NotNull Class<T> serviceClass, @NotNull T instance) { services.put(serviceClass, instance); }
  @Override public <T> @Nullable T getService(@NotNull Class<T> serviceClass) { return serviceClass.cast(services.get(serviceClass)); }
  @Override public boolean hasService(@NotNull Class<?> serviceClass) { return services.containsKey(serviceClass); }
  @Override public @Nullable File getExtensionDataFolder(@NotNull String extensionName) { return plugin.getExtensionLoader() == null ? null : plugin.getExtensionLoader().getExtensionDataFolder(extensionName); }
  @Override public void saveDefaultExtensionConfig(@NotNull String extensionName) { if (plugin.getExtensionLoader() != null) plugin.getExtensionLoader().saveDefaultExtensionConfig(extensionName); }
  @Override public @Nullable org.bukkit.configuration.file.YamlConfiguration getExtensionConfig(@NotNull String extensionName) { return plugin.getExtensionLoader() == null ? null : plugin.getExtensionLoader().getExtensionConfig(extensionName); }
}
