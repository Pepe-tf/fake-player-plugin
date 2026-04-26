package me.bill.fakePlayerPlugin.fakeplayer.network;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import me.bill.fakePlayerPlugin.FakePlayerPlugin;
import me.bill.fakePlayerPlugin.config.Config;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

public final class FakeServerGamePacketListenerImpl extends ServerGamePacketListenerImpl {

  private static volatile boolean entityIdDirectFailed = false;

  private static volatile boolean entityIdFallbackResolved = false;

  private static volatile Method cachedEntityIdMethod = null;

  private static int resolveEntityId(ClientboundSetEntityMotionPacket packet) {

    if (!entityIdDirectFailed) {
      try {
        return packet.getId();
      } catch (NoSuchMethodError e) {

        entityIdDirectFailed = true;
      }
    }

    if (!entityIdFallbackResolved) {
      synchronized (FakeServerGamePacketListenerImpl.class) {
        if (!entityIdFallbackResolved) {
          for (String name : new String[] {"getEntityId", "id"}) {
            try {
              Method m = ClientboundSetEntityMotionPacket.class.getMethod(name);
              if (m.getReturnType() == int.class) {
                m.setAccessible(true);
                cachedEntityIdMethod = m;
                Config.debugNms("FakePacketListener: entity-ID getter resolved → " + name + "()");
                break;
              }
            } catch (NoSuchMethodException ignored) {
            }
          }
          if (cachedEntityIdMethod == null) {
            Config.debugNms(
                "FakePacketListener: entity-ID getter not found - ID check skipped"
                    + " (packet arrived on this entity's own connection)");
          }
          entityIdFallbackResolved = true;
        }
      }
    }
    if (cachedEntityIdMethod == null) return -1;
    try {
      return (int) cachedEntityIdMethod.invoke(packet);
    } catch (Exception e) {
      return -1;
    }
  }

  private enum KbStrategy {
    UNRESOLVED,
    GET_MOVEMENT,
    GET_XA,
    FIELD_SCAN,
    NONE
  }

  private static volatile KbStrategy kbStrategy = KbStrategy.UNRESOLVED;

  private static Method getMovementMethod;
  private static Method lerpMotionVec3Method;

  private static Method getXaMethod, getYaMethod, getZaMethod;
  private static Method lerpMotion3Method;
  private static Method setDeltaMovementMethod;
  private static Class<?> vec3Class;

  private static Field scanXField, scanYField, scanZField;

  public FakeServerGamePacketListenerImpl(
      MinecraftServer server,
      Connection connection,
      ServerPlayer player,
      CommonListenerCookie cookie) {
    super(server, connection, player, cookie);
  }

  public static FakeServerGamePacketListenerImpl create(
      Object server, Object connection, Object player, Object cookie) {
    return new FakeServerGamePacketListenerImpl(
        (MinecraftServer) server,
        (Connection) connection,
        (ServerPlayer) player,
        (CommonListenerCookie) cookie);
  }

  @Override
  public void onDisconnect(@org.jetbrains.annotations.NotNull DisconnectionDetails details) {
    try {
      super.onDisconnect(details);
    } catch (IllegalStateException e) {
      if ("Already retired".equals(e.getMessage())) {
        Config.debugNms(
            "FakeServerGamePacketListenerImpl: suppressed double-retirement for "
                + this.player.getScoreboardName()
                + " (entity scheduler already retired by death path)");
      } else {
        throw e;
      }
    }
  }

  @Override
  public void send(Packet<?> packet) {
    if (packet instanceof ClientboundSetEntityMotionPacket motionPacket) {
      Config.debugNms(
          "[KB-DEBUG] send(Packet) called for "
              + this.player.getScoreboardName()
              + " packetClass="
              + packet.getClass().getSimpleName());
      applyKnockback(motionPacket);
    }
  }

  @SuppressWarnings("unused")
  public void send(Packet<?> packet, @Nullable PacketSendListener listener) {
    if (packet instanceof ClientboundSetEntityMotionPacket motionPacket) {
      Config.debugNms(
          "[KB-DEBUG] send(Packet,PacketSendListener) called for "
              + this.player.getScoreboardName()
              + " packetClass="
              + packet.getClass().getSimpleName());
      applyKnockback(motionPacket);
    }
  }

