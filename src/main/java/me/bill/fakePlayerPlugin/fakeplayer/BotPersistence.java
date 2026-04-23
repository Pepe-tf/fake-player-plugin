package me.bill.fakePlayerPlugin.fakeplayer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import me.bill.fakePlayerPlugin.FakePlayerPlugin;
import me.bill.fakePlayerPlugin.command.AttackCommand;
import me.bill.fakePlayerPlugin.command.FollowCommand;
import me.bill.fakePlayerPlugin.command.MineCommand;
import me.bill.fakePlayerPlugin.command.MoveCommand;
import me.bill.fakePlayerPlugin.command.PlaceCommand;
import me.bill.fakePlayerPlugin.command.UseCommand;
import me.bill.fakePlayerPlugin.command.WaypointStore;
import me.bill.fakePlayerPlugin.config.Config;
import me.bill.fakePlayerPlugin.util.FppLogger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;

public final class BotPersistence {

  private static final String FILE_NAME = "active-bots.yml";
  private static final String INV_FILE_NAME = "bot-inventories.yml";
  private static final String TASKS_FILE_NAME = "bot-tasks.yml";
  private static final String XP_FILE_NAME = "bot-xp.yml";

  private final File dataFile;
  private final File inventoryFile;
  private final File tasksFile;
  private final File xpFile;
  private final FakePlayerPlugin plugin;

  private MoveCommand moveCommand;
  private MineCommand mineCommand;
  private PlaceCommand placeCommand;
  private UseCommand useCommand;
  private AttackCommand attackCommand;
  private FollowCommand followCommand;
  private WaypointStore waypointStore;

  public void setMoveCommand(MoveCommand cmd) {
    this.moveCommand = cmd;
  }

  public void setMineCommand(MineCommand cmd) {
    this.mineCommand = cmd;
  }

  public void setPlaceCommand(PlaceCommand cmd) {
    this.placeCommand = cmd;
  }

  public void setUseCommand(UseCommand cmd) {
    this.useCommand = cmd;
  }

  public void setAttackCommand(AttackCommand cmd) {
    this.attackCommand = cmd;
  }

  public void setFollowCommand(FollowCommand cmd) {
    this.followCommand = cmd;
  }

  public void setWaypointStore(WaypointStore s) {
    this.waypointStore = s;
  }

  private Map<String, Map<String, String>> loadedInventories = null;

  private Map<String, XpEntry> loadedXp = null;

  private Map<String, TaskEntry> loadedTasks = null;

  public BotPersistence(FakePlayerPlugin plugin) {
    this.plugin = plugin;
    File dataDir = new File(plugin.getDataFolder(), "data");
    if (!dataDir.exists() && !dataDir.mkdirs()) {
      FppLogger.warn(
          "BotPersistence: could not create data directory: " + dataDir.getAbsolutePath());
    }
    this.dataFile = new File(dataDir, FILE_NAME);
    this.inventoryFile = new File(dataDir, INV_FILE_NAME);
    this.tasksFile = new File(dataDir, TASKS_FILE_NAME);
    this.xpFile = new File(dataDir, XP_FILE_NAME);
  }

  public void save(Iterable<FakePlayer> players) {
    saveInternal(players);
    saveInventoriesInternal(players);
    saveXpInternal(players);
    saveTasksInternal(players);
  }

  public void saveAsync(Iterable<FakePlayer> players) {

    List<Object> list = buildList(players);
    Map<String, Map<String, String>> invSnap = snapshotInventories(players);
    Map<String, XpEntry> xpSnap = snapshotXp(players);
    Map<String, TaskEntry> taskSnap = snapshotTasks(players);
    Bukkit.getScheduler()
        .runTaskAsynchronously(
            plugin,
            () -> {
              YamlConfiguration yaml = new YamlConfiguration();
              yaml.set("bots", list);
              try {
                yaml.save(dataFile);
              } catch (IOException e) {
                FppLogger.error("Failed to auto-save active bots: " + e.getMessage());
              }
              writeInventorySnapshot(invSnap);
              writeXpSnapshot(xpSnap);
              writeTaskSnapshot(taskSnap);
            });
  }

  private void saveInternal(Iterable<FakePlayer> players) {
    YamlConfiguration yaml = new YamlConfiguration();
    yaml.set("bots", buildList(players));
    try {
      yaml.save(dataFile);
      FppLogger.info("Saved bot list to " + FILE_NAME + ".");
    } catch (IOException e) {
      FppLogger.error("Failed to save active bots: " + e.getMessage());
    }
  }

  private void saveInventoriesInternal(Iterable<FakePlayer> players) {
    writeInventorySnapshot(snapshotInventories(players));
  }

  private void saveXpInternal(Iterable<FakePlayer> players) {
    writeXpSnapshot(snapshotXp(players));
  }

  private void saveTasksInternal(Iterable<FakePlayer> players) {
    writeTaskSnapshot(snapshotTasks(players));
  }

