package me.bill.fakePlayerPlugin.api;

import java.util.Collections;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/**
 * An addon-contributed sub-command that plugs into the {@code /fpp} command tree.
 *
 * <p>Register instances via {@link FppApi#registerCommand(FppAddonCommand)}.
 * The command will appear in {@code /fpp help} automatically.
 *
 * <p>Example:
 * <pre>{@code
 * public class MyCommand implements FppAddonCommand {
 *     public String getName()        { return "mything"; }
 *     public String getDescription() { return "Does my thing."; }
 *     public String getUsage()       { return "/fpp mything <bot>"; }
 *     public String getPermission()  { return "myaddon.mything"; }
 *
 *     public boolean execute(CommandSender sender, String[] args) {
 *         sender.sendMessage("Hello from mything!");
 *         return true;
 *     }
 * }
 * }</pre>
 */
public interface FppAddonCommand {

  /** The primary sub-command name (lowercase, no spaces). E.g. {@code "mything"}. */
  @NotNull String getName();

  /** Short human-readable description shown in {@code /fpp help}. */
  @NotNull String getDescription();

  /** Usage string shown in {@code /fpp help}. E.g. {@code "/fpp mything <bot>"}. */
  @NotNull String getUsage();

  /**
   * Permission node required to run this command.
   * Return an empty string or {@code null} to allow all players.
   */
  @NotNull String getPermission();

  /**
   * Alternative names this command can be invoked by.
   * Defaults to an empty list (no aliases).
   */
  default @NotNull List<String> getAliases() {
    return Collections.emptyList();
  }

  /**
   * Called when a player or console runs {@code /fpp <name> [args...]}.
   *
   * @param sender the command sender
   * @param args   the arguments after the sub-command name (may be empty)
   * @return {@code true} if the command was handled; {@code false} to print usage
   */
  boolean execute(@NotNull CommandSender sender, @NotNull String[] args);

  /**
   * Called for tab-completion of {@code /fpp <name> [args...]}.
   * Return an empty list (the default) to show no suggestions.
   *
   * @param sender the command sender
   * @param args   the arguments after the sub-command name (may be empty)
   */
  default @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
    return Collections.emptyList();
  }
}
