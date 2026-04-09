package me.bill.fakePlayerPlugin.command;

import me.bill.fakePlayerPlugin.gui.HelpGui;
import me.bill.fakePlayerPlugin.lang.Lang;
import me.bill.fakePlayerPlugin.permission.Perm;
import me.bill.fakePlayerPlugin.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Handles {@code /fpp help [page]}.
 *
 * <p>For in-game players: opens the {@link HelpGui} double-chest GUI.
 * For console / non-player senders: falls back to the original paginated text output.
 *
 * <p>The command list is read live from the {@link CommandManager} so new commands
 * appear automatically without any manual wiring.
 */
public class HelpCommand implements FppCommand {

    private static final int PAGE_SIZE = 6;
    private static final TextColor ACCENT = TextColor.fromHexString("#0079FF");

    private final CommandManager manager;

    /** Injected after onEnable creates the GUI. {@code null} until then → text fallback. */
    private volatile HelpGui helpGui = null;

    public HelpCommand(CommandManager manager) {
        this.manager = manager;
    }

    /** Called by CommandManager once the GUI is ready. */
    public void setHelpGui(HelpGui gui) {
        this.helpGui = gui;
    }

    // ── FppCommand impl ──────────────────────────────────────────────────────

    @Override public String getName()        { return "help"; }
    @Override public String getUsage()       { return "[page]"; }
    @Override public String getDescription() { return "Shows the command help menu."; }
    @Override public String getPermission()  { return Perm.HELP; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        // ── GUI for players ──────────────────────────────────────────────────
        if (sender instanceof Player player && helpGui != null) {
            helpGui.open(player);
            return true;
        }

        // ── Text fallback for console / pre-GUI init ─────────────────────────
        List<FppCommand> visible = manager.getCommands().stream()
                .filter(cmd -> cmd.canUse(sender))
                .toList();

        int totalPages = Math.max(1, (int) Math.ceil(visible.size() / (double) PAGE_SIZE));

        int page = 1;
        if (args.length > 0) {
            try { page = Integer.parseInt(args[0]); } catch (NumberFormatException ignored) {}
        }
        page = Math.min(Math.max(page, 1), totalPages);

        // ── Header ──────────────────────────────────────────────────────────
        sender.sendMessage(Lang.get("help-header"));
        sender.sendMessage(Component.empty());

        // ── Entries ─────────────────────────────────────────────────────────
        int from = (page - 1) * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, visible.size());

        for (FppCommand cmd : visible.subList(from, to)) {
            String usage = cmd.getUsage().isEmpty() ? "" : " " + cmd.getUsage();
            String raw = Lang.raw("help-entry",
                    "cmd",  "fpp " + cmd.getName(),
                    "args", usage,
                    "desc", cmd.getDescription());
            sender.sendMessage(TextUtil.colorize(raw));
        }

        // ── Clickable pagination bar (below entries) ─────────────────────────
        sender.sendMessage(Component.empty());
        sender.sendMessage(buildPaginationBar(page, totalPages));
        sender.sendMessage(Lang.get("help-footer"));
        return true;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Builds the clickable « page/total » pagination bar.
     * Prev/next arrows are greyed out and non-clickable when on the first/last page.
     */
    private Component buildPaginationBar(int page, int totalPages) {
        // « prev
        Component prev;
        if (page > 1) {
            prev = Component.text("« ᴘʀᴇᴠ")
                    .color(ACCENT)
                    .decoration(TextDecoration.BOLD, true)
                    .clickEvent(ClickEvent.runCommand("/fpp help " + (page - 1)))
                    .hoverEvent(HoverEvent.showText(
                            Component.text("Go to page " + (page - 1)).color(NamedTextColor.GRAY)));
        } else {
            prev = Component.text("« ᴘʀᴇᴠ")
                    .color(NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.BOLD, true);
        }

        // page / total (centre)
        Component pageInfo = Component.text("  " + page + " / " + totalPages + "  ")
                .color(NamedTextColor.WHITE);

        // next »
        Component next;
        if (page < totalPages) {
            next = Component.text("ɴᴇxᴛ »")
                    .color(ACCENT)
                    .decoration(TextDecoration.BOLD, true)
                    .clickEvent(ClickEvent.runCommand("/fpp help " + (page + 1)))
                    .hoverEvent(HoverEvent.showText(
                            Component.text("Go to page " + (page + 1)).color(NamedTextColor.GRAY)));
        } else {
            next = Component.text("ɴᴇxᴛ »")
                    .color(NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.BOLD, true);
        }

        return Component.text(" ").append(prev).append(pageInfo).append(next);
    }
}

