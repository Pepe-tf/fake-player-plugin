package me.bill.fakePlayerPlugin;

import me.bill.fakePlayerPlugin.command.CommandManager;
import me.bill.fakePlayerPlugin.command.DeleteCommand;
import me.bill.fakePlayerPlugin.command.ReloadCommand;
import me.bill.fakePlayerPlugin.command.SpawnCommand;
import me.bill.fakePlayerPlugin.config.BotNameConfig;
import me.bill.fakePlayerPlugin.config.Config;
import me.bill.fakePlayerPlugin.fakeplayer.ChunkLoader;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayerManager;
import me.bill.fakePlayerPlugin.lang.Lang;
import me.bill.fakePlayerPlugin.listener.FakePlayerEntityListener;
import me.bill.fakePlayerPlugin.listener.PlayerJoinListener;
import me.bill.fakePlayerPlugin.util.FppLogger;
import org.bukkit.plugin.java.JavaPlugin;

public final class FakePlayerPlugin extends JavaPlugin {

    private static FakePlayerPlugin instance;
    public static FakePlayerPlugin getInstance() { return instance; }

    private CommandManager commandManager;
    private FakePlayerManager fakePlayerManager;
    private ChunkLoader chunkLoader;

    @Override
    public void onEnable() {
        instance = this;
        FppLogger.init(getLogger());

        // Load config then language file
        Config.init(this);
        Config.debug("Config loaded.");
        Lang.init(this);
        Config.debug("Language file loaded (lang=" + Config.getLanguage() + ").");
        BotNameConfig.init(this);
        Config.debug("Bot name pool loaded (" + BotNameConfig.getNames().size() + " names).");

        // Fake player manager + chunk loader
        fakePlayerManager = new FakePlayerManager(this);
        chunkLoader = new ChunkLoader(this, fakePlayerManager);
        fakePlayerManager.setChunkLoader(chunkLoader);

        // Register commands
        commandManager = new CommandManager();
        commandManager.register(new ReloadCommand(this));
        commandManager.register(new SpawnCommand(fakePlayerManager));
        commandManager.register(new DeleteCommand(fakePlayerManager));
        Config.debug("Commands registered: " + commandManager.getCommands().size() + " total.");

        //noinspection DataFlowIssue
        getCommand("fpp").setExecutor(commandManager);
        getCommand("fpp").setTabCompleter(commandManager);

        // Listeners
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this, fakePlayerManager), this);
        getServer().getPluginManager().registerEvents(new FakePlayerEntityListener(this, fakePlayerManager, chunkLoader), this);

        FppLogger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        FppLogger.info("  ꜰᴀᴋᴇ ᴘʟᴀʏᴇʀ ᴘʟᴜɢɪɴ  v" + getPluginMeta().getVersion());
        FppLogger.info("  Author: " + String.join(", ", getPluginMeta().getAuthors()));
        FppLogger.success("  Plugin enabled successfully!");
        FppLogger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        Config.debug("onEnable complete.");
    }

    @Override
    public void onDisable() {
        Config.debug("onDisable called.");
        if (chunkLoader != null) chunkLoader.releaseAll();
        if (fakePlayerManager != null) fakePlayerManager.removeAllSync();
        FppLogger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        FppLogger.warn("  ꜰᴀᴋᴇ ᴘʟᴀʏᴇʀ ᴘʟᴜɢɪɴ is shutting down...");
        FppLogger.info("  Goodbye! All fake players have been removed.");
        FppLogger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    public CommandManager getCommandManager()       { return commandManager; }
    public FakePlayerManager getFakePlayerManager() { return fakePlayerManager; }
}
