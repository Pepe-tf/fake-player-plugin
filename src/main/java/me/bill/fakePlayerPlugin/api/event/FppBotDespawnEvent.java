package me.bill.fakePlayerPlugin.api.event;

import me.bill.fakePlayerPlugin.api.FppBot;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired just before a bot is fully removed from the server.
 * The bot is still accessible via {@link #getBot()} at the time this event fires,
 * but its entity may already be offline.
 */
public class FppBotDespawnEvent extends FppBotEvent {

  private static final HandlerList HANDLERS = new HandlerList();

  public FppBotDespawnEvent(@NotNull FppBot bot) {
    super(bot);
  }

  @Override
  public @NotNull HandlerList getHandlers() {
    return HANDLERS;
  }

  public static @NotNull HandlerList getHandlerList() {
    return HANDLERS;
  }
}
