package me.bill.fakePlayerPlugin.api.impl;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import me.bill.fakePlayerPlugin.api.FppBot;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayer;
import me.bill.fakePlayerPlugin.util.AttributeCompat;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class FppBotImpl implements FppBot {

  private final FakePlayer fp;

  public FppBotImpl(@NotNull FakePlayer fp) {
    this.fp = fp;
  }

  public @NotNull FakePlayer getHandle() { return fp; }
  @Override public @NotNull String getName() { return fp.getName(); }
  @Override public @NotNull UUID getUuid() { return fp.getUuid(); }
  @Override public @NotNull String getDisplayName() { return fp.getDisplayName(); }
  @Override public void setDisplayName(@NotNull String name) { fp.setDisplayName(name); }
  @Override public @NotNull Location getLocation() { return fp.getLiveLocation(); }
  @Override public @NotNull String getWorldName() { return getLocation().getWorld() != null ? getLocation().getWorld().getName() : "unknown"; }
  @Override public @Nullable Player getEntity() { return fp.getPlayer(); }
  @Override public boolean isBodyless() { return fp.isBodyless(); }
  @Override public boolean isFrozen() { return fp.isFrozen(); }
  @Override public void setFrozen(boolean frozen) { fp.setFrozen(frozen); }
  @Override public boolean isAlive() { return fp.isAlive(); }
  @Override public boolean isRespawning() { return fp.isRespawning(); }
  @Override public boolean isChatEnabled() { return fp.isChatEnabled(); }
  @Override public void setChatEnabled(boolean enabled) { fp.setChatEnabled(enabled); }
  @Override public @Nullable String getChatTier() { return fp.getChatTier(); }
  @Override public void setChatTier(@Nullable String tier) { fp.setChatTier(tier); }
  @Override public @Nullable String getAiPersonality() { return fp.getAiPersonality(); }
  @Override public void setAiPersonality(@Nullable String personality) { fp.setAiPersonality(personality); }
  @Override public boolean isHeadAiEnabled() { return fp.isHeadAiEnabled(); }
  @Override public void setHeadAiEnabled(boolean enabled) { fp.setHeadAiEnabled(enabled); }
  @Override public boolean isSwimAiEnabled() { return fp.isSwimAiEnabled(); }
  @Override public void setSwimAiEnabled(boolean enabled) { fp.setSwimAiEnabled(enabled); }
  @Override public boolean isPickUpItemsEnabled() { return fp.isPickUpItemsEnabled(); }
  @Override public void setPickUpItemsEnabled(boolean enabled) { fp.setPickUpItemsEnabled(enabled); }
  @Override public boolean isPickUpXpEnabled() { return fp.isPickUpXpEnabled(); }
  @Override public void setPickUpXpEnabled(boolean enabled) { fp.setPickUpXpEnabled(enabled); }
  @Override public boolean isNavParkour() { return fp.isNavParkour(); }
  @Override public void setNavParkour(boolean enabled) { fp.setNavParkour(enabled); }
  @Override public boolean isNavBreakBlocks() { return fp.isNavBreakBlocks(); }
  @Override public void setNavBreakBlocks(boolean enabled) { fp.setNavBreakBlocks(enabled); }
  @Override public boolean isNavPlaceBlocks() { return fp.isNavPlaceBlocks(); }
  @Override public void setNavPlaceBlocks(boolean enabled) { fp.setNavPlaceBlocks(enabled); }
  @Override public int getChunkLoadRadius() { return fp.getChunkLoadRadius(); }
  @Override public void setChunkLoadRadius(int radius) { fp.setChunkLoadRadius(radius); }
  @Override public boolean isPveEnabled() { return fp.isPveEnabled(); }
  @Override public void setPveEnabled(boolean enabled) { fp.setPveEnabled(enabled); }
  @Override public double getPveRange() { return fp.getPveRange(); }
  @Override public void setPveRange(double range) { fp.setPveRange(range); }
  @Override public @Nullable String getPvePriority() { return fp.getPvePriority(); }
  @Override public void setPvePriority(@Nullable String priority) { fp.setPvePriority(priority); }
  @Override public @NotNull String getSpawnedBy() { return fp.getSpawnedBy(); }
  @Override public @NotNull UUID getSpawnedByUuid() { return fp.getSpawnedByUuid(); }
  @Override public boolean isOwnedBy(@NotNull UUID playerUuid) { return playerUuid.equals(fp.getSpawnedByUuid()); }
  @Override public boolean hasControllerAccess(@NotNull UUID playerUuid) { return isOwnedBy(playerUuid) || fp.hasSharedController(playerUuid); }
  @Override public @NotNull Set<UUID> getSharedControllerUuids() { return fp.getSharedControllers(); }
  @Override public boolean grantControllerAccess(@NotNull UUID playerUuid) { return fp.addSharedController(playerUuid); }
  @Override public boolean revokeControllerAccess(@NotNull UUID playerUuid) { return fp.removeSharedController(playerUuid); }
  @Override public @NotNull Duration getUptime() { return fp.getUptime(); }
  @Override public int getDeathCount() { return fp.getDeathCount(); }
  @Override public double getTotalDamageTaken() { return fp.getTotalDamageTaken(); }
  @Override public boolean isInWater() { return player() != null && player().isInWater(); }
  @Override public boolean isInLava() { return player() != null && player().isInLava(); }
  @Override public boolean isSprinting() { return player() != null && player().isSprinting(); }
  @Override public int getPing() { return fp.getEffectivePing(); }
  @Override public double getHealth() { return player() != null ? player().getHealth() : 0.0; }
  @Override public void setHealth(double health) { if (player() != null) player().setHealth(health); }
  @Override public double getMaxHealth() {
    if (player() == null) return 20.0;
    var attr = player().getAttribute(AttributeCompat.maxHealth());
    return attr == null ? 20.0 : attr.getValue();
  }
  @Override public void setMaxHealth(double health) {
    if (player() == null) return;
    var attr = player().getAttribute(AttributeCompat.maxHealth());
    if (attr != null) attr.setBaseValue(health);
  }
  @Override public boolean isDead() { return player() == null || player().isDead(); }
  @Override public @NotNull GameMode getGameMode() { return player() != null ? player().getGameMode() : GameMode.SURVIVAL; }
  @Override public void setGameMode(@NotNull GameMode mode) { if (player() != null) player().setGameMode(mode); }
  @Override public @Nullable PlayerInventory getInventory() { return player() != null ? player().getInventory() : null; }
  @Override public @Nullable ItemStack getItemInMainHand() { return getInventory() != null ? getInventory().getItemInMainHand() : null; }
  @Override public void setItemInMainHand(@Nullable ItemStack item) { if (getInventory() != null) getInventory().setItemInMainHand(item); }
  @Override public @Nullable ItemStack getItemInOffHand() { return getInventory() != null ? getInventory().getItemInOffHand() : null; }
  @Override public void setItemInOffHand(@Nullable ItemStack item) { if (getInventory() != null) getInventory().setItemInOffHand(item); }
  @Override public boolean teleport(@NotNull Location location) { return player() != null && player().teleport(location); }
  @Override public @NotNull Location getEyeLocation() { return player() != null ? player().getEyeLocation() : getLocation().clone().add(0, 1.62, 0); }
  @Override public void lookAt(@NotNull Location location) {
    if (player() == null) return;
    Location current = player().getLocation();
    current.setDirection(location.toVector().subtract(current.toVector()));
    player().teleport(current);
  }
  @Override public @NotNull Vector getVelocity() { return player() != null ? player().getVelocity() : new Vector(); }
  @Override public void setVelocity(@NotNull Vector velocity) { if (player() != null) player().setVelocity(velocity); }
  @Override public int getLevel() { return player() != null ? player().getLevel() : 0; }
  @Override public void setLevel(int level) { if (player() != null) player().setLevel(level); }
  @Override public float getExp() { return player() != null ? player().getExp() : 0.0f; }
  @Override public void setExp(float exp) { if (player() != null) player().setExp(exp); }
  @Override public int getTotalExperience() { return player() != null ? player().getTotalExperience() : 0; }
  @Override public void setTotalExperience(int exp) { if (player() != null) player().setTotalExperience(exp); }
  @Override public boolean isSleeping() { return fp.isSleeping(); }
  @Override public @Nullable Location getSleepOrigin() { return fp.getSleepOrigin(); }
  @Override public void setSleepOrigin(@Nullable Location origin) { fp.setSleepOrigin(origin); }
  @Override public double getSleepRadius() { return fp.getSleepRadius(); }
  @Override public void setSleepRadius(double radius) { fp.setSleepRadius(radius); }
  @Override public boolean isNavAvoidWater() { return fp.isNavAvoidWater(); }
  @Override public void setNavAvoidWater(boolean enabled) { fp.setNavAvoidWater(enabled); }
  @Override public boolean isNavAvoidLava() { return fp.isNavAvoidLava(); }
  @Override public void setNavAvoidLava(boolean enabled) { fp.setNavAvoidLava(enabled); }
  @Override public @NotNull String getBotTypeName() { return fp.getBotTypeName(); }
  @Override public void setBotTypeName(@NotNull String type) { fp.setBotTypeName(type); }
  @Override public @Nullable String getLuckpermsGroup() { return fp.getLuckpermsGroup(); }
  @Override public void setLuckpermsGroup(@Nullable String group) { fp.setLuckpermsGroup(group); }
  @Override public void sendMessage(@NotNull String message) { if (player() != null) player().sendMessage(message); }
  @Override public boolean hasPermission(@NotNull String permission) { return player() != null && player().hasPermission(permission); }
  @Override public boolean isOnline() { return player() != null && player().isOnline(); }
  @Override public void swingMainHand() { if (player() != null) player().swingMainHand(); }
  @Override public void swingOffHand() { if (player() != null) player().swingOffHand(); }
  @Override public boolean isSneaking() { return player() != null && player().isSneaking(); }
  @Override public void setSneaking(boolean sneaking) { if (player() != null) player().setSneaking(sneaking); }
  @Override public void setSprinting(boolean sprinting) { if (player() != null) player().setSprinting(sprinting); }
  @Override public boolean isOnGround() { return player() != null && player().isOnGround(); }
  @Override public boolean isClimbing() { return player() != null && player().isClimbing(); }
  @Override public boolean isPassenger() { return player() != null && player().isInsideVehicle(); }
  @Override public boolean hasVehicle() { return player() != null && player().getVehicle() != null; }
  @Override public double getReachDistance() { return 4.5; }
  @Override public void performRespawn() { if (player() != null) player().spigot().respawn(); }
  @Override public void setMetadata(@NotNull String key, @Nullable Object value) { fp.setMetadata(key, value); }
  @Override public @Nullable Object getMetadata(@NotNull String key) { return fp.getMetadata(key); }
  @Override public boolean hasMetadata(@NotNull String key) { return fp.hasMetadata(key); }
  @Override public void removeMetadata(@NotNull String key) { fp.removeMetadata(key); }
  @Override public @NotNull Map<String, Object> getMetadataMap() { return fp.getMetadataMap(); }

  private Player player() {
    return fp.getPlayer();
  }
}
