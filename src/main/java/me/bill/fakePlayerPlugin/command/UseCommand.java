package me.bill.fakePlayerPlugin.command;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import me.bill.fakePlayerPlugin.FakePlayerPlugin;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayer;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayerManager;
import me.bill.fakePlayerPlugin.fakeplayer.NmsPlayerSpawner;
import me.bill.fakePlayerPlugin.fakeplayer.PathfindingService;
import me.bill.fakePlayerPlugin.lang.Lang;
import me.bill.fakePlayerPlugin.permission.Perm;
import me.bill.fakePlayerPlugin.util.FppScheduler;
import me.bill.fakePlayerPlugin.api.event.FppBotInteractEvent;
import me.bill.fakePlayerPlugin.api.impl.FppApiImpl;
import me.bill.fakePlayerPlugin.api.impl.FppBotImpl;
import org.bukkit.Bukkit;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

public final class UseCommand implements FppCommand {

  private static final double USE_ACTION_ARRIVAL_DISTANCE = 0.35;
  private static final int INSTANT_USE_COOLDOWN = 4;

  private final FakePlayerPlugin plugin;
  private final FakePlayerManager manager;
  private final PathfindingService pathfinding;

  private final Map<UUID, Integer> useTasks = new ConcurrentHashMap<>();
  private final Map<UUID, Location> activeUseLocations = new ConcurrentHashMap<>();
  private final Map<UUID, Boolean> activeUseOnceFlags = new ConcurrentHashMap<>();

  public UseCommand(
      FakePlayerPlugin plugin, FakePlayerManager manager, PathfindingService pathfinding) {
    this.plugin = plugin;
    this.manager = manager;
    this.pathfinding = pathfinding;
  }

  @Override
  public String getName() {
    return "use";
  }

  @Override
  public String getUsage() {
    return "<bot> [--once|--stop]  |  --stop";
  }

  @Override
  public String getDescription() {
    return "Walk a bot to your position then right-click what it's looking at.";
  }

  @Override
  public String getPermission() {
    return Perm.USE_CMD;
  }

  @Override
  public boolean canUse(CommandSender sender) {
    return Perm.has(sender, Perm.USE_CMD);
  }

  @Override
  public boolean execute(CommandSender sender, String[] args) {
    if (args.length == 0) {
      sender.sendMessage(Lang.get("use-usage"));
      return true;
    }

    if (args[0].equalsIgnoreCase("stop") || args[0].equalsIgnoreCase("--stop")) {
      if (args.length == 1) {
        stopAll();
        sender.sendMessage(Lang.get("use-stopped-all"));
        return true;
      }
    }

    String botName = args[0];
    FakePlayer fp = manager.getByName(botName);
    if (fp == null) {
      sender.sendMessage(Lang.get("use-not-found", "name", botName));
      return true;
    }

    Player bot = fp.getPlayer();
    if (bot == null || !bot.isOnline()) {
      sender.sendMessage(Lang.get("use-bot-offline", "name", fp.getDisplayName()));
      return true;
    }

    if (args.length >= 2) {
      String action = args[1].toLowerCase();
      if (action.equals("stop") || action.equals("--stop")) {
        cancelAll(fp.getUuid());
        sender.sendMessage(Lang.get("use-stopped", "name", fp.getDisplayName()));
        return true;
      }
    }

    boolean once =
        args.length >= 2
            && (args[1].equalsIgnoreCase("once") || args[1].equalsIgnoreCase("--once"));

    Location dest =
        (sender instanceof Player sp) ? sp.getLocation().clone() : bot.getLocation().clone();

    cancelAll(fp.getUuid());

    double xzDist = PathfindingService.xzDist(bot.getLocation(), dest);
    if (xzDist <= USE_ACTION_ARRIVAL_DISTANCE) {
      lockAndStartUsing(fp, once, dest);
      sender.sendMessage(
          once
              ? Lang.get("use-started-once", "name", fp.getDisplayName())
              : Lang.get("use-started", "name", fp.getDisplayName()));
    } else {
      startNavigation(fp, once, dest);
      sender.sendMessage(Lang.get("use-walking", "name", fp.getDisplayName()));
    }
    return true;
  }

  @Override
  public List<String> tabComplete(CommandSender sender, String[] args) {
    if (!canUse(sender)) return List.of();

    if (args.length == 1) {
      String prefix = args[0].toLowerCase();
      List<String> out = new ArrayList<>();
      if ("--stop".startsWith(prefix)) out.add("--stop");
      if ("stop".startsWith(prefix)) out.add("stop");
      for (FakePlayer fp : manager.getActivePlayers())
        if (fp.getName().toLowerCase().startsWith(prefix)) out.add(fp.getName());
      return out;
    }

    if (args.length == 2
        && !args[0].equalsIgnoreCase("stop")
        && !args[0].equalsIgnoreCase("--stop")) {
      String prefix = args[1].toLowerCase();
      List<String> out = new ArrayList<>();
      if ("--once".startsWith(prefix)) out.add("--once");
      if ("--stop".startsWith(prefix)) out.add("--stop");
      if ("once".startsWith(prefix)) out.add("once");
      if ("stop".startsWith(prefix)) out.add("stop");
      return out;
    }

    return List.of();
  }

