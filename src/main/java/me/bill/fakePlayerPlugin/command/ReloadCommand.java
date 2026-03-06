package me.bill.fakePlayerPlugin.command;

import me.bill.fakePlayerPlugin.FakePlayerPlugin;
import me.bill.fakePlayerPlugin.config.Config;
import me.bill.fakePlayerPlugin.lang.Lang;
import me.bill.fakePlayerPlugin.util.FppLogger;
import org.bukkit.command.CommandSender;

public class ReloadCommand implements FppCommand {

    private final FakePlayerPlugin plugin;

    public ReloadCommand(FakePlayerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String getName()        { return "reload"; }
    @Override public String getUsage()       { return ""; }
    @Override public String getDescription() { return "Reloads the plugin configuration."; }
    @Override public String getPermission()  { return "fpp.reload"; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        long start = System.currentTimeMillis();

        Config.debug("Reload triggered by " + sender.getName() + ".");
        Config.reload();
        Config.debug("Config reloaded.");
        Lang.reload();
        Config.debug("Language file reloaded.");

        long ms = System.currentTimeMillis() - start;
        Config.debug("Reload finished in " + ms + "ms.");
        FppLogger.success("Plugin reloaded by " + sender.getName() + " in " + ms + "ms.");
        sender.sendMessage(Lang.get("reload-success", "ms", String.valueOf(ms)));
        return true;
    }
}

