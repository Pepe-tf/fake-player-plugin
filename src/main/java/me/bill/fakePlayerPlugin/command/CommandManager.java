package me.bill.fakePlayerPlugin.command;

import me.bill.fakePlayerPlugin.FakePlayerPlugin;
import me.bill.fakePlayerPlugin.config.Config;
import me.bill.fakePlayerPlugin.lang.Lang;
import me.bill.fakePlayerPlugin.permission.Perm;
import me.bill.fakePlayerPlugin.util.TextUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class CommandManager implements CommandExecutor, TabCompleter {

    private static final TextColor ACCENT = TextColor.fromHexString("#0079FF");
    private static final TextColor DARK_GRAY = NamedTextColor.DARK_GRAY;
    private static final TextColor GRAY = NamedTextColor.GRAY;
    private static final TextColor WHITE = NamedTextColor.WHITE;

    private final List<FppCommand> commands = new ArrayList<>();
    private final Map<String, FppCommand> byName = new LinkedHashMap<>();
    private final FakePlayerPlugin plugin;

    public CommandManager(FakePlayerPlugin plugin) {
        this.plugin = plugin;
        register(new HelpCommand(this));
    }

    public void register(FppCommand command) {
        if (!byName.containsKey(command.getName().toLowerCase())) {
            commands.add(command);
            byName.put(command.getName().toLowerCase(), command);

            for (String alias : command.getAliases()) {
                byName.putIfAbsent(alias.toLowerCase(), command);
            }
            Config.debug("Registered command: fpp " + command.getName());
        }
    }

    public List<FppCommand> getCommands() {
        return Collections.unmodifiableList(commands);
    }

    public void setHelpGui(me.bill.fakePlayerPlugin.gui.HelpGui helpGui) {
        FppCommand help = byName.get("help");
        if (help instanceof HelpCommand hc) {
            hc.setHelpGui(helpGui);
        }
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String @NotNull [] args) {

        if (args.length == 0) {

            if (!sender.hasPermission(Perm.COMMAND)) return true;

            if (sender.hasPermission(Perm.PLUGIN_INFO)) {

                sendPluginInfo(sender);
            } else {

                sendHelpHint(sender);
            }
            return true;
        }

        if (plugin.isVersionUnsupported()) {
            sender.sendMessage(
                    Lang.get("version-unsupported", "version", plugin.getDetectedMcVersion()));
            return true;
        }

        String subName = args[0].toLowerCase();
        FppCommand sub = byName.get(subName);

        if (sub == null) {
            Config.debug(sender.getName() + " used unknown sub-command: " + subName);
            sender.sendMessage(Lang.get("unknown-command", label));
            return true;
        }

        if (!sub.canUse(sender)) {
            Config.debug(sender.getName() + " was denied: fpp " + subName);
            sender.sendMessage(Lang.get("no-permission"));
            return true;
        }

        Config.debug(sender.getName() + " executed: fpp " + String.join(" ", args));
        // Pass the root command label to HelpCommand so the GUI can display the correct prefix
        if (sub instanceof HelpCommand hc) hc.setLastLabel(label);
        sub.execute(sender, Arrays.copyOfRange(args, 1, args.length));
        return true;
    }

    @Override
    public List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String @NotNull [] args) {

        if (args.length == 1) {

            return commands.stream()
                    .filter(cmd -> cmd.canUse(sender))
                    .map(FppCommand::getName)
                    .filter(name -> name.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length >= 2) {
            FppCommand sub = byName.get(args[0].toLowerCase());

            if (sub != null && sub.canUse(sender)) {
                return sub.tabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
            }
        }

        return Collections.emptyList();
    }

    private void sendHelpHint(CommandSender sender) {
        Component divider = TextUtil.colorize(Lang.raw("divider"));
        sender.sendMessage(divider);
        sender.sendMessage(
                Component.empty()
                        .append(Component.text("  ").color(DARK_GRAY))
                        .append(Component.text("ᴛʏᴘᴇ ").color(GRAY))
                        .append(
                                Component.text("/fpp help")
                                        .color(ACCENT)
                                        .clickEvent(ClickEvent.runCommand("/fpp help"))
                                        .hoverEvent(
                                                HoverEvent.showText(
                                                        Component.text(
                                                                        "Click to open the help"
                                                                            + " menu")
                                                                .color(GRAY))))
                        .append(Component.text(" ꜰᴏʀ ᴀ ʟɪꜱᴛ ᴏꜰ ᴄᴏᴍᴍᴀɴᴅꜱ.").color(GRAY)));
        sender.sendMessage(divider);
    }

    private void sendPluginInfo(CommandSender sender) {
        String version = plugin.getPluginMeta().getVersion();
        List<String> authors = plugin.getPluginMeta().getAuthors();
        String author = authors.isEmpty() ? "Unknown" : String.join(", ", authors);

        Component divider = TextUtil.colorize(Lang.raw("divider"));
        Component header = TextUtil.colorize(Lang.raw("info-screen-header"));

        sender.sendMessage(divider);
        sender.sendMessage(header);
        sender.sendMessage(Component.empty());

        sender.sendMessage(row("ᴠᴇʀꜱɪᴏɴ", version));
        sender.sendMessage(row("ᴀᴜᴛʜᴏʀ", author));

        me.bill.fakePlayerPlugin.fakeplayer.FakePlayerManager fpm = plugin.getFakePlayerManager();
        if (fpm != null) {
            sender.sendMessage(row("ᴀᴄᴛɪᴠᴇ ʙᴏᴛꜱ", String.valueOf(fpm.getCount())));
        }

        sender.sendMessage(
                Component.empty()
                        .append(Component.text("  ").color(DARK_GRAY))
                        .append(Component.text("ᴅᴏᴡɴʟᴏᴀᴅ ").color(GRAY))
                        .append(Component.text("→ ").color(DARK_GRAY))
                        .append(
                                Component.text("Modrinth")
                                        .color(ACCENT)
                                        .decorate(TextDecoration.UNDERLINED)
                                        .clickEvent(
                                                ClickEvent.openUrl(
                                                        "https://modrinth.com/plugin/fake-player-plugin-(fpp)"))
                                        .hoverEvent(
                                                HoverEvent.showText(
                                                        Component.text("Click to open Modrinth")
                                                                .color(GRAY))))
                        .append(Component.text(", ").color(GRAY))
                        .append(
                                Component.text("SpigotMC")
                                        .color(ACCENT)
                                        .decorate(TextDecoration.UNDERLINED)
                                        .clickEvent(
                                                ClickEvent.openUrl(
                                                        "https://www.spigotmc.org/resources/fake-player-plugin-fpp.133572/"))
                                        .hoverEvent(
                                                HoverEvent.showText(
                                                        Component.text("Click to open SpigotMC")
                                                                .color(GRAY))))
                        .append(Component.text(", ").color(GRAY))
                        .append(
                                Component.text("PaperMC")
                                        .color(ACCENT)
                                        .decorate(TextDecoration.UNDERLINED)
                                        .clickEvent(
                                                ClickEvent.openUrl(
                                                        "https://hangar.papermc.io/Pepe-tf/FakePlayerPlugin"))
                                        .hoverEvent(
                                                HoverEvent.showText(
                                                        Component.text(
                                                                        "Click to open PaperMC"
                                                                            + " Hangar")
                                                                .color(GRAY))))
                        .append(Component.text(", ").color(GRAY))
                        .append(
                                Component.text("BuiltByBit")
                                        .color(ACCENT)
                                        .decorate(TextDecoration.UNDERLINED)
                                        .clickEvent(
                                                ClickEvent.openUrl(
                                                        "https://builtbybit.com/resources/fake-player-plugin.98704/"))
                                        .hoverEvent(
                                                HoverEvent.showText(
                                                        Component.text("Click to open BuiltByBit")
                                                                .color(GRAY)))));

        sender.sendMessage(
                Component.empty()
                        .append(Component.text("  ").color(DARK_GRAY))
                        .append(Component.text("ꜱᴜᴘᴘᴏʀᴛ  ").color(GRAY))
                        .append(Component.text("→ ").color(DARK_GRAY))
                        .append(
                                Component.text("Discord")
                                        .color(ACCENT)
                                        .decorate(TextDecoration.UNDERLINED)
                                        .clickEvent(
                                                ClickEvent.openUrl("https://discord.gg/RfjEJDG2TM"))
                                        .hoverEvent(
                                                HoverEvent.showText(
                                                        Component.text(
                                                                        "Click to join the support"
                                                                            + " Discord")
                                                                .color(GRAY)))));

        sender.sendMessage(Component.empty());

        sender.sendMessage(
                Component.empty()
                        .append(Component.text("  ").color(DARK_GRAY))
                        .append(Component.text("ᴛʏᴘᴇ ").color(GRAY))
                        .append(
                                Component.text("/fpp help")
                                        .color(ACCENT)
                                        .clickEvent(ClickEvent.runCommand("/fpp help"))
                                        .hoverEvent(
                                                HoverEvent.showText(
                                                        Component.text(
                                                                        "Click to open the help"
                                                                            + " menu")
                                                                .color(GRAY))))
                        .append(Component.text(" ꜰᴏʀ ᴀ ʟɪꜱᴛ ᴏꜰ ᴄᴏᴍᴍᴀɴᴅꜱ.").color(GRAY)));

        sender.sendMessage(divider);
    }

    private Component row(String label, String value) {
        return Component.empty()
                .append(Component.text("  ").color(DARK_GRAY))
                .append(Component.text(label).color(GRAY))
                .append(Component.text(" → ").color(DARK_GRAY))
                .append(Component.text(value).color(WHITE));
    }
}
