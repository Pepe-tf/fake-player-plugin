package me.bill.fakePlayerPlugin.command;

import java.util.ArrayList;
import java.util.List;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayer;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayerManager;
import me.bill.fakePlayerPlugin.gui.BotSettingGui;
import me.bill.fakePlayerPlugin.gui.SettingGui;
import me.bill.fakePlayerPlugin.permission.Perm;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class SettingCommand implements FppCommand {

  private final SettingGui settingGui;
  private final BotSettingGui botSettingGui;
  private final FakePlayerManager manager;

  public SettingCommand(SettingGui settingGui, BotSettingGui botSettingGui, FakePlayerManager manager) {
    this.settingGui = settingGui;
    this.botSettingGui = botSettingGui;
    this.manager = manager;
  }

  @Override
  public String getName() {
    return "settings";
  }

  @Override
  public String getDescription() {
    return "Open global settings, or per-bot settings for a bot.";
  }

  @Override
  public String getUsage() {
    return "/fpp settings [bot]";
  }

  @Override
  public String getPermission() {
    return Perm.SETTINGS;
  }

  @Override
  public boolean execute(CommandSender sender, String[] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage("Only players can open the settings GUI.");
      return true;
    }
    if (args.length == 0) {
      settingGui.open(player, 0, 0);
      return true;
    }
    FakePlayer bot = manager.getByName(args[0]);
    if (bot == null) {
      sender.sendMessage("Bot not found: " + args[0]);
      return true;
    }
    botSettingGui.open(player, bot);
    return true;
  }

  @Override
  public List<String> tabComplete(CommandSender sender, String[] args) {
    if (args.length != 1) return List.of();
    String prefix = args[0].toLowerCase();
    List<String> names = new ArrayList<>();
    for (FakePlayer fp : manager.getActivePlayers()) {
      if (fp.getName().toLowerCase().startsWith(prefix)) names.add(fp.getName());
    }
    return names;
  }
}