  private Map<String, TaskEntry> snapshotTasks(Iterable<FakePlayer> players) {
    Map<String, TaskEntry> snap = new LinkedHashMap<>();
    for (FakePlayer fp : players) {
      String uuidStr = fp.getUuid().toString();

      String rcc = fp.getRightClickCommand();

      String patrolRoute = null;
      boolean patrolRandom = false;
      if (moveCommand != null) {
        patrolRoute = moveCommand.getActiveRouteForBot(fp.getUuid());
        patrolRandom = moveCommand.isRandomPatrolForBot(fp.getUuid());
      }

      String useWorld = null;
      double useX = 0, useY = 0, useZ = 0;
      float useYaw = 0, usePitch = 0;
      boolean useOnce = false;
      if (useCommand != null) {
        Location useLoc = useCommand.getActiveUseLocation(fp.getUuid());
        if (useLoc != null && useLoc.getWorld() != null) {
          useWorld = useLoc.getWorld().getName();
          useX = useLoc.getX();
          useY = useLoc.getY();
          useZ = useLoc.getZ();
          useYaw = useLoc.getYaw();
          usePitch = useLoc.getPitch();
          useOnce = useCommand.isActiveUseOnce(fp.getUuid());
        }
      }

      String mineWorld = null;
      double mineX = 0, mineY = 0, mineZ = 0;
      float mineYaw = 0, minePitch = 0;
      boolean mineOnce = false;
      if (mineCommand != null) {
        Location mineLoc = mineCommand.getActiveMineLocation(fp.getUuid());
        if (mineLoc != null && mineLoc.getWorld() != null) {
          mineWorld = mineLoc.getWorld().getName();
          mineX = mineLoc.getX();
          mineY = mineLoc.getY();
          mineZ = mineLoc.getZ();
          mineYaw = mineLoc.getYaw();
          minePitch = mineLoc.getPitch();
          mineOnce = mineCommand.isActiveMineOnce(fp.getUuid());
        }
      }

      String placeWorld = null;
      double placeX = 0, placeY = 0, placeZ = 0;
      float placeYaw = 0, placePitch = 0;
      boolean placeOnce = false;
      if (placeCommand != null) {
        Location placeLoc = placeCommand.getActivePlaceLocation(fp.getUuid());
        if (placeLoc != null && placeLoc.getWorld() != null) {
          placeWorld = placeLoc.getWorld().getName();
          placeX = placeLoc.getX();
          placeY = placeLoc.getY();
          placeZ = placeLoc.getZ();
          placeYaw = placeLoc.getYaw();
          placePitch = placeLoc.getPitch();
          placeOnce = placeCommand.isActivePlaceOnce(fp.getUuid());
        }
      }

      String areaPos1World = null, areaPos2World = null;
      double areaPos1X = 0, areaPos1Y = 0, areaPos1Z = 0;
      double areaPos2X = 0, areaPos2Y = 0, areaPos2Z = 0;
      boolean areaActive = false;
      if (mineCommand != null) {
        Location aPos1 = mineCommand.getSelectionPos1(fp.getUuid());
        Location aPos2 = mineCommand.getSelectionPos2(fp.getUuid());
        if (aPos1 != null
            && aPos1.getWorld() != null
            && aPos2 != null
            && aPos2.getWorld() != null) {
          areaPos1World = aPos1.getWorld().getName();
          areaPos1X = aPos1.getX();
          areaPos1Y = aPos1.getY();
          areaPos1Z = aPos1.getZ();
          areaPos2World = aPos2.getWorld().getName();
          areaPos2X = aPos2.getX();
          areaPos2Y = aPos2.getY();
          areaPos2Z = aPos2.getZ();
          areaActive = mineCommand.hasActiveAreaJob(fp.getUuid());
        }
      }

      String attackWorld = null;
      double attackX = 0, attackY = 0, attackZ = 0;
      float attackYaw = 0, attackPitch = 0;
      boolean attackOnce = false;
      if (attackCommand != null) {
        Location attackLoc = attackCommand.getActiveAttackLocation(fp.getUuid());
        if (attackLoc != null && attackLoc.getWorld() != null) {
          attackWorld = attackLoc.getWorld().getName();
          attackX = attackLoc.getX();
          attackY = attackLoc.getY();
          attackZ = attackLoc.getZ();
          attackYaw = attackLoc.getYaw();
          attackPitch = attackLoc.getPitch();
          attackOnce = attackCommand.isActiveAttackOnce(fp.getUuid());
        }
      }

      String followTargetUuid = null;
      if (followCommand != null && followCommand.isFollowing(fp.getUuid())) {
        java.util.UUID targetUuid = followCommand.getFollowTarget(fp.getUuid());
        if (targetUuid != null) followTargetUuid = targetUuid.toString();
      }

      String roamWorld = null;
      double roamX = 0, roamY = 0, roamZ = 0;
      double roamRadius = 0;
      if (moveCommand != null && moveCommand.isRoaming(fp.getUuid())) {
        Location roamCenter = moveCommand.getRoamCenter(fp.getUuid());
        Double roamR = moveCommand.getRoamRadius(fp.getUuid());
        if (roamCenter != null && roamCenter.getWorld() != null && roamR != null) {
          roamWorld = roamCenter.getWorld().getName();
          roamX = roamCenter.getX();
          roamY = roamCenter.getY();
          roamZ = roamCenter.getZ();
          roamRadius = roamR;
        }
      }

      if (rcc != null
          || patrolRoute != null
          || mineWorld != null
          || useWorld != null
          || areaPos1World != null
          || placeWorld != null
          || attackWorld != null
          || followTargetUuid != null
          || roamWorld != null) {
        snap.put(
            uuidStr,
            new TaskEntry(
                rcc,
                patrolRoute,
                patrolRandom,
                mineWorld,
                mineX,
                mineY,
                mineZ,
                mineYaw,
                minePitch,
                mineOnce,
                useWorld,
                useX,
                useY,
                useZ,
                useYaw,
                usePitch,
                useOnce,
                areaPos1World,
                areaPos1X,
                areaPos1Y,
                areaPos1Z,
                areaPos2World,
                areaPos2X,
                areaPos2Y,
                areaPos2Z,
                areaActive,
                placeWorld,
                placeX,
                placeY,
                placeZ,
                placeYaw,
                placePitch,
                placeOnce,
                attackWorld,
                attackX,
                attackY,
                attackZ,
                attackYaw,
                attackPitch,
                attackOnce,
                followTargetUuid,
                roamWorld,
                roamX,
                roamY,
                roamZ,
                roamRadius));
      }
    }
    return snap;
  }

  private void writeTaskSnapshot(Map<String, TaskEntry> snap) {

    YamlConfiguration yaml = new YamlConfiguration();
    for (Map.Entry<String, TaskEntry> e : snap.entrySet()) {
      String sec = e.getKey() + ".";
      TaskEntry t = e.getValue();
      if (t.rightClickCommand() != null)
        yaml.set(sec + "right-click-command", t.rightClickCommand());
      if (t.patrolRoute() != null) {
        yaml.set(sec + "patrol-route", t.patrolRoute());
        yaml.set(sec + "patrol-random", t.patrolRandom());
      }
      if (t.mineWorld() != null) {
        yaml.set(sec + "mine-world", t.mineWorld());
        yaml.set(sec + "mine-x", t.mineX());
        yaml.set(sec + "mine-y", t.mineY());
        yaml.set(sec + "mine-z", t.mineZ());
        yaml.set(sec + "mine-yaw", (double) t.mineYaw());
        yaml.set(sec + "mine-pitch", (double) t.minePitch());
        yaml.set(sec + "mine-once", t.mineOnce());
      }
      if (t.useWorld() != null) {
        yaml.set(sec + "use-world", t.useWorld());
        yaml.set(sec + "use-x", t.useX());
        yaml.set(sec + "use-y", t.useY());
        yaml.set(sec + "use-z", t.useZ());
        yaml.set(sec + "use-yaw", (double) t.useYaw());
        yaml.set(sec + "use-pitch", (double) t.usePitch());
        yaml.set(sec + "use-once", t.useOnce());
      }
      if (t.placeWorld() != null) {
        yaml.set(sec + "place-world", t.placeWorld());
        yaml.set(sec + "place-x", t.placeX());
        yaml.set(sec + "place-y", t.placeY());
        yaml.set(sec + "place-z", t.placeZ());
        yaml.set(sec + "place-yaw", (double) t.placeYaw());
        yaml.set(sec + "place-pitch", (double) t.placePitch());
        yaml.set(sec + "place-once", t.placeOnce());
      }
      if (t.attackWorld() != null) {
        yaml.set(sec + "attack-world", t.attackWorld());
        yaml.set(sec + "attack-x", t.attackX());
        yaml.set(sec + "attack-y", t.attackY());
        yaml.set(sec + "attack-z", t.attackZ());
        yaml.set(sec + "attack-yaw", (double) t.attackYaw());
        yaml.set(sec + "attack-pitch", (double) t.attackPitch());
        yaml.set(sec + "attack-once", t.attackOnce());
      }
      if (t.followTargetUuid() != null) {
        yaml.set(sec + "follow-target", t.followTargetUuid());
      }
      if (t.roamWorld() != null) {
        yaml.set(sec + "roam-world", t.roamWorld());
        yaml.set(sec + "roam-x", t.roamX());
        yaml.set(sec + "roam-y", t.roamY());
        yaml.set(sec + "roam-z", t.roamZ());
        yaml.set(sec + "roam-radius", t.roamRadius());
      }
      if (t.areaPos1World() != null && t.areaPos2World() != null) {
        yaml.set(sec + "area-pos1-world", t.areaPos1World());
        yaml.set(sec + "area-pos1-x", t.areaPos1X());
        yaml.set(sec + "area-pos1-y", t.areaPos1Y());
        yaml.set(sec + "area-pos1-z", t.areaPos1Z());
        yaml.set(sec + "area-pos2-world", t.areaPos2World());
        yaml.set(sec + "area-pos2-x", t.areaPos2X());
        yaml.set(sec + "area-pos2-y", t.areaPos2Y());
        yaml.set(sec + "area-pos2-z", t.areaPos2Z());
        yaml.set(sec + "area-active", t.areaActive());
      }
    }
    try {
      yaml.save(tasksFile);
      Config.debug("Saved task state for " + snap.size() + " bot(s) to YAML.");
    } catch (IOException ex) {
      FppLogger.error("Failed to save bot tasks: " + ex.getMessage());
    }

    me.bill.fakePlayerPlugin.database.DatabaseManager db = plugin.getDatabaseManager();
    if (db != null) {
      db.saveBotTasks(buildTaskRows(snap));
    }
  }

