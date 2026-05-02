package me.bill.fakePlayerPlugin.command;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import me.bill.fakePlayerPlugin.FakePlayerPlugin;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayer;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayerManager;
import me.bill.fakePlayerPlugin.fakeplayer.SkinManager;
import me.bill.fakePlayerPlugin.lang.Lang;
import me.bill.fakePlayerPlugin.permission.Perm;
import me.bill.fakePlayerPlugin.util.NameTagHelper;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SkinCommand implements FppCommand {

  private final FakePlayerPlugin plugin;
  private final FakePlayerManager manager;

  public SkinCommand(FakePlayerPlugin plugin, FakePlayerManager manager) {
    this.plugin = plugin;
    this.manager = manager;
  }

  @Override
  public String getName() {
    return "skin";
  }

  @Override
  public String getUsage() {
    return "<bot> <username|url|reset>";
  }

  @Override
  public String getDescription() {
    return "Apply a skin to a bot from a Minecraft username, URL, or reset it.";
  }

  @Override
  public String getPermission() {
    return Perm.SKIN;
  }

  @Override
  public boolean canUse(CommandSender sender) {
    return Perm.has(sender, Perm.SKIN);
  }

  @Override
  public boolean execute(CommandSender sender, String[] args) {
    if (args.length < 1) {
      sender.sendMessage(Lang.get("skin-usage"));
      return true;
    }

    String botName = args[0];
    FakePlayer fp = manager.getByName(botName);
    if (fp == null) {
      sender.sendMessage(Lang.get("skin-bot-not-found", "name", botName));
      return true;
    }

    if (args.length < 2) {
      sender.sendMessage(Lang.get("skin-usage"));
      return true;
    }

    SkinManager skinManager = plugin.getSkinManager();
    if (skinManager == null) {
      sender.sendMessage(Lang.get("skin-failed", "name", fp.getDisplayName()));
      return true;
    }

    if (plugin.isNameTagAvailable() && NameTagHelper.getSkin(fp.getUuid()) != null) {
      sender.sendMessage(Lang.get("skin-no-nametag"));
      return true;
    }

    String skinArg = args[1];

    if (skinArg.equalsIgnoreCase("reset")) {
      boolean ok = skinManager.resetToDefaultSkin(fp);
      if (ok) {
        sender.sendMessage(Lang.get("skin-reset", "name", fp.getDisplayName()));
      } else {
        sender.sendMessage(Lang.get("skin-failed", "name", fp.getDisplayName()));
      }
      return true;
    }

    if (skinArg.startsWith("http://") || skinArg.startsWith("https://") || skinArg.startsWith("data:image")) {
      sender.sendMessage(Lang.get("skin-applying", "name", fp.getDisplayName()));
      skinManager.applySkinByUrl(fp, skinArg).thenAccept(success -> {
        me.bill.fakePlayerPlugin.util.FppScheduler.runSync(plugin, () -> {
          if (success) {
            sender.sendMessage(Lang.get("skin-applied", "name", fp.getDisplayName()));
          } else {
            sender.sendMessage(Lang.get("skin-failed", "name", fp.getDisplayName()));
          }
        });
      });
      return true;
    }

    sender.sendMessage(Lang.get("skin-applying-player", "name", fp.getDisplayName(), "player", skinArg));
    skinManager.applySkinByUsername(fp, skinArg).thenAccept(success -> {
      me.bill.fakePlayerPlugin.util.FppScheduler.runSync(plugin, () -> {
        if (success) {
          sender.sendMessage(Lang.get("skin-applied", "name", fp.getDisplayName()));
        } else {
          sender.sendMessage(Lang.get("skin-player-not-found", "name", fp.getDisplayName(), "player", skinArg));
        }
      });
    });

    return true;
  }

  @Override
  public List<String> tabComplete(CommandSender sender, String[] args) {
    if (args.length == 1) {
      String prefix = args[0].toLowerCase();
      return manager.getActiveNames().stream()
          .filter(n -> n.toLowerCase().startsWith(prefix))
          .collect(Collectors.toList());
    }
    if (args.length == 2) {
      String prefix = args[1].toLowerCase();
      List<String> options = new ArrayList<>();
      options.add("reset");
      for (Player p : Bukkit.getOnlinePlayers()) {
        if (manager.getByUuid(p.getUniqueId()) == null) {
          options.add(p.getName());
        }
      }
      return options.stream()
          .filter(o -> o.toLowerCase().startsWith(prefix))
          .collect(Collectors.toList());
    }
    return List.of();
  }
}