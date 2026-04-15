package me.bill.fakePlayerPlugin.gui;

import me.bill.fakePlayerPlugin.command.CommandManager;
import me.bill.fakePlayerPlugin.command.FppCommand;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.*;

public final class HelpGui implements Listener {

    private static final TextColor ACCENT = TextColor.fromHexString("#0079FF");
    private static final TextColor ON_GREEN = TextColor.fromHexString("#66CC66");
    private static final TextColor ORANGE = TextColor.fromHexString("#FFA500");
    private static final TextColor DARK_GRAY = NamedTextColor.DARK_GRAY;
    private static final TextColor GRAY = NamedTextColor.GRAY;
    private static final TextColor WHITE = NamedTextColor.WHITE;
    private static final TextColor OFF_RED = NamedTextColor.RED;
    private static final TextColor YELLOW = NamedTextColor.YELLOW;

    private static final int SIZE = 54;
    private static final int CMDS_PER_PAGE = 45;
    private static final int SLOT_PREV = 46;
    private static final int SLOT_PAGE = 49;
    private static final int SLOT_NEXT = 52;
    private static final int SLOT_CLOSE = 53;

    private final Plugin plugin;
    private final CommandManager commandManager;

    private final Map<UUID, Integer> sessions = new HashMap<>();

    public HelpGui(Plugin plugin, CommandManager commandManager) {
        this.plugin = plugin;
        this.commandManager = commandManager;
    }

    public void open(Player player) {
        sessions.put(player.getUniqueId(), 0);
        build(player);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof Holder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null) return;
        if (!event.getClickedInventory().equals(event.getInventory())) return;

        Integer page = sessions.get(holder.uuid);
        if (page == null) return;

        int slot = event.getSlot();
        int totalPages = totalPages(player);

        if (slot == SLOT_PREV && page > 0) {
            sessions.put(holder.uuid, page - 1);
            playClick(player, 1.0f);
            build(player);
            return;
        }
        if (slot == SLOT_NEXT && page < totalPages - 1) {
            sessions.put(holder.uuid, page + 1);
            playClick(player, 1.0f);
            build(player);
            return;
        }
        if (slot == SLOT_CLOSE) {
            playClick(player, 0.8f);
            player.closeInventory();
            return;
        }

