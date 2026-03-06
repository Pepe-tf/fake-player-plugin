package me.bill.fakePlayerPlugin.fakeplayer;

import me.bill.fakePlayerPlugin.config.Config;
import me.bill.fakePlayerPlugin.util.FppLogger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Sends NMS packets via reflection — zero NMS imports, compiles against paper-api only.
 */
public final class PacketHelper {

    private PacketHelper() {}

    private static boolean ready  = false;
    private static boolean broken = false;

    private static Class<?> craftPlayerClass;
    private static Class<?> gameProfileClass;
    private static Class<?> playerInfoUpdatePacketClass;
    private static Class<?> playerInfoUpdateActionClass;
    private static Class<?> playerInfoUpdateEntryClass;
    private static Class<?> playerInfoRemovePacketClass;
    private static Class<?> addEntityPacketClass;
    private static Class<?> removeEntitiesPacketClass;
    private static Class<?> vec3Class;
    private static Class<?> entityTypeClass;
    private static Object   vec3Zero;
    private static Object   gameTypeSurvival;
    private static Object   entityTypePlayer;
    private static Object   componentClass; // kept as Object — resolved via reflection

    // The packet constructor found at init time
    private static Constructor<?> playerInfoUpdateCtor;

    private static synchronized void init() {
        if (ready || broken) return;
        try {
            craftPlayerClass = getCraftClass("entity.CraftPlayer");

            // Get the NMS classloader from a live online player's NMS handle.
            // Paper loads net.minecraft.* in a child classloader not reachable via
            // CraftServer.getClassLoader() (which returns a URLClassLoader).
            ClassLoader nmsLoader = findNmsClassLoader();
            if (nmsLoader == null) {
                throw new IllegalStateException("Cannot find NMS classloader — no online players?");
            }

            gameProfileClass = nmsLoader.loadClass("com.mojang.authlib.GameProfile");

            String pkg = "net.minecraft.network.protocol.game.";
            playerInfoUpdatePacketClass = nmsLoader.loadClass(pkg + "ClientboundPlayerInfoUpdatePacket");
            playerInfoUpdateActionClass = nmsLoader.loadClass(pkg + "ClientboundPlayerInfoUpdatePacket$Action");
            playerInfoUpdateEntryClass  = nmsLoader.loadClass(pkg + "ClientboundPlayerInfoUpdatePacket$Entry");
            playerInfoRemovePacketClass = nmsLoader.loadClass(pkg + "ClientboundPlayerInfoRemovePacket");
            addEntityPacketClass        = nmsLoader.loadClass(pkg + "ClientboundAddEntityPacket");
            removeEntitiesPacketClass   = nmsLoader.loadClass(pkg + "ClientboundRemoveEntitiesPacket");

            Class<?> gameTypeClass = nmsLoader.loadClass("net.minecraft.world.level.GameType");
            gameTypeSurvival = Enum.valueOf(rawEnum(gameTypeClass), "SURVIVAL");

            componentClass = nmsLoader.loadClass("net.minecraft.network.chat.Component");

            vec3Class = nmsLoader.loadClass("net.minecraft.world.phys.Vec3");
            vec3Zero  = vec3Class.getField("ZERO").get(null);

            entityTypeClass  = nmsLoader.loadClass("net.minecraft.world.entity.EntityType");
            entityTypePlayer = entityTypeClass.getField("PLAYER").get(null);

            playerInfoUpdateCtor = findPlayerInfoUpdateCtor();
            if (playerInfoUpdateCtor == null) {
                throw new IllegalStateException("Cannot find ClientboundPlayerInfoUpdatePacket constructor. " +
                        "Available: " + Arrays.toString(playerInfoUpdatePacketClass.getDeclaredConstructors()));
            }

            ready = true;
            FppLogger.info("PacketHelper ready.");
        } catch (Exception e) {
            broken = true;
            FppLogger.warn("PacketHelper init failed: " + e.getMessage());
        }
    }