  private List<me.bill.fakePlayerPlugin.database.DatabaseManager.BotTaskRow> buildTaskRows(
      Map<String, TaskEntry> snap) {
    List<me.bill.fakePlayerPlugin.database.DatabaseManager.BotTaskRow> rows = new ArrayList<>();
    String serverId = me.bill.fakePlayerPlugin.config.Config.serverId();
    for (Map.Entry<String, TaskEntry> e : snap.entrySet()) {
      String uuid = e.getKey();
      TaskEntry t = e.getValue();
      if (t.patrolRoute() != null) {
        rows.add(
            new me.bill.fakePlayerPlugin.database.DatabaseManager.BotTaskRow(
                uuid,
                serverId,
                "PATROL",
                null,
                0,
                0,
                0,
                0f,
                0f,
                false,
                t.patrolRoute(),
                t.patrolRandom()));
      }
      if (t.mineWorld() != null) {
        rows.add(
            new me.bill.fakePlayerPlugin.database.DatabaseManager.BotTaskRow(
                uuid,
                serverId,
                "MINE",
                t.mineWorld(),
                t.mineX(),
                t.mineY(),
                t.mineZ(),
                t.mineYaw(),
                t.minePitch(),
                t.mineOnce(),
                null,
                false));
      }
      if (t.useWorld() != null) {
        rows.add(
            new me.bill.fakePlayerPlugin.database.DatabaseManager.BotTaskRow(
                uuid,
                serverId,
                "USE",
                t.useWorld(),
                t.useX(),
                t.useY(),
                t.useZ(),
                t.useYaw(),
                t.usePitch(),
                t.useOnce(),
                null,
                false));
      }
      if (t.placeWorld() != null) {
        rows.add(
            new me.bill.fakePlayerPlugin.database.DatabaseManager.BotTaskRow(
                uuid,
                serverId,
                "PLACE",
                t.placeWorld(),
                t.placeX(),
                t.placeY(),
                t.placeZ(),
                t.placeYaw(),
                t.placePitch(),
                t.placeOnce(),
                null,
                false));
      }
      if (t.attackWorld() != null) {
        rows.add(
            new me.bill.fakePlayerPlugin.database.DatabaseManager.BotTaskRow(
                uuid,
                serverId,
                "ATTACK",
                t.attackWorld(),
                t.attackX(),
                t.attackY(),
                t.attackZ(),
                t.attackYaw(),
                t.attackPitch(),
                t.attackOnce(),
                null,
                false));
      }
      if (t.followTargetUuid() != null) {
        rows.add(
            new me.bill.fakePlayerPlugin.database.DatabaseManager.BotTaskRow(
                uuid,
                serverId,
                "FOLLOW",
                null,
                0,
                0,
                0,
                0f,
                0f,
                false,
                t.followTargetUuid(),
                false));
      }
      if (t.roamWorld() != null) {
        rows.add(
            new me.bill.fakePlayerPlugin.database.DatabaseManager.BotTaskRow(
                uuid,
                serverId,
                "ROAM",
                t.roamWorld(),
                t.roamX(),
                t.roamY(),
                t.roamZ(),
                (float) t.roamRadius(),
                0f,
                false,
                null,
                false));
      }
    }
    return rows;
  }

  private Map<String, Map<String, String>> snapshotInventories(Iterable<FakePlayer> players) {
    Map<String, Map<String, String>> snap = new LinkedHashMap<>();
    for (FakePlayer fp : players) {
      Player bot = fp.getPlayer();
      if (bot == null || !bot.isValid()) continue;
      Map<String, String> slots = serializeInventory(bot.getInventory());
      if (!slots.isEmpty()) {
        snap.put(fp.getUuid().toString(), slots);
      }
    }
    return snap;
  }

  private Map<String, XpEntry> snapshotXp(Iterable<FakePlayer> players) {
    Map<String, XpEntry> snap = new LinkedHashMap<>();
    for (FakePlayer fp : players) {
      Player bot = fp.getPlayer();
      if (bot == null || !bot.isValid()) continue;
      snap.put(
          fp.getUuid().toString(),
          new XpEntry(bot.getTotalExperience(), bot.getLevel(), bot.getExp()));
    }
    return snap;
  }

  private void writeInventorySnapshot(Map<String, Map<String, String>> snap) {
    YamlConfiguration yaml = new YamlConfiguration();
    for (Map.Entry<String, Map<String, String>> entry : snap.entrySet()) {
      String uuidKey = entry.getKey();
      for (Map.Entry<String, String> slot : entry.getValue().entrySet()) {
        yaml.set("inventories." + uuidKey + "." + slot.getKey(), slot.getValue());
      }
    }
    try {
      yaml.save(inventoryFile);
      Config.debug("Saved inventories for " + snap.size() + " bot(s).");
    } catch (IOException e) {
      FppLogger.error("Failed to save bot inventories: " + e.getMessage());
    }
  }

  private void writeXpSnapshot(Map<String, XpEntry> snap) {
    YamlConfiguration yaml = new YamlConfiguration();
    for (Map.Entry<String, XpEntry> entry : snap.entrySet()) {
      String base = "xp." + entry.getKey() + ".";
      XpEntry xp = entry.getValue();
      yaml.set(base + "total", xp.totalExperience());
      yaml.set(base + "level", xp.level());
      yaml.set(base + "progress", (double) xp.progress());
    }
    try {
      yaml.save(xpFile);
      Config.debug("Saved XP for " + snap.size() + " bot(s).");
    } catch (IOException e) {
      FppLogger.error("Failed to save bot XP: " + e.getMessage());
    }
  }

  private static Map<String, String> serializeInventory(PlayerInventory inv) {
    Map<String, String> slots = new LinkedHashMap<>();

    ItemStack[] contents = inv.getContents();
    for (int i = 0; i < contents.length; i++) {
      if (contents[i] != null && contents[i].getType() != Material.AIR) {
        try {
          slots.put(
              String.valueOf(i),
              Base64.getEncoder().encodeToString(contents[i].serializeAsBytes()));
        } catch (Exception ignored) {
        }
      }
    }

    ItemStack[] armour = inv.getArmorContents();
    for (int i = 0; i < armour.length; i++) {
      if (armour[i] != null && armour[i].getType() != Material.AIR) {
        try {
          slots.put(
              String.valueOf(36 + i),
              Base64.getEncoder().encodeToString(armour[i].serializeAsBytes()));
        } catch (Exception ignored) {
        }
      }
    }

    ItemStack offhand = inv.getItemInOffHand();
    if (offhand != null && offhand.getType() != Material.AIR) {
      try {
        slots.put("40", Base64.getEncoder().encodeToString(offhand.serializeAsBytes()));
      } catch (Exception ignored) {
      }
    }
    return slots;
  }

