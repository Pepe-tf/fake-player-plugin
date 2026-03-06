package me.bill.fakePlayerPlugin.command;

import me.bill.fakePlayerPlugin.fakeplayer.FakePlayerManager;
import me.bill.fakePlayerPlugin.lang.Lang;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * {@code /fpp spawn [amount]} — spawns one or more fake players at the
 * sender's location. Aliased as {@code /fpp summon}.
 */
public class SpawnCommand implements FppCommand {

    private final FakePlayerManager manager;

    public SpawnCommand(FakePlayerManager manager) {
        this.manager = manager;
    }

    @Override public String getName()        { return "spawn"; }
    @Override public String getUsage()       { return "[amount]"; }
    @Override public String getDescription() { return "Spawns fake player(s) at your location."; }
    @Override public String getPermission()  { return "fpp.spawn"; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Lang.get("player-only"));
            return true;
        }

        int amount = 1;
        if (args.length > 0) {
            try {
                amount = Integer.parseInt(args[0]);
                if (amount < 1) amount = 1;
            } catch (NumberFormatException e) {
                sender.sendMessage(Lang.get("spawn-invalid-amount"));
                return true;
            }
        }

        int spawned = manager.spawn(player.getLocation(), amount);
        sender.sendMessage(Lang.get("spawn-success",
                "count", String.valueOf(spawned)));
        return true;
    }
}

