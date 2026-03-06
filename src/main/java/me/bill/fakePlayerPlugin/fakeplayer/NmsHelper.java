package me.bill.fakePlayerPlugin.fakeplayer;

import me.bill.fakePlayerPlugin.FakePlayerPlugin;
import me.bill.fakePlayerPlugin.util.FppLogger;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.lang.reflect.*;
import java.util.UUID;

/**
 * Creates and manages real NMS ServerPlayer objects for fake players.
 *
 * Using a real ServerPlayer means:
 *  - Automatically counted in the server list (player count)
 *  - Automatically in the tab list for ALL players (past and future)
 *  - A real entity in the world that persists across player rejoins
 *  - Join/quit messages handled by vanilla
 *
 * All NMS access is via reflection — zero compile-time NMS dependency.
 */
public final class NmsHelper {

    private NmsHelper() {}

    private static boolean ready  = false;
    private static boolean broken = false;

    // Cached classes
    private static Class<?> minecraftServerClass;
    private static Class<?> serverPlayerClass;
    private static Class<?> playerListClass;
    private static Class<?> gameProfileClass;
    private static Class<?> serverLevelClass;
    private static Class<?> connectionClass;       // net.minecraft.network.Connection
    private static Class<?> packetFlowClass;       // net.minecraft.network.PacketFlow
    private static ClassLoader nmsLoader;           // server classloader — bypasses Paper's rewriter

    // Cached methods / fields
    private static Method  getServerMethod;
    private static Method  getPlayerListMethod;
    private static Method  placeNewPlayerMethod;
    private static Method  removeMethod;
    private static Method  getHandleWorldMethod;
    private static Object  craftServerInstance;

    private static synchronized void init() {
        if (ready || broken) return;
        try {
            Class<?> craftServerClass = getCraftClass("CraftServer");
            Class<?> craftWorldClass  = getCraftClass("CraftWorld");

            craftServerInstance  = craftServerClass.cast(Bukkit.getServer());
            getServerMethod      = craftServerClass.getMethod("getServer");
            getHandleWorldMethod = craftWorldClass.getMethod("getHandle");

            nmsLoader = findNmsClassLoader(craftServerClass);
            if (nmsLoader == null) {
                throw new IllegalStateException("Cannot find NMS classloader");
            }
            FppLogger.info("NmsHelper: loader = " + nmsLoader.getClass().getName());

            gameProfileClass     = nmsLoader.loadClass("com.mojang.authlib.GameProfile");
            minecraftServerClass = nmsLoader.loadClass("net.minecraft.server.MinecraftServer");
            serverPlayerClass    = nmsLoader.loadClass("net.minecraft.server.level.ServerPlayer");
            playerListClass      = nmsLoader.loadClass("net.minecraft.server.players.PlayerList");
            serverLevelClass     = nmsLoader.loadClass("net.minecraft.server.level.ServerLevel");
            connectionClass      = nmsLoader.loadClass("net.minecraft.network.Connection");
            packetFlowClass      = nmsLoader.loadClass("net.minecraft.network.PacketFlow");

            getPlayerListMethod  = minecraftServerClass.getMethod("getPlayerList");
            removeMethod         = playerListClass.getMethod("remove", serverPlayerClass);

            placeNewPlayerMethod = findPlaceNewPlayer();
            if (placeNewPlayerMethod == null) {
                throw new IllegalStateException("Could not find PlayerList.placeNewPlayer");
            }

            ready = true;
            FppLogger.info("NmsHelper ready (real ServerPlayer support).");
        } catch (Exception e) {
            broken = true;
            FppLogger.warn("NmsHelper init failed (" + e.getMessage() + ").");
            e.printStackTrace();
        }
    }