  private List<Object> buildList(Iterable<FakePlayer> players) {
    List<Object> list = new ArrayList<>();
    for (FakePlayer fp : players) {
      org.bukkit.entity.Entity body = fp.getPhysicsEntity();
      Location loc = (body != null && body.isValid()) ? body.getLocation() : fp.getSpawnLocation();
      if (loc == null || loc.getWorld() == null) continue;

      var section = new java.util.LinkedHashMap<String, Object>();
      section.put("name", fp.getName());
      section.put("uuid", fp.getUuid().toString());
      section.put("display-name", fp.getDisplayName());
      section.put("spawned-by", fp.getSpawnedBy());
      section.put("spawned-by-uuid", fp.getSpawnedByUuid().toString());
      section.put("world", loc.getWorld().getName());
      section.put("x", loc.getX());
      section.put("y", loc.getY());
      section.put("z", loc.getZ());
      section.put("yaw", (double) loc.getYaw());
      section.put("pitch", (double) loc.getPitch());
      section.put("bot-type", fp.getBotType().name());
      section.put("chat-enabled", fp.isChatEnabled());
      section.put("head-ai-enabled", fp.isHeadAiEnabled());
      section.put("pickup-items", fp.isPickUpItemsEnabled());
      section.put("pickup-xp", fp.isPickUpXpEnabled());
      section.put("frozen", fp.isFrozen());
      section.put("nav-parkour", fp.isNavParkour());
      section.put("nav-break-blocks", fp.isNavBreakBlocks());
      section.put("nav-place-blocks", fp.isNavPlaceBlocks());
      section.put("nav-sprint-jump", fp.isNavSprintJump());
      section.put("swim-ai-enabled", fp.isSwimAiEnabled());
      section.put("chunk-load-radius", fp.getChunkLoadRadius());
      section.put("pve-enabled", fp.isPveEnabled());
      section.put("pve-range", fp.getPveRange());
      if (fp.getPvePriority() != null) section.put("pve-priority", fp.getPvePriority());
      if (fp.getPveMobType() != null) section.put("pve-mob-type", fp.getPveMobType());
      Player bot = fp.getPlayer();
      if (bot != null) {
        section.put("xp-total", bot.getTotalExperience());
        section.put("xp-level", bot.getLevel());
        section.put("xp-progress", (double) bot.getExp());
      }
      if (fp.hasCustomPing()) {
        section.put("ping", fp.getPing());
      }
      if (fp.getChatTier() != null) {
        section.put("chat-tier", fp.getChatTier());
      }
      if (fp.getAiPersonality() != null) {
        section.put("ai-personality", fp.getAiPersonality());
      }
      if (fp.getRightClickCommand() != null) {
        section.put("right-click-command", fp.getRightClickCommand());
      }
      SkinProfile skin = fp.getResolvedSkin();
      if (skin != null && skin.isValid()) {
        section.put("skin-texture", skin.getValue());
        if (skin.getSignature() != null) {
          section.put("skin-signature", skin.getSignature());
        }
      }
      list.add(section);
    }
    return list;
  }

