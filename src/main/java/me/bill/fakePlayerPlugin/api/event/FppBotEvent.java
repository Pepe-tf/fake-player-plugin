package me.bill.fakePlayerPlugin.api.event;

import me.bill.fakePlayerPlugin.api.FppBot;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

/**
 * Base class for all FakePlayerPlugin events.
 * All FPP events are fired on the <em>main server thread</em>.
 */
public abstract class FppBotEvent extends Event {

  private final FppBot bot;

  protected FppBotEvent(@NotNull FppBot bot) {
    this.bot = bot;
  }

  /** The bot this event concerns. */
  public @NotNull FppBot getBot() {
    return bot;
  }
}
