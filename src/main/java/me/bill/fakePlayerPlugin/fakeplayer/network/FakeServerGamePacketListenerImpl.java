package me.bill.fakePlayerPlugin.fakeplayer.network;

import me.bill.fakePlayerPlugin.FakePlayerPlugin;
import me.bill.fakePlayerPlugin.config.Config;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.Bukkit;

import java.lang.reflect.Method;

/**
 * Fake packet listener for bot players.
 *
 * <p>Extends {@link ServerGamePacketListenerImpl} so the server accepts it as a
 * valid connection object for a {@link ServerPlayer}.
 *
 * <p>Key behaviour: {@link #send(Packet)} discards ALL outbound packets except
 * {@link ClientboundSetEntityMotionPacket} (knockback), which is applied server-side.
 * Because {@code send()} is a no-op, {@code awaitingPositionFromClient} on this fresh
 * instance stays {@code null} — the server's {@code connection.tick()} never snaps the
 * bot back to a stale spawn position (the root cause of bots "floating").
 *
 * <h3>Knockback strategy resolution (one-time, lazy)</h3>
 * <ol>
 *   <li><b>GET_MOVEMENT</b> (1.21.9+): {@code packet.getMovement()} returns a {@code Vec3};
 *       apply via {@code player.lerpMotion(Vec3)}.  Matches the hello09x reference plugin.</li>
 *   <li><b>GET_XA</b> (≤ 1.21.8): {@code packet.getXa/Ya/Za()} (or {@code xa/ya/za()})
 *       return individual doubles; apply via {@code lerpMotion(double,double,double)} or,
 *       if that method is absent, via {@code setDeltaMovement(Vec3)}.</li>
 *   <li><b>NONE</b>: no compatible API found — knockback is silently skipped.</li>
 * </ol>
 */
public final class FakeServerGamePacketListenerImpl extends ServerGamePacketListenerImpl {

    // ── Knockback strategy (resolved once on first hit) ───────────────────────

    private enum KbStrategy { UNRESOLVED, GET_MOVEMENT, GET_XA, NONE }

    private static volatile KbStrategy kbStrategy = KbStrategy.UNRESOLVED;

    // Strategy GET_MOVEMENT
    private static Method getMovementMethod;       // ClientboundSetEntityMotionPacket.getMovement()
    private static Method lerpMotionVec3Method;    // Entity.lerpMotion(Vec3)

    // Strategy GET_XA (fallback for older versions)
    private static Method getXaMethod, getYaMethod, getZaMethod;
    private static Method lerpMotion3Method;       // Entity.lerpMotion(double,double,double) — may be null
    private static Method setDeltaMovementMethod;  // Entity.setDeltaMovement(Vec3)            — fallback
    private static Class<?> vec3Class;

    // ─────────────────────────────────────────────────────────────────────────

    public FakeServerGamePacketListenerImpl(
            MinecraftServer server,
            Connection connection,
            ServerPlayer player,
            CommonListenerCookie cookie
    ) {
        super(server, connection, player, cookie);
    }

    /**
     * Factory for use from reflection-based code in {@code NmsPlayerSpawner}.
     */
    public static FakeServerGamePacketListenerImpl create(
            Object server, Object connection, Object player, Object cookie) {
        return new FakeServerGamePacketListenerImpl(
                (MinecraftServer) server,
                (Connection) connection,
                (ServerPlayer) player,
                (CommonListenerCookie) cookie
        );
    }

    /**
     * Suppress the {@code "Already retired"} {@link IllegalStateException} that Paper 1.21+
     * throws when {@link net.minecraft.server.players.PlayerList#remove} tries to retire
     * the entity's scheduler a second time on bot death (double-disconnect path).
     *
     * <p>The player-list cleanup that matters has already completed before the exception
     * is thrown, so suppressing it here is safe.
     */
    @Override
    public void onDisconnect(@org.jetbrains.annotations.NotNull DisconnectionDetails details) {
        try {
            super.onDisconnect(details);
        } catch (IllegalStateException e) {
            if ("Already retired".equals(e.getMessage())) {
                Config.debugNms("FakeServerGamePacketListenerImpl: suppressed double-retirement for "
                        + this.player.getScoreboardName()
                        + " (entity scheduler already retired by death path)");
            } else {
                throw e;
            }
        }
    }

    /**
     * Intercept outgoing packets.
     * <ul>
     *   <li>{@link ClientboundSetEntityMotionPacket} — apply knockback server-side.</li>
     *   <li>Everything else — silently discard.</li>
     * </ul>
     */
    @Override
    public void send(Packet<?> packet) {
        if (packet instanceof ClientboundSetEntityMotionPacket motionPacket) {
            applyKnockback(motionPacket);
        }
        // All other packets discarded
    }

    // ── Knockback application ─────────────────────────────────────────────────

