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

@SuppressWarnings("unused")
public class TphCommand implements FppCommand {

    private static final TextColor ACCENT = TextColor.fromHexString("#0079FF");
    private static final TextColor MUTED = NamedTextColor.GRAY;

    private final FakePlayerManager manager;

    public TphCommand(FakePlayerManager manager) {
        this.manager = manager;
    }

    @Override
    public String getName() {
        return "tph";
    }

    @Override
    public String getUsage() {
        return "[botname]";
    }

    @Override
    public String getDescription() {
        return "Teleports your bot(s) to you.";
    }

    @Override
    public String getPermission() {
        return Perm.USER_TPH;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Lang.get("player-only"));
            return true;
        }

        boolean isAdmin = Perm.has(sender, Perm.OP);

        List<FakePlayer> candidates =
                isAdmin
                        ? List.copyOf(manager.getActivePlayers())
                        : manager.getBotsOwnedBy(player.getUniqueId());

        if (!manager.physicalBodiesEnabled()) {
            sender.sendMessage(
                    Component.text("[ꜰᴘᴘ] ")
                            .color(ACCENT)
                            .append(Component.text("No body to tp to or from.").color(MUTED)));
            return true;
        }
        if (candidates.isEmpty()) {
            sender.sendMessage(Lang.get("tph-no-bots"));
            return true;
        }

        FakePlayer target;

        if (args.length == 0) {

            if (candidates.size() > 1) {
                sender.sendMessage(Lang.get("tph-specify-name"));
                listBots(sender, candidates);
                return true;
            }
            target = candidates.getFirst();
        } else {

            String name = args[0];
            target =
                    candidates.stream()
                            .filter(fp -> fp.getName().equalsIgnoreCase(name))
                            .findFirst()
                            .orElse(null);

            if (target == null) {
                if (!isAdmin) {
                    sender.sendMessage(Lang.get("tph-not-yours", "name", name));
                } else {
                    sender.sendMessage(Lang.get("tph-not-found", "name", name));
                }
                return true;
            }
        }

        boolean ok = manager.teleportBot(target, player.getLocation());
        if (ok) {
            sender.sendMessage(
                    Component.empty()
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
        boolean isAdmin = Perm.has(sender, Perm.OP);
        List<FakePlayer> pool =
                isAdmin
                        ? List.copyOf(manager.getActivePlayers())
                        : (sender instanceof Player p
                                ? manager.getBotsOwnedBy(p.getUniqueId())
                                : List.of());
        return pool.stream()
                .map(FakePlayer::getName)
                .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                .toList();
    }

    private void listBots(CommandSender sender, List<FakePlayer> bots) {
        sender.sendMessage(
                Component.empty()
                        .append(Component.text("  Your bots: ").color(MUTED))
                        .append(
                                Component.text(
                                                String.join(
                                                        ", ",
                                                        bots.stream()
                                                                .map(FakePlayer::getDisplayName)
                                                                .toList()))
                                        .color(ACCENT)));
    }
}
