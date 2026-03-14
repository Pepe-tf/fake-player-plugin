package me.bill.fakePlayerPlugin.command;

import me.bill.fakePlayerPlugin.FakePlayerPlugin;
import me.bill.fakePlayerPlugin.config.BotMessageConfig;
import me.bill.fakePlayerPlugin.config.BotNameConfig;
import me.bill.fakePlayerPlugin.config.Config;
import me.bill.fakePlayerPlugin.fakeplayer.SkinFetcher;
import me.bill.fakePlayerPlugin.fakeplayer.SkinRepository;
import me.bill.fakePlayerPlugin.lang.Lang;
import me.bill.fakePlayerPlugin.permission.Perm;
import me.bill.fakePlayerPlugin.util.ConfigValidator;
import me.bill.fakePlayerPlugin.util.FppLogger;
import me.bill.fakePlayerPlugin.util.UpdateChecker;
import org.bukkit.command.CommandSender;

public class ReloadCommand implements FppCommand {

    private final FakePlayerPlugin plugin;

    public ReloadCommand(FakePlayerPlugin plugin) { this.plugin = plugin; }

    @Override public String getName()        { return "reload"; }
    @Override public String getUsage()       { return ""; }
    @Override public String getDescription() { return "Reloads the plugin configuration."; }
    @Override public String getPermission()  { return Perm.RELOAD; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        long start = System.currentTimeMillis();

        Config.debug("Reload triggered by " + sender.getName() + ".");
        Config.reload();
        Config.debug("Config reloaded.");
        Lang.reload();
        Config.debug("Language file reloaded.");
        BotNameConfig.reload();
        Config.debug("Bot name pool reloaded (" + BotNameConfig.getNames().size() + " names).");
        BotMessageConfig.reload();
        Config.debug("Bot message pool reloaded (" + BotMessageConfig.getMessages().size() + " messages).");

        // Clear skin cache so bots get fresh skins after reload (fetch mode)
        if (Config.skinClearCacheOnReload()) {
            SkinFetcher.clearCache();
            Config.debug("Skin cache cleared (" + Config.skinMode() + " mode).");
        }
        // Reinitialise the skin repository (picks up new folder files + config pool)
        SkinRepository.get().reload();
        Config.debug("Skin repository reloaded (mode=" + Config.skinMode()
                + ", folder=" + SkinRepository.get().getFolderSkinCount()
                + ", pool=" + SkinRepository.get().getPoolSkinCount() + ").");

        long ms = System.currentTimeMillis() - start;
        Config.debug("Reload finished in " + ms + "ms.");

        // Reload tab-list header/footer (picks up new toggle + interval)
        if (plugin.getTabListManager() != null) {
            plugin.getTabListManager().reload();
            Config.debug("TabListManager reloaded.");
        }

        // Re-validate config and warn if issues found
        int issues = ConfigValidator.validate();
        if (issues > 0) {
            FppLogger.warn("Reload completed with " + issues + " config issue(s) — see above.");
        }

        FppLogger.success("Plugin reloaded by " + sender.getName() + " in " + ms + "ms.");
        sender.sendMessage(Lang.get("reload-success", "ms", String.valueOf(ms)));
        // Re-run update check after reload (async, non-blocking)
        UpdateChecker.check(plugin);
        return true;
    }
}
