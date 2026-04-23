package me.bill.fakePlayerPlugin.api;

import java.time.Duration;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Stable API view of a single FakePlayer bot.
 *
 * <p>Addon code should depend only on this interface — never on
 * {@code me.bill.fakePlayerPlugin.fakeplayer.FakePlayer} directly.
 * The backing implementation may change between plugin versions.
 */
public interface FppBot {

  // ── Identity ──────────────────────────────────────────────────────────────

  /** The bot's internal Minecraft username (max 16 chars, no colour). */
  @NotNull String getName();

  /** Stable UUID for this bot.  Remains the same across server restarts. */
  @NotNull UUID getUuid();

  /**
   * The display name shown in tab-list and chat (may include colour / LP prefix).
   */
  @NotNull String getDisplayName();

  /** The skin name used to resolve this bot's texture. */
  @Nullable String getSkinName();

  // ── Location ──────────────────────────────────────────────────────────────

  /**
   * The most accurate current location: the live NMS body position when the
   * body is online, otherwise the last recorded spawn location.
   */
  @NotNull Location getLocation();

  /**
   * The world name the bot last occupied, or is currently in.
   */
  @NotNull String getWorldName();

  // ── Entity reference ──────────────────────────────────────────────────────

  /**
   * The underlying {@link Player} entity for this bot, or {@code null} if the
   * bot is bodyless or has not yet fully spawned.
   *
   * <p><strong>Avoid storing this reference</strong> — it becomes stale if the
   * bot dies and respawns.  Always re-fetch via {@link FppApi#getBot(String)}.
   */
  @Nullable Player getEntity();

  /** True if this bot has no physical entity in the world (console-spawned bodyless mode). */
  boolean isBodyless();

  // ── State flags ───────────────────────────────────────────────────────────

  boolean isFrozen();
  void setFrozen(boolean frozen);

  boolean isAlive();

  /** True during the respawn delay after a death. */
  boolean isRespawning();

  // ── Chat / AI ─────────────────────────────────────────────────────────────

  boolean isChatEnabled();
  void setChatEnabled(boolean enabled);

  @Nullable String getChatTier();
  void setChatTier(@Nullable String tier);

  @Nullable String getAiPersonality();
  void setAiPersonality(@Nullable String personality);

  // ── Per-bot toggles ───────────────────────────────────────────────────────

  boolean isHeadAiEnabled();
  void setHeadAiEnabled(boolean enabled);

  boolean isSwimAiEnabled();
  void setSwimAiEnabled(boolean enabled);

  boolean isPickUpItemsEnabled();
  void setPickUpItemsEnabled(boolean enabled);

  boolean isPickUpXpEnabled();
  void setPickUpXpEnabled(boolean enabled);

  // ── Pathfinding overrides ─────────────────────────────────────────────────

  boolean isNavParkour();
  void setNavParkour(boolean enabled);

  boolean isNavBreakBlocks();
  void setNavBreakBlocks(boolean enabled);

  boolean isNavPlaceBlocks();
  void setNavPlaceBlocks(boolean enabled);

  boolean isNavSprintJump();
  void setNavSprintJump(boolean enabled);

  int getChunkLoadRadius();
  void setChunkLoadRadius(int radius);

  // ── PvE settings ──────────────────────────────────────────────────────────

  boolean isPveEnabled();
  void setPveEnabled(boolean enabled);

  double getPveRange();
  void setPveRange(double range);

  @Nullable String getPvePriority();
  void setPvePriority(@Nullable String priority);

  // ── Ownership ─────────────────────────────────────────────────────────────

  /** The username of the player who spawned this bot, or {@code "CONSOLE"}. */
  @NotNull String getSpawnedBy();

  /** The UUID of the player who spawned this bot, or {@code UUID(0,0)} for CONSOLE. */
  @NotNull UUID getSpawnedByUuid();

  // ── Stats ─────────────────────────────────────────────────────────────────

  @NotNull Duration getUptime();

  int getDeathCount();

  double getTotalDamageTaken();
}
