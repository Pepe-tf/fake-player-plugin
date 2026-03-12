package me.bill.fakePlayerPlugin;

import me.bill.fakePlayerPlugin.command.ChatCommand;
import me.bill.fakePlayerPlugin.command.CommandManager;
import me.bill.fakePlayerPlugin.command.DeleteCommand;
import me.bill.fakePlayerPlugin.command.InfoCommand;
import me.bill.fakePlayerPlugin.command.ListCommand;
import me.bill.fakePlayerPlugin.command.ReloadCommand;
import me.bill.fakePlayerPlugin.command.SpawnCommand;
import me.bill.fakePlayerPlugin.command.SwapCommand;
import me.bill.fakePlayerPlugin.command.TpCommand;
import me.bill.fakePlayerPlugin.command.TphCommand;
import me.bill.fakePlayerPlugin.config.BotMessageConfig;
import me.bill.fakePlayerPlugin.config.BotNameConfig;
import me.bill.fakePlayerPlugin.config.Config;
import me.bill.fakePlayerPlugin.database.DatabaseManager;
import me.bill.fakePlayerPlugin.fakeplayer.BotChatAI;
import me.bill.fakePlayerPlugin.fakeplayer.BotHeadAI;
import me.bill.fakePlayerPlugin.fakeplayer.BotPersistence;
import me.bill.fakePlayerPlugin.fakeplayer.BotSwapAI;
import me.bill.fakePlayerPlugin.fakeplayer.ChunkLoader;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayerManager;
import me.bill.fakePlayerPlugin.fakeplayer.SkinRepository;
import me.bill.fakePlayerPlugin.lang.Lang;
import me.bill.fakePlayerPlugin.listener.BotCollisionListener;
import me.bill.fakePlayerPlugin.listener.FakePlayerEntityListener;
import me.bill.fakePlayerPlugin.listener.ServerListListener;
import me.bill.fakePlayerPlugin.listener.PlayerJoinListener;
import me.bill.fakePlayerPlugin.util.FppLogger;
import me.bill.fakePlayerPlugin.util.UpdateChecker;
import org.bukkit.plugin.java.JavaPlugin;

public final class FakePlayerPlugin extends JavaPlugin {

    private static FakePlayerPlugin instance;
    @SuppressWarnings("unused")
    public static FakePlayerPlugin getInstance() { return instance; }

    private CommandManager    commandManager;
    private FakePlayerManager fakePlayerManager;
    private ChunkLoader       chunkLoader;
    private DatabaseManager   databaseManager;
    private BotPersistence    botPersistence;
    private BotSwapAI         botSwapAI;

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
        BotMessageConfig.init(this);
        Config.debug("Bot message pool loaded (" + BotMessageConfig.getMessages().size() + " messages).");

        // Ensure plugin data directories exist regardless of config settings
        ensureDataDirectories();

        // Skin repository — must be init'd after Config so skin.mode is readable
        SkinRepository.get().init(this);
        Config.debug("Skin repository initialised (mode=" + Config.skinMode() + ").");

        // Database
        databaseManager = new DatabaseManager();
        boolean dbOk = databaseManager.init(getDataFolder());
        if (!dbOk) {
            FppLogger.warn("Database could not be initialised — session tracking disabled.");
            databaseManager = null;
        }

        // Fake player manager + chunk loader
        fakePlayerManager = new FakePlayerManager(this);
        if (databaseManager != null) fakePlayerManager.setDatabaseManager(databaseManager);
        chunkLoader = new ChunkLoader(this, fakePlayerManager);
        fakePlayerManager.setChunkLoader(chunkLoader);

        // Bot persistence
        botPersistence = new BotPersistence(this);
        fakePlayerManager.setBotPersistence(botPersistence);

        // Swap AI — leave/rejoin rotation system
        botSwapAI = new BotSwapAI(this, fakePlayerManager);
        fakePlayerManager.setSwapAI(botSwapAI);

        // Register commands — help is always first so the help menu lists it first
        commandManager = new CommandManager(this);
        commandManager.register(new SpawnCommand(fakePlayerManager));
        commandManager.register(new DeleteCommand(fakePlayerManager));
        commandManager.register(new ListCommand(fakePlayerManager));
        commandManager.register(new TphCommand(fakePlayerManager));
        commandManager.register(new TpCommand(fakePlayerManager));
        commandManager.register(new ChatCommand(this));
        commandManager.register(new SwapCommand(this));
        commandManager.register(new ReloadCommand(this));
        commandManager.register(new InfoCommand(databaseManager, fakePlayerManager));
        Config.debug("Commands registered: " + commandManager.getCommands().size() + " total.");

        var fppCmd = getCommand("fpp");
        if (fppCmd != null) {
            fppCmd.setExecutor(commandManager);
            fppCmd.setTabCompleter(commandManager);
        }

