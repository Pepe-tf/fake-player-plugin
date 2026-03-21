package me.bill.fakePlayerPlugin.command;

import me.bill.fakePlayerPlugin.FakePlayerPlugin;
import me.bill.fakePlayerPlugin.config.Config;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayer;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayerManager;
import me.bill.fakePlayerPlugin.lang.Lang;
import me.bill.fakePlayerPlugin.permission.Perm;
import me.bill.fakePlayerPlugin.util.LuckPermsHelper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Diagnostic command that shows LuckPerms integration status and how weights
 * are being applied to bots. Helps admins troubleshoot tab-list ordering.
 */
public class LpInfoCommand implements FppCommand {

    private final FakePlayerPlugin plugin;
    private final FakePlayerManager manager;

    public LpInfoCommand(FakePlayerPlugin plugin, FakePlayerManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override public String getName() { return "lpinfo"; }
    @Override public String getUsage() { return "[bot-name]"; }
    @Override public String getDescription() {
        return "Show LuckPerms integration status and bot tab-list ordering info.";
    }
    @Override public String getPermission() { return Perm.ALL; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Component header = Component.text("═══════ ", NamedTextColor.DARK_GRAY)
                .append(Component.text("LuckPerms Info", NamedTextColor.BLUE))
                .append(Component.text(" ═══════", NamedTextColor.DARK_GRAY));
        sender.sendMessage(header);

        // LP availability
        boolean lpAvailable = LuckPermsHelper.isAvailable();
        sender.sendMessage(Component.text("LP Available: ", NamedTextColor.GRAY)
                .append(Component.text(lpAvailable ? "YES ✓" : "NO", 
                        lpAvailable ? NamedTextColor.GREEN : NamedTextColor.RED)));

        if (!lpAvailable) {
            sender.sendMessage(Component.text("Install LuckPerms to enable prefix/weight ordering.", 
                    NamedTextColor.GOLD));
            return true;
        }

        // Config settings
        sender.sendMessage(Component.text("─────── Config ───────", NamedTextColor.DARK_GRAY));
        status(sender, "use-prefix", Config.luckpermsUsePrefix());
        status(sender, "weight-ordering", Config.luckpermsWeightOrderingEnabled());
        sender.sendMessage(Component.text("bot-group: ", NamedTextColor.GRAY)
                .append(Component.text(Config.luckpermsBotGroup().isBlank() 
                        ? "(auto)" : Config.luckpermsBotGroup(), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("packet-prefix-char: ", NamedTextColor.GRAY)
                .append(Component.text(Config.luckpermsPacketPrefixChar().isBlank() 
                        ? "(none)" : "'" + Config.luckpermsPacketPrefixChar() + "'", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("weight-offset: ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(Config.luckpermsWeightOffset()), NamedTextColor.WHITE)));

        // Loaded groups summary
        sender.sendMessage(Component.text("─────── Groups ───────", NamedTextColor.DARK_GRAY));
        String summary = LuckPermsHelper.buildGroupSummary();
        sender.sendMessage(Component.text(summary, NamedTextColor.GRAY));

        // Bot data (applies to ALL bots regardless of spawner)
        sender.sendMessage(Component.text("─────── Bot Rank ─────", NamedTextColor.DARK_GRAY));
        LuckPermsHelper.LpData botData = LuckPermsHelper.getBotLpData(null);
        sender.sendMessage(Component.text("All bots use:", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  Prefix: ", NamedTextColor.GRAY)
                .append(Component.text(botData.prefix().isBlank() 
                        ? "(none)" : botData.prefix(), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  Weight: ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(botData.weight()), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  Sample packet name: ", NamedTextColor.GRAY)
                .append(Component.text(LuckPermsHelper.buildPacketProfileName(botData.weight(), "BotTest"), 
                        NamedTextColor.AQUA)));

        // Show a specific bot's data if name argument provided
        if (args.length > 0) {
            String botName = args[0];
            FakePlayer fp = manager.getByName(botName);
            if (fp == null) {
                sender.sendMessage(Lang.get("delete-not-found", "name", botName));
                return true;
            }
            sender.sendMessage(Component.text("─────── Bot: " + fp.getName() + " ────", NamedTextColor.DARK_GRAY));
            sender.sendMessage(Component.text("Display: ", NamedTextColor.GRAY)
                    .append(Component.text(fp.getDisplayName(), NamedTextColor.WHITE)));
            sender.sendMessage(Component.text("Packet profile: ", NamedTextColor.GRAY)
                    .append(Component.text(fp.getPacketProfileName(), NamedTextColor.AQUA)));
            sender.sendMessage(Component.text("Spawned by: ", NamedTextColor.GRAY)
                    .append(Component.text(fp.getSpawnedBy(), NamedTextColor.WHITE)));
        }

        Component footer = Component.text("══════════════════════", NamedTextColor.DARK_GRAY);
        sender.sendMessage(footer);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            for (FakePlayer fp : manager.getActivePlayers()) {
                names.add(fp.getName());
            }
            String partial = args[0].toLowerCase();
            return names.stream().filter(n -> n.toLowerCase().startsWith(partial)).toList();
        }
        return List.of();
    }

    private void status(CommandSender sender, String label, boolean value) {
        sender.sendMessage(Component.text(label + ": ", NamedTextColor.GRAY)
                .append(Component.text(value ? "enabled ✓" : "disabled", 
                        value ? NamedTextColor.GREEN : NamedTextColor.DARK_GRAY)));
    }
}


