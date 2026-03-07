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
 * <p>
 * Register a new command once with {@link #register(FppCommand)} — the dynamic
 * help menu will pick it up automatically without any further changes.
 */
public class CommandManager implements CommandExecutor, TabCompleter {

    /** Ordered list of every registered sub-command. */
    private final List<FppCommand> commands = new ArrayList<>();
    /** Fast lookup by name. */
    private final Map<String, FppCommand> byName = new LinkedHashMap<>();

    public CommandManager() {
        // Help is always the first command in the list
        register(new HelpCommand(this));
    }

    // ── Registration ────────────────────────────────────────────────────────

    /**
     * Registers a sub-command.  Duplicate names are silently ignored.
     */
    public void register(FppCommand command) {
        if (!byName.containsKey(command.getName().toLowerCase())) {
            commands.add(command);
            byName.put(command.getName().toLowerCase(), command);
            Config.debug("Registered command: fpp " + command.getName());
        } else {
            Config.debug("Skipped duplicate command: fpp " + command.getName());
        }
    }

    /** Returns an unmodifiable view of all registered commands. */
    public List<FppCommand> getCommands() {
        return Collections.unmodifiableList(commands);
    }

    // ── CommandExecutor ──────────────────────────────────────────────────────

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {

        if (args.length == 0) {
            // No sub-command → show help page 1
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

        String perm = sub.getPermission();
        if (perm != null && !sender.hasPermission(perm)) {
            Config.debug(sender.getName() + " was denied access to: fpp " + subName + " (missing " + perm + ")");
            sender.sendMessage(Lang.get("no-permission"));
            return true;
        }

        Config.debug(sender.getName() + " executed: fpp " + subName + " " + String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
        // Strip sub-command name from args before forwarding
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        sub.execute(sender, subArgs);
        return true;
    }

    // ── TabCompleter ─────────────────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender,
                                      @NotNull Command command,
                                      @NotNull String label,
                                      @NotNull String[] args) {
        if (args.length == 1) {
            return commands.stream()
                    .filter(cmd -> {
                        String perm = cmd.getPermission();
                        return perm == null || sender.hasPermission(perm);
                    })
                    .map(FppCommand::getName)
                    .filter(name -> name.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        // Forward to the matched sub-command for argument-level tab completion
        if (args.length >= 2) {
            FppCommand sub = byName.get(args[0].toLowerCase());
            if (sub != null) {
                String perm = sub.getPermission();
                if (perm == null || sender.hasPermission(perm)) {
                    return sub.tabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
                }
            }
        }

        return Collections.emptyList();
    }
}

