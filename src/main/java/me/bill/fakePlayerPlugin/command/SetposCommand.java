package me.bill.fakePlayerPlugin.command;

import me.bill.fakePlayerPlugin.fakeplayer.FakePlayer;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayerManager;
import me.bill.fakePlayerPlugin.lang.Lang;
import me.bill.fakePlayerPlugin.permission.Perm;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

/**
 * {@code /fpp setpos <botname>} — admin command to teleport any bot
 * to the sender's current location (inverse of {@code /fpp tp}).
 *
 * <p>Permissions: {@link Perm#SETPOS} (default: op).
 */
public class SetposCommand implements FppCommand {

    private final FakePlayerManager manager;

    public SetposCommand(FakePlayerManager manager) {
        this.manager = manager;
    }

    @Override public String getName()        { return "setpos"; }
    @Override public String getUsage()       { return "<botname>"; }
    @Override public String getDescription() { return "Teleport a bot to your location."; }
    @Override public String getPermission()  { return Perm.SETPOS; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Lang.get("player-only"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(Lang.get("setpos-usage"));
            return true;
        }

        String   botName = args[0];
        FakePlayer fp    = manager.getByName(botName);

        if (fp == null) {
            sender.sendMessage(Lang.get("setpos-not-found", "name", botName));
            return true;
        }

        boolean ok = manager.teleportBot(fp, player.getLocation());
        if (ok) {
            sender.sendMessage(Lang.get("setpos-success", "name", fp.getDisplayName()));
        } else {
            sender.sendMessage(Lang.get("setpos-failed", "name", fp.getDisplayName()));
        }
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
        return List.of();
    }
}

