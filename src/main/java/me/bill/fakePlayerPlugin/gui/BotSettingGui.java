package me.bill.fakePlayerPlugin.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import me.bill.fakePlayerPlugin.FakePlayerPlugin;
import me.bill.fakePlayerPlugin.api.FppSettingsItem;
import me.bill.fakePlayerPlugin.api.FppSettingsTab;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayer;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayerManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

public final class BotSettingGui implements Listener {

  private static final int SIZE = 54;
  private static final int PER_PAGE = 45;

  private final FakePlayerPlugin plugin;
  private final FakePlayerManager manager;

  public BotSettingGui(FakePlayerPlugin plugin, FakePlayerManager manager) {
    this.plugin = plugin;
    this.manager = manager;
  }

  public void open(Player player, FakePlayer bot) {
    open(player, bot, 0, 0);
  }

  private void open(Player player, FakePlayer bot, int category, int page) {
    List<Category> categories = categories(player, bot);
    int safeCategory = Math.max(0, Math.min(category, categories.size() - 1));
    Category selected = categories.get(safeCategory);
    List<Entry> entries = selected.entries();
    int pages = Math.max(1, (int) Math.ceil(entries.size() / (double) PER_PAGE));
    int safePage = Math.max(0, Math.min(page, pages - 1));
    Inventory inv =
        Bukkit.createInventory(
            new Holder(bot.getName(), safeCategory, safePage),
            SIZE,
            "Bot Settings - " + bot.getDisplayName());

    int start = safePage * PER_PAGE;
    int end = Math.min(start + PER_PAGE, entries.size());
    for (int i = start; i < end; i++) inv.setItem(i - start, entries.get(i).item());

    fillFooter(inv);
    for (int i = 0; i < Math.min(7, categories.size()); i++) {
      Category current = categories.get(i);
      inv.setItem(46 + i, categoryItem(current, i == safeCategory));
    }
    inv.setItem(45, item(Material.ARROW, ChatColor.AQUA + "Previous Page"));
    inv.setItem(53, item(Material.ARROW, ChatColor.AQUA + "Next Page"));
    player.openInventory(inv);
  }

  @EventHandler
  public void onClick(InventoryClickEvent event) {
    if (!(event.getInventory().getHolder() instanceof Holder holder)) return;
    event.setCancelled(true);
    if (!(event.getWhoClicked() instanceof Player player)) return;
    FakePlayer bot = manager.getByName(holder.botName());
    if (bot == null) {
      player.closeInventory();
      player.sendMessage(ChatColor.RED + "That bot is no longer online.");
      return;
    }

    int slot = event.getRawSlot();
    if (slot < 0 || slot >= SIZE) return;
    List<Category> categories = categories(player, bot);
    if (slot == 45) {
      open(player, bot, holder.category(), holder.page() - 1);
      return;
    }
    if (slot == 53) {
      open(player, bot, holder.category(), holder.page() + 1);
      return;
    }
    if (slot >= 46 && slot <= 52) {
      int category = slot - 46;
      if (category < categories.size()) open(player, bot, category, 0);
      return;
    }

    Category selected = categories.get(Math.max(0, Math.min(holder.category(), categories.size() - 1)));
    int index = holder.page() * PER_PAGE + slot;
    if (index < selected.entries().size()) {
      selected.entries().get(index).click().accept(player);
      open(player, bot, holder.category(), holder.page());
    }
  }

  @EventHandler
  public void onDrag(InventoryDragEvent event) {
    if (event.getInventory().getHolder() instanceof Holder) event.setCancelled(true);
  }

  private List<Category> categories(Player player, FakePlayer bot) {
    List<Category> categories = new ArrayList<>();
    categories.add(new Category("general", "General", Material.PLAYER_HEAD, () -> generalEntries(bot)));
    categories.add(new Category("chat", "Chat", Material.WRITABLE_BOOK, () -> chatEntries(bot)));
    categories.add(new Category("combat", "PvE", Material.IRON_SWORD, () -> pveEntries(bot)));
    if (plugin.getFppApiImpl() != null) {
      for (FppSettingsTab tab : plugin.getFppApiImpl().getBotSettingsTabs()) {
        if (tab.isVisible(player)) categories.add(wrap(tab, player));
      }
    }
    return categories;
  }

  private List<Entry> generalEntries(FakePlayer bot) {
    return List.of(
        toggle("Frozen", Material.PACKED_ICE, bot::isFrozen, bot::setFrozen, "Pause bot physics ticks"),
        action(
            "Teleport Here",
            Material.ENDER_PEARL,
            () -> "Click",
            "Move the bot entity to you",
            player -> {
              Player entity = bot.getPlayer();
              if (entity != null) entity.teleport(player.getLocation());
            }),
        toggle(
            "Respawn On Death",
            Material.TOTEM_OF_UNDYING,
            bot::isRespawnOnDeath,
            bot::setRespawnOnDeath,
            "Respawn instead of despawning after death"),
        cycleGameMode(bot),
        info("Owner", Material.PLAYER_HEAD, bot.getSpawnedBy()));
  }

  private List<Entry> chatEntries(FakePlayer bot) {
    return List.of(
        toggle("Chat Enabled", Material.OAK_SIGN, bot::isChatEnabled, bot::setChatEnabled, "Allow bot chat features"),
        textCycle("Chat Tier", Material.BOOK, bot::getChatTier, bot::setChatTier, "default", "basic", "smart"),
        textCycle("AI Personality", Material.AMETHYST_SHARD, bot::getAiPersonality, bot::setAiPersonality, "default", "friendly", "grumpy", "noob"));
  }

