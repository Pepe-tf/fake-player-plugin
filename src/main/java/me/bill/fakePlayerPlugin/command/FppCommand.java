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

    /**
     * Optional aliases for this sub-command (e.g. {@code ["inv"]} for {@code inventory}).
     * Aliases are resolved by {@link CommandManager} but do NOT appear in the help menu.
     */
    default List<String> getAliases() { return Collections.emptyList(); }

    /** Short usage hint shown in the help list, e.g. {@code <player> [world]}. */
    String getUsage();

    /** One-line description shown next to the command in the help list. */
    String getDescription();

    /**
     * Primary permission node for this command; {@code null} → no single
     * permission (override {@link #canUse} for multi-tier commands).
     */
    default String getPermission() { return null; }

    /**
     * Returns {@code true} if {@code sender} is allowed to use this command
     * and should see it in tab-complete and the help menu.
     *
     * <p>The default implementation checks {@link #getPermission()}:
     * <ul>
     *   <li>If the permission is {@code null}, everyone can use it.</li>
     *   <li>Otherwise the sender must have the node.</li>
     * </ul>
     *
     * <p>Override this for dual-tier commands (e.g. spawn accepts both
     * {@code fpp.spawn} and {@code fpp.user.spawn}).
     */
    default boolean canUse(CommandSender sender) {
        String perm = getPermission();
        return perm == null || sender.hasPermission(perm);
    }

    /**
     * Execute the command.
     *
     * @param sender the command sender
     * @param args   arguments that follow the sub-command name
     * @return {@code true} if the command was handled.
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