  public void restore(FakePlayerManager manager) {
    if (!Config.persistOnRestart()) {
      deleteFile(dataFile);
      FppLogger.info("Bot persistence is disabled - skipping restore.");
      return;
    }

    manager.setRestorationInProgress(true);

    loadInventoryFile();
    loadXpFile();

    loadTasksFile();

    me.bill.fakePlayerPlugin.database.DatabaseManager db = plugin.getDatabaseManager();
    if (db != null) {

      List<me.bill.fakePlayerPlugin.database.DatabaseManager.ActiveBotRow> rows =
          db.getActiveBotsForThisServer();
      if (!rows.isEmpty()) {
        FppLogger.info(
            "Restoring "
                + rows.size()
                + " bot(s) from database (server='"
                + me.bill.fakePlayerPlugin.config.Config.serverId()
                + "')...");

        db.clearActiveBots();

        deleteFile(dataFile);

        List<SavedBot> saved = new ArrayList<>();
        for (var row : rows) {
          try {
            saved.add(
                new SavedBot(
                    row.botName(),
                    UUID.fromString(row.botUuid()),
                    row.botDisplay(),
                    row.spawnedBy(),
                    UUID.fromString(row.spawnedByUuid()),
                    row.world(),
                    row.x(),
                    row.y(),
                    row.z(),
                    row.yaw(),
                    row.pitch(),
                    null,
                    row.botName().startsWith("pvp_") ? BotType.PVP : BotType.AFK,
                    row.chatEnabled(),
                    row.chatTier(),
                    row.aiPersonality(),
                    row.headAiEnabled(),
                    row.pickUpItems(),
                    row.pickUpXp(),
                    0,
                    0,
                    0f,
                    row.frozen(),
                    row.navParkour(),
                    row.navBreakBlocks(),
                    row.navPlaceBlocks(),
                    Config.pathfindingSprintJump(),
                    row.swimAiEnabled(),
                    row.ping(),
                    row.rightClickCmd(),
                    row.pveEnabled(),
                    row.pveRange(),
                    row.pvePriority(),
                    row.pveMobType(),
                    row.skinTexture(),
                    row.skinSignature()));
          } catch (Exception e) {
            FppLogger.warn("Skipping malformed DB active-bot row: " + e.getMessage());
          }
        }
        if (!saved.isEmpty()) {
          Bukkit.getScheduler().runTaskLater(plugin, () -> restoreChain(manager, saved, 0), 40L);
        } else {

          manager.setRestorationInProgress(false);
        }
        return;
      }
    }

    if (!dataFile.exists()) {
      manager.setRestorationInProgress(false);
      return;
    }

    YamlConfiguration yaml = YamlConfiguration.loadConfiguration(dataFile);
    List<?> raw = yaml.getList("bots");
    if (raw == null || raw.isEmpty()) {
      deleteFile(dataFile);
      manager.setRestorationInProgress(false);
      return;
    }

    List<SavedBot> saved = new ArrayList<>();
    for (Object obj : raw) {
      if (!(obj instanceof java.util.Map<?, ?> map)) continue;
      try {
        String name = (String) map.get("name");
        UUID uuid = UUID.fromString((String) map.get("uuid"));
        String displayName = (String) map.get("display-name");
        Object sbRaw = map.get("spawned-by");
        String spawnedBy = sbRaw instanceof String s ? s : "SERVER";
        Object sbuRaw = map.get("spawned-by-uuid");
        UUID spawnedByUuid = sbuRaw instanceof String str ? UUID.fromString(str) : new UUID(0, 0);
        String worldName = (String) map.get("world");
        double x = toDouble(map.get("x"));
        double y = toDouble(map.get("y"));
        double z = toDouble(map.get("z"));
        float yaw = (float) toDouble(map.get("yaw"));
        float pitch = (float) toDouble(map.get("pitch"));
        Object btRaw = map.get("bot-type");
        BotType botType = btRaw instanceof String bts ? BotType.parse(bts) : BotType.AFK;
        Object ceRaw = map.get("chat-enabled");
        boolean chatEnabled = !(ceRaw instanceof Boolean b) || b;
        Object headAiRaw = map.get("head-ai-enabled");
        boolean headAiEnabled = !(headAiRaw instanceof Boolean hai) || hai;
        Object pickupItemsRaw = map.get("pickup-items");
        boolean pickUpItems = pickupItemsRaw instanceof Boolean pi ? pi : Config.bodyPickUpItems();
        Object pickupXpRaw = map.get("pickup-xp");
        boolean pickUpXp = pickupXpRaw instanceof Boolean px ? px : Config.bodyPickUpXp();
        Object frozenRaw = map.get("frozen");
        boolean frozen = frozenRaw instanceof Boolean fr && fr;
        Object navPkRaw = map.get("nav-parkour");
        boolean navParkour = navPkRaw instanceof Boolean npk ? npk : Config.pathfindingParkour();
        Object navBbRaw = map.get("nav-break-blocks");
        boolean navBreakBlocks =
            navBbRaw instanceof Boolean nbb ? nbb : Config.pathfindingBreakBlocks();
        Object navPbRaw = map.get("nav-place-blocks");
        boolean navPlaceBlocks =
            navPbRaw instanceof Boolean npb ? npb : Config.pathfindingPlaceBlocks();
        Object navSjRaw = map.get("nav-sprint-jump");
        boolean navSprintJump = navSjRaw instanceof Boolean nsj ? nsj : Config.pathfindingSprintJump();
        Object swimAiRaw = map.get("swim-ai-enabled");
        boolean swimAiEnabled = !(swimAiRaw instanceof Boolean sae) || sae;
        Object clrRaw = map.get("chunk-load-radius");
        int chunkLoadRadius = clrRaw instanceof Number clrn ? clrn.intValue() : -1;
        Object pingRaw = map.get("ping");
        int ping = pingRaw instanceof Number pr ? pr.intValue() : -1;
        Object rccRaw = map.get("right-click-command");
        String rightClickCommand = rccRaw instanceof String rcc ? rcc : null;
        Object xpTotalRaw = map.get("xp-total");
        int xpTotal = xpTotalRaw instanceof Number n1 ? n1.intValue() : 0;
        Object xpLevelRaw = map.get("xp-level");
        int xpLevel = xpLevelRaw instanceof Number n2 ? n2.intValue() : 0;
        Object xpProgressRaw = map.get("xp-progress");
        float xpProgress = xpProgressRaw instanceof Number n3 ? n3.floatValue() : 0f;
        Object ctRaw = map.get("chat-tier");
        String chatTier = ctRaw instanceof String s2 ? s2 : null;
        Object apRaw = map.get("ai-personality");
        String aiPersonality = apRaw instanceof String s3 ? s3 : null;
        Object pveEnRaw = map.get("pve-enabled");
        boolean pveEnabled = pveEnRaw instanceof Boolean pve && pve;
        Object pveRgRaw = map.get("pve-range");
        double pveRange =
            pveRgRaw instanceof Number prn ? prn.doubleValue() : Config.attackMobDefaultRange();
        Object pvePrRaw = map.get("pve-priority");
        String pvePriority = pvePrRaw instanceof String pps ? pps : null;
        Object pveMtRaw = map.get("pve-mob-type");
        String pveMobType = pveMtRaw instanceof String pmt ? pmt : null;
        Object skinTexRaw = map.get("skin-texture");
        String skinTexture = skinTexRaw instanceof String st ? st : null;
        Object skinSigRaw = map.get("skin-signature");
        String skinSignature = skinSigRaw instanceof String ss ? ss : null;
        if (name == null || worldName == null) continue;
        saved.add(
            new SavedBot(
                name,
                uuid,
                displayName,
                spawnedBy,
                spawnedByUuid,
                worldName,
                x,
                y,
                z,
                yaw,
                pitch,
                null,
                botType,
                chatEnabled,
                chatTier,
                aiPersonality,
                headAiEnabled,
                pickUpItems,
                pickUpXp,
                xpTotal,
                xpLevel,
                xpProgress,
                frozen,
                navParkour,
                navBreakBlocks,
                navPlaceBlocks,
                navSprintJump,
                swimAiEnabled,
                ping,
                rightClickCommand,
                pveEnabled,
                pveRange,
                pvePriority,
                pveMobType,
                skinTexture,
                skinSignature));
      } catch (Exception e) {
        FppLogger.warn("Skipping malformed bot entry in " + FILE_NAME + ": " + e.getMessage());
      }
    }
    deleteFile(dataFile);
    if (saved.isEmpty()) {
      manager.setRestorationInProgress(false);
      return;
    }

    FppLogger.info("Restoring " + saved.size() + " bot(s) from YAML fallback...");
    Bukkit.getScheduler().runTaskLater(plugin, () -> restoreChain(manager, saved, 0), 40L);
  }

