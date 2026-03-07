package me.bill.fakePlayerPlugin.command;

import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

/**
 * Every FPP sub-command implements this interface so that the
 * {@link CommandManager} can introspect metadata for the dynamic help listing.
 */
public interface FppCommand {

    /** Primary name used in {@code /fpp <name>}. */
    String getName();

    /** Short usage hint shown in the help list, e.g. {@code <player> [world]}. */
    String getUsage();

    /** One-line description shown next to the command in the help list. */
    String getDescription();

    /** Permission node required to use this command; {@code null} → no permission needed. */
    default String getPermission() { return null; }

    /**
     * Execute the command.
     *
     * @param sender the command sender
     * @param args   arguments that follow the sub-command name
     * @return {@code true} if the command was handled; {@code false} sends usage to the sender.
     */
    boolean execute(CommandSender sender, String[] args);

    /**
     * Tab-completion for this sub-command's arguments.
     * Override to provide custom completions; default returns an empty list.
     *
     * @param sender the command sender
     * @param args   arguments after the sub-command name (args[0] is what's being typed)
     */
    default List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}