    private void applyKnockback(ClientboundSetEntityMotionPacket packet) {
        if (packet.getId() != this.player.getId() || !this.player.hurtMarked) return;

        Bukkit.getScheduler().runTask(FakePlayerPlugin.getInstance(), () -> {
            try {
                this.player.hurtMarked = true;

                switch (resolveStrategy()) {
                    case GET_MOVEMENT -> {
                        // 1.21.9+: packet.getMovement() → Vec3 → player.lerpMotion(Vec3)
                        Object movement = getMovementMethod.invoke(packet);
                        lerpMotionVec3Method.invoke(this.player, movement);
                        Config.debugNms("FakePacketListener: knockback (GET_MOVEMENT) → "
                                + this.player.getScoreboardName());
                    }
                    case GET_XA -> {
                        // ≤1.21.8: individual xa/ya/za doubles
                        double xa = (double) getXaMethod.invoke(packet);
                        double ya = (double) getYaMethod.invoke(packet);
                        double za = (double) getZaMethod.invoke(packet);
                        if (lerpMotion3Method != null) {
                            lerpMotion3Method.invoke(this.player, xa, ya, za);
                        } else {
                            // setDeltaMovement(Vec3) fallback
                            Object v = vec3Class
                                    .getConstructor(double.class, double.class, double.class)
                                    .newInstance(xa, ya, za);
                            setDeltaMovementMethod.invoke(this.player, v);
                        }
                        Config.debugNms("FakePacketListener: knockback (GET_XA) → "
                                + this.player.getScoreboardName());
                    }
                    case NONE ->
                        Config.debugNms("FakePacketListener: knockback skipped (no strategy)");
                    default -> { /* UNRESOLVED shouldn't reach here */ }
                }
            } catch (Exception e) {
                Config.debug("FakePacketListener: knockback error: " + e.getMessage());
            }
        });
    }

    // ── Strategy resolution (lazy, one-time) ──────────────────────────────────

    private static synchronized KbStrategy resolveStrategy() {
        if (kbStrategy != KbStrategy.UNRESOLVED) return kbStrategy;

        // ── Strategy 1: GET_MOVEMENT (1.21.9+) ────────────────────────────────
        // packet.getMovement() returns Vec3; apply via player.lerpMotion(Vec3)
        // This matches the hello09x/fakeplayer reference implementation exactly.
        try {
            Method gm = ClientboundSetEntityMotionPacket.class.getMethod("getMovement");
            Class<?> returnType = gm.getReturnType(); // net.minecraft.world.phys.Vec3
            Method lm = findLerpMotionVec3(returnType);
            if (lm != null) {
                getMovementMethod    = gm;
                lerpMotionVec3Method = lm;
                Config.debugNms("FakePacketListener: knockback strategy resolved → GET_MOVEMENT"
                        + " (lerpMotion(" + returnType.getSimpleName() + "))");
                return kbStrategy = KbStrategy.GET_MOVEMENT;
            }
        } catch (NoSuchMethodException ignored) {
            // getMovement() not present — older MC version
        } catch (Exception e) {
            Config.debugNms("FakePacketListener: GET_MOVEMENT probe failed: " + e.getMessage());
        }

        // ── Strategy 2: GET_XA (≤1.21.8) ──────────────────────────────────────
        // packet.getXa/Ya/Za() or xa/ya/za() → doubles
        Method xa = probeMethod(ClientboundSetEntityMotionPacket.class, "getXa", "xa");
        Method ya = probeMethod(ClientboundSetEntityMotionPacket.class, "getYa", "ya");
        Method za = probeMethod(ClientboundSetEntityMotionPacket.class, "getZa", "za");
        if (xa != null && ya != null && za != null) {
            getXaMethod = xa;
            getYaMethod = ya;
            getZaMethod = za;
            // Try lerpMotion(double, double, double)
            lerpMotion3Method = findMethod(ServerPlayer.class,
                    "lerpMotion", double.class, double.class, double.class);
            if (lerpMotion3Method == null) {
                // Fall back to setDeltaMovement(Vec3)
                try {
                    vec3Class = Class.forName("net.minecraft.world.phys.Vec3");
                    setDeltaMovementMethod = findMethod(ServerPlayer.class,
                            "setDeltaMovement", vec3Class);
                } catch (ClassNotFoundException e) {
                    Config.debugNms("FakePacketListener: Vec3 class not found: " + e.getMessage());
                }
            }
            Config.debugNms("FakePacketListener: knockback strategy resolved → GET_XA"
                    + " (lerpMotion3=" + (lerpMotion3Method != null)
                    + ", setDelta=" + (setDeltaMovementMethod != null) + ")");
            return kbStrategy = KbStrategy.GET_XA;
        }

        Config.debugNms("FakePacketListener: knockback strategy = NONE (no compatible MC API found)");
        return kbStrategy = KbStrategy.NONE;
    }

    /**
     * Walks the full {@link ServerPlayer} superclass chain looking for a
     * single-parameter {@code lerpMotion} method whose parameter type is
     * assignable from (or equal to) {@code vec3Type}.
     */
    private static Method findLerpMotionVec3(Class<?> vec3Type) {
        Class<?> cur = ServerPlayer.class;
        while (cur != null && cur != Object.class) {
            for (Method m : cur.getDeclaredMethods()) {
                if ("lerpMotion".equals(m.getName())
                        && m.getParameterCount() == 1
                        && m.getParameterTypes()[0].isAssignableFrom(vec3Type)) {
                    m.setAccessible(true);
                    return m;
                }
            }
            cur = cur.getSuperclass();
        }
        return null;
    }

    /** Tries each candidate name in order and returns the first method found, or null. */
    private static Method probeMethod(Class<?> clazz, String... names) {
        for (String name : names) {
            try { return clazz.getMethod(name); } catch (NoSuchMethodException ignored) {}
        }
        return null;
    }

    /** Walks the full superclass chain for a method with the given name + parameter types. */
    private static Method findMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
        Class<?> cur = clazz;
        while (cur != null && cur != Object.class) {
            try {
                Method m = cur.getDeclaredMethod(name, paramTypes);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException ignored) {}
            cur = cur.getSuperclass();
        }
        return null;
    }
}

