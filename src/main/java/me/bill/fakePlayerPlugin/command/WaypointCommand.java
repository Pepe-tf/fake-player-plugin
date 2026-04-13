package me.bill.fakePlayerPlugin.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import me.bill.fakePlayerPlugin.lang.Lang;
import me.bill.fakePlayerPlugin.permission.Perm;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class WaypointCommand implements FppCommand {

  private final WaypointStore store;

  public WaypointCommand(WaypointStore store) {
    this.store = store;
  }

  @Override
  public String getName() {
    return "waypoint";
  }

  @Override
  public List<String> getAliases() {
    return List.of("wp");
  }

  @Override
  public String getUsage() {
    return "create <route> | add <route> | remove <route> <index> | delete <route> | clear <route>"
               + " | list [route]";
  }

  @Override
  public String getDescription() {
    return "Manage named waypoint patrol routes for bots.";
  }

  @Override
  public String getPermission() {
    return Perm.WAYPOINT;
  }

  @Override
  public boolean canUse(CommandSender sender) {
    return Perm.has(sender, Perm.WAYPOINT);
  }

  @Override
  public boolean execute(CommandSender sender, String[] args) {
    if (args.length == 0) {
      sender.sendMessage(Lang.get("wp-usage"));
      return true;
    }

    String sub = args[0].toLowerCase();

    switch (sub) {
      case "create", "new" -> {
        if (args.length < 2) {
          sender.sendMessage(Lang.get("wp-create-usage"));
          return true;
        }
        String name = args[1];
        if (!name.matches("[a-zA-Z0-9_\\-]+")) {
          sender.sendMessage(Lang.get("wp-invalid-name", "name", name));
          return true;
        }
        if (!store.createRoute(name)) {
          sender.sendMessage(Lang.get("wp-already-exists", "name", name));
          return true;
        }
        sender.sendMessage(Lang.get("wp-created", "name", name));
      }

      case "add" -> {
        if (!(sender instanceof Player player)) {
          sender.sendMessage(Lang.get("player-only"));
          return true;
        }
        if (args.length < 2) {
          sender.sendMessage(Lang.get("wp-add-usage"));
          return true;
        }
        String name = args[1];
        if (!name.matches("[a-zA-Z0-9_\\-]+")) {
          sender.sendMessage(Lang.get("wp-invalid-name", "name", name));
          return true;
        }
        int count = store.addPos(name, player.getLocation());
        sender.sendMessage(
            Lang.get(
                "wp-pos-added",
                "name",
                name,
                "index",
                String.valueOf(count),
                "x",
                String.valueOf(player.getLocation().getBlockX()),
                "y",
                String.valueOf(player.getLocation().getBlockY()),
                "z",
                String.valueOf(player.getLocation().getBlockZ()),
                "world",
                player.getWorld().getName()));
      }

      case "remove", "removepos" -> {
        if (args.length < 3) {
          sender.sendMessage(Lang.get("wp-remove-usage"));
          return true;
        }
        String name = args[1];
        if (!store.hasRoute(name)) {
          sender.sendMessage(Lang.get("wp-not-found", "name", name));
          return true;
        }
        int index;
        try {
          index = Integer.parseInt(args[2]) - 1;
        } catch (NumberFormatException e) {
          sender.sendMessage(Lang.get("wp-invalid-index", "input", args[2]));
          return true;
        }
        if (!store.removePos(name, index)) {
          sender.sendMessage(
              Lang.get(
                  "wp-index-out-of-range",
                  "name",
                  name,
                  "index",
                  args[2],
                  "count",
                  String.valueOf(store.getPositionCount(name))));
          return true;
        }
        sender.sendMessage(Lang.get("wp-pos-removed", "name", name, "index", args[2]));
      }

      case "delete", "del" -> {
        if (args.length < 2) {
          sender.sendMessage(Lang.get("wp-delete-usage"));
          return true;
        }
        String name = args[1];
        if (!store.delete(name)) {
          sender.sendMessage(Lang.get("wp-not-found", "name", name));
          return true;
        }
        sender.sendMessage(Lang.get("wp-deleted", "name", name));
      }

      case "clear" -> {
        if (args.length < 2) {
          sender.sendMessage(Lang.get("wp-clear-usage"));
          return true;
        }
        String name = args[1];
        if (!store.clear(name)) {
          sender.sendMessage(Lang.get("wp-not-found", "name", name));
          return true;
        }
        sender.sendMessage(Lang.get("wp-cleared", "name", name));
      }

      case "list", "info" -> {
        if (args.length >= 2) {

          String name = args[1];
          List<Location> positions = store.getRoute(name);
          if (positions == null || positions.isEmpty()) {
            sender.sendMessage(Lang.get("wp-not-found", "name", name));
            return true;
          }
          sender.sendMessage(
              Lang.get("wp-route-header", "name", name, "count", String.valueOf(positions.size())));
          for (int i = 0; i < positions.size(); i++) {
            Location loc = positions.get(i);
            sender.sendMessage(
                Lang.get(
                    "wp-route-entry",
                    "num",
                    String.valueOf(i + 1),
                    "world",
                    loc.getWorld() != null ? loc.getWorld().getName() : "?",
                    "x",
                    String.valueOf(loc.getBlockX()),
                    "y",
                    String.valueOf(loc.getBlockY()),
                    "z",
                    String.valueOf(loc.getBlockZ())));
          }
        } else {

          Set<String> names = store.getNames();
          if (names.isEmpty()) {
            sender.sendMessage(Lang.get("wp-list-empty"));
            return true;
          }
          sender.sendMessage(Lang.get("wp-list-header", "count", String.valueOf(names.size())));
          int i = 1;
          for (String name : names) {
            sender.sendMessage(
                Lang.get(
                    "wp-list-entry",
                    "num",
                    String.valueOf(i++),
                    "name",
                    name,
                    "count",
                    String.valueOf(store.getPositionCount(name))));
          }
        }
      }

      default -> sender.sendMessage(Lang.get("wp-usage"));
    }
    return true;
  }

  @Override
  public List<String> tabComplete(CommandSender sender, String[] args) {
    List<String> out = new ArrayList<>();

    if (args.length == 1) {
      String in = args[0].toLowerCase();
      for (String s : List.of("create", "add", "remove", "delete", "clear", "list")) {
        if (s.startsWith(in)) out.add(s);
      }
    } else if (args.length == 2) {
      String sub2 = args[0].toLowerCase();
      String in = args[1].toLowerCase();

      if (!sub2.equals("create") && !sub2.equals("new")) {
        for (String name : store.getNames()) {
          if (name.toLowerCase().startsWith(in)) out.add(name);
        }
      }
    } else if (args.length == 3
        && (args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("removepos"))) {

      int count = store.getPositionCount(args[1]);
      for (int i = 1; i <= count; i++) {
        out.add(String.valueOf(i));
      }
    }

    return out;
  }
}
