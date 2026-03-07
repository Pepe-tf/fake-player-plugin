package me.bill.fakePlayerPlugin.database;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a historical or live record of a fake player (bot).
 * Stored in the database for auditing and analytics.
 */
public class BotRecord {

    private final long    id;            // auto-increment PK from DB (0 for unsaved)
    private final String  botName;
    private final UUID    botUuid;
    private final String  spawnedBy;     // player name who ran /fpp spawn
    private final UUID    spawnedByUuid; // UUID of the spawning player
    private final String  worldName;
    private final double  spawnX;
    private final double  spawnY;
    private final double  spawnZ;
    private final float   spawnYaw;
    private final float   spawnPitch;
    // Last known position — updated periodically, used by persistence on restart
    private       String  lastWorld;
    private       double  lastX, lastY, lastZ;
    private       float   lastYaw, lastPitch;
    private final Instant spawnedAt;
    private       Instant removedAt;    // null while bot is still active
    private       String  removeReason; // "DELETED", "DIED", "SHUTDOWN", etc.

    public BotRecord(long id, String botName, UUID botUuid,
                     String spawnedBy, UUID spawnedByUuid,
                     String worldName,
                     double spawnX, double spawnY, double spawnZ,
                     float spawnYaw, float spawnPitch,
                     String lastWorld, double lastX, double lastY, double lastZ,
                     float lastYaw, float lastPitch,
                     Instant spawnedAt, Instant removedAt, String removeReason) {
        this.id             = id;
        this.botName        = botName;
        this.botUuid        = botUuid;
        this.spawnedBy      = spawnedBy;
        this.spawnedByUuid  = spawnedByUuid;
        this.worldName      = worldName;
        this.spawnX         = spawnX;
        this.spawnY         = spawnY;
        this.spawnZ         = spawnZ;
        this.spawnYaw       = spawnYaw;
        this.spawnPitch     = spawnPitch;
        this.lastWorld      = lastWorld != null ? lastWorld : worldName;
        this.lastX          = lastX;
        this.lastY          = lastY;
        this.lastZ          = lastZ;
        this.lastYaw        = lastYaw;
        this.lastPitch      = lastPitch;
        this.spawnedAt      = spawnedAt;
        this.removedAt      = removedAt;
        this.removeReason   = removeReason;
    }

    /** Convenience constructor — no last-location yet (set to spawn coords). */
    public BotRecord(long id, String botName, UUID botUuid,
                     String spawnedBy, UUID spawnedByUuid,
                     String worldName,
                     double spawnX, double spawnY, double spawnZ,
                     float spawnYaw, float spawnPitch,
                     Instant spawnedAt, Instant removedAt, String removeReason) {
        this(id, botName, botUuid, spawnedBy, spawnedByUuid, worldName,
             spawnX, spawnY, spawnZ, spawnYaw, spawnPitch,
             worldName, spawnX, spawnY, spawnZ, spawnYaw, spawnPitch,
             spawnedAt, removedAt, removeReason);
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public long    getId()             { return id; }
    public String  getBotName()        { return botName; }
    public UUID    getBotUuid()        { return botUuid; }
    public String  getSpawnedBy()      { return spawnedBy; }
    public UUID    getSpawnedByUuid()  { return spawnedByUuid; }
    public String  getWorldName()      { return worldName; }
    public double  getSpawnX()         { return spawnX; }
    public double  getSpawnY()         { return spawnY; }
    public double  getSpawnZ()         { return spawnZ; }
    public float   getSpawnYaw()       { return spawnYaw; }
    public float   getSpawnPitch()     { return spawnPitch; }
    public String  getLastWorld()      { return lastWorld; }
    public double  getLastX()          { return lastX; }
    public double  getLastY()          { return lastY; }
    public double  getLastZ()          { return lastZ; }
    public float   getLastYaw()        { return lastYaw; }
    public float   getLastPitch()      { return lastPitch; }
    public Instant getSpawnedAt()      { return spawnedAt; }
    public Instant getRemovedAt()      { return removedAt; }
    public String  getRemoveReason()   { return removeReason; }
    public boolean isActive()          { return removedAt == null; }

    // ── Setters (mutable after removal) ──────────────────────────────────────

    public void setLastLocation(String world, double x, double y, double z, float yaw, float pitch) {
        this.lastWorld = world; this.lastX = x; this.lastY = y; this.lastZ = z;
        this.lastYaw = yaw; this.lastPitch = pitch;
    }

    /** @deprecated Use {@link #setLastLocation(String, double, double, double, float, float)} */
    @Deprecated
    public void setLastLocation(String world, double x, double y, double z) {
        setLastLocation(world, x, y, z, this.lastYaw, this.lastPitch);
    }
    public void setRemovedAt(Instant ts)      { this.removedAt = ts; }
    public void setRemoveReason(String reason) { this.removeReason = reason; }

    @Override
    public String toString() {
        return "BotRecord{id=" + id + ", bot=" + botName + ", by=" + spawnedBy
                + ", world=" + worldName + ", active=" + isActive() + "}";
    }
}
