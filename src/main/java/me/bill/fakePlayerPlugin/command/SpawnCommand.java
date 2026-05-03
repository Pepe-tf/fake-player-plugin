package me.bill.fakePlayerPlugin.command;

import java.util.List;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayer;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayerManager;
import me.bill.fakePlayerPlugin.permission.Perm;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class SpawnCommand implements FppCommand {

  private final FakePlayerManager manager;

  public SpawnCommand(FakePlayerManager manager) {
    this.manager = manager;
  }

  @Override
  public String getName() {
    return "spawn";
  }

  @Override
  public String getDescription() {
    return "Spawns fake players.";
  }

  @Override
  public String getUsage() {
    return "[name|amount] [--name <name>] [world x y z]";
  }

  @Override
  public String getPermission() {
    return Perm.SPAWN;
  }

  @Override
  public boolean execute(CommandSender sender, String[] args) {
    int count = 1;
    String name = null;
    int index = 0;

    if (index < args.length && isInteger(args[index])) {
      count = Math.max(1, Integer.parseInt(args[index++]));
    }

    for (int i = index; i < args.length; i++) {
      if ("--name".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
        name = args[++i];
      }
    }

    Location location = defaultLocation(sender);
    if (args.length - index == 1 && name == null && !args[index].startsWith("--")) {
      name = args[index];
    } else if (args.length - index >= 4 && !"--name".equalsIgnoreCase(args[index])) {
      World world = Bukkit.getWorld(args[index]);
      if (world == null) {
        sender.sendMessage("World not found: " + args[index]);
        return true;
      }
      try {
        location =
            new Location(
                world,
                Double.parseDouble(args[index + 1]),
                Double.parseDouble(args[index + 2]),
                Double.parseDouble(args[index + 3]));
      } catch (NumberFormatException e) {
        sender.sendMessage("Invalid coordinates.");
        return true;
      }
    }

    List<FakePlayer> spawned =
        manager.spawnBots(location, count, sender instanceof Player p ? p : null, name);
    if (spawned.isEmpty()) {
      sender.sendMessage("No fake players were spawned.");
      return true;
    }
    for (FakePlayer fp : spawned) {
      sender.sendMessage("[FPP] " + fp.getDisplayName() + " joined the game.");
    }
    return true;
  }

  @Override
  public List<String> tabComplete(CommandSender sender, String[] args) {
    if (args.length == 1) {
      return Bukkit.getWorlds().stream().map(World::getName).toList();
    }
    return List.of();
  }

  private static Location defaultLocation(CommandSender sender) {
    if (sender instanceof Player player) return player.getLocation();
    return Bukkit.getWorlds().isEmpty()
        ? null
        : Bukkit.getWorlds().getFirst().getSpawnLocation();
  }

  private static boolean isInteger(String value) {
    try {
      Integer.parseInt(value);
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }
}