  private List<Entry> pveEntries(FakePlayer bot) {
    return List.of(
        toggle("PvE Enabled", Material.IRON_SWORD, bot::isPveEnabled, bot::setPveEnabled, "Allow PvE automation flags"),
        numberCycle("PvE Range", Material.TARGET, bot::getPveRange, bot::setPveRange, 8.0, 16.0, 24.0, 32.0),
        textCycle("PvE Priority", Material.CROSSBOW, bot::getPvePriority, bot::setPvePriority, "nearest", "lowest_health", "highest_health"));
  }

  private Category wrap(FppSettingsTab tab, Player player) {
    return new Category(
        tab.getId(),
        tab.getLabel(),
        tab.getActiveMaterial(),
        () -> tab.getItems(player).stream().map(this::wrap).toList());
  }

  private Entry wrap(FppSettingsItem item) {
    return new Entry(
        item.getLabel(),
        item.getIcon(),
        () -> item.getValue() == null ? "" : item.getValue(),
        item.getDescription(),
        item::onClick);
  }

  private Entry toggle(
      String label, Material icon, Supplier<Boolean> getter, Consumer<Boolean> setter, String description) {
    return new Entry(
        label,
        icon,
        () -> getter.get() ? "Enabled" : "Disabled",
        description,
        player -> setter.accept(!getter.get()));
  }

  private Entry textCycle(
      String label, Material icon, Supplier<String> getter, Consumer<String> setter, String... values) {
    return new Entry(
        label,
        icon,
        () -> getter.get() == null || getter.get().isBlank() ? "default" : getter.get(),
        "Click to cycle",
        player -> {
          String current = getter.get() == null || getter.get().isBlank() ? values[0] : getter.get();
          int index = 0;
          for (int i = 0; i < values.length; i++) {
            if (values[i].equalsIgnoreCase(current)) index = i;
          }
          String next = values[(index + 1) % values.length];
          setter.accept("default".equalsIgnoreCase(next) ? null : next);
        });
  }

  private Entry numberCycle(
      String label, Material icon, Supplier<Double> getter, Consumer<Double> setter, double... values) {
    return new Entry(
        label,
        icon,
        () -> String.valueOf(getter.get()),
        "Click to cycle",
        player -> {
          double current = getter.get();
          int index = 0;
          for (int i = 0; i < values.length; i++) {
            if (Double.compare(values[i], current) == 0) index = i;
          }
          setter.accept(values[(index + 1) % values.length]);
        });
  }

  private Entry cycleGameMode(FakePlayer bot) {
    return new Entry(
        "Game Mode",
        Material.DIAMOND_PICKAXE,
        () -> bot.getPlayer() == null ? "Offline" : bot.getPlayer().getGameMode().name(),
        "Click to cycle Survival/Creative/Adventure",
        player -> {
          Player entity = bot.getPlayer();
          if (entity == null) return;
          GameMode mode =
              switch (entity.getGameMode()) {
                case SURVIVAL -> GameMode.CREATIVE;
                case CREATIVE -> GameMode.ADVENTURE;
                default -> GameMode.SURVIVAL;
              };
          entity.setGameMode(mode);
        });
  }

  private Entry action(
      String label, Material icon, Supplier<String> value, String description, Consumer<Player> click) {
    return new Entry(label, icon, value, description, click);
  }

  private Entry info(String label, Material icon, String value) {
    return new Entry(label, icon, () -> value, "Read-only", player -> {});
  }

  private ItemStack categoryItem(Category category, boolean active) {
    return item(
        category.icon(),
        (active ? ChatColor.AQUA : ChatColor.BLUE) + category.label(),
        active ? ChatColor.WHITE + "Selected" : ChatColor.AQUA + "Click to open");
  }

  private void fillFooter(Inventory inv) {
    ItemStack glass = item(Material.BLUE_STAINED_GLASS_PANE, " ");
    for (int i = 45; i < SIZE; i++) {
      if (inv.getItem(i) == null) inv.setItem(i, glass);
    }
  }

  private static ItemStack item(Material material, String name, String... lore) {
    ItemStack stack = new ItemStack(material);
    ItemMeta meta = stack.getItemMeta();
    if (meta != null) {
      meta.setDisplayName(name);
      meta.setLore(List.of(lore));
      stack.setItemMeta(meta);
    }
    return stack;
  }

  private record Category(String id, String label, Material icon, Supplier<List<Entry>> supplier) {
    List<Entry> entries() {
      return supplier.get();
    }
  }

  private record Entry(
      String label, Material icon, Supplier<String> value, String description, Consumer<Player> click) {
    ItemStack item() {
      return BotSettingGui.item(
          icon,
          ChatColor.YELLOW + label,
          ChatColor.GRAY + description,
          "",
          ChatColor.WHITE + "Value: " + ChatColor.AQUA + value.get(),
          ChatColor.DARK_GRAY + "Click to change");
    }
  }

  private record Holder(String botName, int category, int page) implements InventoryHolder {
    @Override
    public @NotNull Inventory getInventory() {
      return Bukkit.createInventory(this, SIZE);
    }
  }
}