  private void applyKnockback(ClientboundSetEntityMotionPacket packet) {
    int packetEntityId = resolveEntityId(packet);
    int myId = this.player.getId();
    Config.debugNms(
        "[KB-DEBUG] applyKnockback: bot="
            + this.player.getScoreboardName()
            + " myId="
            + myId
            + " packetId="
            + packetEntityId
            + " hurtMarked="
            + this.player.hurtMarked);

    if (packetEntityId != -1 && packetEntityId != myId) {
      Config.debugNms(
          "[KB-DEBUG] applyKnockback: SKIPPED - packet is for entity "
              + packetEntityId
              + " not this bot ("
              + myId
              + ")");
      return;
    }

    Config.debugNms(
        "[KB-DEBUG] applyKnockback: PROCEEDING - scheduling task for "
            + this.player.getScoreboardName()
            + " strategy="
            + kbStrategy);

    Bukkit.getScheduler()
        .runTask(
            FakePlayerPlugin.getInstance(),
            () -> {
              try {
                KbStrategy strategy = resolveStrategy();
                Config.debugNms(
                    "[KB-DEBUG] task running for "
                        + this.player.getScoreboardName()
                        + " strategy="
                        + strategy
                        + " alive="
                        + !this.player.isRemoved());

                switch (strategy) {
                  case GET_MOVEMENT -> {
                    Object movement = getMovementMethod.invoke(packet);

                    Config.debugNms(
                        "[KB-DEBUG] GET_MOVEMENT velocity="
                            + movement
                            + " applyMethod="
                            + lerpMotionVec3Method.getName());
                    lerpMotionVec3Method.invoke(this.player, movement);

                    logPostApplyVelocity("GET_MOVEMENT");
                  }
                  case GET_XA -> {
                    double xa = (double) getXaMethod.invoke(packet);
                    double ya = (double) getYaMethod.invoke(packet);
                    double za = (double) getZaMethod.invoke(packet);
                    Config.debugNms(
                        "[KB-DEBUG] GET_XA velocity=(" + xa + "," + ya + "," + za + ")");
                    if (lerpMotion3Method != null) {
                      lerpMotion3Method.invoke(this.player, xa, ya, za);
                    } else {
                      Object v =
                          vec3Class
                              .getConstructor(double.class, double.class, double.class)
                              .newInstance(xa, ya, za);
                      setDeltaMovementMethod.invoke(this.player, v);
                    }
                    logPostApplyVelocity("GET_XA");
                  }
                  case FIELD_SCAN -> {
                    double fx = (double) scanXField.get(packet);
                    double fy = (double) scanYField.get(packet);
                    double fz = (double) scanZField.get(packet);
                    Config.debugNms(
                        "[KB-DEBUG] FIELD_SCAN velocity=(" + fx + "," + fy + "," + fz + ")");
                    if (lerpMotion3Method != null) {
                      lerpMotion3Method.invoke(this.player, fx, fy, fz);
                    } else if (setDeltaMovementMethod != null && vec3Class != null) {
                      Object v =
                          vec3Class
                              .getConstructor(double.class, double.class, double.class)
                              .newInstance(fx, fy, fz);
                      setDeltaMovementMethod.invoke(this.player, v);
                    }
                    logPostApplyVelocity("FIELD_SCAN");
                  }
                  case NONE ->
                      Config.debugNms(
                          "[KB-DEBUG] knockback NONE - no compatible MC" + " API found");
                  default -> Config.debugNms("[KB-DEBUG] unexpected strategy: " + strategy);
                }
              } catch (Exception e) {
                me.bill.fakePlayerPlugin.util.FppLogger.warn(
                    "[KB-DEBUG] knockback task exception: "
                        + e.getClass().getSimpleName()
                        + ": "
                        + e.getMessage());
                if (Config.debugNms()) {
                  e.printStackTrace();
                }
              }
            });
  }

  private void logPostApplyVelocity(String strategyName) {
    try {

      org.bukkit.entity.Player bukkit =
          this.player.getBukkitEntity() instanceof org.bukkit.entity.Player p ? p : null;
      if (bukkit != null) {
        org.bukkit.util.Vector v = bukkit.getVelocity();
        Config.debugNms(
            "[KB-DEBUG] "
                + strategyName
                + " POST-APPLY velocity on "
                + this.player.getScoreboardName()
                + ": x="
                + String.format("%.4f", v.getX())
                + " y="
                + String.format("%.4f", v.getY())
                + " z="
                + String.format("%.4f", v.getZ()));
      }
    } catch (Exception e) {
      Config.debugNms("[KB-DEBUG] could not read post-apply velocity: " + e.getMessage());
    }
  }

