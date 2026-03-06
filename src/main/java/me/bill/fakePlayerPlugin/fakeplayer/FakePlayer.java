package me.bill.fakePlayerPlugin.fakeplayer;

import com.destroystokyo.paper.profile.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents a single fake player.
 * Holds the NMS ServerPlayer object so it is a real entity in the world.
 */
public final class FakePlayer {

    private static final AtomicInteger ENTITY_ID_COUNTER = new AtomicInteger(100_000);

    private final UUID          uuid;
    private final String        name;
    private final PlayerProfile profile;
    private final int           entityId;

    /** The actual NMS ServerPlayer — non-null after NmsHelper.addToServer() succeeds. */
    private Object nmsServerPlayer;

    /** The spawn location stored for syncing to late-joining players. */
    private Location spawnLocation;

    public FakePlayer(UUID uuid, String name, PlayerProfile profile) {
        this.uuid     = uuid;
        this.name     = name;
        this.profile  = profile;
        this.entityId = ENTITY_ID_COUNTER.getAndIncrement();
    }

    public UUID          getUuid()           { return uuid; }
    public String        getName()           { return name; }
    public PlayerProfile getProfile()        { return profile; }
    public int           getEntityId()       { return entityId; }
    public Object        getNmsPlayer()      { return nmsServerPlayer; }
    public Location      getSpawnLocation()  { return spawnLocation; }

    public void setNmsPlayer(Object nms)          { this.nmsServerPlayer = nms; }
    public void setSpawnLocation(Location loc)    { this.spawnLocation = loc; }

    /** Returns the player count contribution — 1 if the NMS player is set, else 0. */
    public int getPlayerCountContribution() { return nmsServerPlayer != null ? 1 : 0; }
}
