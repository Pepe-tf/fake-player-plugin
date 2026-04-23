package me.bill.fakePlayerPlugin.api;

import org.jetbrains.annotations.NotNull;

/**
 * A per-tick AI hook that runs every game tick for each active, non-frozen bot
 * that has a physical body online.
 *
 * <p>Register instances via {@link FppApi#registerTickHandler(FppBotTickHandler)}.
 * Handlers run on the <em>main server thread</em>, inside the same tick loop
 * as all other FPP AI logic, just before {@code tickPhysics()} fires.
 *
 * <p>Keep implementations fast — heavy computation will lag the server.
 * If you need async work, schedule it via {@code Bukkit.getScheduler().runTaskAsynchronously()}.
 *
 * <p>Example:
 * <pre>{@code
 * api.registerTickHandler((bot, entity) -> {
 *     if (bot.getName().startsWith("guard_")) {
 *         // custom guard AI logic here
 *     }
 * });
 * }</pre>
 */
@FunctionalInterface
public interface FppBotTickHandler {

  /**
   * Called once per game tick for each active, non-frozen, bodied bot.
   *
   * @param bot    the stable API view of the bot
   * @param entity the underlying {@link org.bukkit.entity.Player} entity —
   *               convenience reference, equivalent to {@code bot.getEntity()}
   */
  void onTick(@NotNull FppBot bot, @NotNull org.bukkit.entity.Player entity);
}
