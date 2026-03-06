package me.bill.fakePlayerPlugin;

import me.bill.fakePlayerPlugin.command.CommandManager;
import me.bill.fakePlayerPlugin.command.ReloadCommand;
import me.bill.fakePlayerPlugin.command.SpawnCommand;
import me.bill.fakePlayerPlugin.command.SummonCommand;
import me.bill.fakePlayerPlugin.config.Config;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayerManager;
import me.bill.fakePlayerPlugin.lang.Lang;
import me.bill.fakePlayerPlugin.listener.PlayerJoinListener;
import me.bill.fakePlayerPlugin.util.FppLogger;
import org.bukkit.plugin.java.JavaPlugin;

public final class FakePlayerPlugin extends JavaPlugin {

    private static FakePlayerPlugin instance;
    public static FakePlayerPlugin getInstance() { return instance; }

    private CommandManager commandManager;
    private FakePlayerManager fakePlayerManager;

    @Override
    public void onEnable() {
        instance = this;
        FppLogger.init(getLogger());

        // Load config then language file
        Config.init(this);
        Config.debug("Config loaded.");
        Lang.init(this);
        Config.debug("Language file loaded (lang=" + Config.getLanguage() + ").");

        // Fake player manager
        fakePlayerManager = new FakePlayerManager(this);

        // Register commands
        commandManager = new CommandManager();
        commandManager.register(new ReloadCommand(this));
        commandManager.register(new SpawnCommand(fakePlayerManager));
        commandManager.register(new SummonCommand(fakePlayerManager));
        Config.debug("Commands registered: " + commandManager.getCommands().size() + " total.");

        //noinspection DataFlowIssue
        getCommand("fpp").setExecutor(commandManager);
        getCommand("fpp").setTabCompleter(commandManager);

        // Listeners
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this, fakePlayerManager), this);

        FppLogger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        FppLogger.info("  ꜰᴀᴋᴇ ᴘʟᴀʏᴇʀ ᴘʟᴜɢɪɴ  v" + getDescription().getVersion());
        FppLogger.info("  Author: " + String.join(", ", getDescription().getAuthors()));
        FppLogger.success("  Plugin enabled successfully!");
        FppLogger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        Config.debug("onEnable complete.");
    }

    @Override
    public void onDisable() {
        Config.debug("onDisable called.");
        // Clean up all fake players so they don't linger after restart
        if (fakePlayerManager != null) {
            fakePlayerManager.removeAll();
        }
        FppLogger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        FppLogger.warn("  ꜰᴀᴋᴇ ᴘʟᴀʏᴇʀ ᴘʟᴜɢɪɴ is shutting down...");
        FppLogger.info("  Goodbye! All fake players have been removed.");
        FppLogger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    public CommandManager getCommandManager()       { return commandManager; }
    public FakePlayerManager getFakePlayerManager() { return fakePlayerManager; }
}
