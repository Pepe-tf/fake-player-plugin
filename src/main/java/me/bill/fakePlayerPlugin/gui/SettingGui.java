package me.bill.fakePlayerPlugin.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import me.bill.fakePlayerPlugin.FakePlayerPlugin;
import me.bill.fakePlayerPlugin.api.FppSettingsItem;
import me.bill.fakePlayerPlugin.api.FppSettingsTab;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
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

public final class SettingGui implements Listener {

  private static final int SIZE = 54;
  private static final int PER_PAGE = 45;

  private final FakePlayerPlugin plugin;

  public SettingGui(FakePlayerPlugin plugin) {
    this.plugin = plugin;
  }

  public void open(Player player, int tab, int page) {
    List<Tab> tabs = tabs(player);
    int safeTab = Math.max(0, Math.min(tab, tabs.size() - 1));
    Tab selected = tabs.get(safeTab);
    List<Entry> entries = selected.entries();
    int pages = Math.max(1, (int) Math.ceil(entries.size() / (double) PER_PAGE));
    int safePage = Math.max(0, Math.min(page, pages - 1));
    Inventory inv =
        Bukkit.createInventory(
            new Holder(safeTab, safePage), SIZE, "FPP Settings - " + selected.label());

    int start = safePage * PER_PAGE;
    int end = Math.min(start + PER_PAGE, entries.size());
    for (int i = start; i < end; i++) inv.setItem(i - start, entries.get(i).item());

    fillFooter(inv);
    for (int i = 0; i < Math.min(7, tabs.size()); i++) {
      Tab current = tabs.get(i);
      inv.setItem(46 + i, tabItem(current, i == safeTab));
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
    int slot = event.getRawSlot();
    if (slot < 0 || slot >= SIZE) return;

    List<Tab> tabs = tabs(player);
    if (slot == 45) {
      open(player, holder.tab(), holder.page() - 1);
      return;
    }
    if (slot == 53) {
      open(player, holder.tab(), holder.page() + 1);
      return;
    }
    if (slot >= 46 && slot <= 52) {
      int tab = slot - 46;
      if (tab < tabs.size()) open(player, tab, 0);
      return;
    }
    Tab selected = tabs.get(Math.max(0, Math.min(holder.tab(), tabs.size() - 1)));
    int index = holder.page() * PER_PAGE + slot;
    if (index < selected.entries().size()) {
      selected.entries().get(index).click().accept(player);
      open(player, holder.tab(), holder.page());
    }
  }

  @EventHandler
  public void onDrag(InventoryDragEvent event) {
    if (event.getInventory().getHolder() instanceof Holder) event.setCancelled(true);
  }

  private List<Tab> tabs(Player player) {
    List<Tab> tabs = new ArrayList<>();
    tabs.add(new Tab("general", "General", Material.COMPARATOR, this::generalEntries));
    tabs.add(new Tab("bots", "Bots", Material.PLAYER_HEAD, this::botEntries));
    tabs.add(new Tab("death", "Death", Material.TOTEM_OF_UNDYING, this::deathEntries));
    tabs.add(new Tab("extensions", "Extensions", Material.CHEST, () -> extensionEntries(player)));
    if (plugin.getFppApiImpl() != null) {
      for (FppSettingsTab tab : plugin.getFppApiImpl().getSettingsTabs()) {
        if (tab.isVisible(player)) tabs.add(wrap(tab, player));
      }
    }
    return tabs;
  }

  private List<Entry> generalEntries() {
    return List.of(
        booleanConfig("debug", "Debug Logging", Material.REDSTONE_TORCH, "Verbose plugin logging"),
        booleanConfig("body.pushable", "Body Pushable", Material.SLIME_BALL, "Bots receive knockback"),
        action(
            "Save Config",
            Material.WRITABLE_BOOK,
            "Persist the current config.yml",
            player -> {
              plugin.saveConfig();
              player.sendMessage(ChatColor.GREEN + "FPP config saved.");
            }),
        action(
            "Reload Plugin Config",
            Material.CLOCK,
            "Reload config.yml from disk",
            player -> {
              plugin.reloadConfig();
              player.sendMessage(ChatColor.GREEN + "FPP config reloaded.");
            }));
  }

  private List<Entry> deathEntries() {
    return List.of(
        booleanConfig(
            "death.respawn-on-death",
            "Respawn On Death",
            Material.TOTEM_OF_UNDYING,
            "New bots respawn instead of despawning"),
        booleanConfig(
            "death.suppress-drops",
            "Suppress Drops",
            Material.HOPPER,
            "Clear bot drops and XP on death"),
        booleanConfig(
            "messages.death-message",
            "Death Messages",
            Material.OAK_SIGN,
            "Show vanilla bot death messages"),
        intCycle(
            "death.respawn-delay",
            "Respawn Delay",
            Material.CLOCK,
            "Ticks before respawning",
            20,
            60,
            100,
            200),
        intCycle(
            "death.despawn-delay",
            "Despawn Delay",
            Material.SKELETON_SKULL,
            "Ticks before dead bots are removed",
            1,
            20,
            40,
            100));
  }

  private List<Entry> botEntries() {
    return List.of(
        info("Active Bots", Material.PLAYER_HEAD, String.valueOf(plugin.getFakePlayerManager().getActivePlayers().size())),
        info("Name Pool", Material.NAME_TAG, String.valueOf(plugin.getConfig().getStringList("bot-names").size())),
        info("Body Physics", Material.IRON_BOOTS, plugin.getConfig().getBoolean("body.pushable", true) ? "Pushable" : "Static"));
  }

  private List<Entry> extensionEntries(Player player) {
    List<Entry> entries = new ArrayList<>();
    int global = plugin.getFppApiImpl() == null ? 0 : plugin.getFppApiImpl().getSettingsTabs().size();
    int bot = plugin.getFppApiImpl() == null ? 0 : plugin.getFppApiImpl().getBotSettingsTabs().size();
    entries.add(info("Global Tabs", Material.MAP, String.valueOf(global)));
    entries.add(info("Bot Tabs", Material.MAP, String.valueOf(bot)));
    return entries;
  }

  private Tab wrap(FppSettingsTab tab, Player player) {
    return new Tab(
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

  private Entry booleanConfig(String path, String label, Material icon, String description) {
    return new Entry(
        label,
        icon,
        () -> plugin.getConfig().getBoolean(path, false) ? "Enabled" : "Disabled",
        description,
        player -> {
          FileConfiguration cfg = plugin.getConfig();
          cfg.set(path, !cfg.getBoolean(path, false));
          plugin.saveConfig();
        });
  }

  private Entry intCycle(String path, String label, Material icon, String description, int... values) {
    return new Entry(
        label,
        icon,
        () -> String.valueOf(plugin.getConfig().getInt(path, values[0])),
        description,
        player -> {
          int current = plugin.getConfig().getInt(path, values[0]);
          int index = 0;
          for (int i = 0; i < values.length; i++) {
            if (values[i] == current) index = i;
          }
          plugin.getConfig().set(path, values[(index + 1) % values.length]);
          plugin.saveConfig();
        });
  }

  private Entry action(String label, Material icon, String description, Consumer<Player> click) {
    return new Entry(label, icon, () -> "Click", description, click);
  }

  private Entry info(String label, Material icon, String value) {
    return new Entry(label, icon, () -> value, "Read-only", player -> {});
  }

  private ItemStack tabItem(Tab tab, boolean active) {
    return item(
        tab.icon(),
        (active ? ChatColor.AQUA : ChatColor.BLUE) + tab.label(),
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

  private record Tab(String id, String label, Material icon, Supplier<List<Entry>> supplier) {
    List<Entry> entries() {
      return supplier.get();
    }
  }

  private record Entry(
      String label, Material icon, Supplier<String> value, String description, Consumer<Player> click) {
    ItemStack item() {
      return SettingGui.item(
          icon,
          ChatColor.YELLOW + label,
          ChatColor.GRAY + description,
          "",
          ChatColor.WHITE + "Value: " + ChatColor.AQUA + value.get(),
          ChatColor.DARK_GRAY + "Click to change");
    }
  }

  private record Holder(int tab, int page) implements InventoryHolder {
    @Override
    public @NotNull Inventory getInventory() {
      return Bukkit.createInventory(this, SIZE);
    }
  }
}
