package me.bill.fakePlayerPlugin.fakeplayer;

import me.bill.fakePlayerPlugin.util.FppLogger;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.lang.reflect.Method;

/**
 * NmsHelper - safely resolves the NMS classloader and provides
 * a handle to the server-level object for world access.
 *
 * <p>We NO LONGER attempt to call {@code PlayerList.placeNewPlayer()} with a
 * fake {@code Connection}; that approach always crashes the server because
 * Paper 1.21 validates the connection immediately.  Fake players are instead
 * managed entirely through packets via {@link PacketHelper}.</p>
 */
public final class NmsHelper {

    private NmsHelper() {}

    // ── Classloader discovery ─────────────────────────────────────────────

    /**
     * Returns a {@link ClassLoader} that can load NMS classes, or {@code null}
     * if none is found.  Only safe, read-only operations are used here.
     */
    public static ClassLoader findNmsClassLoader() {
        // Strategy 1: walk the CraftServer → MinecraftServer class hierarchy
        try {
            Class<?> craftServerClass = getCraftClass("CraftServer");
            Method getServer = craftServerClass.getMethod("getServer");
            Object mcServer  = getServer.invoke(craftServerClass.cast(Bukkit.getServer()));
            Class<?> c = mcServer.getClass();
            while (c != null && c != Object.class) {
                ClassLoader cl = c.getClassLoader();
                if (cl != null && canLoadNms(cl)) return cl;
                c = c.getSuperclass();
            }
        } catch (Exception ignored) {}

        // Strategy 2: NMS handle of any online player
        try {
            Class<?> craftPlayerClass = getCraftClass("entity.CraftPlayer");
            Method getHandle = craftPlayerClass.getMethod("getHandle");
            for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
                try {
                    Object nmsPlayer = getHandle.invoke(craftPlayerClass.cast(p));
                    Class<?> c = nmsPlayer.getClass();
                    while (c != null && c != Object.class) {
                        ClassLoader cl = c.getClassLoader();
                        if (cl != null && canLoadNms(cl)) return cl;
                        c = c.getSuperclass();
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}

        // Strategy 3: thread context classloader chain
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        while (cl != null) {
            if (canLoadNms(cl)) return cl;
            cl = cl.getParent();
        }

        return null;
    }

    private static boolean canLoadNms(ClassLoader cl) {
        for (String probe : new String[]{
                "net.minecraft.server.players.PlayerList",
                "net.minecraft.server.MinecraftServer",
                "net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket"}) {
            try { cl.loadClass(probe); return true; }
            catch (ClassNotFoundException ignored) {}
        }
        return false;
    }

    // ── World handle helper (used by PacketHelper / entity spawning) ──────

    /**
     * Returns the NMS {@code ServerLevel} for the given Bukkit world, or
     * {@code null} on failure.
     */
    public static Object getServerLevel(org.bukkit.World world) {
        try {
            Class<?> craftWorldClass = getCraftClass("CraftWorld");
            Method getHandle = craftWorldClass.getMethod("getHandle");
            return getHandle.invoke(craftWorldClass.cast(world));
        } catch (Exception e) {
            FppLogger.warn("NmsHelper.getServerLevel failed: " + e.getMessage());
            return null;
        }
    }

    // ── Utility ───────────────────────────────────────────────────────────

    static Class<?> getCraftClass(String suffix) throws ClassNotFoundException {
        try { return Class.forName("org.bukkit.craftbukkit." + suffix); }
        catch (ClassNotFoundException ignored) {}
        String[] parts = Bukkit.getServer().getClass().getPackage().getName().split("\\.");
        if (parts.length >= 4)
            return Class.forName("org.bukkit.craftbukkit." + parts[3] + "." + suffix);
        throw new ClassNotFoundException("Cannot resolve CraftBukkit class: " + suffix);
    }
}

