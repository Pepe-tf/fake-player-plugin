package me.bill.fakePlayerPlugin;

import me.bill.fakePlayerPlugin.api.FppApi;
import me.bill.fakePlayerPlugin.api.impl.FppApiImpl;
import me.bill.fakePlayerPlugin.command.CommandManager;
import me.bill.fakePlayerPlugin.command.DeleteCommand;
import me.bill.fakePlayerPlugin.command.HelpCommand;
import me.bill.fakePlayerPlugin.command.InventoryCommand;
import me.bill.fakePlayerPlugin.command.ListCommand;
import me.bill.fakePlayerPlugin.command.ReloadCommand;
import me.bill.fakePlayerPlugin.command.SettingCommand;
import me.bill.fakePlayerPlugin.command.SpawnCommand;
import me.bill.fakePlayerPlugin.config.Config;
import me.bill.fakePlayerPlugin.extension.ExtensionLoader;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayerManager;
import me.bill.fakePlayerPlugin.fakeplayer.NmsPlayerSpawner;
import me.bill.fakePlayerPlugin.gui.BotSettingGui;
import me.bill.fakePlayerPlugin.gui.HelpGui;
import me.bill.fakePlayerPlugin.gui.SettingGui;
import me.bill.fakePlayerPlugin.listener.BotCollisionListener;
import me.bill.fakePlayerPlugin.listener.FakePlayerConnectionMessageListener;
import me.bill.fakePlayerPlugin.listener.FakePlayerDeathListener;
import me.bill.fakePlayerPlugin.util.FppLogger;
import org.bukkit.plugin.java.JavaPlugin;

public final class FakePlayerPlugin extends JavaPlugin {

  private static FakePlayerPlugin instance;

  private FakePlayerManager fakePlayerManager;
  private CommandManager commandManager;
  private FppApiImpl fppApi;
  private ExtensionLoader extensionLoader;
  private SettingGui settingGui;
  private BotSettingGui botSettingGui;
  private HelpGui helpGui;
  private InventoryCommand inventoryCommand;

  public static FakePlayerPlugin getInstance() {
    return instance;
  }

  @Override
  public void onEnable() {
    instance = this;
    FppLogger.init(getLogger());
    saveDefaultConfig();
    Config.init(this);

    NmsPlayerSpawner.init();

    fakePlayerManager = new FakePlayerManager(this);
    fppApi = new FppApiImpl(this, fakePlayerManager);

    commandManager = new CommandManager(this);
    commandManager.register(new SpawnCommand(fakePlayerManager));
    commandManager.register(new DeleteCommand(fakePlayerManager));
    commandManager.register(new ListCommand(fakePlayerManager));
    commandManager.register(new ReloadCommand(this));
    settingGui = new SettingGui(this);
    botSettingGui = new BotSettingGui(this, fakePlayerManager);
    inventoryCommand = new InventoryCommand(fakePlayerManager, this, botSettingGui);
    commandManager.register(new SettingCommand(settingGui, botSettingGui, fakePlayerManager));
    commandManager.register(inventoryCommand);
    HelpCommand helpCommand = new HelpCommand(commandManager);
    commandManager.register(helpCommand);
    commandManager.bind();

    helpGui = new HelpGui(this, commandManager);
    helpCommand.setHelpGui(helpGui);
    getServer().getPluginManager().registerEvents(settingGui, this);
    getServer().getPluginManager().registerEvents(botSettingGui, this);
    getServer().getPluginManager().registerEvents(helpGui, this);
    getServer().getPluginManager().registerEvents(inventoryCommand, this);
    getServer().getPluginManager().registerEvents(new BotCollisionListener(this, fakePlayerManager), this);
    getServer()
        .getPluginManager()
        .registerEvents(new FakePlayerConnectionMessageListener(fakePlayerManager), this);
    getServer()
        .getPluginManager()
        .registerEvents(new FakePlayerDeathListener(this, fakePlayerManager), this);

    extensionLoader = new ExtensionLoader(this);
    extensionLoader.loadExtensions();

    FppLogger.info("FakePlayerPlugin minimal core enabled.");
  }

  @Override
  public void onDisable() {
    if (extensionLoader != null) {
      extensionLoader.disableExtensions();
      extensionLoader.closeClassLoaders();
    }
    if (fakePlayerManager != null) {
      fakePlayerManager.removeAll();
    }
    instance = null;
  }

  public FakePlayerManager getFakePlayerManager() {
    return fakePlayerManager;
  }

  public CommandManager getCommandManager() {
    return commandManager;
  }

  public FppApi getFppApi() {
    return fppApi;
  }

  public FppApiImpl getFppApiImpl() {
    return fppApi;
  }

  public ExtensionLoader getExtensionLoader() {
    return extensionLoader;
  }

  public SettingGui getSettingGui() {
    return settingGui;
  }

  public BotSettingGui getBotSettingGui() {
    return botSettingGui;
  }

  public HelpGui getHelpGui() {
    return helpGui;
  }

  public InventoryCommand getInventoryCommand() {
    return inventoryCommand;
  }
}
