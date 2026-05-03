package me.bill.fakePlayerPlugin.command;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import me.bill.fakePlayerPlugin.api.event.FppBotDespawnEvent;
import me.bill.fakePlayerPlugin.config.Config;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayer;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayerManager;
import me.bill.fakePlayerPlugin.gui.BotSettingGui;
import me.bill.fakePlayerPlugin.permission.Perm;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public final class InventoryCommand implements FppCommand, Listener {

  private static final int GUI_SIZE = 54;
  private static final int[] GUI_TO_BOT = new int[GUI_SIZE];
  private static final Set<Integer> DECO = new HashSet<>();
  private static final Set<Integer> EQUIP_SLOTS = Set.of(45, 46, 47, 48, 50);
  private static final Material LABEL_MAT = Material.LIGHT_BLUE_STAINED_GLASS_PANE;
  private static final Material BLANK_MAT = Material.BLUE_STAINED_GLASS_PANE;

  static {
    Arrays.fill(GUI_TO_BOT, -1);
    for (int i = 0; i < 27; i++) GUI_TO_BOT[i] = i + 9;
    for (int i = 0; i < 9; i++) GUI_TO_BOT[27 + i] = i;
    for (int i = 36; i <= 44; i++) DECO.add(i);
    GUI_TO_BOT[45] = 36;
    GUI_TO_BOT[46] = 37;
    GUI_TO_BOT[47] = 38;
    GUI_TO_BOT[48] = 39;
    DECO.add(49);
    GUI_TO_BOT[50] = 40;
    DECO.add(51);
    DECO.add(52);
    DECO.add(53);
  }

  private final FakePlayerManager manager;
  private final Plugin plugin;
  private final BotSettingGui botSettingGui;
  private final Map<UUID, UUID> sessions = new ConcurrentHashMap<>();
  private final Map<Inventory, UUID> invToBot = new ConcurrentHashMap<>();
  private final Map<UUID, UUID> botLocks = new ConcurrentHashMap<>();

  public InventoryCommand(FakePlayerManager manager, Plugin plugin, BotSettingGui botSettingGui) {
    this.manager = manager;
    this.plugin = plugin;
    this.botSettingGui = botSettingGui;
  }

  @Override
  public String getName() {
    return "inventory";
  }

  @Override
  public List<String> getAliases() {
    return List.of("inv");
  }

  @Override
  public String getDescription() {
    return "Open a bot's inventory.";
  }

  @Override
  public String getUsage() {
    return "/fpp inventory <bot>";
  }

  @Override
  public String getPermission() {
    return Perm.INVENTORY;
  }

  @Override
  public boolean execute(CommandSender sender, String[] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage("Only players can open bot inventories.");
      return true;
    }
    if (args.length == 0) {
      sender.sendMessage("Usage: /fpp inventory <bot>");
      return true;
    }
    FakePlayer fp = manager.getByName(args[0]);
    if (fp == null) {
      sender.sendMessage("Bot not found: " + args[0]);
      return true;
    }
    openGui(player, fp);
    return true;
  }

  @Override
  public List<String> tabComplete(CommandSender sender, String[] args) {
    if (args.length == 1) {
      String lower = args[0].toLowerCase();
      return manager.getActivePlayers().stream()
          .map(FakePlayer::getName)
          .filter(name -> name.toLowerCase().startsWith(lower))
          .toList();
    }
    return List.of();
  }

  public void openGui(Player viewer, FakePlayer fp) {
    Player bot = fp.getPlayer();
    UUID owner = botLocks.putIfAbsent(fp.getUuid(), viewer.getUniqueId());
    if (owner != null && !owner.equals(viewer.getUniqueId())) {
      viewer.sendMessage(ChatColor.RED + "That bot inventory is already open.");
      return;
    }
    if (bot == null || !bot.isOnline() || fp.isBodyless()) {
      botLocks.remove(fp.getUuid(), viewer.getUniqueId());
      viewer.sendMessage(ChatColor.RED + "That bot has no active body.");
      return;
    }

    Inventory gui = Bukkit.createInventory(new Holder(fp.getUuid()), GUI_SIZE, "Bot Inventory - " + fp.getDisplayName());
    PlayerInventory botInv = bot.getInventory();
    for (int slot = 0; slot < GUI_SIZE; slot++) {
      if (DECO.contains(slot)) {
        gui.setItem(slot, deco(slot));
      } else {
        int botSlot = GUI_TO_BOT[slot];
        if (botSlot >= 0) {
          ItemStack item = botInv.getItem(botSlot);
          if (item != null && item.getType() != Material.AIR) gui.setItem(slot, item.clone());
        }
      }
    }
    sessions.put(viewer.getUniqueId(), fp.getUuid());
    invToBot.put(gui, fp.getUuid());
    viewer.openInventory(gui);
    bot.getWorld().playSound(bot.getLocation(), Sound.BLOCK_CHEST_OPEN, SoundCategory.BLOCKS, 0.5f, 1.0f);
  }

  public boolean isInventoryOpen(UUID botUuid) {
    return botLocks.containsKey(botUuid);
  }

  @EventHandler(priority = EventPriority.LOW)
  public void onInventoryClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player player)) return;
    Inventory top = event.getView().getTopInventory();
    UUID botUuid = invToBot.get(top);
    if (botUuid == null) return;
    int rawSlot = event.getRawSlot();
    boolean inTop = rawSlot >= 0 && rawSlot < GUI_SIZE;
    if (inTop && DECO.contains(rawSlot)) {
      event.setCancelled(true);
      return;
    }

    FakePlayer fp = manager.getByUuid(botUuid);
    if (fp == null || fp.getPlayer() == null || fp.isBodyless()) {
      event.setCancelled(true);
      player.closeInventory();
      return;
    }

    if (inTop && EQUIP_SLOTS.contains(rawSlot)) {
      ItemStack incoming = incomingItem(event);
      if (incoming != null && incoming.getType() != Material.AIR && !isCompatible(rawSlot, incoming)) {
        event.setCancelled(true);
        player.sendActionBar(ChatColor.RED + "That item does not fit this equipment slot.");
        return;
      }
    }
    scheduleSync(botUuid, top);
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onInventoryDrag(InventoryDragEvent event) {
    Inventory top = event.getView().getTopInventory();
    UUID botUuid = invToBot.get(top);
    if (botUuid == null) return;
    if (event.getRawSlots().stream().anyMatch(DECO::contains)) {
      event.setCancelled(true);
      return;
    }
    for (int slot : event.getRawSlots()) {
      if (EQUIP_SLOTS.contains(slot) && !isCompatible(slot, event.getCursor())) {
        event.setCancelled(true);
        if (event.getWhoClicked() instanceof Player player) {
          player.sendActionBar(ChatColor.RED + "That item does not fit this equipment slot.");
        }
        return;
      }
    }
    scheduleSync(botUuid, top);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onInventoryClose(InventoryCloseEvent event) {
    if (!(event.getPlayer() instanceof Player viewer)) return;
    Inventory top = event.getView().getTopInventory();
    UUID botUuid = invToBot.remove(top);
    if (botUuid == null) return;
    botLocks.remove(botUuid, viewer.getUniqueId());
    sessions.remove(viewer.getUniqueId());
    FakePlayer fp = manager.getByUuid(botUuid);
    if (fp != null && fp.getPlayer() != null && !fp.isBodyless()) {
      syncToBotInventory(top, fp.getPlayer().getInventory());
      fp.getPlayer().getWorld().playSound(fp.getPlayer().getLocation(), Sound.BLOCK_CHEST_CLOSE, SoundCategory.BLOCKS, 0.5f, 1.0f);
    }
  }

  @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
  public void onRightClickBot(PlayerInteractAtEntityEvent event) {
    if (event.getHand() != EquipmentSlot.HAND) return;
    if (!(event.getRightClicked() instanceof Player botPlayer)) return;
    FakePlayer fp = manager.getByUuid(botPlayer.getUniqueId());
    if (fp == null || fp.isBodyless()) return;

    Player player = event.getPlayer();
    if (player.isSneaking() && Config.isBotShiftRightClickSettingsEnabled() && player.hasPermission(Perm.SETTINGS)) {
      event.setCancelled(true);
      botSettingGui.open(player, fp);
      return;
    }
    if (!Config.isBotRightClickInventoryEnabled() || !player.hasPermission(Perm.INVENTORY)) return;
    event.setCancelled(true);
    openGui(player, fp);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onViewerQuit(PlayerQuitEvent event) {
    UUID viewerUuid = event.getPlayer().getUniqueId();
    UUID botUuid = sessions.remove(viewerUuid);
    if (botUuid != null) botLocks.remove(botUuid, viewerUuid);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onBotDespawn(FppBotDespawnEvent event) {
    closeViewers(event.getBot().getUuid());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onBotDeath(PlayerDeathEvent event) {
    FakePlayer fp = manager.getByUuid(event.getEntity().getUniqueId());
    if (fp != null) closeViewers(fp.getUuid());
  }

  public void closeViewers(UUID botUuid) {
    botLocks.remove(botUuid);
    for (Map.Entry<UUID, UUID> entry : new HashMap<>(sessions).entrySet()) {
      if (!botUuid.equals(entry.getValue())) continue;
      Player viewer = Bukkit.getPlayer(entry.getKey());
      if (viewer != null) viewer.closeInventory();
      sessions.remove(entry.getKey());
    }
  }

  private void scheduleSync(UUID botUuid, Inventory gui) {
    Bukkit.getScheduler()
        .runTask(
            plugin,
            () -> {
              FakePlayer fp = manager.getByUuid(botUuid);
              if (fp != null && fp.getPlayer() != null && !fp.isBodyless()) {
                syncToBotInventory(gui, fp.getPlayer().getInventory());
              }
            });
  }

  private void syncToBotInventory(Inventory gui, PlayerInventory botInv) {
    for (int guiSlot = 0; guiSlot < GUI_SIZE; guiSlot++) {
      if (DECO.contains(guiSlot)) continue;
      int botSlot = GUI_TO_BOT[guiSlot];
      if (botSlot < 0) continue;
      ItemStack item = gui.getItem(guiSlot);
      botInv.setItem(botSlot, item == null || item.getType() == Material.AIR ? null : item.clone());
    }
  }

  private static ItemStack incomingItem(InventoryClickEvent event) {
    InventoryAction action = event.getAction();
    return switch (action) {
      case PLACE_ALL, PLACE_ONE, PLACE_SOME, SWAP_WITH_CURSOR -> event.getCursor();
      case HOTBAR_SWAP -> {
        if (event.getWhoClicked() instanceof Player p) {
          int button = event.getHotbarButton();
          yield button >= 0 ? p.getInventory().getItem(button) : event.getCursor();
        }
        yield null;
      }
      default -> null;
    };
  }

  private static boolean isCompatible(int guiSlot, ItemStack item) {
    if (item == null || item.getType() == Material.AIR) return true;
    String name = item.getType().name();
    return switch (guiSlot) {
      case 45 -> name.endsWith("_BOOTS");
      case 46 -> name.endsWith("_LEGGINGS");
      case 47 -> name.endsWith("_CHESTPLATE") || name.equals("ELYTRA");
      default -> true;
    };
  }

  private static ItemStack deco(int slot) {
    return switch (slot) {
      case 36 -> label("Boots", "Accepts boots");
      case 37 -> label("Leggings", "Accepts leggings");
      case 38 -> label("Chest", "Accepts chestplates or elytra");
      case 39 -> label("Helmet", "Accepts any helmet slot item");
      case 41 -> label("Offhand", "Accepts any item");
      default -> pane(BLANK_MAT, " ");
    };
  }

  private static ItemStack label(String name, String lore) {
    return pane(LABEL_MAT, ChatColor.AQUA + name, ChatColor.GRAY + lore);
  }

  private static ItemStack pane(Material material, String name, String... lore) {
    ItemStack item = new ItemStack(material);
    ItemMeta meta = item.getItemMeta();
    if (meta != null) {
      meta.setDisplayName(name);
      meta.setLore(List.of(lore));
      item.setItemMeta(meta);
    }
    return item;
  }

  private record Holder(UUID botUuid) implements InventoryHolder {
    @Override
    public @NotNull Inventory getInventory() {
      return Bukkit.createInventory(this, GUI_SIZE);
    }
  }
}
