package me.bill.fakePlayerPlugin.command;

import me.bill.fakePlayerPlugin.fakeplayer.FakePlayerManager;
import me.bill.fakePlayerPlugin.lang.Lang;
import me.bill.fakePlayerPlugin.permission.Perm;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public class DeleteCommand implements FppCommand {

    private final FakePlayerManager manager;

    public DeleteCommand(FakePlayerManager manager) { this.manager = manager; }

    @Override public String getName()        { return "delete"; }
    @Override public String getDescription() { return "Deletes a fake player bot by name."; }
    @Override public String getUsage()       { return "<name|all>"; }
    @Override public String getPermission()  { return Perm.DELETE; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Lang.get("unknown-command", "0", "fpp"));
            return true;
        }

        if (args[0].equalsIgnoreCase("all")) {
            // Requires fpp.delete.all (child of fpp.delete, but can be negated separately)
            if (Perm.missing(sender, Perm.DELETE_ALL)) {
                sender.sendMessage(Lang.get("no-permission"));
                return true;
            }
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

        // Resolve the FakePlayer to get its display name for the success message
        me.bill.fakePlayerPlugin.fakeplayer.FakePlayer fp = manager.getActivePlayers().stream()
                .filter(p -> p.getName().equalsIgnoreCase(targetName)
                        || p.getDisplayName().equalsIgnoreCase(targetName))
                .findFirst().orElse(null);

        if (fp == null) {
            sender.sendMessage(Lang.get("delete-not-found", "name", targetName));
            return true;
        }

        String shown = fp.getDisplayName();
        manager.delete(fp.getName());
        sender.sendMessage(Lang.get("delete-success", "name", shown));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new java.util.ArrayList<>();
            // Only suggest "all" if sender has fpp.delete.all
            if (Perm.has(sender, Perm.DELETE_ALL)) {
                suggestions.add("all");
            }
            // Suggest display names so admins see "[bot] PlayerName" not "b_PlayerName"
            manager.getActivePlayers().stream()
                    .filter(p -> p.getDisplayName().toLowerCase().startsWith(args[0].toLowerCase())
                            || p.getName().toLowerCase().startsWith(args[0].toLowerCase()))
                    .map(p -> p.getDisplayName())
                    .forEach(suggestions::add);
            return suggestions;
        }
        return Collections.emptyList();
    }
}