  // ── Navigation ──

  private void startNavigation(FakePlayer fp, boolean once, Location dest) {
    pathfinding.navigate(
        fp,
        new PathfindingService.NavigationRequest(
            PathfindingService.Owner.USE,
            () -> dest,
            USE_ACTION_ARRIVAL_DISTANCE,
            0.0,
            Integer.MAX_VALUE,
            () -> lockAndStartUsing(fp, once, dest),
            null,
            null));
  }

  // ── Lock + Use Loop ──

  private void lockAndStartUsing(FakePlayer fp, boolean once, Location lockLoc) {
    FppApiImpl.fireTaskEvent(fp, "use", me.bill.fakePlayerPlugin.api.event.FppBotTaskEvent.Action.START);
    UUID uuid = fp.getUuid();
    Player bot = fp.getPlayer();
    if (bot == null) return;

    bot.setRotation(lockLoc.getYaw(), lockLoc.getPitch());
    NmsPlayerSpawner.setHeadYaw(bot, lockLoc.getYaw());
    NmsPlayerSpawner.setMovementForward(bot, 0f);
    bot.setSprinting(false);

    Location actualLoc = bot.getLocation().clone();
    actualLoc.setYaw(lockLoc.getYaw());
    actualLoc.setPitch(lockLoc.getPitch());
    manager.lockForAction(uuid, actualLoc);

    activeUseLocations.put(uuid, actualLoc.clone());
    activeUseOnceFlags.put(uuid, once);

    final int[] cooldown = {0};

    int taskId =
        FppScheduler.runSyncRepeatingWithId(
            plugin,
            () -> {
              Player b = fp.getPlayer();
              if (b == null || !b.isOnline()) {
                stopUsing(uuid);
                return;
              }

              ServerPlayer nms = ((CraftPlayer) b).getHandle();
              nms.resetLastActionTime();

              if (plugin.getInventoryCommand().isInventoryOpen(uuid)) {
                if (nms.isUsingItem()) {
                  ((CraftPlayer) b).getHandle().releaseUsingItem();
                }
                return;
              }

              if (nms.isUsingItem()) {
                if (once) stopUsing(uuid);
                return;
              }

              if (cooldown[0] > 0) {
                cooldown[0]--;
                return;
              }

              HitResult hit = rayTrace(nms);

              boolean acted = false;
              boolean startedHolding = false;

              for (InteractionHand hand : InteractionHand.values()) {
                if (acted) break;

                if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
                  BlockHitResult blockHit = (BlockHitResult) hit;
                  var pos = blockHit.getBlockPos();
                  Direction side = blockHit.getDirection();

                  if (pos.getY() < nms.level().getMaxY() - (side == Direction.UP ? 1 : 0)
                      && nms.level().mayInteract(nms, pos)) {
                    Object result = NmsPlayerSpawner.useItemOn(nms, hand, blockHit);
                    if (NmsPlayerSpawner.consumesAction(result)) {
                      nms.swing(hand);
                      acted = true;
                      break;
                    }
                  }

                } else if (hit != null && hit.getType() == HitResult.Type.ENTITY) {
                  EntityHitResult entityHit = (EntityHitResult) hit;
                  var entity = entityHit.getEntity();

                  boolean handWasEmpty = nms.getItemInHand(hand).isEmpty();
                  boolean itemFrameEmpty =
                      (entity instanceof ItemFrame ife) && ife.getItem().isEmpty();

                  var bukkitEntity = entity.getBukkitEntity();
                  var equipSlot =
                      hand == InteractionHand.MAIN_HAND
                          ? org.bukkit.inventory.EquipmentSlot.HAND
                          : org.bukkit.inventory.EquipmentSlot.OFF_HAND;
                  var interactEvent =
                      new FppBotInteractEvent(new FppBotImpl(fp), bukkitEntity, equipSlot);
                  Bukkit.getPluginManager().callEvent(interactEvent);
                  if (interactEvent.isCancelled()) {
                    acted = true;
                    break;
                  }

                  if (NmsPlayerSpawner.interactOnEntity(nms, entity, hand)
                      && !(handWasEmpty && itemFrameEmpty)) {
                    nms.swing(hand);
                    acted = true;
                    break;
                  }
                }

                Object useResult = NmsPlayerSpawner.useItem(nms, hand);
                if (NmsPlayerSpawner.consumesAction(useResult)) {
                  if (nms.isUsingItem()) {
                    startedHolding = true;
                  }
                  acted = true;
                  break;
                }
              }

              if (acted) {
                if (startedHolding) {
                  // holding-use item (food, shield, bow, etc.) — stays active
                } else {
                  cooldown[0] = INSTANT_USE_COOLDOWN;
                }
              }

              if (once && acted) stopUsing(uuid);
            },
            0L,
            1L);

