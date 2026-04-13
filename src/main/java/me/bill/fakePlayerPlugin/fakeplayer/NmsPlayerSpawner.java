package me.bill.fakePlayerPlugin.fakeplayer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.*;
import me.bill.fakePlayerPlugin.fakeplayer.network.FakeConnection;
import me.bill.fakePlayerPlugin.fakeplayer.network.FakeServerGamePacketListenerImpl;
import me.bill.fakePlayerPlugin.util.FppLogger;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

public final class NmsPlayerSpawner {

  private static volatile boolean initialized = false;
  private static volatile boolean failed = false;

  private static Method craftPlayerGetHandleMethod;
  private static Method craftServerGetServerMethod;
  private static Method craftWorldGetHandleMethod;

  private static Class<?> minecraftServerClass;
  private static Class<?> serverLevelClass;
  private static Class<?> serverPlayerClass;
  private static Class<?> clientInformationClass;
  private static Class<?> connectionClass;
  private static Class<?> commonListenerCookieClass;
  private static Class<?> serverGamePacketListenerClass;
  private static Class<?> packetFlowClass;

  private static Constructor<?> gameProfileConstructor;
  private static Method setPosMethod;
  private static Method doTickMethod;
  private static Method getPlayerListMethod;

  private static Field xoField;
  private static Field yoField;
  private static Field zoField;

  private static Field jumpingField;

  private static Field yHeadRotField;

  private static Field zzaField;

  private static Field xxaField;

  private static Field connectionFieldInPlayer;

  private static Method attackMethod;

  private static Method playerListRemoveMethod;

  private static java.lang.reflect.Field playerDataStorageField;

  private static Method playerDataSaveMethod;

  private static Method getPlayerDirMethod;

  private static Object clientInfoDefault;

  private static final Set<UUID> firstTickSet = Collections.synchronizedSet(new HashSet<>());

  private static Object skinPartsDataAccessor = null;

  private static Method synchedEntityDataSetMethod = null;

  private static Field entityDataFieldForSkinParts = null;

  private NmsPlayerSpawner() {}

