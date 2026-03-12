package me.bill.fakePlayerPlugin.command;

import me.bill.fakePlayerPlugin.fakeplayer.FakePlayer;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayerManager;
import me.bill.fakePlayerPlugin.lang.Lang;
import me.bill.fakePlayerPlugin.permission.Perm;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * {@code /fpp tph [botname]} — teleports the player's own bot(s) to the player.
 *
 * <p>Permission: {@code fpp.user.tph} (included in {@code fpp.user.*} and {@code fpp.*}).
 *
 * <ul>
 *   <li>If the player owns only one bot, {@code [botname]} is optional.</li>
 *   <li>If the player owns multiple bots, they must specify a name.</li>
 *   <li>Players can only tph bots they personally spawned.</li>
 *   <li>Admins ({@code fpp.*}) can tph ANY bot regardless of ownership.</li>
 * </ul>
 */
@SuppressWarnings("unused") // Registered dynamically via CommandManager.register()
public class TphCommand implements FppCommand {

    private static final TextColor ACCENT = TextColor.fromHexString("#0079FF");
    private static final TextColor MUTED  = NamedTextColor.GRAY;

    private final FakePlayerManager manager;

    public TphCommand(FakePlayerManager manager) {
        this.manager = manager;
    }

    @Override public String getName()        { return "tph"; }
    @Override public String getUsage()       { return "[botname]"; }
    @Override public String getDescription() { return "Teleports your bot(s) to you."; }
    @Override public String getPermission()  { return Perm.TPH; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Lang.get("player-only"));
            return true;
        }

        boolean isAdmin = Perm.has(sender, Perm.ALL);

        // Determine candidate pool — admin sees all, user sees own only
        List<FakePlayer> candidates = isAdmin
                ? List.copyOf(manager.getActivePlayers())
                : manager.getBotsOwnedBy(player.getUniqueId());

        if (candidates.isEmpty()) {
            sender.sendMessage(Lang.get("tph-no-bots"));
            return true;
        }

        FakePlayer target;

        if (args.length == 0) {
            // No name given — only valid if exactly one bot in pool
            if (candidates.size() > 1) {
                sender.sendMessage(Lang.get("tph-specify-name"));
                listBots(sender, candidates);
                return true;
            }
            target = candidates.getFirst();
        } else {
            // Match by display name first, then internal name
            String name = args[0];
            target = candidates.stream()
                    .filter(fp -> fp.getName().equalsIgnoreCase(name))
                    .findFirst().orElse(null);

            if (target == null) {
                if (!isAdmin) {
                    sender.sendMessage(Lang.get("tph-not-yours", "name", name));
                } else {
                    sender.sendMessage(Lang.get("tph-not-found", "name", name));
                }
                return true;
            }
        }

        // Teleport bot to player
        boolean ok = manager.teleportBot(target, player.getLocation());
        if (ok) {
            sender.sendMessage(Component.empty()
                    .append(Component.text("[ꜰᴘᴘ] ").color(ACCENT))
                    .append(Component.text("Teleported ").color(MUTED))
                    .append(Component.text(target.getDisplayName()).color(ACCENT))
                    .append(Component.text(" to you.").color(MUTED)));
        } else {
            sender.sendMessage(Lang.get("tph-failed", "name", target.getDisplayName()));
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length != 1) return List.of();
        boolean isAdmin = Perm.has(sender, Perm.ALL);
        List<FakePlayer> pool = isAdmin
                ? List.copyOf(manager.getActivePlayers())
                : (sender instanceof Player p ? manager.getBotsOwnedBy(p.getUniqueId()) : List.of());
        return pool.stream()
                .map(FakePlayer::getName)
                .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                .toList();
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private void listBots(CommandSender sender, List<FakePlayer> bots) {
        sender.sendMessage(Component.empty()
                .append(Component.text("  Your bots: ").color(MUTED))
                .append(Component.text(
                        String.join(", ", bots.stream().map(FakePlayer::getDisplayName).toList())
                ).color(ACCENT)));
    }
}