  private void restoreChain(FakePlayerManager manager, List<SavedBot> saved, int index) {
    if (index >= saved.size()) {

      manager.setRestorationInProgress(false);
      loadedInventories = null;
      loadedXp = null;
      loadedTasks = null;
      deleteFile(tasksFile);
      FppLogger.info("Bot restoration complete: " + saved.size() + " bot(s) restored.");
      return;
    }

    SavedBot sb = saved.get(index);

    World world = Bukkit.getWorld(sb.worldName);
    if (world == null) {
      FppLogger.warn(
          "Cannot restore bot '"
              + sb.name
              + "' - world '"
              + sb.worldName
              + "' not found. Skipping.");
      restoreChain(manager, saved, index + 1);
      return;
    }

    Location loc = new Location(world, sb.x, sb.y, sb.z, sb.yaw, sb.pitch);

    manager.spawnRestored(
        sb.name, sb.uuid, sb.displayName, sb.spawnedBy, sb.spawnedByUuid, loc, sb.botType);

    FakePlayer fp = manager.getByName(sb.name);
    if (fp != null) {

      if (sb.skinTexture != null && !sb.skinTexture.isBlank()) {
        fp.setResolvedSkin(
            new SkinProfile(sb.skinTexture, sb.skinSignature, "persisted:" + sb.name));
        Config.debugSkin("Restored persisted skin for bot '" + sb.name + "'");
      }

      fp.setChatEnabled(sb.chatEnabled);
      fp.setHeadAiEnabled(sb.headAiEnabled);
      fp.setPickUpItemsEnabled(sb.pickUpItemsEnabled);
      fp.setPickUpXpEnabled(sb.pickUpXpEnabled);
      fp.setFrozen(sb.frozen);
      fp.setNavParkour(sb.navParkour);
      fp.setNavBreakBlocks(sb.navBreakBlocks);
      fp.setNavPlaceBlocks(sb.navPlaceBlocks);
      fp.setNavSprintJump(sb.navSprintJump);
      fp.setSwimAiEnabled(sb.swimAiEnabled);
      fp.setPing(sb.ping);
      fp.setPveEnabled(sb.pveEnabled);
      fp.setPveRange(sb.pveRange);
      if (sb.pvePriority != null) fp.setPvePriority(sb.pvePriority);
      if (sb.pveMobType != null) fp.setPveMobType(sb.pveMobType);
      if (sb.chatTier != null) fp.setChatTier(sb.chatTier);

      if (sb.aiPersonality != null) fp.setAiPersonality(sb.aiPersonality);

      if (sb.rightClickCommand != null && fp.getRightClickCommand() == null) {
        fp.setRightClickCommand(sb.rightClickCommand);
      }

      manager.persistBotSettings(fp);

      if (fp.isPveEnabled()) {
        final FakePlayer pveBot = fp;
        Bukkit.getScheduler()
            .runTaskLater(
                plugin,
                () -> {
                  var attackCmd = plugin.getAttackCommand();
                  if (attackCmd != null
                      && pveBot.getPlayer() != null
                      && pveBot.getPlayer().isOnline()) {
                    attackCmd.startMobModeFromSettings(pveBot);
                  }
                },
                15L);
      }
    }

    if (loadedTasks != null) {
      TaskEntry te = loadedTasks.get(sb.uuid.toString());
      if (te != null && fp != null && te.rightClickCommand() != null) {
        fp.setRightClickCommand(te.rightClickCommand());
      }
    }

    if (loadedInventories != null) {
      Map<String, String> invSlots = loadedInventories.get(sb.uuid.toString());
      if (invSlots != null && !invSlots.isEmpty()) {
        final UUID restoredUuid = sb.uuid;
        final String restoredName = sb.name;
        Bukkit.getScheduler()
            .runTaskLater(
                plugin,
                () -> {
                  FakePlayer restored = manager.getByUuid(restoredUuid);
                  if (restored == null) return;
                  Player bot = restored.getPlayer();
                  if (bot == null || !bot.isValid()) return;
                  applyInventory(bot.getInventory(), invSlots);
                  Config.debug("Restored inventory for bot '" + restoredName + "'.");
                },
                10L);
      }
    }

    XpEntry xpEntry = loadedXp != null ? loadedXp.get(sb.uuid.toString()) : null;
    if (xpEntry == null && (sb.xpTotal > 0 || sb.xpLevel > 0 || sb.xpProgress > 0f)) {
      xpEntry = new XpEntry(sb.xpTotal, sb.xpLevel, sb.xpProgress);
    }
    final XpEntry xpToRestore = xpEntry;
    if (xpToRestore != null) {
      final UUID restoredUuid = sb.uuid;
      final String restoredName = sb.name;
      Bukkit.getScheduler()
          .runTaskLater(
              plugin,
              () -> {
                FakePlayer restored = manager.getByUuid(restoredUuid);
                if (restored == null) return;
                Player bot = restored.getPlayer();
                if (bot == null || !bot.isValid()) return;
                bot.setTotalExperience(0);
                bot.setLevel(0);
                bot.setExp(0f);
                bot.setLevel(xpToRestore.level());
                bot.setExp(xpToRestore.progress());
                bot.setTotalExperience(xpToRestore.totalExperience());
                Config.debug("Restored XP for bot '" + restoredName + "'.");
              },
              12L);
    }

    if (loadedTasks != null) {
      TaskEntry te = loadedTasks.get(sb.uuid.toString());
      if (te != null
          && (te.patrolRoute() != null
              || te.mineWorld() != null
              || te.useWorld() != null
              || te.areaPos1World() != null
              || te.placeWorld() != null
              || te.roamWorld() != null)) {
        final TaskEntry task = te;
        final UUID restoredUuid = sb.uuid;
        Bukkit.getScheduler()
            .runTaskLater(
                plugin,
                () -> {
                  FakePlayer restored = manager.getByUuid(restoredUuid);
                  if (restored == null) return;
                  Player bot = restored.getPlayer();
                  if (bot == null || !bot.isOnline()) return;

                  if (task.patrolRoute() != null && moveCommand != null && waypointStore != null) {
                    List<Location> route = waypointStore.getRoute(task.patrolRoute());
                    if (route != null
                        && !route.isEmpty()
                        && route.get(0).getWorld() != null
                        && route.get(0).getWorld().equals(bot.getWorld())) {
                      moveCommand.resumePatrol(bot, route, task.patrolRandom(), task.patrolRoute());
                      Config.debug(
                          "Resumed patrol '"
                              + task.patrolRoute()
                              + "' for bot '"
                              + restored.getName()
                              + "'.");
                    }
                  }

                  if (task.mineWorld() != null && mineCommand != null) {
                    World w = Bukkit.getWorld(task.mineWorld());
                    if (w != null && w.equals(bot.getWorld())) {
                      Location mineLoc =
                          new Location(
                              w,
                              task.mineX(),
                              task.mineY(),
                              task.mineZ(),
                              task.mineYaw(),
                              task.minePitch());
                      mineCommand.resumeMining(restored, task.mineOnce(), mineLoc);
                      Config.debug("Resumed mine task for bot '" + restored.getName() + "'.");
                    }
                  }

                  if (task.areaActive()
                      && task.areaPos1World() != null
                      && task.areaPos2World() != null
                      && mineCommand != null) {
                    World w1 = Bukkit.getWorld(task.areaPos1World());
                    World w2 = Bukkit.getWorld(task.areaPos2World());
                    if (w1 != null && w2 != null && w1.equals(w2) && w1.equals(bot.getWorld())) {
                      Location pos1 =
                          new Location(w1, task.areaPos1X(), task.areaPos1Y(), task.areaPos1Z());
                      Location pos2 =
                          new Location(w2, task.areaPos2X(), task.areaPos2Y(), task.areaPos2Z());
                      mineCommand.restoreAreaJob(restored, pos1, pos2);
                    }
                  } else if (!task.areaActive()
                      && task.areaPos1World() != null
                      && task.areaPos2World() != null
                      && mineCommand != null) {

                    Config.debug(
                        "Area selection for bot '"
                            + restored.getName()
                            + "' will be lazily loaded from"
                            + " MineSelectionStore on next --start.");
                  }

                  if (task.useWorld() != null && useCommand != null) {
                    World w = Bukkit.getWorld(task.useWorld());
                    if (w != null && w.equals(bot.getWorld())) {
                      Location useLoc =
                          new Location(
                              w,
                              task.useX(),
                              task.useY(),
                              task.useZ(),
                              task.useYaw(),
                              task.usePitch());
                      useCommand.resumeUsing(restored, task.useOnce(), useLoc);
                      Config.debug("Resumed use loop for bot '" + restored.getName() + "'.");
                    }
                  }

                  if (task.placeWorld() != null && placeCommand != null) {
                    World w = Bukkit.getWorld(task.placeWorld());
                    if (w != null && w.equals(bot.getWorld())) {
                      Location placeLoc =
                          new Location(
                              w,
                              task.placeX(),
                              task.placeY(),
                              task.placeZ(),
                              task.placeYaw(),
                              task.placePitch());
                      placeCommand.resumePlacing(restored, task.placeOnce(), placeLoc);
                      Config.debug("Resumed place task for bot '" + restored.getName() + "'.");
                    }
                  }

                  if (task.attackWorld() != null && attackCommand != null) {
                    World w = Bukkit.getWorld(task.attackWorld());
                    if (w != null && w.equals(bot.getWorld())) {
                      Location attackLoc =
                          new Location(
                              w,
                              task.attackX(),
                              task.attackY(),
                              task.attackZ(),
                              task.attackYaw(),
                              task.attackPitch());
                      attackCommand.resumeAttacking(restored, task.attackOnce(), attackLoc);
                      Config.debug("Resumed attack task for bot '" + restored.getName() + "'.");
                    }
                  }

                  if (task.followTargetUuid() != null && followCommand != null) {
                    try {
                      java.util.UUID targetUuid =
                          java.util.UUID.fromString(task.followTargetUuid());
                      followCommand.resumeFollowing(restored, targetUuid);
                      Config.debug("Resumed follow task for bot '" + restored.getName() + "'.");
                    } catch (IllegalArgumentException ignored) {
                    }
                  }

                  if (task.roamWorld() != null && moveCommand != null) {
                    World w = Bukkit.getWorld(task.roamWorld());
                    if (w != null && w.equals(bot.getWorld())) {
                      Location roamCenter =
                          new Location(w, task.roamX(), task.roamY(), task.roamZ());
                      double roamR = task.roamRadius();
                      if (roamR < 3) roamR = 20;
                      moveCommand.resumeRoaming(restored, roamCenter, roamR);
                      Config.debug(
                          "Resumed roaming for bot '"
                              + restored.getName()
                              + "' (radius "
                              + (int) roamR
                              + ").");
                    }
                  }
                },
                25L);
      }
    }

    int delayMinTicks = Config.joinDelayMin();
    int delayMaxTicks = Math.max(delayMinTicks, Config.joinDelayMax());
    long delayTicks;
    if (delayMaxTicks <= 0) {
      delayTicks = 1L;
    } else {
      int spread = delayMaxTicks - delayMinTicks;
      int t =
          delayMinTicks
              + (spread > 0
                  ? java.util.concurrent.ThreadLocalRandom.current().nextInt(spread + 1)
                  : 0);
      delayTicks = Math.max(1L, (long) t);
    }

    Bukkit.getScheduler()
        .runTaskLater(plugin, () -> restoreChain(manager, saved, index + 1), delayTicks);
  }