  public static synchronized void init() {
    if (initialized || failed) return;
    try {

      String packageName = Bukkit.getServer().getClass().getPackage().getName();
      String ver = packageName.substring(packageName.lastIndexOf('.') + 1);
      String cbPkg =
          ver.equals("craftbukkit") ? "org.bukkit.craftbukkit" : "org.bukkit.craftbukkit." + ver;
      FppLogger.debug("NmsPlayerSpawner: CraftBukkit package = " + cbPkg);

      Class<?> craftServerClass = Class.forName(cbPkg + ".CraftServer");
      Class<?> craftWorldClass = Class.forName(cbPkg + ".CraftWorld");
      Class<?> craftPlayerClass = Class.forName(cbPkg + ".entity.CraftPlayer");
      ClassLoader nmsLoader = craftServerClass.getClassLoader();

      craftServerGetServerMethod = craftServerClass.getMethod("getServer");
      craftWorldGetHandleMethod = craftWorldClass.getMethod("getHandle");
      craftPlayerGetHandleMethod = craftPlayerClass.getMethod("getHandle");

      minecraftServerClass = nmsLoader.loadClass("net.minecraft.server.MinecraftServer");
      try {
        serverLevelClass = nmsLoader.loadClass("net.minecraft.server.level.ServerLevel");
        serverPlayerClass = nmsLoader.loadClass("net.minecraft.server.level.ServerPlayer");
        FppLogger.debug("NmsPlayerSpawner: using Mojang-mapped NMS names");
      } catch (ClassNotFoundException e) {
        serverLevelClass = nmsLoader.loadClass("net.minecraft.server.level.WorldServer");
        serverPlayerClass = nmsLoader.loadClass("net.minecraft.server.level.EntityPlayer");
        FppLogger.debug("NmsPlayerSpawner: using Spigot-mapped NMS names");
      }

      try {
        connectionClass = nmsLoader.loadClass("net.minecraft.network.Connection");
      } catch (ClassNotFoundException e) {
        connectionClass = nmsLoader.loadClass("net.minecraft.network.NetworkManager");
      }
      try {
        commonListenerCookieClass =
            nmsLoader.loadClass("net.minecraft.server.network.CommonListenerCookie");
      } catch (ClassNotFoundException ignored) {
      }
      try {
        serverGamePacketListenerClass =
            nmsLoader.loadClass("net.minecraft.server.network.ServerGamePacketListenerImpl");
      } catch (ClassNotFoundException e) {
        try {
          serverGamePacketListenerClass =
              nmsLoader.loadClass("net.minecraft.server.network.PlayerConnection");
        } catch (ClassNotFoundException ignored) {
        }
      }
      try {
        packetFlowClass = nmsLoader.loadClass("net.minecraft.network.protocol.PacketFlow");
      } catch (ClassNotFoundException ignored) {
      }

      try {
        clientInformationClass =
            nmsLoader.loadClass("net.minecraft.server.level.ClientInformation");
        try {
          clientInfoDefault = clientInformationClass.getMethod("createDefault").invoke(null);
          FppLogger.debug("NmsPlayerSpawner: ClientInformation.createDefault() cached");
        } catch (Exception ignored) {
        }
      } catch (ClassNotFoundException ignored) {
      }

      Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
      gameProfileConstructor = gameProfileClass.getConstructor(UUID.class, String.class);

      getPlayerListMethod = minecraftServerClass.getMethod("getPlayerList");

      for (Method m : serverPlayerClass.getMethods()) {
        if ("setPos".equals(m.getName()) && m.getParameterCount() == 3) {
          Class<?>[] p = m.getParameterTypes();
          if (p[0] == double.class && p[1] == double.class && p[2] == double.class) {
            setPosMethod = m;
            break;
          }
        }
      }
      if (setPosMethod == null)
        setPosMethod =
            findMethodBySignature(serverPlayerClass, 3, double.class, double.class, double.class);

      doTickMethod = findMethod(serverPlayerClass, "doTick", 0);
      if (doTickMethod == null) doTickMethod = findMethod(serverPlayerClass, "tick", 0);
      if (doTickMethod != null) {
        FppLogger.debug("NmsPlayerSpawner: doTick cached as " + doTickMethod.getName() + "()");
      } else {
        FppLogger.warn("NmsPlayerSpawner: doTick() not found - bots will have no physics");
      }

      Class<?> entityClass;
      try {
        entityClass = nmsLoader.loadClass("net.minecraft.world.entity.Entity");
      } catch (ClassNotFoundException e) {
        entityClass = serverPlayerClass;
      }
      xoField = findFieldByName(entityClass, "xo");
      yoField = findFieldByName(entityClass, "yo");
      zoField = findFieldByName(entityClass, "zo");
      FppLogger.debug(
          "NmsPlayerSpawner: xo/yo/zo fields " + (xoField != null ? "cached" : "not found"));

      try {
        Class<?> livingEntityClass = nmsLoader.loadClass("net.minecraft.world.entity.LivingEntity");
        jumpingField = findFieldByName(livingEntityClass, "jumping");
      } catch (ClassNotFoundException ignored) {

        jumpingField = findFieldByName(serverPlayerClass, "jumping");
      }
      FppLogger.debug(
          "NmsPlayerSpawner: jumping field "
              + (jumpingField != null ? "cached" : "not found - swim AI inactive"));

      yHeadRotField = findFieldByName(serverPlayerClass, "yHeadRot");
      FppLogger.debug(
          "NmsPlayerSpawner: yHeadRot field "
              + (yHeadRotField != null
                  ? "cached"
                  : "not found - head AI will rely on setRotation only"));

      zzaField = findFieldByName(serverPlayerClass, "zza");

      xxaField = findFieldByName(serverPlayerClass, "xxa");
      FppLogger.debug(
          "NmsPlayerSpawner: movement input fields "
              + (zzaField != null && xxaField != null
                  ? "cached"
                  : "not found - move command inactive"));

      if (serverGamePacketListenerClass != null) {
        connectionFieldInPlayer = findFieldByName(serverPlayerClass, "connection");
        if (connectionFieldInPlayer == null)
          connectionFieldInPlayer = findFieldByName(serverPlayerClass, "playerConnection");
        if (connectionFieldInPlayer == null)
          connectionFieldInPlayer = findFieldByName(serverPlayerClass, "playerGameConnection");
        if (connectionFieldInPlayer == null) {
          for (Field f : getAllDeclaredFields(serverPlayerClass)) {
            if (serverGamePacketListenerClass.isAssignableFrom(f.getType())
                || f.getType().isAssignableFrom(serverGamePacketListenerClass)) {
              f.setAccessible(true);
              connectionFieldInPlayer = f;
              break;
            }
          }
        }
        if (connectionFieldInPlayer != null) {
          FppLogger.debug(
              "NmsPlayerSpawner: connection field = " + connectionFieldInPlayer.getName());
        } else {
          FppLogger.warn(
              "NmsPlayerSpawner: ServerPlayer.connection field not found"
                  + " - fake listener injection will be skipped");
        }
      }

      try {
        Class<?> entityClassForAttack = nmsLoader.loadClass("net.minecraft.world.entity.Entity");
        attackMethod = findMethod(serverPlayerClass, "attack", 1, entityClassForAttack);
        if (attackMethod != null) {
          FppLogger.debug("NmsPlayerSpawner: attack(Entity) method cached");
        } else {
          FppLogger.warn(
              "NmsPlayerSpawner: attack(Entity) method not found - PVP bots will use fallback"
                  + " damage");
        }
      } catch (Exception e) {
        FppLogger.warn("NmsPlayerSpawner: Failed to cache attack method: " + e.getMessage());
      }

      try {
        Class<?> playerListClass = getPlayerListMethod.getReturnType();
        playerListRemoveMethod = findMethod(playerListClass, "remove", 1);

        for (java.lang.reflect.Field f : playerListClass.getDeclaredFields()) {
          String typeName = f.getType().getSimpleName();
          if (typeName.contains("WorldNBTStorage") || typeName.contains("PlayerDataStorage")) {
            f.setAccessible(true);
            playerDataStorageField = f;
            break;
          }
        }
        if (playerDataStorageField != null) {
          Class<?> storageClass = playerDataStorageField.getType();

          try {
            getPlayerDirMethod = storageClass.getMethod("getPlayerDir");
          } catch (Exception ignored) {
          }

          for (java.lang.reflect.Method m : storageClass.getDeclaredMethods()) {
            if ("a".equals(m.getName())
                && m.getParameterCount() == 1
                && m.getReturnType() == void.class) {
              m.setAccessible(true);
              playerDataSaveMethod = m;
              break;
            }
          }
        }
        FppLogger.debug(
            "NmsPlayerSpawner: PlayerList lifecycle - remove="
                + (playerListRemoveMethod != null ? "ok" : "missing")
                + " storage="
                + (playerDataStorageField != null ? "ok" : "missing")
                + " save="
                + (playerDataSaveMethod != null ? "ok" : "missing")
                + " getPlayerDir="
                + (getPlayerDirMethod != null ? "ok" : "missing"));
      } catch (Exception e) {
        FppLogger.debug("NmsPlayerSpawner: PlayerList lifecycle init failed: " + e.getMessage());
      }

      initialized = true;
      FppLogger.info(
          "NmsPlayerSpawner initialised (doTick="
              + (doTickMethod != null)
              + ", connectionField="
              + (connectionFieldInPlayer != null)
              + ", attack="
              + (attackMethod != null)
              + ", playerDataDir="
              + (getPlayerDirMethod != null)
              + ")");

      try {
        Class<?> playerNmsClass;
        try {
          playerNmsClass = nmsLoader.loadClass("net.minecraft.world.entity.player.Player");
        } catch (ClassNotFoundException ignored) {
          playerNmsClass = serverPlayerClass;
        }

        Field spField = findFieldByName(playerNmsClass, "DATA_PLAYER_MODE_CUSTOMISATION");
        if (spField != null && java.lang.reflect.Modifier.isStatic(spField.getModifiers())) {
          spField.setAccessible(true);
          skinPartsDataAccessor = spField.get(null);
        }

        Class<?> syncDataClass =
            nmsLoader.loadClass("net.minecraft.network.syncher.SynchedEntityData");
        for (Method m : syncDataClass.getDeclaredMethods()) {
          if ("set".equals(m.getName()) && m.getParameterCount() == 2) {
            m.setAccessible(true);
            synchedEntityDataSetMethod = m;
            break;
          }
        }

        entityDataFieldForSkinParts = findFieldByName(serverPlayerClass, "entityData");

        FppLogger.debug(
            "NmsPlayerSpawner: skin-parts init - accessor="
                + (skinPartsDataAccessor != null)
                + " entityData="
                + (entityDataFieldForSkinParts != null)
                + " setMethod="
                + (synchedEntityDataSetMethod != null));
      } catch (Exception e) {
        FppLogger.debug("NmsPlayerSpawner: skin-parts init failed (non-fatal): " + e.getMessage());
      }

    } catch (Exception e) {
      failed = true;
      FppLogger.error("NmsPlayerSpawner.init() failed: " + e.getMessage());
    }
  }

