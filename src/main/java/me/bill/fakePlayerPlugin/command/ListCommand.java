package me.bill.fakePlayerPlugin.command;

import me.bill.fakePlayerPlugin.fakeplayer.FakePlayer;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayerManager;
import me.bill.fakePlayerPlugin.lang.Lang;
import me.bill.fakePlayerPlugin.permission.Perm;
import me.bill.fakePlayerPlugin.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;

/**
 * {@code /fpp list} — displays all currently active bots with uptime,
 * location, and spawner info. Output is colour-coded and readable.
 */
@SuppressWarnings("unused") // Registered dynamically via CommandManager.register()
public class ListCommand implements FppCommand {

    private static final TextColor ACCENT     = TextColor.fromHexString("#0079FF");
    private static final TextColor MUTED      = NamedTextColor.DARK_GRAY;
    private static final TextColor LABEL      = NamedTextColor.GRAY;
    private static final TextColor VALUE      = NamedTextColor.WHITE;

    private final FakePlayerManager manager;

    public ListCommand(FakePlayerManager manager) {
        this.manager = manager;
    }

    @Override public String getName()        { return "list"; }
    @Override public String getUsage()       { return ""; }
    @Override public String getDescription() { return "Lists all currently active bots."; }
    @Override public String getPermission()  { return Perm.LIST; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Collection<FakePlayer> bots = manager.getActivePlayers();

        // ── Header ──────────────────────────────────────────────────────────
        sender.sendMessage(TextUtil.colorize(
                "<dark_gray><st>━━━━━━━━</st> <#0079FF>ᴀᴄᴛɪᴠᴇ ʙᴏᴛꜱ</#0079FF> "
                + "<dark_gray>(<white>" + bots.size() + "<dark_gray>) <st>━━━━━━━━</st>"));

        if (bots.isEmpty()) {
            sender.sendMessage(Lang.get("list-none"));
            return true;
        }

        // ── Entries ──────────────────────────────────────────────────────────
        for (FakePlayer fp : bots) {
            String uptime  = formatUptime(fp.getSpawnTime());
            String locStr  = formatLocation(fp);
            String spawner = fp.getSpawnedBy();

            // Name + uptime on line 1
            Component line1 = Component.empty()
                    .append(Component.text("  "))
                    .append(Component.text(fp.getDisplayName()).color(ACCENT).decorate(TextDecoration.BOLD))
                    .append(Component.text("  ").color(MUTED))
                    .append(Component.text("⏱ ").color(LABEL))
                    .append(Component.text(uptime).color(VALUE));

            // Location + spawner on line 2 (indented)
            Component line2 = Component.empty()
                    .append(Component.text("    "))
                    .append(Component.text("📍 ").color(LABEL))
                    .append(Component.text(locStr).color(VALUE))
                    .append(Component.text("  by ").color(MUTED))
                    .append(Component.text(spawner).color(LABEL));

            sender.sendMessage(line1);
            sender.sendMessage(line2);
        }

        // ── Footer ──────────────────────────────────────────────────────────
        sender.sendMessage(TextUtil.colorize(
                "<dark_gray><st>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</st>"));
        return true;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String formatUptime(Instant spawnTime) {
        if (spawnTime == null) return "?";
        long secs = Duration.between(spawnTime, Instant.now()).getSeconds();
        if (secs < 60)   return secs + "s";
        if (secs < 3600) return (secs / 60) + "m " + (secs % 60) + "s";
        long h = secs / 3600, m = (secs % 3600) / 60;
        return h + "h " + m + "m";
    }

    private static String formatLocation(FakePlayer fp) {
        Entity body = fp.getPhysicsEntity();
        if (body != null && body.isValid()) {
            var loc = body.getLocation();
            return (loc.getWorld() != null ? loc.getWorld().getName() : "?")
                    + " " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
        }
        if (fp.getSpawnLocation() != null) {
            var loc = fp.getSpawnLocation();
            return (loc.getWorld() != null ? loc.getWorld().getName() : "?")
                    + " " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ()
                    + " (spawn)";
        }
        return "unknown";
    }
}