    private static boolean canLoadNms(ClassLoader cl) {
        for (String probe : new String[]{
                "net.minecraft.server.players.PlayerList",
                "net.minecraft.server.MinecraftServer",
                "net.minecraft.network.Connection",
                "net.minecraft.network.PacketFlow"}) {
            try { cl.loadClass(probe); return true; }
            catch (ClassNotFoundException ignored) {}
        }
        return false;
    }

    /** Gets the NMS classloader using multiple strategies — DedicatedServer first. */
    private static ClassLoader findNmsClassLoader() {
        // Strategy 1: DedicatedServer via CraftServer.getServer()
        try {
            Class<?> craftServerClass = getCraftClass("CraftServer");
            Method getServer = craftServerClass.getMethod("getServer");
            Object mcServer  = getServer.invoke(craftServerClass.cast(Bukkit.getServer()));
            // Walk the concrete class and its superclasses
            Class<?> c = mcServer.getClass();
            while (c != null) {
                ClassLoader cl = c.getClassLoader();
                if (cl != null && canLoadNms(cl)) return cl;
                c = c.getSuperclass();
            }
            // Strategy 1b: getPlayerList() return value
            for (Method m : mcServer.getClass().getMethods()) {
                if (m.getName().equals("getPlayerList") && m.getParameterCount() == 0) {
                    Object pl = m.invoke(mcServer);
                    if (pl != null) {
                        ClassLoader cl = pl.getClass().getClassLoader();
                        if (cl != null && canLoadNms(cl)) return cl;
                    }
                    break;
                }
            }
        } catch (Exception ignored) {}
        // Strategy 2: online players
        for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
            try {
                Method getHandle = craftPlayerClass.getMethod("getHandle");
                Object nmsPlayer = getHandle.invoke(craftPlayerClass.cast(p));
                ClassLoader cl = nmsPlayer.getClass().getClassLoader();
                if (cl != null && canLoadNms(cl)) return cl;
            } catch (Exception ignored) {}
        }
        // Strategy 3: context classloader chain
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        while (cl != null) {
            if (canLoadNms(cl)) return cl;
            cl = cl.getParent();
        }
        return null;
    }


    /** Scans all declared constructors to find the (EnumSet, ...) one. */
    private static Constructor<?> findPlayerInfoUpdateCtor() {
        for (Constructor<?> c : playerInfoUpdatePacketClass.getDeclaredConstructors()) {
            Class<?>[] p = c.getParameterTypes();
            if (p.length == 2 && p[0] == EnumSet.class) {
                c.setAccessible(true);
                FppLogger.info("PlayerInfoUpdatePacket ctor found: second param = " + p[1].getName());
                return c;
            }
        }
        return null;
    }

    // ── Public API ────────────────────────────────────────────────────────

    public static void sendTabListAdd(Player receiver, FakePlayer fp) {
        init();
        if (broken) return;
        if (playerInfoUpdateCtor == null) {
            // Diagnostic: log all available constructors so we can fix the signature
            StringBuilder sb = new StringBuilder("PlayerInfoUpdatePacket ctors: ");
            for (Constructor<?> c : playerInfoUpdatePacketClass.getDeclaredConstructors()) {
                sb.append("\n  ").append(c);
            }
            FppLogger.error(sb.toString());
            return;
        }
        try {
            Object nms     = getHandle(receiver);
            Object profile = gameProfileClass
                    .getConstructor(UUID.class, String.class)
                    .newInstance(fp.getUuid(), fp.getName());

            Object displayName = ((Class<?>) componentClass)
                    .getMethod("literal", String.class)
                    .invoke(null, fp.getName());

            Object entry   = buildEntry(fp.getUuid(), profile, displayName);
            Object actions = buildActionSet();

            // Second param can be: Entry (single), Entry[] (varargs), or Collection<Entry>
            Object secondArg;
            Class<?> secondParamType = playerInfoUpdateCtor.getParameterTypes()[1];
            if (secondParamType == playerInfoUpdateEntryClass) {
                // (EnumSet, Entry) — single entry directly
                secondArg = entry;
            } else if (secondParamType.isArray()) {
                // (EnumSet, Entry[]) — varargs
                Object arr = java.lang.reflect.Array.newInstance(secondParamType.getComponentType(), 1);
                java.lang.reflect.Array.set(arr, 0, entry);
                secondArg = arr;
            } else {
                // (EnumSet, Collection<Entry>)
                secondArg = List.of(entry);
            }

            Object packet = playerInfoUpdateCtor.newInstance(actions, secondArg);
            sendPacket(nms, packet);
            Config.debug("Tab ADD → " + receiver.getName() + " for " + fp.getName());
        } catch (Exception e) {
            FppLogger.error("sendTabListAdd failed: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getName()));
            if (Config.isDebug()) e.printStackTrace();
        }
    }

    public static void sendTabListRemove(Player receiver, FakePlayer fp) {
        init();
        if (broken) return;
        try {
            Object nms = getHandle(receiver);
            // Try List constructor first, then fall back to scanning
            Constructor<?> ctor = findCtor(playerInfoRemovePacketClass, List.class);
            if (ctor == null) ctor = playerInfoRemovePacketClass.getDeclaredConstructors()[0];
            ctor.setAccessible(true);
            sendPacket(nms, ctor.newInstance(List.of(fp.getUuid())));
            Config.debug("Tab REMOVE → " + receiver.getName() + " for " + fp.getName());
        } catch (Exception e) {
            FppLogger.error("sendTabListRemove failed: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getName()));
        }
    }

    public static void spawnFakePlayer(Player receiver, FakePlayer fp, Location loc) {
        init();
        if (broken) return;
        try {
            Object nms = getHandle(receiver);
            Constructor<?> ctor = addEntityPacketClass.getConstructor(
                    int.class, UUID.class,
                    double.class, double.class, double.class,
                    float.class, float.class,
                    entityTypeClass, int.class, vec3Class, double.class);
            Object packet = ctor.newInstance(
                    fp.getEntityId(), fp.getUuid(),
                    loc.getX(), loc.getY(), loc.getZ(),
                    loc.getPitch(), loc.getYaw(),
                    entityTypePlayer, 0, vec3Zero, 0.0);
            sendPacket(nms, packet);
            Config.debug("Spawn entity → " + receiver.getName() + " for " + fp.getName());
        } catch (Exception e) {
            FppLogger.error("spawnFakePlayer failed: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getName()));
            if (Config.isDebug()) e.printStackTrace();
        }
    }

    public static void despawnFakePlayer(Player receiver, FakePlayer fp) {
        init();
        if (broken) return;
        try {
            Object nms = getHandle(receiver);
            Constructor<?> ctor = removeEntitiesPacketClass.getConstructor(int[].class);
            sendPacket(nms, ctor.newInstance((Object) new int[]{fp.getEntityId()}));
            Config.debug("Despawn entity → " + receiver.getName() + " for " + fp.getName());
        } catch (Exception e) {
            FppLogger.error("despawnFakePlayer failed: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getName()));
        }
    }

    // ── Reflection helpers ────────────────────────────────────────────────

    private static Object getHandle(Player player) throws Exception {
        return craftPlayerClass.getMethod("getHandle").invoke(craftPlayerClass.cast(player));
    }

    private static void sendPacket(Object serverPlayer, Object packet) throws Exception {
        Field connField = findField(serverPlayer.getClass(), "connection");
        if (connField == null) throw new IllegalStateException("ServerPlayer.connection not found");
        connField.setAccessible(true);
        Object conn = connField.get(serverPlayer);
        for (Method m : conn.getClass().getMethods()) {
            if (m.getName().equals("send") && m.getParameterCount() == 1) {
                m.invoke(conn, packet);
                return;
            }
        }
        throw new IllegalStateException("connection.send(Packet) not found");
    }

    private static Field findField(Class<?> clazz, String name) {
        while (clazz != null) {
            try { return clazz.getDeclaredField(name); }
            catch (NoSuchFieldException ignored) { clazz = clazz.getSuperclass(); }
        }
        return null;
    }

    private static Constructor<?> findCtor(Class<?> clazz, Class<?>... params) {
        try { Constructor<?> c = clazz.getDeclaredConstructor(params); c.setAccessible(true); return c; }
        catch (NoSuchMethodException ignored) { return null; }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Class<? extends Enum> rawEnum(Class<?> c) { return (Class<? extends Enum>) c; }

    private static Class<?> getCraftClass(String suffix) throws ClassNotFoundException {
        try { return Class.forName("org.bukkit.craftbukkit." + suffix); }
        catch (ClassNotFoundException ignored) {}
        String[] parts = Bukkit.getServer().getClass().getPackage().getName().split("\\.");
        return Class.forName("org.bukkit.craftbukkit." + parts[3] + "." + suffix);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object buildActionSet() throws Exception {
        Class<? extends Enum> e = rawEnum(playerInfoUpdateActionClass);
        Enum add  = Enum.valueOf(e, "ADD_PLAYER");
        Enum list = Enum.valueOf(e, "UPDATE_LISTED");
        Enum lat  = Enum.valueOf(e, "UPDATE_LATENCY");
        Enum gm   = Enum.valueOf(e, "UPDATE_GAME_MODE");
        Enum dn   = Enum.valueOf(e, "UPDATE_DISPLAY_NAME");
        return EnumSet.of(add, list, lat, gm, dn);
    }

    /**
     * Builds a ClientboundPlayerInfoUpdatePacket.Entry by iterating all its
     * declared constructors and matching params by simple type name.
     * This is robust against the exact parameter order changing across builds.
     */
    private static Object buildEntry(UUID uuid, Object profile, Object displayName) throws Exception {
        Constructor<?>[] ctors = playerInfoUpdateEntryClass.getDeclaredConstructors();
        Arrays.sort(ctors, (a, b) -> b.getParameterCount() - a.getParameterCount());
        Exception last = null;
        for (Constructor<?> ctor : ctors) {
            ctor.setAccessible(true);
            Class<?>[] types = ctor.getParameterTypes();
            // Log param types on first attempt so we can debug mismatches
            if (Config.isDebug()) {
                StringBuilder sb = new StringBuilder("Entry ctor params (").append(types.length).append("): ");
                for (Class<?> t : types) sb.append(t.getSimpleName()).append(", ");
                FppLogger.info(sb.toString());
            }
            try {
                Object[] args = mapEntryArgs(types, uuid, profile, displayName);
                return ctor.newInstance(args);
            } catch (Exception ex) {
                last = ex;
                Config.debug("Entry ctor failed: " + (ex.getMessage() != null ? ex.getMessage() : ex.getClass().getName()));
            }
        }
        throw new IllegalStateException("No Entry ctor worked. Last: " + (last != null ? last.getMessage() : "?"));
    }

    private static Object[] mapEntryArgs(Class<?>[] types, UUID uuid, Object profile, Object displayName) {
        Object[] args = new Object[types.length];
        int boolCount = 0;
        for (int i = 0; i < types.length; i++) {
            String t = types[i].getSimpleName();
            args[i] = switch (t) {
                case "UUID"        -> uuid;
                case "GameProfile" -> profile;
                case "boolean"     -> (boolCount++ == 0); // first boolean = listed=true
                case "int"         -> 0;                  // latency=0 / priority=0
                case "GameType"    -> gameTypeSurvival;
                case "Component"   -> displayName;
                default            -> null;               // RemoteChatSession.Data etc.
            };
        }
        return args;
    }
}