  public static boolean isAvailable() {
    if (!initialized && !failed) init();
    return initialized;
  }

  public static Player spawnFakePlayer(
      UUID uuid, String name, World world, double x, double y, double z) {
    return spawnFakePlayer(uuid, name, null, world, x, y, z);
  }

  public static Player spawnFakePlayer(
      UUID uuid, String name, SkinProfile skin, World world, double x, double y, double z) {
    if (!isAvailable()) {
      FppLogger.warn("NmsPlayerSpawner not available - cannot spawn " + name);
      return null;
    }
    try {

      Object gameProfile = gameProfileConstructor.newInstance(uuid, name);
      if (skin != null && skin.isValid()) {
        try {
          SkinProfileInjector.apply(gameProfile, skin);
          FppLogger.debug("NmsPlayerSpawner: injected skin for '" + name + "'");
        } catch (Exception e) {
          FppLogger.warn("NmsPlayerSpawner: skin injection failed: " + e.getMessage());
        }
      }

      Object minecraftServer = craftServerGetServerMethod.invoke(Bukkit.getServer());
      Object serverLevel = craftWorldGetHandleMethod.invoke(world);
      Object clientInfo = getClientInformation();

      Object serverPlayer =
          createServerPlayer(minecraftServer, serverLevel, gameProfile, clientInfo);
      if (serverPlayer == null) {
        FppLogger.warn("NmsPlayerSpawner: failed to create ServerPlayer for " + name);
        return null;
      }

      if (setPosMethod != null) setPosMethod.invoke(serverPlayer, x, y, z);
      initPreviousPosition(serverPlayer, x, y, z);

      Object conn = createFakeConnection();
      if (conn == null) {
        FppLogger.warn("NmsPlayerSpawner: failed to create fake connection for " + name);
        return null;
      }

      FppLogger.debug("NmsPlayerSpawner: spawning '" + name + "' uuid=" + uuid);
      ensurePlayerDataExists(minecraftServer, serverPlayer, name, uuid);

      boolean placed = placePlayer(minecraftServer, conn, serverPlayer, gameProfile, clientInfo);
      if (!placed) {
        FppLogger.warn("NmsPlayerSpawner: placeNewPlayer failed for " + name);
        return null;
      }

      if (setPosMethod != null) setPosMethod.invoke(serverPlayer, x, y, z);
      initPreviousPosition(serverPlayer, x, y, z);

      injectFakeListener(minecraftServer, conn, serverPlayer, gameProfile, clientInfo);

      Method getBukkitEntity = serverPlayerClass.getMethod("getBukkitEntity");
      Object entity = getBukkitEntity.invoke(serverPlayer);
      if (entity instanceof Player result) {
        result.setGameMode(org.bukkit.GameMode.SURVIVAL);

        forceAllSkinParts(result);
        firstTickSet.add(uuid);
        FppLogger.debug("NmsPlayerSpawner: spawned " + name + " (" + uuid + ")");
        return result;
      }

      FppLogger.warn("NmsPlayerSpawner: getBukkitEntity did not return a Player for " + name);
      return null;

    } catch (Exception e) {
      FppLogger.error(
          "NmsPlayerSpawner.spawnFakePlayer failed for " + name + ": " + e.getMessage());
      FppLogger.debug(Arrays.toString(e.getStackTrace()));
      return null;
    }
  }

