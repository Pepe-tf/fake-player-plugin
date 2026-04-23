package me.bill.fakePlayerPlugin.api.event;

import me.bill.fakePlayerPlugin.api.FppBot;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired after a bot has been successfully renamed (despawn + respawn completed).
 * The {@link FppBot} returned by {@link #getBot()} reflects the new name and UUID.
 */
public class FppBotRenameEvent extends FppBotEvent {

  private static final HandlerList HANDLERS = new HandlerList();

  private final String oldName;
  private final String newName;

  public FppBotRenameEvent(@NotNull FppBot bot, @NotNull String oldName, @NotNull String newName) {
    super(bot);
    this.oldName = oldName;
    this.newName = newName;
  }

  /** The bot's previous name. */
  public @NotNull String getOldName() {
    return oldName;
  }

  /** The bot's new name. */
  public @NotNull String getNewName() {
    return newName;
  }

  @Override
  public @NotNull HandlerList getHandlers() {
    return HANDLERS;
  }

  public static @NotNull HandlerList getHandlerList() {
    return HANDLERS;
  }
}