        // Listeners
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this, fakePlayerManager), this);
        getServer().getPluginManager().registerEvents(new FakePlayerEntityListener(this, fakePlayerManager, chunkLoader), this);
        getServer().getPluginManager().registerEvents(new BotCollisionListener(this, fakePlayerManager), this);
        getServer().getPluginManager().registerEvents(new ServerListListener(fakePlayerManager), this);

        // Head AI — bots look at the nearest player within range
        new BotHeadAI(this, fakePlayerManager);
        // Chat AI — bots randomly send chat messages
        new BotChatAI(this, fakePlayerManager);

        FppLogger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        FppLogger.info("  ꜰᴀᴋᴇ ᴘʟᴀʏᴇʀ ᴘʟᴜɢɪɴ  v" + getPluginMeta().getVersion());
        FppLogger.info("  Author: " + String.join(", ", getPluginMeta().getAuthors()));
        FppLogger.success("  Plugin enabled successfully!");
        FppLogger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        // Check for updates (async, non-blocking)
        UpdateChecker.check(this);

        // Restore bots from previous session (deferred — worlds must be loaded first).
        // First purge any orphaned Mannequin bodies left by a crash, then restore.
        botPersistence.purgeOrphanedBodiesAndRestore(fakePlayerManager);

        // Log LuckPerms prefix detection after server fully starts (1-tick delay)
        getServer().getScheduler().runTaskLater(this, () -> {
            String detected = fakePlayerManager.detectLuckPermsPrefix();
            if (detected.isEmpty()) {
                FppLogger.info("[FPP] LuckPerms: no prefix detected on default group.");
            } else {
                FppLogger.info("[FPP] LuckPerms: detected prefix → " + detected);
            }
        }, 1L);

        Config.debug("onEnable complete.");
    }

    @Override
    public void onDisable() {
        Config.debug("onDisable called.");

        // Save active bots BEFORE removing them so the file is written with real locations
        if (botPersistence != null && fakePlayerManager != null) {
            if (Config.persistOnRestart()) {
                botPersistence.save(fakePlayerManager.getActivePlayers());
            }
        }

        if (chunkLoader != null) chunkLoader.releaseAll();
        // Cancel swap timers before sync removal to prevent ghost rejoin tasks
        if (botSwapAI != null) botSwapAI.cancelAll();
        // removeAllSync sends leave messages + cleans up entities
        if (fakePlayerManager != null) fakePlayerManager.removeAllSync();

        // Flush DB — mark all open sessions as SHUTDOWN before closing
        if (databaseManager != null) {
            databaseManager.recordAllShutdown();
            databaseManager.close();
        }

        FppLogger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        FppLogger.warn("  ꜰᴀᴋᴇ ᴘʟᴀʏᴇʀ ᴘʟᴜɢɪɴ is shutting down...");
        FppLogger.info("  Goodbye! All fake players have been removed.");
        FppLogger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    @SuppressWarnings("unused") // Public API — available for addons
    public CommandManager    getCommandManager()       { return commandManager; }
    @SuppressWarnings("unused")
    public FakePlayerManager getFakePlayerManager()    { return fakePlayerManager; }
    public DatabaseManager   getDatabaseManager()      { return databaseManager; }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Creates all plugin data sub-directories on first run (or after deletion).
     * Called before any subsystem reads from these folders so they always exist.
     *
     * <ul>
     *   <li>{@code plugins/FakePlayerPlugin/}          — plugin root</li>
     *   <li>{@code plugins/FakePlayerPlugin/skins/}    — PNG skin files</li>
     *   <li>{@code plugins/FakePlayerPlugin/data/}     — SQLite database</li>
     *   <li>{@code plugins/FakePlayerPlugin/language/} — lang files</li>
     * </ul>
     */
    private void ensureDataDirectories() {
        java.io.File root = getDataFolder();
        String[] dirs = { "skins", "data", "language" };
        for (String dir : dirs) {
            java.io.File d = new java.io.File(root, dir);
            if (!d.exists()) {
                boolean ok = d.mkdirs();
                FppLogger.debug("Created directory: " + d.getPath() + (ok ? " ✓" : " (already exists or failed)"));
            }
        }

        // Drop a README inside skins/ so admins know what to put there
        java.io.File skinsReadme = new java.io.File(root, "skins/README.txt");
        if (!skinsReadme.exists()) {
            try (java.io.PrintWriter w = new java.io.PrintWriter(skinsReadme)) {
                w.println("# FakePlayerPlugin — Skin Folder");
                w.println("#");
                w.println("# Place PNG skin files here to use them for bots.");
                w.println("# Requires: skin.mode = custom  in config.yml");
                w.println("#");
                w.println("# Naming rules:");
                w.println("#   <botname>.png  — assigned exclusively to the bot with that name");
                w.println("#   anything.png   — added to the random skin pool");
                w.println("#");
                w.println("# Skin files must be standard 64x64 or 64x32 Minecraft skin PNGs.");
                w.println("# Run /fpp reload after adding or removing skin files.");
            } catch (java.io.IOException e) {
                FppLogger.debug("Could not write skins/README.txt: " + e.getMessage());
            }
        }
    }
}