  public static void tickPhysics(Player bot) {
    if (!initialized || doTickMethod == null || craftPlayerGetHandleMethod == null) return;
    if (!bot.isOnline() || !bot.isValid() || bot.isDead()) return;
    try {
      Object nmsPlayer = craftPlayerGetHandleMethod.invoke(bot);

      if (firstTickSet.remove(bot.getUniqueId())) {

        org.bukkit.Location loc = bot.getLocation();
        double x = loc.getX(), y = loc.getY(), z = loc.getZ();

        initPreviousPosition(nmsPlayer, x, y, z);
        doTickMethod.invoke(nmsPlayer);

        if (setPosMethod != null) setPosMethod.invoke(nmsPlayer, x, y, z);
        initPreviousPosition(nmsPlayer, x, y, z);

      } else {

        doTickMethod.invoke(nmsPlayer);
      }

    } catch (Exception e) {
      FppLogger.debug(
          "NmsPlayerSpawner.tickPhysics failed for " + bot.getName() + ": " + e.getMessage());
    }
  }

  public static void setPosition(Player bot, double x, double y, double z) {
    if (!initialized || setPosMethod == null || craftPlayerGetHandleMethod == null) return;
    try {
      Object nmsPlayer = craftPlayerGetHandleMethod.invoke(bot);
      setPosMethod.invoke(nmsPlayer, x, y, z);
    } catch (Exception e) {
      FppLogger.debug("NmsPlayerSpawner.setPosition failed: " + e.getMessage());
    }
  }