  private void loadInventoryFile() {
    loadedInventories = new HashMap<>();
    if (!inventoryFile.exists()) return;
    YamlConfiguration yaml = YamlConfiguration.loadConfiguration(inventoryFile);
    ConfigurationSection invSection = yaml.getConfigurationSection("inventories");
    if (invSection == null) return;
    for (String uuidKey : invSection.getKeys(false)) {
      ConfigurationSection botSection = invSection.getConfigurationSection(uuidKey);
      if (botSection == null) continue;
      Map<String, String> slots = new LinkedHashMap<>();
      for (String slot : botSection.getKeys(false)) {
        String val = botSection.getString(slot);
        if (val != null && !val.isEmpty()) slots.put(slot, val);
      }
      if (!slots.isEmpty()) loadedInventories.put(uuidKey, slots);
    }
    Config.debug(
        "Loaded inventories for "
            + loadedInventories.size()
            + " bot(s) from "
            + INV_FILE_NAME
            + ".");
  }

  private void loadXpFile() {
    loadedXp = new HashMap<>();
    if (!xpFile.exists()) return;
    YamlConfiguration yaml = YamlConfiguration.loadConfiguration(xpFile);
    ConfigurationSection xpSection = yaml.getConfigurationSection("xp");
    if (xpSection == null) return;
    for (String uuidKey : xpSection.getKeys(false)) {
      ConfigurationSection sec = xpSection.getConfigurationSection(uuidKey);
      if (sec == null) continue;
      loadedXp.put(
          uuidKey,
          new XpEntry(
              sec.getInt("total", 0),
              sec.getInt("level", 0),
              (float) sec.getDouble("progress", 0.0)));
    }
    Config.debug("Loaded XP for " + loadedXp.size() + " bot(s) from " + XP_FILE_NAME + ".");
  }

  private void loadTasksFile() {
    loadedTasks = new HashMap<>();

    me.bill.fakePlayerPlugin.database.DatabaseManager db = plugin.getDatabaseManager();
    if (db != null) {
      List<me.bill.fakePlayerPlugin.database.DatabaseManager.BotTaskRow> dbRows =
          db.loadBotTasksForThisServer();
      if (!dbRows.isEmpty()) {
        loadedTasks = buildTasksFromDbRows(dbRows);

        db.clearBotTasks();
        Config.debug("Loaded task state for " + loadedTasks.size() + " bot(s) from DB.");
        return;
      }
    }

    if (!tasksFile.exists()) return;
    YamlConfiguration yaml = YamlConfiguration.loadConfiguration(tasksFile);
    for (String uuidStr : yaml.getKeys(false)) {
      ConfigurationSection sec = yaml.getConfigurationSection(uuidStr);
      if (sec == null) continue;
      String rcc = sec.getString("right-click-command");
      String patrolRoute = sec.getString("patrol-route");
      boolean patrolRandom = sec.getBoolean("patrol-random", false);
      String mineWorld = sec.getString("mine-world");
      double mineX = sec.getDouble("mine-x");
      double mineY = sec.getDouble("mine-y");
      double mineZ = sec.getDouble("mine-z");
      float mineYaw = (float) sec.getDouble("mine-yaw");
      float minePitch = (float) sec.getDouble("mine-pitch");
      boolean mineOnce = sec.getBoolean("mine-once", false);
      String useWorld = sec.getString("use-world");
      double useX = sec.getDouble("use-x");
      double useY = sec.getDouble("use-y");
      double useZ = sec.getDouble("use-z");
      float useYaw = (float) sec.getDouble("use-yaw");
      float usePitch = (float) sec.getDouble("use-pitch");
      boolean useOnce = sec.getBoolean("use-once", false);

      String placeWorld = sec.getString("place-world");
      double placeX = sec.getDouble("place-x");
      double placeY = sec.getDouble("place-y");
      double placeZ = sec.getDouble("place-z");
      float placeYaw = (float) sec.getDouble("place-yaw");
      float placePitch = (float) sec.getDouble("place-pitch");
      boolean placeOnce = sec.getBoolean("place-once", false);

      String areaPos1World = sec.getString("area-pos1-world");
      double areaPos1X = sec.getDouble("area-pos1-x");
      double areaPos1Y = sec.getDouble("area-pos1-y");
      double areaPos1Z = sec.getDouble("area-pos1-z");
      String areaPos2World = sec.getString("area-pos2-world");
      double areaPos2X = sec.getDouble("area-pos2-x");
      double areaPos2Y = sec.getDouble("area-pos2-y");
      double areaPos2Z = sec.getDouble("area-pos2-z");
      boolean areaActive = sec.getBoolean("area-active", false);

      String attackWorld = sec.getString("attack-world");
      double attackX = sec.getDouble("attack-x");
      double attackY = sec.getDouble("attack-y");
      double attackZ = sec.getDouble("attack-z");
      float attackYaw = (float) sec.getDouble("attack-yaw");
      float attackPitch = (float) sec.getDouble("attack-pitch");
      boolean attackOnce = sec.getBoolean("attack-once", false);

      String followTarget = sec.getString("follow-target");

      String roamWorld = sec.getString("roam-world");
      double roamX = sec.getDouble("roam-x");
      double roamY = sec.getDouble("roam-y");
      double roamZ = sec.getDouble("roam-z");
      double roamRadius = sec.getDouble("roam-radius");

      loadedTasks.put(
          uuidStr,
          new TaskEntry(
              rcc,
              patrolRoute,
              patrolRandom,
              mineWorld,
              mineX,
              mineY,
              mineZ,
              mineYaw,
              minePitch,
              mineOnce,
              useWorld,
              useX,
              useY,
              useZ,
              useYaw,
              usePitch,
              useOnce,
              areaPos1World,
              areaPos1X,
              areaPos1Y,
              areaPos1Z,
              areaPos2World,
              areaPos2X,
              areaPos2Y,
              areaPos2Z,
              areaActive,
              placeWorld,
              placeX,
              placeY,
              placeZ,
              placeYaw,
              placePitch,
              placeOnce,
              attackWorld,
              attackX,
              attackY,
              attackZ,
              attackYaw,
              attackPitch,
              attackOnce,
              followTarget,
              roamWorld,
              roamX,
              roamY,
              roamZ,
              roamRadius));
    }
    Config.debug(
        "Loaded task state for " + loadedTasks.size() + " bot(s) from " + TASKS_FILE_NAME + ".");
  }

