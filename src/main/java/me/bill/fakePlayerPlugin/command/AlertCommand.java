package me.bill.fakePlayerPlugin.command;

import me.bill.fakePlayerPlugin.FakePlayerPlugin;
import me.bill.fakePlayerPlugin.lang.Lang;
import me.bill.fakePlayerPlugin.permission.Perm;

import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public final class AlertCommand implements FppCommand {

    private final FakePlayerPlugin plugin;

    public AlertCommand(FakePlayerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "alert";
    }

    @Override
    public String getUsage() {
        return "<message>";
    }

    @Override
    public String getDescription() {
        return "Broadcast alert to all servers";
    }

    @Override
    public String getPermission() {
        return Perm.ALERT;
    }

    @Override
    public boolean canUse(CommandSender sender) {
        return Perm.has(sender, Perm.ALERT);
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(Lang.get("alert-usage"));
            return false;
        }

        String message = String.join(" ", args);

        var vc = plugin.getVelocityChannel();
        if (vc == null) {
            sender.sendMessage(Lang.get("prefix") + "<red>Plugin messaging not initialized.");
            return false;
        }

        vc.broadcastGlobalAlert(message);

        sender.sendMessage(Lang.get("alert-sent"));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {

        return Collections.emptyList();
    }
}