  public static void setJumping(Player bot, boolean jumping) {
    if (!initialized || jumpingField == null || craftPlayerGetHandleMethod == null) return;
    try {
      Object nmsPlayer = craftPlayerGetHandleMethod.invoke(bot);
      jumpingField.setBoolean(nmsPlayer, jumping);
    } catch (Exception e) {
      FppLogger.debug("NmsPlayerSpawner.setJumping failed: " + e.getMessage());
    }
  }

  public static void setHeadYaw(Player bot, float yaw) {
    if (!initialized || craftPlayerGetHandleMethod == null) return;
    try {
      Object nmsPlayer = craftPlayerGetHandleMethod.invoke(bot);
      if (yHeadRotField != null) {
        yHeadRotField.setFloat(nmsPlayer, yaw);
      }
    } catch (Exception e) {
      FppLogger.debug("NmsPlayerSpawner.setHeadYaw failed: " + e.getMessage());
    }
  }

  public static void performAttack(Player bot, org.bukkit.entity.Entity target, double damage) {
    if (!initialized || craftPlayerGetHandleMethod == null) {

      if (target instanceof org.bukkit.entity.Damageable damageable) {
        damageable.damage(damage, bot);
      }
      return;
    }

    try {
      Object nmsBot = craftPlayerGetHandleMethod.invoke(bot);
      Object nmsTarget = craftPlayerGetHandleMethod.invoke(target);

      if (attackMethod != null && nmsTarget != null) {

        attackMethod.invoke(nmsBot, nmsTarget);
      } else {

        if (target instanceof org.bukkit.entity.Damageable damageable) {
          damageable.damage(damage, bot);
        }
      }
    } catch (Exception e) {
      FppLogger.debug("NmsPlayerSpawner.performAttack failed: " + e.getMessage());

      if (target instanceof org.bukkit.entity.Damageable damageable) {
        damageable.damage(damage, bot);
      }
    }
  }

  public static void setMovementForward(Player bot, float forward) {
    if (!initialized || zzaField == null || craftPlayerGetHandleMethod == null) return;
    try {
      Object nmsPlayer = craftPlayerGetHandleMethod.invoke(bot);
      zzaField.setFloat(nmsPlayer, forward);
    } catch (Exception e) {
      FppLogger.debug("NmsPlayerSpawner.setMovementForward failed: " + e.getMessage());
    }
  }

  public static void setMovementStrafe(Player bot, float strafe) {
    if (!initialized || xxaField == null || craftPlayerGetHandleMethod == null) return;
    try {
      Object nmsPlayer = craftPlayerGetHandleMethod.invoke(bot);
      xxaField.setFloat(nmsPlayer, strafe);
    } catch (Exception e) {
      FppLogger.debug("NmsPlayerSpawner.setMovementStrafe failed: " + e.getMessage());
    }
  }

  public static void removeFakePlayer(Player player) {
    if (player == null) return;
    try {
      firstTickSet.remove(player.getUniqueId());
      if (player.isOnline()) {
        final String name = player.getName();
        final UUID uuid = player.getUniqueId();

        FppLogger.debug("NmsPlayerSpawner: removing '" + name + "' uuid=" + uuid);

        try {
          player.saveData();
          FppLogger.debug("NmsPlayerSpawner: saved playerdata for '" + name + "' uuid=" + uuid);
        } catch (Exception e) {
          FppLogger.warn(
              "NmsPlayerSpawner: saveData failed for '"
                  + name
                  + "' uuid="
                  + uuid
                  + ": "
                  + e.getMessage());
        }

        boolean removedViaPlayerList = false;
        if (initialized
            && craftPlayerGetHandleMethod != null
            && craftServerGetServerMethod != null
            && getPlayerListMethod != null
            && playerListRemoveMethod != null) {
          try {
            Object nmsPlayer = craftPlayerGetHandleMethod.invoke(player);
            Object minecraftServer =
                craftServerGetServerMethod.invoke(org.bukkit.Bukkit.getServer());
            Object playerList = getPlayerListMethod.invoke(minecraftServer);
            playerListRemoveMethod.invoke(playerList, nmsPlayer);
            removedViaPlayerList = true;
            FppLogger.debug(
                "NmsPlayerSpawner: removed '" + name + "' via PlayerList.remove() uuid=" + uuid);
          } catch (Exception e) {
            FppLogger.debug(
                "NmsPlayerSpawner: PlayerList.remove failed for '"
                    + name
                    + "' uuid="
                    + uuid
                    + ": "
                    + e.getMessage()
                    + " - falling back to kick");
          }
        }

        if (!removedViaPlayerList && player.isOnline()) {
          player.kick(net.kyori.adventure.text.Component.empty());
        }
      }
    } catch (Exception e) {
      FppLogger.debug(
          "NmsPlayerSpawner.removeFakePlayer failed for "
              + player.getName()
              + ": "
              + e.getMessage());
    }
  }

