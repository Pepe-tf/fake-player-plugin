package me.bill.fakePlayerPlugin.command;

import me.bill.fakePlayerPlugin.fakeplayer.FakePlayer;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayerManager;
import me.bill.fakePlayerPlugin.lang.Lang;
import me.bill.fakePlayerPlugin.permission.Perm;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public class DeleteCommand implements FppCommand {

    private final FakePlayerManager manager;

    public DeleteCommand(FakePlayerManager manager) { this.manager = manager; }

    @Override public String getName()        { return "delete"; }
    @Override public String getDescription() { return "Deletes a fake player bot by name."; }
    @Override public String getUsage()       { return "<name|all>"; }
    @Override public String getPermission()  { return Perm.DELETE; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Lang.get("unknown-command", "0", "fpp"));
            return true;
        }

        if (args[0].equalsIgnoreCase("all")) {
            // Requires fpp.delete.all (child of fpp.delete, but can be negated separately)
            if (Perm.missing(sender, Perm.DELETE_ALL)) {
                sender.sendMessage(Lang.get("no-permission"));
                return true;
            }
            int count = manager.getCount();
            if (count == 0) {
                sender.sendMessage(Lang.get("delete-none"));
                return true;
            }
            manager.removeAll();
            sender.sendMessage(Lang.get("delete-all", "count", String.valueOf(count)));
            return true;
        }

        String input = args[0];

        // Match by internal name first (exact, case-insensitive) — this always works
        // regardless of colour tags or LuckPerms prefixes in the display name.
        FakePlayer fp = manager.getActivePlayers().stream()
                .filter(p -> p.getName().equalsIgnoreCase(input))
                .findFirst()
                .orElse(null);

        // Fallback: match by plain-text display name (colour tags stripped)
        if (fp == null) {
            String inputLower = input.toLowerCase();
            fp = manager.getActivePlayers().stream()
                    .filter(p -> plainOf(p.getDisplayName()).toLowerCase().contains(inputLower))
                    .findFirst()
                    .orElse(null);
        }

        if (fp == null) {
            sender.sendMessage(Lang.get("delete-not-found", "name", input));
            return true;
        }

        String shown = plainOf(fp.getDisplayName());
        manager.delete(fp.getName());
        sender.sendMessage(Lang.get("delete-success", "name", shown));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new java.util.ArrayList<>();
            // Only suggest "all" if sender has fpp.delete.all
            if (Perm.has(sender, Perm.DELETE_ALL)) {
                suggestions.add("all");
            }

            String typed = args[0].toLowerCase();
            // Suggest internal names — these are always safe to type in the terminal
            manager.getActivePlayers().stream()
                    .map(FakePlayer::getName)
                    .filter(n -> n.toLowerCase().startsWith(typed))
                    .forEach(suggestions::add);
            return suggestions;
        }
        return Collections.emptyList();
    }

    /** Strips MiniMessage colour tags and returns plain text. */
    private static String plainOf(String miniMessage) {
        try {
            return PlainTextComponentSerializer.plainText()
                    .serialize(MiniMessage.miniMessage().deserialize(miniMessage));
        } catch (Exception e) {
            return miniMessage;
        }
    }
}
