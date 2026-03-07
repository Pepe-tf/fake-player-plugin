package me.bill.fakePlayerPlugin.fakeplayer;

import me.bill.fakePlayerPlugin.config.Config;
import me.bill.fakePlayerPlugin.util.FppLogger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
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
    private static Class<?> moveEntityRotPacketClass;
    private static Class<?> rotateHeadPacketClass;
    private static Class<?> vec3Class;
    private static Class<?> entityTypeClass;
    private static Object   vec3Zero;
    private static Object   gameTypeSurvival;
    private static Object   entityTypePlayer;
    private static Object   componentClass;

    // Skin / property helpers — resolved at init so loader is consistent
    private static Constructor<?> propertyCtorThree;      // Property(name, value, signature)
    private static Constructor<?> propertyCtorTwo;        // Property(name, value)
    private static Constructor<?> gameProfileCtor;        // GameProfile(UUID, String)
    private static Method         propMapPut;             // PropertyMap/Multimap.put(key, value)
    private static Field          profilePropertiesField; // GameProfile field holding PropertyMap
    private static Field          forwardingDelegateField;// ForwardingMultimap.delegate field
    private static Method         linkedHashMultimapCreate; // LinkedHashMultimap.create()
    private static Method         componentLiteral;       // Component.literal(String)
    private static Method         craftPlayerGetHandle;   // CraftPlayer.getHandle()

    private static Constructor<?> playerInfoUpdateCtor;

    private static synchronized void init() {
        if (ready || broken) return;
        try {
            craftPlayerClass = getCraftClass("entity.CraftPlayer");
            // Resolve getHandle() via array scan
            for (Method m : craftPlayerClass.getDeclaredMethods()) {
                if (m.getName().equals("getHandle") && m.getParameterCount() == 0) {
                    m.setAccessible(true);
                    craftPlayerGetHandle = m;
                    break;
                }
            }

            ClassLoader nmsLoader = findNmsClassLoader();
            if (nmsLoader == null) {
                throw new IllegalStateException("Cannot find NMS classloader — no online players?");
            }

            // ── GameProfile + Property (same loader, always consistent) ──
            gameProfileClass = nmsLoader.loadClass("com.mojang.authlib.GameProfile");
            Class<?> propertyClass    = nmsLoader.loadClass("com.mojang.authlib.properties.Property");
            Class<?> propertyMapClass = nmsLoader.loadClass("com.mojang.authlib.properties.PropertyMap");

            // Resolve GameProfile(UUID, String) constructor via array scan
            for (Constructor<?> c : gameProfileClass.getDeclaredConstructors()) {
                Class<?>[] pt = c.getParameterTypes();
                if (pt.length == 2 && pt[0] == UUID.class && pt[1] == String.class) {
                    c.setAccessible(true);
                    gameProfileCtor = c;
                    break;
                }
            }

            // In authlib 4.x, getProperties() does not exist as a method —
            // the PropertyMap is stored as a field. Scan all fields on GameProfile
            // and its superclasses to find the one typed PropertyMap.
            if (Config.isDebug()) {
                FppLogger.info("GameProfile fields:");
                for (Class<?> c = gameProfileClass; c != null && c != Object.class; c = c.getSuperclass()) {
                    for (Field f : c.getDeclaredFields())
                        FppLogger.info("  [" + c.getSimpleName() + "] " + f.getType().getSimpleName() + " " + f.getName());
                }
            }
            outer2:
            for (Class<?> c = gameProfileClass; c != null && c != Object.class; c = c.getSuperclass()) {
                for (Field f : c.getDeclaredFields()) {
                    if (propertyMapClass.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        profilePropertiesField = f;
                        Config.debug("PacketHelper: found PropertyMap field: " + f.getName()
                                + " on " + c.getSimpleName());
                        break outer2;
                    }
                }
            }
            if (profilePropertiesField == null) {
                FppLogger.warn("PacketHelper: PropertyMap field not found on GameProfile — skin will be skipped");
            }

            // Property constructors — scan declared constructors array (safe from rewriter)
            for (Constructor<?> c : propertyClass.getDeclaredConstructors()) {
                Class<?>[] pt = c.getParameterTypes();
                if (pt.length == 3 && pt[0] == String.class && pt[1] == String.class && pt[2] == String.class) {
                    c.setAccessible(true);
                    propertyCtorThree = c;
                } else if (pt.length == 2 && pt[0] == String.class && pt[1] == String.class) {
                    c.setAccessible(true);
                    propertyCtorTwo = c;
                }
            }

            // PropertyMap.put — scan declared + inherited methods arrays
            outer:
            for (Class<?> c = propertyMapClass; c != null && c != Object.class; c = c.getSuperclass()) {
                for (Method m : c.getDeclaredMethods()) {
                    if (m.getName().equals("put") && m.getParameterCount() == 2) {
                        m.setAccessible(true);
                        propMapPut = m;
                        break outer;
                    }
                }
            }

            // Find ForwardingMultimap's delegate field by instantiating a fresh PropertyMap
            // and looking for the field whose value is an ImmutableMultimap.
            // We do this at init so we don't need the GameProfile instance yet.
            try {
                // Create a temporary GameProfile just to inspect what the properties field contains
                Object tempProfile = gameProfileCtor.newInstance(UUID.randomUUID(), "_probe_");
                Object tempPropMap = profilePropertiesField.get(tempProfile);
                Class<?> immutableMultimapClass = Class.forName("com.google.common.collect.ImmutableMultimap");

                outerDelegate:
                for (Class<?> c = tempPropMap.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
                    for (Field f : c.getDeclaredFields()) {
                        f.setAccessible(true);
                        Object val = f.get(tempPropMap);
                        if (val != null && immutableMultimapClass.isInstance(val)) {
                            forwardingDelegateField = f;
                            Config.debug("PacketHelper: delegate field found: "
                                    + f.getName() + " (" + val.getClass().getSimpleName()
                                    + ") on " + c.getSimpleName());
                            break outerDelegate;
                        }
                    }
                }
            } catch (Exception e) {
                FppLogger.warn("PacketHelper: delegate field probe failed: " + e.getMessage());
            }

            // LinkedHashMultimap.create() — gives us a fresh mutable multimap
            try {
                Class<?> llmClass = Class.forName("com.google.common.collect.LinkedHashMultimap");
                for (Method m : llmClass.getDeclaredMethods()) {
                    if (m.getName().equals("create") && m.getParameterCount() == 0) {
                        m.setAccessible(true);
                        linkedHashMultimapCreate = m;
                        break;
                    }
                }
            } catch (Exception ignored) {}

            String pkg = "net.minecraft.network.protocol.game.";
            playerInfoUpdatePacketClass = nmsLoader.loadClass(pkg + "ClientboundPlayerInfoUpdatePacket");
            playerInfoUpdateActionClass = nmsLoader.loadClass(pkg + "ClientboundPlayerInfoUpdatePacket$Action");
            playerInfoUpdateEntryClass  = nmsLoader.loadClass(pkg + "ClientboundPlayerInfoUpdatePacket$Entry");
            playerInfoRemovePacketClass = nmsLoader.loadClass(pkg + "ClientboundPlayerInfoRemovePacket");
            addEntityPacketClass        = nmsLoader.loadClass(pkg + "ClientboundAddEntityPacket");
            removeEntitiesPacketClass   = nmsLoader.loadClass(pkg + "ClientboundRemoveEntitiesPacket");
            moveEntityRotPacketClass    = nmsLoader.loadClass(pkg + "ClientboundMoveEntityPacket$Rot");
            rotateHeadPacketClass       = nmsLoader.loadClass(pkg + "ClientboundRotateHeadPacket");

            Class<?> gameTypeClass = nmsLoader.loadClass("net.minecraft.world.level.GameType");
            gameTypeSurvival = Enum.valueOf(rawEnum(gameTypeClass), "SURVIVAL");

            componentClass = nmsLoader.loadClass("net.minecraft.network.chat.Component");
            // Resolve Component.literal(String) via array scan — safe from Paper's rewriter
            for (Method m : ((Class<?>) componentClass).getDeclaredMethods()) {
                if (m.getName().equals("literal") && m.getParameterCount() == 1
                        && m.getParameterTypes()[0] == String.class) {
                    m.setAccessible(true);
                    componentLiteral = m;
                    break;
                }
            }

            vec3Class = nmsLoader.loadClass("net.minecraft.world.phys.Vec3");
            vec3Zero  = scanField(vec3Class, "ZERO");

            entityTypeClass  = nmsLoader.loadClass("net.minecraft.world.entity.EntityType");
            entityTypePlayer = scanField(entityTypeClass, "PLAYER");

            playerInfoUpdateCtor = findPlayerInfoUpdateCtor();
            if (playerInfoUpdateCtor == null) {
                throw new IllegalStateException("Cannot find ClientboundPlayerInfoUpdatePacket constructor.");
            }

            Config.debug("PacketHelper ready. propertyCtor="
                    + (propertyCtorThree != null ? "3-arg" : propertyCtorTwo != null ? "2-arg" : "NONE")
                    + " propMapPut=" + (propMapPut != null ? "found" : "MISSING"));
            ready = true;
        } catch (Exception e) {
            broken = true;
            FppLogger.warn("PacketHelper init failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Delegates to NmsHelper for a safe, crash-free classloader search. */
    private static ClassLoader findNmsClassLoader() {
        return NmsHelper.findNmsClassLoader();
    }

    /** Scans all declared constructors to find the (EnumSet, ...) one. */
    private static Constructor<?> findPlayerInfoUpdateCtor() {
        for (Constructor<?> c : playerInfoUpdatePacketClass.getDeclaredConstructors()) {
            Class<?>[] p = c.getParameterTypes();
            if (p.length == 2 && p[0] == EnumSet.class) {
                c.setAccessible(true);
                Config.debug("PlayerInfoUpdatePacket ctor: second param = " + p[1].getName());
                return c;
            }
        }
        return null;
    }

    // ── Public API ────────────────────────────────────────────────────────

    public static void sendTabListAdd(Player receiver, FakePlayer fp) {
        init();
        if (broken) return;
        try {
            Object nms     = getHandle(receiver);
            Object profile = gameProfileCtor != null
                    ? gameProfileCtor.newInstance(fp.getUuid(), fp.getName())
                    : gameProfileClass.getDeclaredConstructors()[0].newInstance(fp.getUuid(), fp.getName());

            // Attach skin only when both fetch-skin AND show-skin are enabled.
            // An empty GameProfile causes the client to assign a random default skin.
            if (fp.getSkinValue() != null && Config.fetchSkin() && Config.showSkin()) {
                attachSkin(profile, fp.getSkinValue(), fp.getSkinSignature());
            }

            Object displayName = componentLiteral != null
                    ? componentLiteral.invoke(null, fp.getName())
                    : fp.getName(); // fallback: plain string (won't render but won't crash)

            Object entry   = buildEntry(fp.getUuid(), profile, displayName);
            Object actions = buildActionSet();

            Object secondArg;
            Class<?> secondParamType = playerInfoUpdateCtor.getParameterTypes()[1];
            if (secondParamType == playerInfoUpdateEntryClass) {
                secondArg = entry;
            } else if (secondParamType.isArray()) {
                Object arr = java.lang.reflect.Array.newInstance(secondParamType.getComponentType(), 1);
                java.lang.reflect.Array.set(arr, 0, entry);
                secondArg = arr;
            } else {
                secondArg = List.of(entry);
            }

            Object packet = playerInfoUpdateCtor.newInstance(actions, secondArg);
            sendPacket(nms, packet);
            Config.debug("Tab ADD → " + receiver.getName() + " for " + fp.getName()
                    + (fp.getSkinValue() != null ? " [skinned]" : " [no skin]"));
        } catch (Exception e) {
            FppLogger.error("sendTabListAdd failed: " + e.getMessage());
            if (Config.isDebug()) e.printStackTrace();
        }
    }

    public static void sendTabListRemove(Player receiver, FakePlayer fp) {
        init();
        if (broken) return;
        try {
            Object nms = getHandle(receiver);
            Constructor<?> ctor = findCtor(playerInfoRemovePacketClass, List.class);
            if (ctor == null) ctor = playerInfoRemovePacketClass.getDeclaredConstructors()[0];
            ctor.setAccessible(true);
            sendPacket(nms, ctor.newInstance(List.of(fp.getUuid())));
            Config.debug("Tab REMOVE → " + receiver.getName() + " for " + fp.getName());
        } catch (Exception e) {
            FppLogger.error("sendTabListRemove failed: " + e.getMessage());
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
            FppLogger.error("spawnFakePlayer failed: " + e.getMessage());
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
            FppLogger.error("despawnFakePlayer failed: " + e.getMessage());
        }
    }

    /**
     * Sends a ClientboundTeleportEntityPacket (or MoveEntityPacket.Pos on older builds)
     * to move the visual player-skin entity to the given location.
     */
    public static void sendTeleport(Player receiver, FakePlayer fp, Location loc) {
        init();
        if (broken) return;
        try {
            Object nms = getHandle(receiver);
            ClassLoader cl = nms.getClass().getClassLoader();

            // Try ClientboundEntityPositionSyncPacket (1.21.2+) then ClientboundTeleportEntityPacket
            String[] candidates = {
                "net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket",
                "net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket"
            };
            for (String className : candidates) {
                try {
                    Class<?> pktClass = cl.loadClass(className);
                    // Try ctor (int, double, double, double, float, float, boolean)
                    for (Constructor<?> c : pktClass.getDeclaredConstructors()) {
                        c.setAccessible(true);
                        Class<?>[] pt = c.getParameterTypes();
                        try {
                            Object pkt = null;
                            if (pt.length == 7 && pt[0] == int.class && pt[1] == double.class) {
                                pkt = c.newInstance(fp.getEntityId(),
                                        loc.getX(), loc.getY(), loc.getZ(),
                                        loc.getYaw(), loc.getPitch(), true);
                            } else if (pt.length == 1) {
                                // newer: takes an Entity NMS object — skip
                                continue;
                            }
                            if (pkt != null) { sendPacket(nms, pkt); return; }
                        } catch (Exception ignored) {}
                    }
                } catch (ClassNotFoundException ignored) {}
            }
        } catch (Exception e) {
            // Teleport failures are non-critical — don't log every tick
        }
    }

    /**
     * Sends a ClientboundAnimatePacket (hurt animation) for the visual entity.
     */
    public static void sendHurtAnimation(Player receiver, FakePlayer fp) {
        init();
        if (broken) return;
        try {
            Object nms = getHandle(receiver);
            ClassLoader cl = nms.getClass().getClassLoader();
            Class<?> animClass = cl.loadClass("net.minecraft.network.protocol.game.ClientboundAnimatePacket");
            // ClientboundAnimatePacket(int entityId, int action) — action 1 = HURT
            for (Constructor<?> c : animClass.getDeclaredConstructors()) {
                c.setAccessible(true);
                Class<?>[] pt = c.getParameterTypes();
                if (pt.length == 2 && pt[0] == int.class && pt[1] == int.class) {
                    sendPacket(nms, c.newInstance(fp.getEntityId(), 1));
                    return;
                }
            }
        } catch (Exception ignored) {}
    }

    // ── Skin attachment ───────────────────────────────────────────────────

    /**
     * Attaches a "textures" property to an NMS GameProfile.
     * Uses the constructors and put-method resolved at init time
     * so all objects share the same classloader — no ClassCastException.
     */
    private static void attachSkin(Object gameProfile, String value, String signature) {
        try {
            if (profilePropertiesField == null) {
                FppLogger.warn("attachSkin: profilePropertiesField null — skin skipped");
                return;
            }

            // Build Property object
            Object property;
            if (propertyCtorThree != null) {
                property = propertyCtorThree.newInstance("textures", value,
                        signature != null ? signature : "");
            } else if (propertyCtorTwo != null) {
                property = propertyCtorTwo.newInstance("textures", value);
            } else {
                FppLogger.warn("attachSkin: no Property constructor found");
                return;
            }

            // Strategy 1: swap the ForwardingMultimap's internal delegate field
            // with a fresh mutable LinkedHashMultimap, then call put on the PropertyMap.
            if (forwardingDelegateField != null && linkedHashMultimapCreate != null) {
                Object propertyMap = profilePropertiesField.get(gameProfile);
                Object mutableMap  = linkedHashMultimapCreate.invoke(null);
                forwardingDelegateField.set(propertyMap, mutableMap);
                propMapPut.invoke(propertyMap, "textures", property);
                Config.debug("attachSkin: skin set via delegate-swap");
                return;
            }

            // Strategy 2: create a fresh mutable multimap, put the property into it,
            // then set the field directly (Field.set ignores declared type at runtime).
            if (linkedHashMultimapCreate != null) {
                Object mutableMap = linkedHashMultimapCreate.invoke(null);
                for (Method m : mutableMap.getClass().getDeclaredMethods()) {
                    if (m.getName().equals("put") && m.getParameterCount() == 2) {
                        m.setAccessible(true);
                        m.invoke(mutableMap, "textures", property);
                        break;
                    }
                }
                profilePropertiesField.set(gameProfile, mutableMap);
                Config.debug("attachSkin: skin set via field-replace");
                return;
            }

            FppLogger.warn("attachSkin: no mutable multimap strategy available — skin skipped");
        } catch (Exception e) {
            FppLogger.warn("attachSkin failed: " + e.getMessage());
            if (Config.isDebug()) e.printStackTrace();
        }
    }

    // ── Reflection helpers ────────────────────────────────────────────────

    private static Object getHandle(Player player) throws Exception {
        if (craftPlayerGetHandle != null) return craftPlayerGetHandle.invoke(craftPlayerClass.cast(player));
        // Fallback: array scan at call time
        for (Method m : craftPlayerClass.getDeclaredMethods()) {
            if (m.getName().equals("getHandle") && m.getParameterCount() == 0) {
                m.setAccessible(true);
                return m.invoke(craftPlayerClass.cast(player));
            }
        }
        throw new NoSuchMethodException("CraftPlayer.getHandle()");
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

    /** Scans getDeclaredFields() array — Paper does NOT intercept this form. */
    private static Object scanField(Class<?> clazz, String name) throws Exception {
        for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (f.getName().equals(name)) {
                    f.setAccessible(true);
                    return f.get(null);
                }
            }
        }
        throw new NoSuchFieldException(clazz.getName() + "." + name);
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

    // ── Rotation ──────────────────────────────────────────────────────────────

    /**
     * Sends body-rotation and head-rotation packets for a fake player so its
     * body and head face the correct direction on all clients.
     */
    public static void sendRotation(Player receiver, FakePlayer fp, float yaw, float pitch, float headYaw) {
        init();
        if (broken || moveEntityRotPacketClass == null || rotateHeadPacketClass == null) return;
        try {
            Object nms      = getHandle(receiver);
            int    entityId = fp.getEntityId();
            if (entityId == -1) return;

            byte encYaw   = angleToByte(yaw);
            byte encPitch = angleToByte(pitch);
            byte encHead  = angleToByte(headYaw);

            // ClientboundMoveEntityPacket.Rot(int entityId, byte yaw, byte pitch, boolean onGround)
            for (Constructor<?> c : moveEntityRotPacketClass.getDeclaredConstructors()) {
                Class<?>[] p = c.getParameterTypes();
                if (p.length == 4 && p[0] == int.class && p[1] == byte.class) {
                    c.setAccessible(true);
                    sendPacket(nms, c.newInstance(entityId, encYaw, encPitch, true));
                    break;
                }
            }

            // ClientboundRotateHeadPacket — head yaw
            for (Constructor<?> c : rotateHeadPacketClass.getDeclaredConstructors()) {
                c.setAccessible(true);
                Class<?>[] p = c.getParameterTypes();
                if (p.length == 2 && p[0] == int.class) {
                    // (entityId, headYaw)
                    sendPacket(nms, c.newInstance(entityId, encHead));
                } else if (p.length == 2) {
                    // (NmsEntity, byte headYaw) — get handle from physics body
                    if (fp.getPhysicsEntity() == null) break;
                    Object nmsEntity = fp.getPhysicsEntity().getClass()
                            .getMethod("getHandle").invoke(fp.getPhysicsEntity());
                    sendPacket(nms, c.newInstance(nmsEntity, encHead));
                }
                break;
            }
        } catch (Exception e) {
            Config.debug("sendRotation failed: " + e.getMessage());
        }
    }

    private static byte angleToByte(float degrees) {
        return (byte) Math.floor(degrees * 256f / 360f);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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

    private static Object buildEntry(UUID uuid, Object profile, Object displayName) throws Exception {
        Constructor<?>[] ctors = playerInfoUpdateEntryClass.getDeclaredConstructors();
        Arrays.sort(ctors, (a, b) -> b.getParameterCount() - a.getParameterCount());
        Exception last = null;
        for (Constructor<?> ctor : ctors) {
            ctor.setAccessible(true);
            Class<?>[] types = ctor.getParameterTypes();
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
                case "boolean"     -> (boolCount++ == 0);
                case "int"         -> 0;
                case "GameType"    -> gameTypeSurvival;
                case "Component"   -> displayName;
                default            -> null;
            };
        }
        return args;
    }
}
