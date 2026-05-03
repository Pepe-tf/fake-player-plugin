package me.bill.fakePlayerPlugin.command;

import me.bill.fakePlayerPlugin.fakeplayer.FakePlayerManager;
import me.bill.fakePlayerPlugin.permission.Perm;
import org.bukkit.command.CommandSender;

public final class ListCommand implements FppCommand {

  private final FakePlayerManager manager;

  public ListCommand(FakePlayerManager manager) {
    this.manager = manager;
  }

  @Override
  public String getName() {
    return "list";
  }

  @Override
  public String getDescription() {
    return "Lists active fake players.";
  }

  @Override
  public String getUsage() {
    return "";
  }

  @Override
  public String getPermission() {
    return Perm.LIST;
  }

  @Override
  public boolean execute(CommandSender sender, String[] args) {
    if (manager.getActivePlayers().isEmpty()) {
      sender.sendMessage("No active fake players.");
      return true;
    }
    sender.sendMessage(
        "Fake players: "
            + String.join(
                ", ", manager.getActivePlayers().stream().map(fp -> fp.getName()).toList()));
    return true;
  }
}
