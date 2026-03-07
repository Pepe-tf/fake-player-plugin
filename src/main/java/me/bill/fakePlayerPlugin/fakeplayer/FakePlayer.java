package me.bill.fakePlayerPlugin.fakeplayer;

import com.destroystokyo.paper.profile.PlayerProfile;
import me.bill.fakePlayerPlugin.database.BotRecord;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mannequin;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a single active fake player.
 *
 * <p>Entity stack (two entities total):
 * <pre>
 *   ArmorStand  — invisible marker, custom name visible, rides the Mannequin
 *       ↓ rides
 *   Mannequin   — physics body + skin.
 *                 setImmovable(false)   → vanilla entity-separation push/knockback
 *                 setGravity(true)      → falls naturally
 *                 setInvulnerable(false)→ takes damage
 *                 setProfile(name)      → client resolves skin automatically
 * </pre>
 */
@SuppressWarnings("unused") // Public API — used by addons and InfoCommand
public final class FakePlayer {

    private final String        name;
    private final PlayerProfile profile;

    private final UUID       uuid;
    private Location   spawnLocation;

    /** The Mannequin — physics body AND visual skin. */
    private Entity     physicsEntity;

    /** Invisible ArmorStand riding the Mannequin — displays the nametag. */
    private ArmorStand nametagEntity;

    // ── Metadata ─────────────────────────────────────────────────────────────
    private String    spawnedBy     = "UNKNOWN";
    private UUID      spawnedByUuid = new UUID(0, 0);
    private BotRecord dbRecord;
    private Instant   spawnTime     = Instant.now();

    /**
     * Optional display name shown in the nametag and tab list.
     * When {@code null}, {@link #getName()} is used for display.
     * User-tier bots set this to "[bot] PlayerName" / "[bot] PlayerName #N"
     * while keeping {@link #name} as a valid Minecraft identifier.
     */
    private String    displayName   = null;

    public FakePlayer(UUID uuid, String name, PlayerProfile profile) {
        this.uuid    = uuid;
        this.name    = name;
        this.profile = profile;
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public UUID          getUuid()           { return uuid; }
    public String        getName()           { return name; }
    public PlayerProfile getProfile()        { return profile; }
    public Location      getSpawnLocation()  { return spawnLocation; }
    public Entity        getPhysicsEntity()  { return physicsEntity; }
    public ArmorStand    getNametagEntity()  { return nametagEntity; }

    /**
     * The text shown in the nametag and tab list.
     * Falls back to {@link #getName()} when no display name is set.
     * User-tier bots return {@code "[bot] PlayerName"} or {@code "[bot] PlayerName #N"}.
     */
    public String        getDisplayName()    { return displayName != null ? displayName : name; }

    /** Convenience cast — physicsEntity is always a Mannequin. */
    public Mannequin getMannequin() {
        return (physicsEntity instanceof Mannequin m) ? m : null;
    }

    /** Entity ID of the Mannequin (used in tab-list packets). */
    public int getEntityId() {
        return physicsEntity != null ? physicsEntity.getEntityId() : -1;
    }

    // ── Setters ───────────────────────────────────────────────────────────────
    public void setSpawnLocation(Location loc)   { this.spawnLocation  = loc; }
    public void setPhysicsEntity(Entity e)        { this.physicsEntity  = e; }
    public void setNametagEntity(ArmorStand as)   { this.nametagEntity  = as; }
    public void setDisplayName(String name)       { this.displayName    = name; }

    // ── Metadata ──────────────────────────────────────────────────────────────
    public String    getSpawnedBy()              { return spawnedBy; }
    public UUID      getSpawnedByUuid()          { return spawnedByUuid; }
    public BotRecord getDbRecord()               { return dbRecord; }
    public Instant   getSpawnTime()              { return spawnTime; }

    public void setSpawnedBy(String name, UUID uuid) {
        this.spawnedBy     = name;
        this.spawnedByUuid = uuid;
    }
    public void setDbRecord(BotRecord record)    { this.dbRecord  = record; }
    public void setSpawnTime(Instant t)          { this.spawnTime = t; }
}
