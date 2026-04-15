package me.bill.fakePlayerPlugin.util;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.regions.RegionQuery;

import org.bukkit.Location;
import org.bukkit.World;

public final class WorldGuardHelper {

    private WorldGuardHelper() {}

    public static boolean isPvpAllowed(Location location) {
        if (location == null || location.getWorld() == null) return true;
        try {
            RegionQuery query =
                    WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
            com.sk89q.worldedit.util.Location wgLoc = BukkitAdapter.adapt(location);

            com.sk89q.worldguard.protection.flags.StateFlag.State state =
                    query.queryState(wgLoc, null, Flags.PVP);
            return state != com.sk89q.worldguard.protection.flags.StateFlag.State.DENY;
        } catch (Exception e) {

            return true;
        }
    }

    public static Location findSafeLocation(World world) {
        if (world == null) return null;
        Location spawn = world.getSpawnLocation().clone().add(0.5, 0, 0.5);
        if (isPvpAllowed(spawn)) return spawn;
        return null;
    }
}