  private static synchronized KbStrategy resolveStrategy() {
    if (kbStrategy != KbStrategy.UNRESOLVED) return kbStrategy;

    try {
      vec3Class = Class.forName("net.minecraft.world.phys.Vec3");
    } catch (ClassNotFoundException e) {
      Config.debugNms("FakePacketListener: Vec3 class not found: " + e.getMessage());
    }

    try {
      Method gm = ClientboundSetEntityMotionPacket.class.getMethod("getMovement");
      Class<?> returnType = gm.getReturnType();

      Method lm = findLerpMotionVec3(returnType);
      if (lm == null && vec3Class != null) {
        lm = findMethod(ServerPlayer.class, "setDeltaMovement", vec3Class);
      }
      if (lm != null) {
        getMovementMethod = gm;
        lerpMotionVec3Method = lm;
        Config.debugNms(
            "FakePacketListener: knockback strategy → GET_MOVEMENT"
                + " (apply="
                + lm.getName()
                + "("
                + returnType.getSimpleName()
                + "))");
        return kbStrategy = KbStrategy.GET_MOVEMENT;
      }

      getMovementMethod = gm;
      Config.debugNms(
          "FakePacketListener: getMovement() found but no Vec3 apply method;"
              + " will try GET_XA / FIELD_SCAN");
    } catch (NoSuchMethodException ignored) {

    } catch (Exception e) {
      Config.debugNms("FakePacketListener: GET_MOVEMENT probe failed: " + e.getMessage());
    }

    Method xa = probeMethod(ClientboundSetEntityMotionPacket.class, "getXa", "xa");
    Method ya = probeMethod(ClientboundSetEntityMotionPacket.class, "getYa", "ya");
    Method za = probeMethod(ClientboundSetEntityMotionPacket.class, "getZa", "za");
    if (xa != null && ya != null && za != null) {
      getXaMethod = xa;
      getYaMethod = ya;
      getZaMethod = za;

      lerpMotion3Method =
          findMethod(ServerPlayer.class, "lerpMotion", double.class, double.class, double.class);
      if (lerpMotion3Method == null && vec3Class != null) {

        setDeltaMovementMethod = findMethod(ServerPlayer.class, "setDeltaMovement", vec3Class);
      }
      Config.debugNms(
          "FakePacketListener: knockback strategy → GET_XA"
              + " (lerpMotion3="
              + (lerpMotion3Method != null)
              + ", setDelta="
              + (setDeltaMovementMethod != null)
              + ")");
      return kbStrategy = KbStrategy.GET_XA;
    }

    setDeltaMovementMethod =
        vec3Class != null ? findMethod(ServerPlayer.class, "setDeltaMovement", vec3Class) : null;
    lerpMotion3Method =
        findMethod(ServerPlayer.class, "lerpMotion", double.class, double.class, double.class);

    if (setDeltaMovementMethod != null || lerpMotion3Method != null) {

      String[][] nameTriplets = {
        {"xa", "ya", "za"},
        {"xd", "yd", "zd"},
        {"motX", "motY", "motZ"},
      };
      for (String[] triplet : nameTriplets) {
        Field fx =
            findDeclaredField(ClientboundSetEntityMotionPacket.class, triplet[0], double.class);
        Field fy =
            findDeclaredField(ClientboundSetEntityMotionPacket.class, triplet[1], double.class);
        Field fz =
            findDeclaredField(ClientboundSetEntityMotionPacket.class, triplet[2], double.class);
        if (fx != null && fy != null && fz != null) {
          scanXField = fx;
          scanYField = fy;
          scanZField = fz;
          Config.debugNms(
              "FakePacketListener: knockback strategy → FIELD_SCAN"
                  + " fields=["
                  + triplet[0]
                  + ","
                  + triplet[1]
                  + ","
                  + triplet[2]
                  + "]"
                  + " applyLerp3="
                  + (lerpMotion3Method != null)
                  + " applySetDelta="
                  + (setDeltaMovementMethod != null));
          return kbStrategy = KbStrategy.FIELD_SCAN;
        }
      }

      java.util.List<Field> doubleFields = new java.util.ArrayList<>();
      for (Field f : ClientboundSetEntityMotionPacket.class.getDeclaredFields()) {
        if (f.getType() == double.class) {
          f.setAccessible(true);
          doubleFields.add(f);
        }
      }
      if (doubleFields.size() >= 3) {
        scanXField = doubleFields.get(0);
        scanYField = doubleFields.get(1);
        scanZField = doubleFields.get(2);
        Config.debugNms(
            "FakePacketListener: knockback strategy → FIELD_SCAN"
                + " (positional double fields: "
                + scanXField.getName()
                + ","
                + scanYField.getName()
                + ","
                + scanZField.getName()
                + ")");
        return kbStrategy = KbStrategy.FIELD_SCAN;
      }
    }

    Config.debugNms("FakePacketListener: knockback strategy = NONE (no compatible MC API found)");
    return kbStrategy = KbStrategy.NONE;
  }

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

  private static Method probeMethod(Class<?> clazz, String... names) {
    for (String name : names) {
      try {
        return clazz.getMethod(name);
      } catch (NoSuchMethodException ignored) {
      }
    }
    return null;
  }

  private static Method findMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
    Class<?> cur = clazz;
    while (cur != null && cur != Object.class) {
      try {
        Method m = cur.getDeclaredMethod(name, paramTypes);
        m.setAccessible(true);
        return m;
      } catch (NoSuchMethodException ignored) {
      }
      cur = cur.getSuperclass();
    }
    return null;
  }

  private static Field findDeclaredField(Class<?> clazz, String name, Class<?> type) {
    try {
      Field f = clazz.getDeclaredField(name);
      if (f.getType() == type) {
        f.setAccessible(true);
        return f;
      }
    } catch (NoSuchFieldException ignored) {
    }
    return null;
  }
}
