package me.bill.fakePlayerPlugin.api.event;

import me.bill.fakePlayerPlugin.api.FppBot;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired after a bot has fully spawned and its body (if applicable) is online.
 * Cancellation is not supported — the bot is already live when this fires.
 */
public class FppBotSpawnEvent extends FppBotEvent {

  private static final HandlerList HANDLERS = new HandlerList();

  /** Whether this spawn is a persistence restore (server restart) rather than a fresh spawn. */
  private final boolean restored;

  public FppBotSpawnEvent(@NotNull FppBot bot, boolean restored) {
    super(bot);
    this.restored = restored;
  }

  /** True if this bot was restored from a previous server session, false for a fresh spawn. */
  public boolean isRestored() {
    return restored;
  }

  @Override
  public @NotNull HandlerList getHandlers() {
    return HANDLERS;
  }

  public static @NotNull HandlerList getHandlerList() {
    return HANDLERS;
  }
}