  private Map<String, TaskEntry> buildTasksFromDbRows(
      List<me.bill.fakePlayerPlugin.database.DatabaseManager.BotTaskRow> rows) {

    Map<String, Map<String, me.bill.fakePlayerPlugin.database.DatabaseManager.BotTaskRow>> byUuid =
        new LinkedHashMap<>();
    for (var row : rows) {
      byUuid.computeIfAbsent(row.botUuid(), k -> new LinkedHashMap<>()).put(row.taskType(), row);
    }
    Map<String, TaskEntry> result = new LinkedHashMap<>();
    for (var entry : byUuid.entrySet()) {
      String uuid = entry.getKey();
      var tasks = entry.getValue();
      var patrol = tasks.get("PATROL");
      var mine = tasks.get("MINE");
      var use = tasks.get("USE");
      var place = tasks.get("PLACE");
      var attack = tasks.get("ATTACK");
      var follow = tasks.get("FOLLOW");
      var roam = tasks.get("ROAM");
      result.put(
          uuid,
          new TaskEntry(
              null,
              patrol != null ? patrol.extraStr() : null,
              patrol != null && patrol.extraBool(),
              mine != null ? mine.worldName() : null,
              mine != null ? mine.posX() : 0,
              mine != null ? mine.posY() : 0,
              mine != null ? mine.posZ() : 0,
              mine != null ? mine.posYaw() : 0f,
              mine != null ? mine.posPitch() : 0f,
              mine != null && mine.onceFlag(),
              use != null ? use.worldName() : null,
              use != null ? use.posX() : 0,
              use != null ? use.posY() : 0,
              use != null ? use.posZ() : 0,
              use != null ? use.posYaw() : 0f,
              use != null ? use.posPitch() : 0f,
              use != null && use.onceFlag(),
              null,
              0,
              0,
              0,
              null,
              0,
              0,
              0,
              false,
              place != null ? place.worldName() : null,
              place != null ? place.posX() : 0,
              place != null ? place.posY() : 0,
              place != null ? place.posZ() : 0,
              place != null ? place.posYaw() : 0f,
              place != null ? place.posPitch() : 0f,
              place != null && place.onceFlag(),
              attack != null ? attack.worldName() : null,
              attack != null ? attack.posX() : 0,
              attack != null ? attack.posY() : 0,
              attack != null ? attack.posZ() : 0,
              attack != null ? attack.posYaw() : 0f,
              attack != null ? attack.posPitch() : 0f,
              attack != null && attack.onceFlag(),
              follow != null ? follow.extraStr() : null,
              roam != null ? roam.worldName() : null,
              roam != null ? roam.posX() : 0,
              roam != null ? roam.posY() : 0,
              roam != null ? roam.posZ() : 0,
              roam != null ? roam.posYaw() : 0));
    }
    return result;
  }

  private static void applyInventory(PlayerInventory inv, Map<String, String> slots) {
    for (Map.Entry<String, String> entry : slots.entrySet()) {
      try {
        int slot = Integer.parseInt(entry.getKey());
        ItemStack item = ItemStack.deserializeBytes(Base64.getDecoder().decode(entry.getValue()));
        if (slot <= 35) inv.setItem(slot, item);
        else if (slot == 36) inv.setBoots(item);
        else if (slot == 37) inv.setLeggings(item);
        else if (slot == 38) inv.setChestplate(item);
        else if (slot == 39) inv.setHelmet(item);
        else if (slot == 40) inv.setItemInOffHand(item);
      } catch (Exception e) {
        FppLogger.warn("Failed to restore item in slot " + entry.getKey() + ": " + e.getMessage());
      }
    }
  }

  public void purgeOrphanedBodiesAndRestore(FakePlayerManager manager) {

    Bukkit.getScheduler()
        .runTaskLater(
            plugin,
            () -> {
              purgeOrphanedBodies();

              Bukkit.getScheduler().runTaskLater(plugin, () -> restore(manager), 5L);
            },
            40L);
  }

  private void purgeOrphanedBodies() {
    NamespacedKey key = FakePlayerManager.FAKE_PLAYER_KEY;
    if (key == null) return;

    int removed = 0;
    for (org.bukkit.World world : org.bukkit.Bukkit.getWorlds()) {
      for (org.bukkit.entity.Entity entity : world.getEntities()) {
        if (!entity.getPersistentDataContainer().has(key, PersistentDataType.STRING)) continue;
        String val = entity.getPersistentDataContainer().get(key, PersistentDataType.STRING);

        if (val != null) {
          entity.remove();
          removed++;
          Config.debug("Purged orphaned entity: " + val);
        }
      }
    }
    if (removed > 0) {
      FppLogger.info("Purged " + removed + " orphaned bot entity/entities from previous session.");
    }
  }

  private static double toDouble(Object o) {
    if (o instanceof Number n) return n.doubleValue();
    return 0.0;
  }

  private static void deleteFile(File f) {
    if (f.exists() && !f.delete()) {
      FppLogger.warn("BotPersistence: could not delete " + f.getName());
    }
  }

  private record SavedBot(
      String name,
      UUID uuid,
      String displayName,
      String spawnedBy,
      UUID spawnedByUuid,
      String worldName,
      double x,
      double y,
      double z,
      float yaw,
      float pitch,
      String luckpermsGroup,
      BotType botType,
      boolean chatEnabled,
      String chatTier,
      String aiPersonality,
      boolean headAiEnabled,
      boolean pickUpItemsEnabled,
      boolean pickUpXpEnabled,
      int xpTotal,
      int xpLevel,
      float xpProgress,
      boolean frozen,
      boolean navParkour,
      boolean navBreakBlocks,
      boolean navPlaceBlocks,
      boolean navSprintJump,
      boolean swimAiEnabled,
      int ping,
      String rightClickCommand,
      boolean pveEnabled,
      double pveRange,
      String pvePriority,
      String pveMobType,
      String skinTexture,
      String skinSignature) {}

  private record TaskEntry(
      String rightClickCommand,
      String patrolRoute,
      boolean patrolRandom,
      String mineWorld,
      double mineX,
      double mineY,
      double mineZ,
      float mineYaw,
      float minePitch,
      boolean mineOnce,
      String useWorld,
      double useX,
      double useY,
      double useZ,
      float useYaw,
      float usePitch,
      boolean useOnce,
      String areaPos1World,
      double areaPos1X,
      double areaPos1Y,
      double areaPos1Z,
      String areaPos2World,
      double areaPos2X,
      double areaPos2Y,
      double areaPos2Z,
      boolean areaActive,
      String placeWorld,
      double placeX,
      double placeY,
      double placeZ,
      float placeYaw,
      float placePitch,
      boolean placeOnce,
      String attackWorld,
      double attackX,
      double attackY,
      double attackZ,
      float attackYaw,
      float attackPitch,
      boolean attackOnce,
      String followTargetUuid,
      String roamWorld,
      double roamX,
      double roamY,
      double roamZ,
      double roamRadius) {}

  private record XpEntry(int totalExperience, int level, float progress) {}
}