  private static void ensurePlayerDataExists(
      Object minecraftServer, Object serverPlayer, String name, UUID uuid) {
    if (playerDataStorageField == null) {
      FppLogger.debug(
          "NmsPlayerSpawner: ensurePlayerDataExists skipped"
              + " - WorldNBTStorage field not cached (name="
              + name
              + " uuid="
              + uuid
              + ")");
      return;
    }
    try {
      Object playerList = getPlayerListMethod.invoke(minecraftServer);
      Object playerDataStorage = playerDataStorageField.get(playerList);

      if (getPlayerDirMethod != null) {
        java.io.File playerDir = (java.io.File) getPlayerDirMethod.invoke(playerDataStorage);
        java.io.File playerFile = new java.io.File(playerDir, uuid + ".dat");
        if (playerFile.exists()) {
          FppLogger.debug(
              "NmsPlayerSpawner: playerdata found for '"
                  + name
                  + "' uuid="
                  + uuid
                  + " - returning player");
          return;
        }
      }

      if (playerDataSaveMethod != null) {
        playerDataSaveMethod.invoke(playerDataStorage, serverPlayer);
        FppLogger.debug(
            "NmsPlayerSpawner: created initial playerdata for '"
                + name
                + "' uuid="
                + uuid
                + " - will be treated as returning player on next spawn");
      } else {
        FppLogger.debug(
            "NmsPlayerSpawner: playerdata file missing but save method"
                + " not cached - first-join message may appear (name="
                + name
                + ")");
      }
    } catch (Exception e) {

      FppLogger.warn(
          "NmsPlayerSpawner: ensurePlayerDataExists failed for '"
              + name
              + "' uuid="
              + uuid
              + ": "
              + e.getMessage());
    }
  }

