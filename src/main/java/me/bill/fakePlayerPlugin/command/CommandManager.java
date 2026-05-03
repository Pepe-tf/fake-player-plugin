package me.bill.fakePlayerPlugin.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import me.bill.fakePlayerPlugin.FakePlayerPlugin;
import me.bill.fakePlayerPlugin.api.FppAddonCommand;
import me.bill.fakePlayerPlugin.api.FppCommandExtension;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

public final class CommandManager implements CommandExecutor, TabCompleter {

  private final FakePlayerPlugin plugin;
  private final Map<String, FppCommand> commands = new LinkedHashMap<>();
  private final Map<String, FppAddonCommand> addonCommands = new LinkedHashMap<>();
  private final List<FppCommandExtension> commandExtensions = new ArrayList<>();

  public CommandManager(FakePlayerPlugin plugin) {
    this.plugin = plugin;
  }

  public void register(FppCommand command) {
    commands.put(command.getName().toLowerCase(Locale.ROOT), command);
    for (String alias : command.getAliases()) {
      commands.put(alias.toLowerCase(Locale.ROOT), command);
    }
  }

  public void bind() {
    PluginCommand root = plugin.getCommand("fpp");
    if (root != null) {
      root.setExecutor(this);
      root.setTabCompleter(this);
    }
  }

  public void registerAddonCommand(FppAddonCommand command) {
    addonCommands.put(command.getName().toLowerCase(Locale.ROOT), command);
    for (String alias : command.getAliases()) {
      addonCommands.put(alias.toLowerCase(Locale.ROOT), command);
    }
  }

  public void unregisterAddonCommand(FppAddonCommand command) {
    addonCommands.entrySet().removeIf(entry -> entry.getValue() == command);
  }

  public void registerCommandExtension(FppCommandExtension extension) {
    commandExtensions.add(extension);
  }

  public void unregisterCommandExtension(FppCommandExtension extension) {
    commandExtensions.remove(extension);
  }

  public void clearAddonRegistrations() {
    addonCommands.clear();
    commandExtensions.clear();
  }

  public List<FppCommand> getCommands() {
    return commands.values().stream().distinct().toList();
  }

  public List<FppAddonCommand> getAddonCommands() {
    return addonCommands.values().stream().distinct().toList();
  }

  @Override
  public boolean onCommand(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String label,
      @NotNull String[] args) {
    if (args.length == 0) {
      sender.sendMessage("FakePlayerPlugin v" + plugin.getPluginMeta().getVersion());
      sender.sendMessage("Active bots: " + plugin.getFakePlayerManager().getActivePlayers().size());
      sender.sendMessage("Use /fpp help for commands.");
      return true;
    }

    String sub = args[0].toLowerCase(Locale.ROOT);
    String[] childArgs = Arrays.copyOfRange(args, 1, args.length);

    FppCommand builtIn = commands.get(sub);
    if (builtIn != null) {
      if (!has(sender, builtIn.getPermission())) {
        sender.sendMessage("You do not have permission to use this command.");
        return true;
      }
      for (FppCommandExtension extension : commandExtensions) {
        if (matchesExtension(extension, sub) && extension.execute(sender, childArgs)) {
          return true;
        }
      }
      boolean handled = builtIn.execute(sender, childArgs);
      return handled;
    }

    FppAddonCommand addon = addonCommands.get(sub);
    if (addon != null) {
      if (!addon.getPermission().isBlank() && !has(sender, addon.getPermission())) {
        sender.sendMessage("You do not have permission to use this command.");
        return true;
      }
      addon.execute(sender, childArgs);
      return true;
    }

    sender.sendMessage("Unknown FPP command: " + sub);
    return true;
  }

  @Override
  public List<String> onTabComplete(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String alias,
      @NotNull String[] args) {
    if (args.length == 1) {
      String prefix = args[0].toLowerCase(Locale.ROOT);
      List<String> out = new ArrayList<>();
      commands.keySet().stream().filter(s -> s.startsWith(prefix)).forEach(out::add);
      addonCommands.keySet().stream().filter(s -> s.startsWith(prefix)).forEach(out::add);
      return out;
    }
    FppCommand builtIn = commands.get(args[0].toLowerCase(Locale.ROOT));
    if (builtIn != null) {
      String[] childArgs = Arrays.copyOfRange(args, 1, args.length);
      for (FppCommandExtension extension : commandExtensions) {
        if (matchesExtension(extension, args[0])) {
          List<String> completions = extension.tabComplete(sender, childArgs);
          if (!completions.isEmpty()) return completions;
        }
      }
      return builtIn.tabComplete(sender, childArgs);
    }
    return List.of();
  }

  private static boolean has(CommandSender sender, String permission) {
    return permission == null || permission.isBlank() || sender.hasPermission(permission);
  }

  private static boolean matchesExtension(FppCommandExtension extension, String commandName) {
    if (extension.getCommandName().equalsIgnoreCase(commandName)) return true;
    for (String alias : extension.getAliases()) {
      if (alias.equalsIgnoreCase(commandName)) return true;
    }
    return false;
  }
}
