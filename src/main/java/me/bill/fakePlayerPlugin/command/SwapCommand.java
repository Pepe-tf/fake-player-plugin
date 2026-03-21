package me.bill.fakePlayerPlugin.command;

import me.bill.fakePlayerPlugin.FakePlayerPlugin;
import me.bill.fakePlayerPlugin.config.Config;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayerManager;
import me.bill.fakePlayerPlugin.lang.Lang;
import me.bill.fakePlayerPlugin.permission.Perm;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.List;

/**
 * {@code /fpp swap [on|off|status]} — toggles the bot swap/rotation system
 * at runtime and persists the change to {@code config.yml}.
 */
@SuppressWarnings("unused") // Registered dynamically via CommandManager.register()
public class SwapCommand implements FppCommand {

    private final FakePlayerPlugin plugin;

    public SwapCommand(FakePlayerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String getName()        { return "swap"; }
    @Override public String getUsage()       { return "[on|off|status]"; }
    @Override public String getDescription() { return "Toggles the bot swap/rotation system."; }
    @Override public String getPermission()  { return Perm.SWAP; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            boolean on = Config.swapEnabled();
            sender.sendMessage(Lang.get(on ? "swap-status-on" : "swap-status-off"));
            return true;
        }

        boolean enable;
        switch (args[0].toLowerCase()) {
            case "on",  "true",  "yes", "1" -> enable = true;
            case "off", "false", "no",  "0" -> enable = false;
            default -> {
                sender.sendMessage(Lang.get("swap-invalid"));
                return true;
            }
        }

        plugin.getConfig().set("swap.enabled", enable);
        plugin.saveConfig();
        Config.reload();

        // When disabling, cancel any pending session timers so bots stop leaving
        if (!enable) {
            FakePlayerManager mgr = plugin.getFakePlayerManager();
            if (mgr != null) mgr.cancelAllSwap();
        }

        sender.sendMessage(Lang.get(enable ? "swap-enabled" : "swap-disabled"));
        Config.debug("swap.enabled set to " + enable + " by " + sender.getName());
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) return Arrays.asList("on", "off", "status");
        return List.of();
    }
}


