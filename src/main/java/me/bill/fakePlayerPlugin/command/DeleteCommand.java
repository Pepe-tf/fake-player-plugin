package me.bill.fakePlayerPlugin.command;

import java.util.List;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayer;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayerManager;
import me.bill.fakePlayerPlugin.permission.Perm;
import org.bukkit.command.CommandSender;

public final class DeleteCommand implements FppCommand {

  private final FakePlayerManager manager;

  public DeleteCommand(FakePlayerManager manager) {
    this.manager = manager;
  }

  @Override
  public String getName() {
    return "despawn";
  }

  @Override
  public String getDescription() {
    return "Despawns fake players.";
  }

  @Override
  public String getUsage() {
    return "<name|--all>";
  }

  @Override
  public String getPermission() {
    return Perm.DESPAWN;
  }

  @Override
  public boolean execute(CommandSender sender, String[] args) {
    if (args.length == 0) {
      sender.sendMessage("Usage: /fpp despawn <name|--all>");
      return true;
    }
    if ("--all".equalsIgnoreCase(args[0])) {
      List<String> names = manager.getActivePlayers().stream().map(FakePlayer::getDisplayName).toList();
      int count = manager.removeAll();
      if (count == 0) {
        sender.sendMessage("No active fake players.");
        return true;
      }
      for (String name : names) {
        sender.sendMessage("[FPP] " + name + " left the game.");
      }
      return true;
    }
    FakePlayer fp = manager.getByName(args[0]);
    if (fp == null) {
      sender.sendMessage("Bot not found.");
      return true;
    }
    String displayName = fp.getDisplayName();
    manager.delete(args[0]);
    sender.sendMessage("[FPP] " + displayName + " left the game.");
    return true;
  }

  @Override
  public List<String> tabComplete(CommandSender sender, String[] args) {
    if (args.length == 1) {
      return manager.getActivePlayers().stream().map(fp -> fp.getName()).toList();
    }
    return List.of();
  }
}
