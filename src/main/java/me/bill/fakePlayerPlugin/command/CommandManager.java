package me.bill.fakePlayerPlugin.command;

import me.bill.fakePlayerPlugin.config.Config;
import me.bill.fakePlayerPlugin.lang.Lang;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Central dispatcher for all {@code /fpp} sub-commands.
 * Register a new command once with {@link #register(FppCommand)} — the dynamic
 * help menu and tab-complete will pick it up automatically.
 */
public class CommandManager implements CommandExecutor, TabCompleter {

    private final List<FppCommand> commands = new ArrayList<>();
    private final Map<String, FppCommand> byName = new LinkedHashMap<>();

    public CommandManager() {
        register(new HelpCommand(this));
    }

    public void register(FppCommand command) {
        if (!byName.containsKey(command.getName().toLowerCase())) {
            commands.add(command);
            byName.put(command.getName().toLowerCase(), command);
            Config.debug("Registered command: fpp " + command.getName());
        }
    }

    public List<FppCommand> getCommands() {
        return Collections.unmodifiableList(commands);
    }

    // ── CommandExecutor ──────────────────────────────────────────────────────

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String @NotNull [] args) {

        if (args.length == 0) {
            byName.get("help").execute(sender, new String[]{});
            return true;
        }

        String subName = args[0].toLowerCase();
        FppCommand sub = byName.get(subName);

        if (sub == null) {
            Config.debug(sender.getName() + " used unknown sub-command: " + subName);
            sender.sendMessage(Lang.get("unknown-command", label));
            return true;
        }

        // Use canUse() — handles both single-perm and dual-tier commands
        if (!sub.canUse(sender)) {
            Config.debug(sender.getName() + " was denied: fpp " + subName);
            sender.sendMessage(Lang.get("no-permission"));
            return true;
        }

        Config.debug(sender.getName() + " executed: fpp " + String.join(" ", args));
        sub.execute(sender, Arrays.copyOfRange(args, 1, args.length));
        return true;
    }

    // ── TabCompleter ─────────────────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender,
                                      @NotNull Command command,
                                      @NotNull String label,
                                      @NotNull String @NotNull [] args) {

        if (args.length == 1) {
            // Only show sub-commands the sender can actually use
            return commands.stream()
                    .filter(cmd -> cmd.canUse(sender))
                    .map(FppCommand::getName)
                    .filter(name -> name.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length >= 2) {
            FppCommand sub = byName.get(args[0].toLowerCase());
            // Only forward if sender is allowed to use this sub-command
            if (sub != null && sub.canUse(sender)) {
                return sub.tabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
            }
        }

        return Collections.emptyList();
    }
}

