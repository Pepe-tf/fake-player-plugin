package me.bill.fakePlayerPlugin.fakeplayer;

import com.destroystokyo.paper.profile.PlayerProfile;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.UUID;

/**
 * Represents a single fake player.
 * The physics zombie is the ONLY entity — its entity ID and UUID are reused
 * for the player-skin packet so clicking/punching the visual hits the zombie.
 */
public final class FakePlayer {

    private final String        name;
    private final PlayerProfile profile;

    private UUID     uuid;          // updated to zombie's UUID after body spawn
    private Location spawnLocation;
    private String   skinValue;
    private String   skinSignature;
    private Entity   physicsEntity;

    public FakePlayer(UUID uuid, String name, PlayerProfile profile) {
        this.uuid    = uuid;
        this.name    = name;
        this.profile = profile;
    }

    public UUID          getUuid()           { return uuid; }
    public String        getName()           { return name; }
    public PlayerProfile getProfile()        { return profile; }
    public Location      getSpawnLocation()  { return spawnLocation; }
    public String        getSkinValue()      { return skinValue; }
    public String        getSkinSignature()  { return skinSignature; }
    public Entity        getPhysicsEntity()  { return physicsEntity; }

    /**
     * Returns the entity ID to use in packets — the zombie's real ID,
     * so that client interactions target the actual entity.
     */
    public int getEntityId() {
        return physicsEntity != null ? physicsEntity.getEntityId() : -1;
    }

    public void setSpawnLocation(Location loc)           { this.spawnLocation = loc; }
    public void setSkin(String value, String signature)  { this.skinValue = value; this.skinSignature = signature; }
    public void setPhysicsEntity(Entity entity)          { this.physicsEntity = entity; }
    /** Called after the zombie spawns to synchronise the UUID. */
    public void setBodyUUID(UUID id)                     { this.uuid = id; }
}