        if (slot < CMDS_PER_PAGE) {
            int idx = page * CMDS_PER_PAGE + slot;
            if (idx < visibleCommands(player).size()) playClick(player, 1.3f);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof Holder)) return;
        sessions.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        sessions.remove(event.getPlayer().getUniqueId());
    }

    private void build(Player player) {
        Integer raw = sessions.get(player.getUniqueId());
        if (raw == null) return;

        List<FppCommand> visible = visibleCommands(player);
        int totalPages = Math.max(1, (int) Math.ceil(visible.size() / (double) CMDS_PER_PAGE));
        int page = Math.min(raw, totalPages - 1);
        sessions.put(player.getUniqueId(), page);

        Holder holder = new Holder(player.getUniqueId());
        Component title =
                Component.empty()
                        .decoration(TextDecoration.ITALIC, false)
                        .append(Component.text("[").color(DARK_GRAY))
                        .append(Component.text("ꜰᴘᴘ").color(ACCENT))
                        .append(Component.text("]  ").color(DARK_GRAY))
                        .append(
                                Component.text("ᴄᴏᴍᴍᴀɴᴅꜱ")
                                        .color(DARK_GRAY)
                                        .decoration(TextDecoration.BOLD, true));

        Inventory inv = Bukkit.createInventory(holder, SIZE, title);

        int start = page * CMDS_PER_PAGE;
        int end = Math.min(start + CMDS_PER_PAGE, visible.size());
        for (int i = start; i < end; i++) {
            inv.setItem(i - start, buildCommandItem(visible.get(i)));
        }

        ItemStack filler = glassFiller();
        for (int s = 45; s <= 53; s++) inv.setItem(s, filler);

        inv.setItem(SLOT_PREV, page > 0 ? buildArrow(false) : filler);
        inv.setItem(SLOT_PAGE, buildPageItem(page + 1, totalPages));
        inv.setItem(SLOT_NEXT, page < totalPages - 1 ? buildArrow(true) : filler);
        inv.setItem(SLOT_CLOSE, buildCloseButton());

        player.openInventory(inv);
    }

    private List<FppCommand> visibleCommands(Player player) {
        return commandManager.getCommands().stream().filter(cmd -> cmd.canUse(player)).toList();
    }

    private int totalPages(Player player) {
        return Math.max(
                1, (int) Math.ceil(visibleCommands(player).size() / (double) CMDS_PER_PAGE));
    }

    private static ItemStack buildCommandItem(FppCommand cmd) {
        ItemStack item = new ItemStack(iconFor(cmd.getName()));
        ItemMeta meta = item.getItemMeta();

        meta.displayName(
                Component.empty()
                        .decoration(TextDecoration.ITALIC, false)
                        .append(
                                Component.text("/fpp ")
                                        .color(DARK_GRAY)
                                        .decoration(TextDecoration.BOLD, true))
                        .append(
                                Component.text(cmd.getName())
                                        .color(ACCENT)
                                        .decoration(TextDecoration.BOLD, true)));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());

        String desc = cmd.getDescription();
        if (desc != null && !desc.isBlank()) {
            for (String line : wrapText(desc, 38)) {
                lore.add(
                        Component.empty()
                                .decoration(TextDecoration.ITALIC, false)
                                .append(Component.text(line).color(GRAY)));
            }
            lore.add(Component.empty());
        }

        String usage = cmd.getUsage();
        if (usage != null && !usage.isBlank()) {
            lore.add(
                    Component.empty()
                            .decoration(TextDecoration.ITALIC, false)
                            .append(Component.text("ᴜꜱᴀɢᴇ").color(DARK_GRAY)));

            String[] modes = usage.split("\\s+\\|\\s+");
            for (String mode : modes) {
                mode = mode.trim();
                if (mode.isBlank()) continue;
                lore.add(
                        Component.empty()
                                .decoration(TextDecoration.ITALIC, false)
                                .append(Component.text(" · ").color(DARK_GRAY))
                                .append(
                                        Component.text("/fpp " + cmd.getName() + " ")
                                                .color(DARK_GRAY))
                                .append(Component.text(mode).color(WHITE)));
            }
            lore.add(Component.empty());
        }

        String perm = cmd.getPermission();
        if (perm != null && !perm.isBlank()) {
            lore.add(
                    Component.empty()
                            .decoration(TextDecoration.ITALIC, false)
                            .append(Component.text("ᴘᴇʀᴍ  ").color(DARK_GRAY))
                            .append(Component.text(perm).color(YELLOW)));
            lore.add(Component.empty());
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static Material iconFor(String name) {
        return switch (name.toLowerCase()) {
            case "spawn" -> Material.PLAYER_HEAD;
            case "despawn", "delete" -> Material.BONE;
            case "list" -> Material.BOOK;
            case "help" -> Material.KNOWLEDGE_BOOK;
            case "info" -> Material.MAP;
            case "chat" -> Material.PAPER;
            case "reload" -> Material.NETHER_STAR;
            case "freeze" -> Material.PACKED_ICE;
            case "stats" -> Material.CLOCK;
            case "tp" -> Material.ENDER_PEARL;
            case "tph" -> Material.ENDER_EYE;
            case "rank" -> Material.GOLDEN_CHESTPLATE;
            case "lpinfo" -> Material.GOLDEN_HELMET;
            case "move" -> Material.COMPASS;
            case "inventory", "inv" -> Material.CHEST;
            case "cmd" -> Material.COMMAND_BLOCK;
            case "mine" -> Material.DIAMOND_PICKAXE;
            case "use" -> Material.WOODEN_AXE;
            case "swap" -> Material.RECOVERY_COMPASS;
            case "peaks" -> Material.SUNFLOWER;
            case "settings" -> Material.COMPARATOR;
            case "migrate" -> Material.ANVIL;
            case "sync" -> Material.OBSERVER;
            case "alert" -> Material.BELL;
            case "xp" -> Material.EXPERIENCE_BOTTLE;
            default -> Material.PAPER;
        };
    }

    private static ItemStack buildArrow(boolean next) {
        Material mat =
                next ? Material.LIME_STAINED_GLASS_PANE : Material.MAGENTA_STAINED_GLASS_PANE;
        String lbl = next ? "▶  ɴᴇxᴛ ᴘᴀɢᴇ" : "◄  ᴘʀᴇᴠ ᴘᴀɢᴇ";
        TextColor col = next ? ON_GREEN : ORANGE;
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(
                Component.empty()
                        .decoration(TextDecoration.ITALIC, false)
                        .append(
                                Component.text(lbl)
                                        .color(col)
                                        .decoration(TextDecoration.BOLD, true)));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildPageItem(int page, int total) {
        ItemStack item = new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(
                Component.empty()
                        .decoration(TextDecoration.ITALIC, false)
                        .append(Component.text("ᴘᴀɢᴇ  ").color(DARK_GRAY))
                        .append(
                                Component.text(page + " / " + total)
                                        .color(WHITE)
                                        .decoration(TextDecoration.BOLD, true)));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildCloseButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(
                Component.empty()
                        .decoration(TextDecoration.ITALIC, false)
                        .append(
                                Component.text("✕  ᴄʟᴏꜱᴇ")
                                        .color(OFF_RED)
                                        .decoration(TextDecoration.BOLD, true)));
        meta.lore(
                List.of(
                        Component.empty()
                                .decoration(TextDecoration.ITALIC, false)
                                .append(Component.text("ᴄʟᴏꜱᴇ ᴛʜᴇ ʜᴇʟᴘ ᴍᴇɴᴜ.").color(DARK_GRAY))));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack glassFiller() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(" ").decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private static void playClick(Player player, float pitch) {
        player.playSound(
                player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 0.5f, pitch);
    }

    private static List<String> wrapText(String text, int maxLen) {
        if (text.length() <= maxLen) return List.of(text);
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!sb.isEmpty() && sb.length() + 1 + word.length() > maxLen) {
                lines.add(sb.toString());
                sb.setLength(0);
            }
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(word);
        }
        if (!sb.isEmpty()) lines.add(sb.toString());
        return lines;
    }

    public static final class Holder implements InventoryHolder {
        public final UUID uuid;

        Holder(UUID uuid) {
            this.uuid = uuid;
        }

        @SuppressWarnings("NullableProblems")
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
