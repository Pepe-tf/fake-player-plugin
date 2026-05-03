package me.bill.fakePlayerPlugin.gui;

import java.util.ArrayList;
import java.util.List;
import me.bill.fakePlayerPlugin.api.FppAddonCommand;
import me.bill.fakePlayerPlugin.command.CommandManager;
import me.bill.fakePlayerPlugin.command.FppCommand;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public final class HelpGui implements Listener {

  private static final int SIZE = 54;
  private static final int PER_PAGE = 45;

  private final Plugin plugin;
  private final CommandManager commandManager;

  public HelpGui(Plugin plugin, CommandManager commandManager) {
    this.plugin = plugin;
    this.commandManager = commandManager;
  }

  public void open(Player player, int page) {
    List<ItemStack> visible = visibleCommandItems(player);
    int pages = Math.max(1, (int) Math.ceil(visible.size() / (double) PER_PAGE));
    int safePage = Math.max(0, Math.min(page, pages - 1));
    Inventory inv =
        Bukkit.createInventory(new Holder(safePage), SIZE, "FPP Help " + (safePage + 1) + "/" + pages);

    int start = safePage * PER_PAGE;
    int end = Math.min(start + PER_PAGE, visible.size());
    for (int i = start; i < end; i++) {
      inv.setItem(i - start, visible.get(i));
    }

    fillFooter(inv);
    inv.setItem(45, navItem(Material.ARROW, "Previous Page", "Show earlier commands"));
    inv.setItem(49, navItem(Material.BOOK, "FakePlayerPlugin", "Interactive command reference"));
    inv.setItem(53, navItem(Material.ARROW, "Next Page", "Show more commands"));
    player.openInventory(inv);
  }

  @EventHandler
  public void onClick(InventoryClickEvent event) {
    if (!(event.getInventory().getHolder() instanceof Holder holder)) return;
    event.setCancelled(true);
    if (!(event.getWhoClicked() instanceof Player player)) return;
    int slot = event.getRawSlot();
    if (slot == 45) {
      open(player, holder.page() - 1);
    } else if (slot == 53) {
      open(player, holder.page() + 1);
    }
  }

  @EventHandler
  public void onDrag(InventoryDragEvent event) {
    if (event.getInventory().getHolder() instanceof Holder) event.setCancelled(true);
  }

  private List<ItemStack> visibleCommandItems(Player player) {
    List<ItemStack> out = new ArrayList<>();
    for (FppCommand command : commandManager.getCommands()) {
      String permission = command.getPermission();
      if (permission == null || permission.isBlank() || player.hasPermission(permission)) {
        out.add(commandItem(command.getName(), command.getDescription(), command.getUsage()));
      }
    }
    for (FppAddonCommand command : commandManager.getAddonCommands()) {
      String permission = command.getPermission();
      if (permission == null || permission.isBlank() || player.hasPermission(permission)) {
        out.add(commandItem(command.getName(), command.getDescription(), command.getUsage()));
      }
    }
    return out;
  }

  private ItemStack commandItem(String name, String description, String usage) {
    Material icon =
        switch (name.toLowerCase()) {
          case "spawn" -> Material.PLAYER_HEAD;
          case "despawn" -> Material.BARRIER;
          case "list" -> Material.PAPER;
          case "reload" -> Material.REDSTONE;
          case "settings" -> Material.COMPARATOR;
          case "help" -> Material.BOOK;
          default -> Material.NAME_TAG;
        };
    return item(
        icon,
        ChatColor.YELLOW + "/fpp " + name,
        ChatColor.GRAY + description,
        "",
        ChatColor.DARK_GRAY + usage);
  }

  private void fillFooter(Inventory inv) {
    ItemStack glass = item(Material.BLUE_STAINED_GLASS_PANE, " ");
    for (int i = 45; i < SIZE; i++) {
      if (inv.getItem(i) == null) inv.setItem(i, glass);
    }
  }

  private ItemStack navItem(Material material, String name, String lore) {
    return item(material, ChatColor.BLUE + name, ChatColor.AQUA + lore);
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

  private record Holder(int page) implements InventoryHolder {
    @Override
    public @NotNull Inventory getInventory() {
      return Bukkit.createInventory(this, SIZE);
    }
  }
}
