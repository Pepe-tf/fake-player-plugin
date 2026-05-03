package me.bill.fakePlayerPlugin.command;

import me.bill.fakePlayerPlugin.FakePlayerPlugin;
import me.bill.fakePlayerPlugin.config.Config;
import me.bill.fakePlayerPlugin.permission.Perm;
import org.bukkit.command.CommandSender;

public final class ReloadCommand implements FppCommand {

  private final FakePlayerPlugin plugin;

  public ReloadCommand(FakePlayerPlugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public String getName() {
    return "reload";
  }

  @Override
  public String getDescription() {
    return "Reloads config and extensions.";
  }

  @Override
  public String getUsage() {
    return "";
  }

  @Override
  public String getPermission() {
    return Perm.RELOAD;
  }

  @Override
  public boolean execute(CommandSender sender, String[] args) {
    Config.init(plugin);
    if (plugin.getExtensionLoader() != null) {
      plugin.getExtensionLoader().reloadExtensions();
    }
    sender.sendMessage("FakePlayerPlugin reloaded.");
    return true;
  }
}
