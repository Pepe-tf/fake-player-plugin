package me.bill.fakePlayerPlugin.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayer;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayerManager;
import me.bill.fakePlayerPlugin.lang.Lang;
import me.bill.fakePlayerPlugin.permission.Perm;
import org.bukkit.command.CommandSender;

public class PingCommand implements FppCommand {

  private static final int DEFAULT_PING = 50;
  private static final int RANDOM_PING_MIN = 20;
  private static final int RANDOM_PING_MAX = 300;

  private final FakePlayerManager manager;

  public PingCommand(FakePlayerManager manager) {
    this.manager = manager;
  }

  @Override
  public String getName() {
    return "ping";
  }

  @Override
  public String getUsage() {
    return "[<bot>|--count <n>] [--ping <ms>|--random|--reset]";
  }

  @Override
  public String getDescription() {
    return "Set a simulated ping (latency) for one or more bots.";
  }

  @Override
  public String getPermission() {
    return Perm.PING;
  }

  @Override
  public boolean execute(CommandSender sender, String[] args) {
    if (args.length == 0) {
      sender.sendMessage(Lang.get("ping-usage"));
      return true;
    }

    String botTarget = null;
    int count = -1;
    int fixedPing = -1;
    boolean random = false;
    boolean reset = false;

    int i = 0;
    while (i < args.length) {
      String arg = args[i];
      if (arg.equalsIgnoreCase("--random")) {
        if (fixedPing >= 0) {
          sender.sendMessage(Lang.get("ping-conflict-options"));
          return true;
        }
        random = true;
        i++;
      } else if (arg.equalsIgnoreCase("--reset")) {
        if (random || fixedPing >= 0) {
          sender.sendMessage(Lang.get("ping-conflict-options"));
          return true;
        }
        reset = true;
        i++;
      } else if (arg.equalsIgnoreCase("--ping")) {
        if (random) {
          sender.sendMessage(Lang.get("ping-conflict-options"));
          return true;
        }
        if (fixedPing >= 0) {
          sender.sendMessage(Lang.get("ping-duplicate-option", "option", "--ping"));
          return true;
        }
        if (i + 1 >= args.length) {
          sender.sendMessage(Lang.get("ping-missing-value", "option", "--ping"));
          return true;
        }
        try {
          fixedPing = Integer.parseInt(args[++i]);
          if (fixedPing < 0 || fixedPing > 9999) {
            sender.sendMessage(Lang.get("ping-out-of-range"));
            return true;
          }
        } catch (NumberFormatException e) {
          sender.sendMessage(Lang.get("ping-invalid-number", "value", args[i]));
          return true;
        }
        i++;
      } else if (arg.equalsIgnoreCase("--count")) {
        if (i + 1 >= args.length) {
          sender.sendMessage(Lang.get("ping-missing-value", "option", "--count"));
          return true;
        }
        try {
          count = Integer.parseInt(args[++i]);
          if (count < 1) {
            sender.sendMessage(Lang.get("ping-count-invalid"));
            return true;
          }
        } catch (NumberFormatException e) {
          sender.sendMessage(Lang.get("ping-invalid-number", "value", args[i]));
          return true;
        }
        i++;
      } else if (!arg.startsWith("--")) {
        if (botTarget != null) {
          sender.sendMessage(Lang.get("ping-usage"));
          return true;
        }
        botTarget = arg;
        i++;
      } else {
        sender.sendMessage(Lang.get("ping-unknown-option", "option", arg));
        return true;
      }
    }

    if (botTarget != null && count >= 0) {
      sender.sendMessage(Lang.get("ping-target-count-conflict"));
      return true;
    }

    if (!random && !reset && fixedPing < 0) {
      fixedPing = DEFAULT_PING;
    }

    if (botTarget != null) {
      return handleSingleBot(sender, botTarget, fixedPing, random, reset);
    }

    return handleMultipleBots(sender, count, fixedPing, random, reset);
  }

  private boolean handleSingleBot(
      CommandSender sender, String name, int fixedPing, boolean random, boolean reset) {
    FakePlayer fp = manager.getByName(name);
    if (fp == null) {
      sender.sendMessage(Lang.get("ping-bot-not-found", "name", name));
      return true;
    }
    int ping = reset ? -1 : (random ? randomPing() : fixedPing);
    manager.applyPing(fp, ping);
    manager.persistBotSettings(fp);
    sender.sendMessage(
        Lang.get(
            "ping-set",
            "name",
            fp.getDisplayName(),
            "ping",
            ping < 0 ? "default" : String.valueOf(ping)));
    return true;
  }

  private boolean handleMultipleBots(
      CommandSender sender, int count, int fixedPing, boolean random, boolean reset) {
    List<FakePlayer> bots = new ArrayList<>(manager.getActivePlayers());
    if (bots.isEmpty()) {
      sender.sendMessage(Lang.get("ping-no-bots"));
      return true;
    }

    if (count >= 0) {
      Collections.shuffle(bots, ThreadLocalRandom.current());
      bots = bots.subList(0, Math.min(count, bots.size()));
    }

    for (FakePlayer fp : bots) {
      int ping = reset ? -1 : (random ? randomPing() : fixedPing);
      manager.applyPing(fp, ping);
      manager.persistBotSettings(fp);
    }

    sender.sendMessage(
        Lang.get(
            "ping-set-multiple",
            "count",
            String.valueOf(bots.size()),
            "ping",
            reset ? "default" : (random ? "random" : String.valueOf(fixedPing))));
    return true;
  }

  private static int randomPing() {
    ThreadLocalRandom rng = ThreadLocalRandom.current();
    int roll = rng.nextInt(100);
    if (roll < 60) {
      return rng.nextInt(RANDOM_PING_MIN, 100);
    } else if (roll < 85) {
      return rng.nextInt(100, 200);
    } else {
      return rng.nextInt(200, RANDOM_PING_MAX + 1);
    }
  }

  @Override
  public List<String> tabComplete(CommandSender sender, String[] args) {
    if (args.length == 1) {
      String prefix = args[0].toLowerCase();
      List<String> opts = new ArrayList<>();
      opts.add("--ping");
      opts.add("--random");
      opts.add("--reset");
      opts.add("--count");
      manager.getActiveNames().stream()
          .filter(n -> n.toLowerCase().startsWith(prefix))
          .forEach(opts::add);
      return opts.stream()
          .filter(s -> s.toLowerCase().startsWith(prefix))
          .collect(Collectors.toList());
    }
    if (args.length >= 2) {
      String prev = args[args.length - 2].toLowerCase();
      if (prev.equals("--ping")) {
        return List.of("20", "50", "100", "150", "200");
      }
      if (prev.equals("--count")) {
        return List.of("1", "3", "5", "10");
      }
      String cur = args[args.length - 1].toLowerCase();
      List<String> opts = new ArrayList<>();
      if ("--ping".startsWith(cur)) opts.add("--ping");
      if ("--random".startsWith(cur)) opts.add("--random");
      if ("--reset".startsWith(cur)) opts.add("--reset");
      if ("--count".startsWith(cur)) opts.add("--count");
      return opts;
    }
    return List.of();
  }
}
