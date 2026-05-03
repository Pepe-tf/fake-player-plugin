package me.bill.fakePlayerPlugin.fakeplayer;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public final class FakePlayer {

  private final UUID uuid;
  private final String name;
  private final Instant spawnTime = Instant.now();
  private final Map<String, Object> metadata = new ConcurrentHashMap<>();
  private final Set<UUID> sharedControllers = ConcurrentHashMap.newKeySet();

  private Player player;
  private Location spawnLocation;
  private String displayName;
  private String spawnedBy = "CONSOLE";
  private UUID spawnedByUuid = new UUID(0, 0);
  private int deathCount;
  private boolean alive = true;
  private boolean respawning;
  private boolean respawnOnDeath = me.bill.fakePlayerPlugin.config.Config.respawnOnDeath();
  private boolean frozen;
  private boolean chatEnabled = true;
  private String chatTier;
  private String aiPersonality;
  private boolean headAiEnabled = true;
  private boolean swimAiEnabled = true;
  private boolean pickUpItemsEnabled = true;
  private boolean pickUpXpEnabled = true;
  private boolean navParkour;
  private boolean navBreakBlocks;
  private boolean navPlaceBlocks;
  private boolean navAvoidWater;
  private boolean navAvoidLava;
  private int chunkLoadRadius = -1;
  private boolean pveEnabled;
  private double pveRange = 16.0;
  private String pvePriority;
  private String luckpermsGroup;
  private String botTypeName = BotType.AFK.name();
  private Location sleepOrigin;
  private double sleepRadius;

  public FakePlayer(UUID uuid, String name) {
    this.uuid = uuid;
    this.name = name;
  }

  public UUID getUuid() { return uuid; }
  public String getName() { return name; }
  public String getDisplayName() { return displayName != null ? displayName : name; }
  public void setDisplayName(String displayName) { this.displayName = displayName; }
  public Player getPlayer() { return player; }
  public Player getPhysicsEntity() { return player; }
  public void setPlayer(Player player) { this.player = player; }
  public Location getSpawnLocation() { return spawnLocation; }
  public void setSpawnLocation(Location spawnLocation) { this.spawnLocation = spawnLocation; }
  public Location getLiveLocation() { return player != null ? player.getLocation() : spawnLocation; }
  public Instant getSpawnTime() { return spawnTime; }
  public Duration getUptime() { return Duration.between(spawnTime, Instant.now()); }
  public String getSpawnedBy() { return spawnedBy; }
  public void setSpawnedBy(String spawnedBy) { this.spawnedBy = spawnedBy; }
  public UUID getSpawnedByUuid() { return spawnedByUuid; }
  public void setSpawnedByUuid(UUID spawnedByUuid) { this.spawnedByUuid = spawnedByUuid; }
  public boolean isBodyless() { return player == null; }
  public boolean isFrozen() { return frozen; }
  public void setFrozen(boolean frozen) { this.frozen = frozen; }
  public boolean isAlive() { return alive && player != null && !player.isDead(); }
  public void setAlive(boolean alive) { this.alive = alive; }
  public boolean isRespawning() { return respawning; }
  public void setRespawning(boolean respawning) { this.respawning = respawning; }
  public boolean isRespawnOnDeath() { return respawnOnDeath; }
  public void setRespawnOnDeath(boolean respawnOnDeath) { this.respawnOnDeath = respawnOnDeath; }
  public boolean isChatEnabled() { return chatEnabled; }
  public void setChatEnabled(boolean chatEnabled) { this.chatEnabled = chatEnabled; }
  public String getChatTier() { return chatTier; }
  public void setChatTier(String chatTier) { this.chatTier = chatTier; }
  public String getAiPersonality() { return aiPersonality; }
  public void setAiPersonality(String aiPersonality) { this.aiPersonality = aiPersonality; }
  public boolean isHeadAiEnabled() { return headAiEnabled; }
  public void setHeadAiEnabled(boolean headAiEnabled) { this.headAiEnabled = headAiEnabled; }
  public boolean isSwimAiEnabled() { return swimAiEnabled; }
  public void setSwimAiEnabled(boolean swimAiEnabled) { this.swimAiEnabled = swimAiEnabled; }
  public boolean isPickUpItemsEnabled() { return pickUpItemsEnabled; }
  public void setPickUpItemsEnabled(boolean pickUpItemsEnabled) { this.pickUpItemsEnabled = pickUpItemsEnabled; }
  public boolean isPickUpXpEnabled() { return pickUpXpEnabled; }
  public void setPickUpXpEnabled(boolean pickUpXpEnabled) { this.pickUpXpEnabled = pickUpXpEnabled; }
  public boolean isNavParkour() { return navParkour; }
  public void setNavParkour(boolean navParkour) { this.navParkour = navParkour; }
  public boolean isNavBreakBlocks() { return navBreakBlocks; }
  public void setNavBreakBlocks(boolean navBreakBlocks) { this.navBreakBlocks = navBreakBlocks; }
  public boolean isNavPlaceBlocks() { return navPlaceBlocks; }
  public void setNavPlaceBlocks(boolean navPlaceBlocks) { this.navPlaceBlocks = navPlaceBlocks; }
  public boolean isNavAvoidWater() { return navAvoidWater; }
  public void setNavAvoidWater(boolean navAvoidWater) { this.navAvoidWater = navAvoidWater; }
  public boolean isNavAvoidLava() { return navAvoidLava; }
  public void setNavAvoidLava(boolean navAvoidLava) { this.navAvoidLava = navAvoidLava; }
  public int getChunkLoadRadius() { return chunkLoadRadius; }
  public void setChunkLoadRadius(int chunkLoadRadius) { this.chunkLoadRadius = chunkLoadRadius; }
  public boolean isPveEnabled() { return pveEnabled; }
  public void setPveEnabled(boolean pveEnabled) { this.pveEnabled = pveEnabled; }
  public double getPveRange() { return pveRange; }
  public void setPveRange(double pveRange) { this.pveRange = pveRange; }
  public String getPvePriority() { return pvePriority; }
  public void setPvePriority(String pvePriority) { this.pvePriority = pvePriority; }
  public int getEffectivePing() { return player != null ? player.getPing() : -1; }
  public int getDeathCount() { return deathCount; }
  public void incrementDeathCount() { deathCount++; }
  public double getTotalDamageTaken() { return 0.0; }
  public Set<UUID> getSharedControllers() { return Set.copyOf(sharedControllers); }
  public boolean hasSharedController(UUID uuid) { return sharedControllers.contains(uuid); }
  public boolean addSharedController(UUID uuid) { return sharedControllers.add(uuid); }
  public boolean removeSharedController(UUID uuid) { return sharedControllers.remove(uuid); }
  public String getBotTypeName() { return botTypeName; }
  public void setBotTypeName(String botTypeName) { this.botTypeName = botTypeName; }
  public String getLuckpermsGroup() { return luckpermsGroup; }
  public void setLuckpermsGroup(String luckpermsGroup) { this.luckpermsGroup = luckpermsGroup; }
  public boolean isSleeping() { return false; }
  public Location getSleepOrigin() { return sleepOrigin; }
  public void setSleepOrigin(@Nullable Location sleepOrigin) { this.sleepOrigin = sleepOrigin; }
  public double getSleepRadius() { return sleepRadius; }
  public void setSleepRadius(double sleepRadius) { this.sleepRadius = sleepRadius; }
  public void setMetadata(String key, Object value) {
    if (value == null) metadata.remove(key);
    else metadata.put(key, value);
  }
  public Object getMetadata(String key) { return metadata.get(key); }
  public boolean hasMetadata(String key) { return metadata.containsKey(key); }
  public void removeMetadata(String key) { metadata.remove(key); }
  public Map<String, Object> getMetadataMap() { return Map.copyOf(metadata); }
}
