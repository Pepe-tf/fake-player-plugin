package me.bill.fakePlayerPlugin.command;

import me.bill.fakePlayerPlugin.api.FppAddonCommand;
import me.bill.fakePlayerPlugin.gui.HelpGui;
import me.bill.fakePlayerPlugin.permission.Perm;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class HelpCommand implements FppCommand {

  private final CommandManager manager;
  private HelpGui helpGui;

  public HelpCommand(CommandManager manager) {
    this.manager = manager;
  }

  public void setHelpGui(HelpGui helpGui) {
    this.helpGui = helpGui;
  }

  @Override
  public String getName() {
    return "help";
  }

  @Override
  public String getDescription() {
    return "Open the interactive command help.";
  }

  @Override
  public String getUsage() {
    return "/fpp help";
  }

  @Override
  public String getPermission() {
    return Perm.HELP;
  }

  @Override
  public boolean execute(CommandSender sender, String[] args) {
    if (sender instanceof Player player && helpGui != null) {
      helpGui.open(player, 0);
      return true;
    }
    sender.sendMessage("FakePlayerPlugin commands:");
    for (FppCommand command : manager.getCommands()) {
      if (command.getPermission() == null
          || command.getPermission().isBlank()
          || sender.hasPermission(command.getPermission())) {
        sender.sendMessage(" - " + command.getUsage() + " - " + command.getDescription());
      }
    }
    for (FppAddonCommand command : manager.getAddonCommands()) {
      if (command.getPermission() == null
          || command.getPermission().isBlank()
          || sender.hasPermission(command.getPermission())) {
        sender.sendMessage(" - " + command.getUsage() + " - " + command.getDescription());
      }
    }
    return true;
  }
}
