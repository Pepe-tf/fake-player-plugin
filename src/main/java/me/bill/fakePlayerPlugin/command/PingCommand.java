package me.bill.fakePlayerPlugin.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import me.bill.fakePlayerPlugin.FakePlayerPlugin;
import me.bill.fakePlayerPlugin.config.Config;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayer;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayerManager;
import me.bill.fakePlayerPlugin.lang.Lang;
import me.bill.fakePlayerPlugin.permission.Perm;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PingCommand implements FppCommand {

  private static final Random RANDOM = new Random();

  private final FakePlayerPlugin plugin;
  private final FakePlayerManager manager;

  public PingCommand(FakePlayerPlugin plugin, FakePlayerManager manager) {
    this.plugin = plugin;
    this.manager = manager;
  }

  @Override
  public String getName() {
    return "ping";
  }

  @Override
  public String getUsage() {
    return "[<bot|--count <n>] [--ping <ms>|--random|--reset]";
  }

  @Override
  public String getDescription() {
    return "View or set a bot's tab-list ping.";
  }

  @Override
  public String getPermission() {
    return Perm.PING;
  }

  @Override
  public boolean canUse(CommandSender sender) {
    return Perm.has(sender, Perm.PING);
  }

  @Override
  public boolean execute(CommandSender sender, String[] args) {
    Integer pingValue = null;
    boolean random = false;
    boolean reset = false;
    Integer count = null;
    String botName = null;
    boolean hasConflictOption = false;
    boolean hasDuplicate = false;
    String duplicateOpt = null;

    for (int i = 0; i < args.length; i++) {
      String arg = args[i].toLowerCase();
      switch (arg) {
        case "--ping":
          if (random || reset) {
            hasConflictOption = true;
          } else {
            i++;
            if (i >= args.length) {
              sender.sendMessage(Lang.get("ping-missing-value", "option", "--ping"));
              return true;
            }
            try {
              pingValue = Integer.parseInt(args[i]);
              if (pingValue < 0 || pingValue > 9999) {
                sender.sendMessage(Lang.get("ping-out-of-range"));
                return true;
              }
            } catch (NumberFormatException e) {
              sender.sendMessage(Lang.get("ping-invalid-number", "value", args[i]));
              return true;
            }
          }
          break;
        case "--random":
          if (pingValue != null || reset) {
            hasConflictOption = true;
          } else {
            random = true;
          }
          break;
        case "--reset":
          if (pingValue != null || random) {
            hasConflictOption = true;
          } else {
            reset = true;
          }
          break;
        case "--count":
          if (!Perm.has(sender, Perm.PING_BULK)) {
            sender.sendMessage(Lang.get("no-permission"));
            return true;
          }
          i++;
          if (i >= args.length) {
            sender.sendMessage(Lang.get("ping-missing-value", "option", "--count"));
            return true;
          }
          try {
            count = Integer.parseInt(args[i]);
            if (count <= 0) count = 1;
          } catch (NumberFormatException e) {
            sender.sendMessage(Lang.get("ping-invalid-number", "value", args[i]));
            return true;
          }
          break;
        default:
          if (botName == null) {
            botName = args[i];
          }
          break;
      }
    }

    if (hasConflictOption) {
      sender.sendMessage(Lang.get("ping-conflict-options"));
      return true;
    }

    boolean hasOption = pingValue != null || random || reset;

    if (count != null) {
      return handleCount(sender, count, pingValue, random, reset);
    }

    if (botName != null && !botName.startsWith("--")) {
      FakePlayer fp = manager.getByName(botName);
      if (fp == null) {
        sender.sendMessage(Lang.get("ping-bot-not-found", "name", botName));
        return true;
      }
      return handleSingle(sender, fp, pingValue, random, reset);
    }

    Collection<FakePlayer> bots = manager.getActivePlayers();
    if (bots.isEmpty()) {
      sender.sendMessage(Lang.get("ping-no-bots"));
      return true;
    }

    if (!hasOption) {
      for (FakePlayer fp : bots) {
        sendCurrentPing(sender, fp);
      }
      return true;
    }

    for (FakePlayer fp : bots) {
      applyPingOption(sender, fp, pingValue, random, reset);
    }
    return true;
  }

  private boolean handleSingle(CommandSender sender, FakePlayer fp, Integer pingValue, boolean random, boolean reset) {
    if (pingValue != null) {
      if (!Perm.has(sender, Perm.PING_SET)) {
        sender.sendMessage(Lang.get("no-permission"));
        return true;
      }
      manager.applyPing(fp, pingValue);
      sender.sendMessage(Lang.get("ping-set", "name", fp.getDisplayName(), "ping", String.valueOf(pingValue)));
    } else if (random) {
      if (!Perm.has(sender, Perm.PING_RANDOM)) {
        sender.sendMessage(Lang.get("no-permission"));
        return true;
      }
      int min = Config.pingMin();
      int max = Config.pingMax();
      int val = min + RANDOM.nextInt(Math.max(1, max - min + 1));
      manager.applyPing(fp, val);
      sender.sendMessage(Lang.get("ping-set", "name", fp.getDisplayName(), "ping", String.valueOf(val)));
    } else if (reset) {
      manager.applyPing(fp, -1);
      sender.sendMessage(Lang.get("ping-reset", "name", fp.getDisplayName()));
    } else {
      sendCurrentPing(sender, fp);
    }
    manager.persistBotSettings(fp);
    return true;
  }

  private boolean handleCount(CommandSender sender, int count, Integer pingValue, boolean random, boolean reset) {
    List<FakePlayer> bots = new ArrayList<>(manager.getActivePlayers());
    if (bots.isEmpty()) {
      sender.sendMessage(Lang.get("ping-no-bots"));
      return true;
    }

    java.util.Collections.shuffle(bots);
    int target = Math.min(count, bots.size());

    if (pingValue != null) {
      if (!Perm.has(sender, Perm.PING_SET)) {
        sender.sendMessage(Lang.get("no-permission"));
        return true;
      }
      for (int i = 0; i < target; i++) {
        FakePlayer fp = bots.get(i);
        manager.applyPing(fp, pingValue);
        manager.persistBotSettings(fp);
      }
      sender.sendMessage(Lang.get("ping-set-multiple", "count", String.valueOf(target), "ping", String.valueOf(pingValue)));
    } else if (random) {
      if (!Perm.has(sender, Perm.PING_RANDOM)) {
        sender.sendMessage(Lang.get("no-permission"));
        return true;
      }
      int min = Config.pingMin();
      int max = Config.pingMax();
      for (int i = 0; i < target; i++) {
        FakePlayer fp = bots.get(i);
        int val = min + RANDOM.nextInt(Math.max(1, max - min + 1));
        manager.applyPing(fp, val);
        manager.persistBotSettings(fp);
      }
      sender.sendMessage(Lang.get("ping-random-multiple", "count", String.valueOf(target)));
    } else if (reset) {
      for (int i = 0; i < target; i++) {
        FakePlayer fp = bots.get(i);
        manager.applyPing(fp, -1);
        manager.persistBotSettings(fp);
      }
      sender.sendMessage(Lang.get("ping-reset-multiple", "count", String.valueOf(target)));
    } else {
      for (int i = 0; i < target; i++) {
        sendCurrentPing(sender, bots.get(i));
      }
    }
    return true;
  }

  private void sendCurrentPing(CommandSender sender, FakePlayer fp) {
    int displayPing = fp.getEffectivePing();
    if (fp.hasCustomPing()) {
      sender.sendMessage(Lang.get("ping-show-spoofed", "name", fp.getDisplayName(), "ping", String.valueOf(displayPing)));
    } else {
      sender.sendMessage(Lang.get("ping-show-default", "name", fp.getDisplayName(), "ping", String.valueOf(displayPing)));
    }
  }

  private void applyPingOption(CommandSender sender, FakePlayer fp, Integer pingValue, boolean random, boolean reset) {
    if (pingValue != null) {
      manager.applyPing(fp, pingValue);
      sender.sendMessage(Lang.get("ping-set", "name", fp.getDisplayName(), "ping", String.valueOf(pingValue)));
    } else if (random) {
      int min = Config.pingMin();
      int max = Config.pingMax();
      int val = min + RANDOM.nextInt(Math.max(1, max - min + 1));
      manager.applyPing(fp, val);
      sender.sendMessage(Lang.get("ping-set", "name", fp.getDisplayName(), "ping", String.valueOf(val)));
    } else if (reset) {
      manager.applyPing(fp, -1);
      sender.sendMessage(Lang.get("ping-reset", "name", fp.getDisplayName()));
    }
    manager.persistBotSettings(fp);
  }

  @Override
  public List<String> tabComplete(CommandSender sender, String[] args) {
    List<String> results = new ArrayList<>();
    String last = args.length > 0 ? args[args.length - 1].toLowerCase() : "";

    if (args.length == 1) {
      List<String> names = manager.getActiveNames().stream()
          .filter(n -> n.toLowerCase().startsWith(last))
          .collect(Collectors.toList());
      results.addAll(names);
      if ("--ping".startsWith(last)) results.add("--ping");
      if ("--random".startsWith(last)) results.add("--random");
      if ("--reset".startsWith(last)) results.add("--reset");
      if ("--count".startsWith(last) && Perm.has(sender, Perm.PING_BULK)) results.add("--count");
    } else {
      for (int i = args.length - 1; i >= 0; i--) {
        if (args[i].equalsIgnoreCase("--ping")) {
          return List.of("<ms>");
        }
        if (args[i].equalsIgnoreCase("--count")) {
          return List.of("<n>");
        }
      }
      if ("--ping".startsWith(last)) results.add("--ping");
      if ("--random".startsWith(last)) results.add("--random");
      if ("--reset".startsWith(last)) results.add("--reset");
      if ("--count".startsWith(last) && Perm.has(sender, Perm.PING_BULK)) results.add("--count");
    }

    return results;
  }
}