package me.bill.fakePlayerPlugin.api.event;

import me.bill.fakePlayerPlugin.api.FppBot;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fired when a bot is killed.
 * The bot's death count has already been incremented when this event fires.
 * If {@code Config.respawnOnDeath()} is true, the bot will respawn automatically.
 */
public class FppBotDeathEvent extends FppBotEvent {

  private static final HandlerList HANDLERS = new HandlerList();

  /** The player who killed the bot, or null if the cause was not a player (fall, fire, etc.). */
  private final Player killer;

  public FppBotDeathEvent(@NotNull FppBot bot, @Nullable Player killer) {
    super(bot);
    this.killer = killer;
  }

  /**
   * The player who killed the bot, or {@code null} for non-player kills.
   */
  public @Nullable Player getKiller() {
    return killer;
  }

  @Override
  public @NotNull HandlerList getHandlers() {
    return HANDLERS;
  }

  public static @NotNull HandlerList getHandlerList() {
    return HANDLERS;
  }
}