    useTasks.put(uuid, taskId);
  }

  // ── Stop / Cancel ──

  private void cancelAll(UUID botUuid) {
    pathfinding.cancel(botUuid);
    stopUsing(botUuid);
    FakePlayer fp = manager.getByUuid(botUuid);
    if (fp != null) {
      Player bot = fp.getPlayer();
      if (bot != null && bot.isOnline()) {
        NmsPlayerSpawner.setMovementForward(bot, 0f);
        NmsPlayerSpawner.setJumping(bot, false);
        bot.setSprinting(false);
      }
    }
  }

  public void stopUsing(UUID botUuid) {
    FakePlayer fp = manager.getByUuid(botUuid);
    if (fp != null) {
      FppApiImpl.fireTaskEvent(fp, "use", me.bill.fakePlayerPlugin.api.event.FppBotTaskEvent.Action.STOP);
    }
    Integer taskId = useTasks.remove(botUuid);
    if (taskId != null) FppScheduler.cancelTask(taskId);
    manager.unlockAction(botUuid);
    activeUseLocations.remove(botUuid);
    activeUseOnceFlags.remove(botUuid);
    if (fp != null) {
      Player bot = fp.getPlayer();
      if (bot != null && bot.isOnline()) ((CraftPlayer) bot).getHandle().releaseUsingItem();
    }
  }

  public void stopAll() {
    pathfinding.cancelAll(PathfindingService.Owner.USE);
    new HashSet<>(useTasks.keySet()).forEach(this::cancelAll);
  }

  // ── Public API ──

  public boolean isNavigating(UUID botUuid) {
    return pathfinding.isNavigating(botUuid);
  }

  public boolean isUsing(UUID botUuid) {
    return useTasks.containsKey(botUuid);
  }

  @org.jetbrains.annotations.Nullable
  public Location getActiveUseLocation(UUID botUuid) {
    return activeUseLocations.get(botUuid);
  }

  public boolean isActiveUseOnce(UUID botUuid) {
    Boolean v = activeUseOnceFlags.get(botUuid);
    return v != null && v;
  }

  public void resumeUsing(FakePlayer fp) {
    UUID uuid = fp.getUuid();
    Location useLoc = getActiveUseLocation(uuid);
    boolean once = isActiveUseOnce(uuid);
    if (useLoc != null) {
      resumeUsing(fp, once, useLoc);
    }
  }

  public void resumeUsing(FakePlayer fp, boolean once, Location loc) {
    if (fp == null || loc == null) return;
    Player bot = fp.getPlayer();
    if (bot == null || !bot.isOnline()) return;
    cancelAll(fp.getUuid());
    if (PathfindingService.xzDist(bot.getLocation(), loc) <= USE_ACTION_ARRIVAL_DISTANCE) {
      lockAndStartUsing(fp, once, loc);
    } else {
      startNavigation(fp, once, loc);
    }
  }

  // ── Ray Trace ──

  @SuppressWarnings("resource")
  private static HitResult rayTrace(ServerPlayer player) {
    double reach = player.gameMode.isCreative() ? 5.0 : 4.5;
    Vec3 eyePos = player.getEyePosition(1.0f);
    Vec3 viewVec = player.getViewVector(1.0f);
    Vec3 endPos = eyePos.add(viewVec.x * reach, viewVec.y * reach, viewVec.z * reach);

    BlockHitResult blockHit;
    try {
      blockHit =
          player
              .level()
              .clip(
                  new ClipContext(
                      eyePos, endPos, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
    } catch (Exception e) {
      return null;
    }

    double maxSqDist = reach * reach;
    if (blockHit.getType() != HitResult.Type.MISS)
      maxSqDist = blockHit.getLocation().distanceToSqr(eyePos);

    AABB searchBox = player.getBoundingBox().expandTowards(viewVec.scale(reach)).inflate(1.0);

    EntityHitResult entityHit = null;
    double entityDistSq = maxSqDist;

    for (var entity :
        player.level().getEntities(player, searchBox, e -> !e.isSpectator() && e.isPickable())) {
      AABB entityBox = entity.getBoundingBox().inflate(entity.getPickRadius());
      var hitOpt = entityBox.clip(eyePos, endPos);
      if (entityBox.contains(eyePos)) {
        if (entityDistSq >= 0) {
          entityHit = new EntityHitResult(entity, hitOpt.orElse(eyePos));
          entityDistSq = 0;
        }
      } else if (hitOpt.isPresent()) {
        double d = eyePos.distanceToSqr(hitOpt.get());
        if (d < entityDistSq || entityDistSq == 0) {
          entityHit = new EntityHitResult(entity, hitOpt.get());
          entityDistSq = d;
        }
      }
    }

    if (entityHit != null) return (HitResult) entityHit;
    return (HitResult) blockHit;
  }
}