  public static void startUsingMainHandItem(Player bot) {
    if (!initialized || craftPlayerGetHandleMethod == null) return;
    try {
      Object nmsPlayer = craftPlayerGetHandleMethod.invoke(bot);
      ClassLoader cl = nmsPlayer.getClass().getClassLoader();

      Class<?> interactionHandClass = cl.loadClass("net.minecraft.world.InteractionHand");
      Object[] hands = interactionHandClass.getEnumConstants();
      if (hands == null || hands.length == 0) return;
      Object mainHand = hands[0];

      for (Method m : nmsPlayer.getClass().getMethods()) {
        if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == interactionHandClass) {
          String name = m.getName();
          if (name.equals("startUsingItem") || name.equals("c")) {
            m.setAccessible(true);
            m.invoke(nmsPlayer, mainHand);
            return;
          }
        }
      }

      for (Method m : nmsPlayer.getClass().getMethods()) {
        if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == interactionHandClass) {
          m.setAccessible(true);
          m.invoke(nmsPlayer, mainHand);
          return;
        }
      }
    } catch (Exception e) {
      FppLogger.debug("NmsPlayerSpawner.startUsingMainHandItem failed: " + e.getMessage());
    }
  }

  public static void forceAllSkinParts(Player bot) {
    if (!initialized
        || skinPartsDataAccessor == null
        || entityDataFieldForSkinParts == null
        || synchedEntityDataSetMethod == null
        || craftPlayerGetHandleMethod == null) return;
    try {
      Object nmsPlayer = craftPlayerGetHandleMethod.invoke(bot);
      Object entityData = entityDataFieldForSkinParts.get(nmsPlayer);

      synchedEntityDataSetMethod.invoke(entityData, skinPartsDataAccessor, (byte) 0x7F);
      FppLogger.debug("NmsPlayerSpawner: skin-parts forced to 0x7F for " + bot.getName());
    } catch (Exception e) {
      FppLogger.debug(
          "NmsPlayerSpawner.forceAllSkinParts failed for " + bot.getName() + ": " + e.getMessage());
    }
  }

  private static void injectFakeListener(
      Object minecraftServer,
      Object conn,
      Object serverPlayer,
      Object gameProfile,
      Object clientInfo) {
    if (connectionFieldInPlayer == null) {
      FppLogger.warn("NmsPlayerSpawner: cannot inject fake listener - connection field not found");
      return;
    }
    try {
      Object cookie = createCookieDynamic(gameProfile, clientInfo);
      if (cookie == null) {
        FppLogger.warn("NmsPlayerSpawner: cannot inject fake listener - cookie creation failed");
        return;
      }

      FakeServerGamePacketListenerImpl fakeListener =
          FakeServerGamePacketListenerImpl.create(minecraftServer, conn, serverPlayer, cookie);

      connectionFieldInPlayer.set(serverPlayer, fakeListener);
      FppLogger.debug(
          "NmsPlayerSpawner: FakeServerGamePacketListenerImpl injected into"
              + " serverPlayer.connection");

      injectPacketListenerIntoConnection(conn, fakeListener);

    } catch (Exception e) {
      FppLogger.warn("NmsPlayerSpawner: fake listener injection failed: " + e.getMessage());
      FppLogger.debug(Arrays.toString(e.getStackTrace()));
    }
  }

  private static void injectPacketListenerIntoConnection(
      Object conn, FakeServerGamePacketListenerImpl fakeListener) {
    if (conn == null || serverGamePacketListenerClass == null) return;
    try {
      for (Field f : getAllDeclaredFields(conn.getClass())) {
        if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
        try {
          f.setAccessible(true);
          Object val = f.get(conn);
          if (val != null && serverGamePacketListenerClass.isInstance(val)) {
            f.set(conn, fakeListener);
            FppLogger.debug(
                "NmsPlayerSpawner: Connection."
                    + f.getName()
                    + " updated to FakeServerGamePacketListenerImpl"
                    + " (was "
                    + val.getClass().getSimpleName()
                    + ")");
            return;
          }
        } catch (Exception ignored) {
        }
      }
      FppLogger.debug(
          "NmsPlayerSpawner: Connection packetListener field not found"
              + " - onDisconnect override may not fire on double-disconnect");
    } catch (Exception e) {
      FppLogger.debug(
          "NmsPlayerSpawner: injectPacketListenerIntoConnection failed: " + e.getMessage());
    }
  }

  private static Object createFakeConnection() {
    try {
      FakeConnection conn = new FakeConnection(InetAddress.getLoopbackAddress());
      FppLogger.debug("NmsPlayerSpawner: FakeConnection created (direct Connection subclass)");
      return conn;

    } catch (Exception e) {
      FppLogger.warn("NmsPlayerSpawner.createFakeConnection failed: " + e.getMessage());
      return null;
    }
  }

  private static Object getClientInformation() {
    if (clientInfoDefault != null) return clientInfoDefault;
    if (clientInformationClass == null) return null;
    try {
      return clientInformationClass.getMethod("createDefault").invoke(null);
    } catch (Exception e) {
      return null;
    }
  }

  private static Object createServerPlayer(
      Object minecraftServer, Object serverLevel, Object gameProfile, Object clientInfo) {

    if (clientInfo != null && clientInformationClass != null) {
      try {
        Constructor<?> ctor =
            serverPlayerClass.getConstructor(
                minecraftServerClass,
                serverLevelClass,
                gameProfile.getClass(),
                clientInformationClass);
        return ctor.newInstance(minecraftServer, serverLevel, gameProfile, clientInfo);
      } catch (NoSuchMethodException ignored) {
      } catch (Exception e) {
        FppLogger.debug("4-arg ServerPlayer ctor failed: " + e.getMessage());
      }
    }

    try {
      Constructor<?> ctor =
          serverPlayerClass.getConstructor(
              minecraftServerClass, serverLevelClass, gameProfile.getClass());
      return ctor.newInstance(minecraftServer, serverLevel, gameProfile);
    } catch (Exception e) {
      FppLogger.error("NmsPlayerSpawner: no ServerPlayer constructor matched: " + e.getMessage());
      return null;
    }
  }

  private static boolean placePlayer(
      Object minecraftServer,
      Object conn,
      Object serverPlayer,
      Object gameProfile,
      Object clientInfo) {
    try {
      Object playerList = getPlayerListMethod.invoke(minecraftServer);
      if (conn == null || commonListenerCookieClass == null) {
        FppLogger.debug("placeNewPlayer skipped (conn=" + conn + ")");
        return false;
      }
      Object cookie = createCookieDynamic(gameProfile, clientInfo);
      if (cookie == null) return false;

      Method placeMethod = findMethod(playerList.getClass(), "placeNewPlayer", 3);
      if (placeMethod != null) {
        placeMethod.setAccessible(true);
        placeMethod.invoke(playerList, conn, serverPlayer, cookie);
        return true;
      }
      FppLogger.warn("NmsPlayerSpawner: placeNewPlayer(3-arg) not found on PlayerList");
    } catch (Exception e) {
      FppLogger.warn("NmsPlayerSpawner.placePlayer failed: " + e.getMessage());
    }
    return false;
  }

  private static Object createCookieDynamic(Object gameProfile, Object clientInfo) {
    if (commonListenerCookieClass == null) return null;

    try {
      Method factory =
          commonListenerCookieClass.getMethod(
              "createInitial", gameProfile.getClass(), boolean.class);
      return factory.invoke(null, gameProfile, false);
    } catch (Exception ignored) {
    }

    for (Constructor<?> c : commonListenerCookieClass.getDeclaredConstructors()) {
      c.setAccessible(true);
      Class<?>[] p = c.getParameterTypes();
      if (p.length > 0 && p[p.length - 1].getSimpleName().contains("DefaultConstructorMarker")) {
        continue;
      }
      try {
        Object result =
            switch (p.length) {
              case 1 -> c.newInstance(gameProfile);
              case 2 -> c.newInstance(gameProfile, 0);
              case 3 -> c.newInstance(gameProfile, 0, clientInfo);
              case 4 -> c.newInstance(gameProfile, 0, clientInfo, false);
              case 5 -> c.newInstance(gameProfile, 0, clientInfo, false, false);
              case 7 ->
                  c.newInstance(
                      gameProfile, 0, clientInfo, false, null, Collections.emptySet(), null);
              default -> null;
            };
        if (result != null) return result;
      } catch (Exception ignored) {
      }
    }
    FppLogger.debug("NmsPlayerSpawner: no CommonListenerCookie constructor succeeded");
    return null;
  }

  private static void initPreviousPosition(Object nmsPlayer, double x, double y, double z) {
    try {
      if (xoField != null) xoField.setDouble(nmsPlayer, x);
      if (yoField != null) yoField.setDouble(nmsPlayer, y);
      if (zoField != null) zoField.setDouble(nmsPlayer, z);
    } catch (Exception ignored) {
    }
  }

  private static Method findMethod(Class<?> clazz, String name, int paramCount) {
    Class<?> cur = clazz;
    while (cur != null && cur != Object.class) {
      for (Method m : cur.getDeclaredMethods()) {
        if (m.getName().equals(name) && m.getParameterCount() == paramCount) {
          m.setAccessible(true);
          return m;
        }
      }
      cur = cur.getSuperclass();
    }
    return null;
  }

  private static Method findMethod(
      Class<?> clazz, String name, int paramCount, Class<?>... paramTypes) {
    Class<?> cur = clazz;
    while (cur != null && cur != Object.class) {
      for (Method m : cur.getDeclaredMethods()) {
        if (!m.getName().equals(name) || m.getParameterCount() != paramCount) continue;
        if (paramTypes.length == 0) {
          m.setAccessible(true);
          return m;
        }

        Class<?>[] mParams = m.getParameterTypes();
        boolean match = true;
        for (int i = 0; i < paramTypes.length && i < mParams.length; i++) {
          if (!mParams[i].isAssignableFrom(paramTypes[i])) {
            match = false;
            break;
          }
        }
        if (match) {
          m.setAccessible(true);
          return m;
        }
      }
      cur = cur.getSuperclass();
    }
    return null;
  }

  private static Method findMethodBySignature(
      Class<?> clazz, int paramCount, Class<?>... paramTypes) {
    Class<?> cur = clazz;
    while (cur != null && cur != Object.class) {
      for (Method m : cur.getDeclaredMethods()) {
        if (m.getParameterCount() == paramCount
            && Arrays.equals(m.getParameterTypes(), paramTypes)) {
          m.setAccessible(true);
          return m;
        }
      }
      cur = cur.getSuperclass();
    }
    return null;
  }

  private static Field findFieldByName(Class<?> clazz, String name) {
    Class<?> cur = clazz;
    while (cur != null && cur != Object.class) {
      for (Field f : cur.getDeclaredFields()) {
        if (f.getName().equals(name)) {
          f.setAccessible(true);
          return f;
        }
      }
      cur = cur.getSuperclass();
    }
    return null;
  }

  private static List<Field> getAllDeclaredFields(Class<?> clazz) {
    List<Field> fields = new ArrayList<>();
    Class<?> cur = clazz;
    while (cur != null && cur != Object.class) {
      Collections.addAll(fields, cur.getDeclaredFields());
      cur = cur.getSuperclass();
    }
    return fields;
  }
}
