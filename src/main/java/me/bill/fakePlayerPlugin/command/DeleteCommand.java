package me.bill.fakePlayerPlugin.command;

import me.bill.fakePlayerPlugin.fakeplayer.FakePlayerManager;
import me.bill.fakePlayerPlugin.lang.Lang;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class DeleteCommand implements FppCommand {

    private final FakePlayerManager manager;

    public DeleteCommand(FakePlayerManager manager) { this.manager = manager; }

    @Override public String getName()        { return "delete"; }
    @Override public String getDescription() { return "Deletes a fake player bot by name."; }
    @Override public String getUsage()       { return "<name|all>"; }
    @Override public String getPermission()  { return "fpp.delete"; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Lang.get("unknown-command", "0", "fpp"));
            return true;
        }

        if (args[0].equalsIgnoreCase("all")) {
            int count = manager.getCount();
            if (count == 0) {
                sender.sendMessage(Lang.get("delete-none"));
                return true;
            }
            manager.removeAll();
            sender.sendMessage(Lang.get("delete-all", "count", String.valueOf(count)));
            return true;
        }

        String targetName = args[0];
        if (manager.getActiveNames().stream().noneMatch(n -> n.equalsIgnoreCase(targetName))) {
            sender.sendMessage(Lang.get("delete-not-found", "name", targetName));
            return true;
        }

        manager.delete(targetName);
        sender.sendMessage(Lang.get("delete-success", "name", targetName));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new java.util.ArrayList<>();
            suggestions.add("all");
            manager.getActiveNames().stream()
                    .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                    .forEach(suggestions::add);
            return suggestions;
        }
        return Collections.emptyList();
    }
}