    /**
     * Gets the NMS classloader. Paper loads net.minecraft.* in an isolated
     * classloader. We find it by:
     *
     * 1. Invoking CraftServer.getServer() to get the DedicatedServer instance,
     *    then calling getPlayerList() on it — PlayerList is an NMS class loaded
     *    by the NMS classloader, so its classloader IS the one we want.
     *
     * 2. Walking the CraftServer superclass chain to find a class whose loader
     *    can load NMS classes (CraftServer extends CraftBukkit which extends
     *    MinecraftServer in some builds).
     *
     * 3. Trying every registered online player's NMS handle.
     *
     * The probe class used is "net.minecraft.server.players.PlayerList" which is
     * loaded by Paper's NMS loader and is NOT intercepted by the reflection rewriter
     * (unlike PacketFlow which has a remapped name).
     */
    private static ClassLoader findNmsClassLoader(Class<?> craftServerClass) {

        // ── Strategy 1: getServer() → PlayerList class's classloader ──────────
        // CraftServer.getServer() returns a DedicatedServer (NMS class).
        // Calling getPlayerList() on it returns a PlayerList (NMS class).
        // That object's class was loaded by the NMS classloader.
        try {
            Method getServer = craftServerClass.getMethod("getServer");
            Object mcServer  = getServer.invoke(craftServerClass.cast(Bukkit.getServer()));

            // mcServer is DedicatedServer extends MinecraftServer.
            // Its class is loaded by the NMS classloader.
            ClassLoader cl = mcServer.getClass().getClassLoader();
            FppLogger.info("NmsHelper: mcServer loader = " + cl.getClass().getName());

            // Try to load a known NMS class from this loader
            // Use a class name that Paper does NOT remap/intercept
            for (String probe : new String[]{
                    "net.minecraft.server.players.PlayerList",
                    "net.minecraft.server.level.ServerLevel",
                    "net.minecraft.server.MinecraftServer",
                    "net.minecraft.network.Connection"}) {
                try {
                    cl.loadClass(probe);
                    FppLogger.info("NmsHelper: probe '" + probe + "' succeeded on mcServer loader");
                    return cl;
                } catch (ClassNotFoundException ignored) {}
            }

            // Walk parent chain
            ClassLoader parent = cl.getParent();
            while (parent != null) {
                for (String probe : new String[]{
                        "net.minecraft.server.players.PlayerList",
                        "net.minecraft.server.MinecraftServer"}) {
                    try {
                        parent.loadClass(probe);
                        FppLogger.info("NmsHelper: found NMS loader via parent: " + parent.getClass().getName());
                        return parent;
                    } catch (ClassNotFoundException ignored) {}
                }
                parent = parent.getParent();
            }
        } catch (Exception e) {
            FppLogger.warn("NmsHelper strategy 1 failed: " + e.getMessage());
        }

        // ── Strategy 2: use getPlayerList() return value's classloader ────────
        try {
            Method getServer     = craftServerClass.getMethod("getServer");
            Object mcServer      = getServer.invoke(craftServerClass.cast(Bukkit.getServer()));
            // Iterate all public methods of mcServer looking for getPlayerList
            for (Method m : mcServer.getClass().getMethods()) {
                if (m.getName().equals("getPlayerList") && m.getParameterCount() == 0) {
                    Object playerList = m.invoke(mcServer);
                    if (playerList != null) {
                        ClassLoader cl = playerList.getClass().getClassLoader();
                        FppLogger.info("NmsHelper: playerList loader = " + cl.getClass().getName());
                        if (canLoadAnyNms(cl)) return cl;
                    }
                    break;
                }
            }
        } catch (Exception e) {
            FppLogger.warn("NmsHelper strategy 2 failed: " + e.getMessage());
        }

        // ── Strategy 3: online player NMS handle ─────────────────────────────
        try {
            Class<?> craftPlayerClass = getCraftClass("entity.CraftPlayer");
            for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
                try {
                    Method getHandle = craftPlayerClass.getMethod("getHandle");
                    Object nmsPlayer = getHandle.invoke(craftPlayerClass.cast(p));
                    ClassLoader cl = nmsPlayer.getClass().getClassLoader();
                    FppLogger.info("NmsHelper: player handle loader = " + cl.getClass().getName());
                    if (canLoadAnyNms(cl)) return cl;
                    // Also try parent
                    if (cl.getParent() != null && canLoadAnyNms(cl.getParent())) return cl.getParent();
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}

        // ── Strategy 4: plugin's own classloader parents ──────────────────────
        ClassLoader cl = FakePlayerPlugin.getInstance().getClass().getClassLoader();
        while (cl != null) {
            FppLogger.info("NmsHelper: trying classloader " + cl.getClass().getName());
            if (canLoadAnyNms(cl)) return cl;
            cl = cl.getParent();
        }

        return null;
    }

    private static boolean canLoadAnyNms(ClassLoader cl) {
        for (String probe : new String[]{
                "net.minecraft.server.players.PlayerList",
                "net.minecraft.server.level.ServerLevel",
                "net.minecraft.server.MinecraftServer",
                "net.minecraft.network.Connection",
                "net.minecraft.network.PacketFlow"}) {
            try { cl.loadClass(probe); return true; }
            catch (ClassNotFoundException ignored) {}
        }
        return false;
    }

    // (findNmsClassLoader with args above handles all strategies)

    /**
     * Adds a fake player as a real ServerPlayer to the server.
     * Returns the NMS ServerPlayer object, or null on failure.
     */
    public static Object addFakePlayer(FakePlayer fp, Location loc) {
        init();
        if (broken) return null;
        try {
            Object mcServer    = getServerMethod.invoke(craftServerInstance);
            Object playerList  = getPlayerListMethod.invoke(mcServer);
            Object serverLevel = getHandleWorldMethod.invoke(loc.getWorld());

            // Build a GameProfile
            Object profile = gameProfileClass
                    .getConstructor(UUID.class, String.class)
                    .newInstance(fp.getUuid(), fp.getName());

            // Create the ServerPlayer — constructor signature varies across Paper builds.
            // Scan all constructors and pick the one containing (MinecraftServer, ServerLevel, GameProfile).
            Constructor<?> spCtor = findServerPlayerConstructor();
            if (spCtor == null) {
                // Log all available constructors so we can debug
                StringBuilder sb = new StringBuilder("ServerPlayer constructors:\n");
                for (Constructor<?> c : serverPlayerClass.getDeclaredConstructors()) {
                    sb.append("  ").append(c).append("\n");
                }
                FppLogger.warn(sb.toString());
                throw new IllegalStateException("Cannot find suitable ServerPlayer constructor");
            }
            spCtor.setAccessible(true);

            // Build args — fill MinecraftServer, ServerLevel, GameProfile; null everything else
            Object serverPlayer = invokeServerPlayerCtor(spCtor, mcServer, serverLevel, profile);

            // Position the player
            setPosition(serverPlayer, loc);

            // Build a real Connection backed by a local Netty channel
            Object connection = createFakeConnection();
            if (connection == null) {
                throw new IllegalStateException("Could not create fake Connection");
            }

            // Add to the server via placeNewPlayer
            invokePlaceNewPlayer(playerList, serverPlayer, connection);

            FppLogger.info("NmsHelper: added real ServerPlayer for " + fp.getName());
            return serverPlayer;
        } catch (Exception e) {
            FppLogger.warn("NmsHelper.addFakePlayer failed: " + e.getMessage() + " — using packet fallback.");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Removes a fake player's ServerPlayer from the server gracefully.
     */
    public static void removeFakePlayer(FakePlayer fp) {
        init();
        if (broken || fp.getNmsPlayer() == null) return;
        try {
            Object mcServer   = getServerMethod.invoke(craftServerInstance);
            Object playerList = getPlayerListMethod.invoke(mcServer);
            removeMethod.invoke(playerList, fp.getNmsPlayer());
            FppLogger.info("NmsHelper: removed real ServerPlayer for " + fp.getName());
        } catch (Exception e) {
            FppLogger.warn("NmsHelper.removeFakePlayer failed: " + e.getMessage());
        }
    }

    // ── Connection factory ────────────────────────────────────────────────

    /**
     * Creates a net.minecraft.network.Connection backed by an embedded Netty
     * LocalChannel so placeNewPlayer never hits a NullPointerException on
     * connection.getLoggableAddress().
     */
    private static Object createFakeConnection() {
        try {
            // PacketFlow.SERVERBOUND
            Object serverBound = null;
            for (Object constant : packetFlowClass.getEnumConstants()) {
                if (constant.toString().equals("SERVERBOUND")) { serverBound = constant; break; }
            }

            // new Connection(PacketFlow.SERVERBOUND)
            Constructor<?> connCtor = connectionClass.getDeclaredConstructor(packetFlowClass);
            connCtor.setAccessible(true);
            Object connection = connCtor.newInstance(serverBound);

            // Wire a Netty EmbeddedChannel so channel/address are never null
            // Netty is loaded by the server classloader too
            Class<?> embeddedChannelClass = nmsLoader.loadClass("io.netty.channel.embedded.EmbeddedChannel");
            Object embeddedChannel = embeddedChannelClass.getConstructor().newInstance();

            // Set channel field by type (io.netty.channel.Channel)
            Class<?> channelClass = nmsLoader.loadClass("io.netty.channel.Channel");
            setFieldByType(connection, connectionClass, channelClass, embeddedChannel);

            // Set address field — Connection.address or the field of type SocketAddress
            java.net.InetSocketAddress loopback = new java.net.InetSocketAddress("127.0.0.1", 25565);
            Field addressField = findField(connectionClass, "address");
            if (addressField != null) {
                addressField.set(connection, loopback);
            } else {
                // Try finding by type
                setFieldByType(connection, connectionClass, java.net.SocketAddress.class, loopback);
            }

            return connection;
        } catch (Exception e) {
            FppLogger.warn("createFakeConnection failed: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // ── Internal helpers ──────────────────────────────────────────────────

    private static void setPosition(Object serverPlayer, Location loc) {
        try {
            // Entity.setPos(double x, double y, double z)
            Method setPos = serverPlayer.getClass().getMethod("setPos", double.class, double.class, double.class);
            setPos.invoke(serverPlayer, loc.getX(), loc.getY(), loc.getZ());
        } catch (Exception ignored) {}
    }

    private static Method findPlaceNewPlayer() {
        // placeNewPlayer signature varies across Paper builds:
        // 1.21.x: placeNewPlayer(Connection, ServerPlayer, CommonListenerCookie)
        // We find it by name and pick the one that takes ServerPlayer as 2nd param
        for (Method m : playerListClass.getMethods()) {
            if (!m.getName().equals("placeNewPlayer")) continue;
            Class<?>[] p = m.getParameterTypes();
            for (Class<?> t : p) {
                if (t == serverPlayerClass) return m;
            }
        }
        return null;
    }

    private static void invokePlaceNewPlayer(Object playerList,
                                              Object serverPlayer,
                                              Object connection) throws Exception {
        Class<?>[] params = placeNewPlayerMethod.getParameterTypes();
        Object[] args = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            Class<?> p = params[i];
            if (p == serverPlayerClass) {
                args[i] = serverPlayer;
            } else if (p.isAssignableFrom(connectionClass) || connectionClass.isAssignableFrom(p) || p == connectionClass) {
                args[i] = connection;
            } else {
                args[i] = createDefaultInstance(p);
            }
        }
        placeNewPlayerMethod.invoke(playerList, args);
    }

    /** Finds the ServerPlayer constructor that takes MinecraftServer + ServerLevel + GameProfile. */
    private static Constructor<?> findServerPlayerConstructor() {
        Constructor<?>[] ctors = serverPlayerClass.getDeclaredConstructors();
        // Always log what we find so mismatches are easy to debug
        StringBuilder sb = new StringBuilder("ServerPlayer constructors found:\n");
        for (Constructor<?> c : ctors) {
            sb.append("  (");
            Class<?>[] p = c.getParameterTypes();
            for (int i = 0; i < p.length; i++) {
                sb.append(p[i].getSimpleName());
                if (i < p.length - 1) sb.append(", ");
            }
            sb.append(")\n");
        }
        FppLogger.info(sb.toString());

        for (Constructor<?> c : ctors) {
            boolean hasServer = false, hasLevel = false, hasProfile = false;
            for (Class<?> p : c.getParameterTypes()) {
                if (p == minecraftServerClass || minecraftServerClass.isAssignableFrom(p)) hasServer = true;
                if (p == serverLevelClass)  hasLevel   = true;
                if (p == gameProfileClass)  hasProfile = true;
            }
            if (hasServer && hasLevel && hasProfile) return c;
        }
        return null;
    }

    /**
     * Invokes the ServerPlayer constructor, filling known params and
     * using null / defaults for anything unknown (e.g. ClientInformation added in 1.21.2+).
     */
    private static Object invokeServerPlayerCtor(Constructor<?> ctor,
                                                  Object mcServer,
                                                  Object serverLevel,
                                                  Object profile) throws Exception {
        Class<?>[] types = ctor.getParameterTypes();
        Object[] args = new Object[types.length];
        for (int i = 0; i < types.length; i++) {
            Class<?> t = types[i];
            if (t == minecraftServerClass || minecraftServerClass.isAssignableFrom(t)) {
                args[i] = mcServer;
            } else if (t == serverLevelClass) {
                args[i] = serverLevel;
            } else if (t == gameProfileClass) {
                args[i] = profile;
            } else {
                // ClientInformation or other new params — try default instance, else null
                args[i] = createDefaultInstance(t);
            }
        }
        return ctor.newInstance(args);
    }

    /** Tries to create a default instance of a class, returning null if impossible. */
    private static Object createDefaultInstance(Class<?> type) {
        if (type.isPrimitive()) return null;

        String simpleName = type.getSimpleName();

        // ClientInformation — added in 1.21.2, has a createDefault() static factory
        if (simpleName.equals("ClientInformation")) {
            try {
                Method m = type.getMethod("createDefault");
                return m.invoke(null);
            } catch (Exception ignored) {}
            // Fallback: try all static no-arg methods returning the same type
            for (Method m : type.getMethods()) {
                if (m.getParameterCount() == 0 && type.isAssignableFrom(m.getReturnType())) {
                    try { return m.invoke(null); } catch (Exception ignored) {}
                }
            }
        }

        // Try static factory methods with common names
        for (String name : new String[]{"createDefault", "getDefault", "defaultValue", "empty", "of", "a"}) {
            try {
                Method m = type.getMethod(name);
                if (type.isAssignableFrom(m.getReturnType())) return m.invoke(null);
            } catch (Exception ignored) {}
        }

        // Try no-arg constructor
        try {
            Constructor<?> c = type.getDeclaredConstructor();
            c.setAccessible(true);
            return c.newInstance();
        } catch (Exception ignored) {}

        FppLogger.info("createDefaultInstance: could not create " + type.getName() + " — passing null");
        return null;
    }

    private static Field findField(Class<?> clazz, String name) {
        while (clazz != null) {
            try { Field f = clazz.getDeclaredField(name); f.setAccessible(true); return f; }
            catch (NoSuchFieldException ignored) { clazz = clazz.getSuperclass(); }
        }
        return null;
    }

    /** Sets the first field in clazz whose type is assignable from targetType. */
    private static void setFieldByType(Object instance, Class<?> clazz,
                                        Class<?> targetType, Object value) {
        Class<?> c = clazz;
        while (c != null) {
            for (Field f : c.getDeclaredFields()) {
                if (targetType.isAssignableFrom(f.getType()) || f.getType().isAssignableFrom(targetType)) {
                    try {
                        f.setAccessible(true);
                        f.set(instance, value);
                        return;
                    } catch (Exception ignored) {}
                }
            }
            c = c.getSuperclass();
        }
    }

    private static void logAllConstructors() {
        StringBuilder sb = new StringBuilder("ServerPlayer constructors (debug):\n");
        for (Constructor<?> c : serverPlayerClass.getDeclaredConstructors())
            sb.append("  ").append(c).append("\n");
        FppLogger.warn(sb.toString());
    }

    private static Class<?> getCraftClass(String name) throws ClassNotFoundException {
        try { return Class.forName("org.bukkit.craftbukkit." + name); }
        catch (ClassNotFoundException ignored) {}
        String ver = Bukkit.getServer().getClass().getPackage().getName();
        String[] parts = ver.split("\\.");
        return Class.forName("org.bukkit.craftbukkit." + parts[3] + "." + name);
    }
}
















