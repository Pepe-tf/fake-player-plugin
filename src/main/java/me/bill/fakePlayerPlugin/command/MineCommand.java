package me.bill.fakePlayerPlugin.command;

import me.bill.fakePlayerPlugin.FakePlayerPlugin;
import me.bill.fakePlayerPlugin.config.Config;
import me.bill.fakePlayerPlugin.fakeplayer.BotPathfinder;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayer;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayerManager;
import me.bill.fakePlayerPlugin.fakeplayer.NmsPlayerSpawner;
import me.bill.fakePlayerPlugin.lang.Lang;
import me.bill.fakePlayerPlugin.permission.Perm;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BlockIterator;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Two-phase mine command:
 *
 * <h3>Phase 1 - Navigation</h3>
 * The bot walks/sprints to the sender's position using A* pathfinding (same engine as
 * {@code MoveCommand}).  During this phase the bot behaves normally - physics, knockback,
 * and pushable are all active.  Head-AI still runs but is overridden every tick by the
 * navigation rotation.
 *
 * <h3>Phase 2 - Mining</h3>
 * Once the bot arrives (XZ distance ≤ {@value #ARRIVAL_DIST}) it is snapped to the exact
 * destination, its position is locked via {@code FakePlayerManager.lockForMining()}, head-AI
 * is suppressed, and the 1-tick mining loop begins.
 *
 * <p>Usage:
 * <ul>
 *   <li>{@code /fpp mine <bot>}      - walk to your spot, mine continuously.</li>
 *   <li>{@code /fpp mine <bot> once} - walk to your spot, mine one block, stop.</li>
 *   <li>{@code /fpp mine <bot> stop} - stop navigating / stop mining.</li>
 *   <li>{@code /fpp mine stop}       - stop all bots.</li>
 * </ul>
 *
 * <p>Permission: {@link Perm#MINE} ({@code fpp.mine}), default {@code op}.
 */
public final class MineCommand implements FppCommand {

    // ── Tuning constants (mirrors MoveCommand) ────────────────────────────────
    /** XZ distance at which the bot is considered to have arrived at the mining spot. */
    private static final double ARRIVAL_DIST    = 1.2;
    private static final double SPRINT_DIST     = 6.0;
    private static final double WP_ARRIVE_XZ    = 0.65;
    private static final int    RECALC_INTERVAL = 60;
    private static final int    STUCK_TICKS     = 8;
    private static final double STUCK_THRESHOLD = 0.04;

    // ── Fields ────────────────────────────────────────────────────────────────

    private final FakePlayerPlugin  plugin;
    private final FakePlayerManager manager;

    /** Phase-1 navigation tasks (walking to the mining spot). */
    private final Map<UUID, BukkitTask> navTasks    = new ConcurrentHashMap<>();
    /** Phase-2 mining task IDs. */
    private final Map<UUID, Integer>    miningTasks  = new ConcurrentHashMap<>();
    /** Per-bot mining state (only populated during phase 2). */
    private final Map<UUID, MiningState> miningStates = new ConcurrentHashMap<>();
    /** Active mining lock locations per bot (populated at lock-and-mine start). */
    private final Map<UUID, Location>   activeMineLocations = new ConcurrentHashMap<>();
    /** Whether the active mine task is once-only per bot. */
    private final Map<UUID, Boolean>    activeMineOnceFlags  = new ConcurrentHashMap<>();

    public MineCommand(FakePlayerPlugin plugin, FakePlayerManager manager) {
        this.plugin  = plugin;
        this.manager = manager;
    }

    // ── FppCommand ────────────────────────────────────────────────────────────

    @Override public String getName()        { return "mine"; }
    @Override public String getUsage()       { return "<bot> [once|stop]  |  stop"; }
    @Override public String getDescription() { return "Walk a bot to your position then mine continuously."; }
    @Override public String getPermission()  { return Perm.MINE; }
    @Override public boolean canUse(CommandSender sender) { return Perm.has(sender, Perm.MINE); }

    // ── execute ───────────────────────────────────────────────────────────────

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Lang.get("mine-usage"));
            return true;
        }

        // /fpp mine stop - stop every bot
        if (args[0].equalsIgnoreCase("stop") && args.length == 1) {
            stopAll();
            sender.sendMessage(Lang.get("mine-stopped-all"));
            return true;
        }

        String botName = args[0];
        FakePlayer fp = manager.getByName(botName);
        if (fp == null) {
            sender.sendMessage(Lang.get("mine-not-found", "name", botName));
            return true;
        }

        Player bot = fp.getPlayer();
        if (bot == null || !bot.isOnline()) {
            sender.sendMessage(Lang.get("mine-bot-offline", "name", fp.getDisplayName()));
            return true;
        }

        // /fpp mine <bot> stop
        if (args.length >= 2 && args[1].equalsIgnoreCase("stop")) {
            cancelAll(fp.getUuid());
            sender.sendMessage(Lang.get("mine-stopped", "name", fp.getDisplayName()));
            return true;
        }

        // /fpp mine <bot> [once]
        boolean once = args.length >= 2 && args[1].equalsIgnoreCase("once");

        // Destination = sender's location (player) or bot's current location (console)
        Location dest = (sender instanceof Player sp)
                ? sp.getLocation().clone()
                : bot.getLocation().clone();

        // Cancel any existing navigation/mining for this bot
        cancelAll(fp.getUuid());

        double xzDist = xzDist(bot.getLocation(), dest);
        if (xzDist <= ARRIVAL_DIST) {
            // Already close enough - skip navigation, lock and mine immediately
            lockAndStartMining(fp, once, dest);
            sender.sendMessage(once
                    ? Lang.get("mine-started-once", "name", fp.getDisplayName())
                    : Lang.get("mine-started",      "name", fp.getDisplayName()));
        } else {
            // Walk/run to destination first
            startNavigation(fp, once, dest);
            sender.sendMessage(Lang.get("mine-walking", "name", fp.getDisplayName()));
        }
        return true;
    }

    // ── Tab-complete ──────────────────────────────────────────────────────────

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (!canUse(sender)) return List.of();

        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            List<String> out = new ArrayList<>();
            if ("stop".startsWith(prefix)) out.add("stop");
            for (FakePlayer fp : manager.getActivePlayers())
                if (fp.getName().toLowerCase().startsWith(prefix)) out.add(fp.getName());
            return out;
        }

        // Second arg only makes sense when the first arg is a bot name, not "stop"
        if (args.length == 2 && !args[0].equalsIgnoreCase("stop")) {
            String prefix = args[1].toLowerCase();
            List<String> out = new ArrayList<>();
            if ("once".startsWith(prefix)) out.add("once");
            if ("stop".startsWith(prefix)) out.add("stop");
            return out;
        }

        return List.of();
    }

    // ── Phase 1: navigation ───────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void startNavigation(FakePlayer fp, boolean once, Location dest) {
        final UUID uuid = fp.getUuid();

        final List<BotPathfinder.Move>[] pathRef = new List[]{null};
        final int[]    wpIdx    = {0};
        final int[]    recalcIn = {0};
        final int[]    stuckFor = {0};
        final double[] prevX    = {fp.getPlayer().getLocation().getX()};
        final double[] prevZ    = {fp.getPlayer().getLocation().getZ()};

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                Player bot = Bukkit.getPlayer(uuid);
                if (bot == null || !bot.isOnline()) {
                    cleanup(null);
                    return;
                }

                Location botLoc = bot.getLocation();
                double dist = xzDist(botLoc, dest);

                // ── Arrived ───────────────────────────────────────────────────
                if (dist <= ARRIVAL_DIST) {
                    cleanup(bot);
                    lockAndStartMining(fp, once, dest);
                    return;
                }

                // ── Path recalculation ────────────────────────────────────────
                boolean exhausted = (pathRef[0] == null || wpIdx[0] >= pathRef[0].size());
                boolean heartbeat = (--recalcIn[0] <= 0);

                if (exhausted || heartbeat) {
                    recalcIn[0] = RECALC_INTERVAL;
                    BotPathfinder.PathOptions opts = new BotPathfinder.PathOptions(
                            Config.pathfindingParkour(),
                            Config.pathfindingBreakBlocks(),
                            Config.pathfindingPlaceBlocks());
                    List<BotPathfinder.Move> newPath = BotPathfinder.findPathMoves(
                            botLoc.getWorld(),
                            botLoc.getBlockX(), botLoc.getBlockY(), botLoc.getBlockZ(),
                            dest.getBlockX(),   dest.getBlockY(),   dest.getBlockZ(),
                            opts);
                    pathRef[0] = newPath;
                    wpIdx[0]   = (newPath != null && newPath.size() > 1) ? 1 : 0;
                    stuckFor[0] = 0;
                }

                // ── Fallback: direct walk when no path ────────────────────────
                List<BotPathfinder.Move> path = pathRef[0];
                if (path == null || path.isEmpty() || wpIdx[0] >= path.size()) {
                    walkToward(bot, dest, dist);
                    return;
                }

                BotPathfinder.Move wp = path.get(wpIdx[0]);
                double wpCX = wp.x() + 0.5, wpCZ = wp.z() + 0.5;

                // ── Waypoint advance ──────────────────────────────────────────
                double wpXZDist = xzDistRaw(botLoc.getX(), botLoc.getZ(), wpCX, wpCZ);
                boolean wpYClose = Math.abs(botLoc.getY() - wp.y()) < 1.2;

                if (wpXZDist < WP_ARRIVE_XZ && wpYClose) {
                    wpIdx[0]++;
                    if (wpIdx[0] >= path.size()) { recalcIn[0] = 0; return; }
                    wp       = path.get(wpIdx[0]);
                    wpCX     = wp.x() + 0.5;
                    wpCZ     = wp.z() + 0.5;
                    wpXZDist = xzDistRaw(botLoc.getX(), botLoc.getZ(), wpCX, wpCZ);
                }

                // ── Face waypoint ─────────────────────────────────────────────
                double dx = wpCX - botLoc.getX(), dz = wpCZ - botLoc.getZ();
                float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
                bot.setRotation(yaw, 0f);
                NmsPlayerSpawner.setHeadYaw(bot, yaw);

                // ── Walk / sprint ─────────────────────────────────────────────
                bot.setSprinting(dist > SPRINT_DIST
                        || wp.type() == BotPathfinder.MoveType.PARKOUR);
                NmsPlayerSpawner.setMovementForward(bot, 1.0f);

                // ── Jump ──────────────────────────────────────────────────────
                if (!bot.isInWater() && !bot.isInLava()) {
                    if (wp.y() > botLoc.getBlockY()) {
                        manager.requestNavJump(uuid);
                    } else if (wp.type() == BotPathfinder.MoveType.PARKOUR
                            && wpXZDist >= 1.0 && wpXZDist <= 3.5) {
                        manager.requestNavJump(uuid);
                    }
                }

                // ── Stuck detection ───────────────────────────────────────────
                double moved = xzDistRaw(botLoc.getX(), botLoc.getZ(), prevX[0], prevZ[0]);
                if (moved < STUCK_THRESHOLD) {
                    if (++stuckFor[0] >= STUCK_TICKS) {
                        if (!bot.isInWater() && !bot.isInLava()) manager.requestNavJump(uuid);
                        recalcIn[0] = 0;
                        stuckFor[0] = 0;
                    }
                } else {
                    stuckFor[0] = 0;
                }
                prevX[0] = botLoc.getX();
                prevZ[0] = botLoc.getZ();
            }

            private void cleanup(Player bot) {
                manager.clearNavJump(uuid);
                if (bot != null) {
                    NmsPlayerSpawner.setMovementForward(bot, 0f);
                    NmsPlayerSpawner.setJumping(bot, false);
                    bot.setSprinting(false);
                }
                cancel();
                navTasks.remove(uuid);
            }

            private void walkToward(Player bot, Location dest, double dist) {
                Location bl = bot.getLocation();
                float yaw = (float) Math.toDegrees(
                        Math.atan2(-(dest.getX() - bl.getX()), dest.getZ() - bl.getZ()));
                bot.setRotation(yaw, 0f);
                NmsPlayerSpawner.setHeadYaw(bot, yaw);
                bot.setSprinting(dist > SPRINT_DIST);
                NmsPlayerSpawner.setMovementForward(bot, 1.0f);
            }

        }.runTaskTimer(plugin, 0L, 1L);

        navTasks.put(uuid, task);
    }

    // ── Phase 2: lock + mine ──────────────────────────────────────────────────

    /**
     * Called once the bot has arrived at the mining destination.
     * Snaps the bot to the exact position, registers the mining lock, and starts the
     * 1-tick mining loop.
     */
    private void lockAndStartMining(FakePlayer fp, boolean once, Location lockLoc) {
        UUID uuid = fp.getUuid();
        Player bot = fp.getPlayer();
        if (bot == null) return;

        // Final snap to exact position (bot is already ≤ ARRIVAL_DIST away, so imperceptible)
        bot.teleport(lockLoc);

        // Lock position + head-AI in FakePlayerManager's tick loop
        manager.lockForMining(uuid, lockLoc);

        // Record active mine state for persistence
        activeMineLocations.put(uuid, lockLoc.clone());
        activeMineOnceFlags.put(uuid, once);

        // Init state
        MiningState state = new MiningState();
        state.once = once;
        miningStates.put(uuid, state);

        // 1-tick mining task
        int taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Player b = fp.getPlayer();
            if (b == null || !b.isOnline()) { stopMining(uuid); return; }
            tickMining(fp, state);
        }, 0L, 1L).getTaskId();

        miningTasks.put(uuid, taskId);
    }

    // ── Stop helpers ──────────────────────────────────────────────────────────

    /**
     * Cancels both the navigation task AND the mining task for the given bot,
     * and releases the position lock.
     */
    private void cancelAll(UUID botUuid) {
        // Cancel phase-1 nav
        BukkitTask t = navTasks.remove(botUuid);
        if (t != null && !t.isCancelled()) t.cancel();
        manager.clearNavJump(botUuid);

        // Stop phase-2 mine
        stopMining(botUuid);

        // Stop movement if bot still exists
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

    /** Stops only the mining phase (task + lock + state cleanup). */
    public void stopMining(UUID botUuid) {
        Integer taskId = miningTasks.remove(botUuid);
        if (taskId != null) Bukkit.getScheduler().cancelTask(taskId);

        manager.unlockMining(botUuid);

        // Clear persistence tracking
        activeMineLocations.remove(botUuid);
        activeMineOnceFlags.remove(botUuid);

        MiningState state = miningStates.remove(botUuid);
        if (state != null && state.currentPos != null) {
            FakePlayer fp = manager.getByUuid(botUuid);
            if (fp != null) {
                Player bot = fp.getPlayer();
                if (bot != null && bot.isOnline()) {
                    ServerPlayer nms = ((CraftPlayer) bot).getHandle();
                    nms.level().destroyBlockProgress(-1, state.currentPos, -1);
                    nms.gameMode.handleBlockBreakAction(
                            state.currentPos,
                            ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK,
                            Direction.DOWN, nms.level().getMaxY(), -1);
                }
            }
        }
    }

    /** Stops all bots (navigation + mining). */
    public void stopAll() {
        new HashSet<>(navTasks.keySet()).forEach(this::cancelAll);
        new HashSet<>(miningTasks.keySet()).forEach(this::cancelAll);
    }

    /** Returns {@code true} if the bot is currently navigating to a mining spot. */
    public boolean isNavigating(UUID botUuid) { return navTasks.containsKey(botUuid); }

    /** Returns {@code true} if the bot is actively mining. */
    public boolean isMining(UUID botUuid)     { return miningTasks.containsKey(botUuid); }

    // ── Persistence API ───────────────────────────────────────────────────────

    /**
     * Returns the active mining lock location for the given bot,
     * or {@code null} when the bot is not mining.
     */
    @org.jetbrains.annotations.Nullable
    public Location getActiveMineLocation(UUID botUuid) {
        return activeMineLocations.get(botUuid);
    }

    /**
     * Returns {@code true} if the bot's active mine task is once-only.
     */
    public boolean isActiveMineOnce(UUID botUuid) {
        Boolean v = activeMineOnceFlags.get(botUuid);
        return v != null && v;
    }

    /**
     * Resumes mining for a bot restored from persistence.
     * If the bot is already close enough to the lock location, starts mining
     * immediately; otherwise navigates there first.
     */
    public void resumeMining(FakePlayer fp, boolean once, Location loc) {
        if (fp == null || loc == null) return;
        Player bot = fp.getPlayer();
        if (bot == null || !bot.isOnline()) return;
        cancelAll(fp.getUuid());
        double xzDist = xzDist(bot.getLocation(), loc);
        if (xzDist <= ARRIVAL_DIST) {
            lockAndStartMining(fp, once, loc);
        } else {
            startNavigation(fp, once, loc);
        }
    }

    // ── Mining tick ───────────────────────────────────────────────────────────

    private void tickMining(FakePlayer fp, MiningState state) {
        Player bot = fp.getPlayer();
        if (bot == null || !bot.isOnline()) { stopMining(fp.getUuid()); return; }

        ServerPlayer nms = ((CraftPlayer) bot).getHandle();

        if (state.freeze > 0) { state.freeze--; return; }

        BlockPos targetPos = getTargetBlock(bot);
        if (targetPos == null) {
            if (state.currentPos != null) abortMining(nms, state);
            return;
        }

        BlockState blockState = nms.level().getBlockState(targetPos);
        if (blockState.isAir()) {
            if (state.currentPos != null && state.currentPos.equals(targetPos)) {
                state.currentPos = null; state.progress = 0;
            }
            return;
        }

        if (nms.blockActionRestricted(nms.level(), targetPos, nms.gameMode.getGameModeForPlayer()))
            return;

        Direction side = Direction.DOWN;

        // Creative - instant break
        if (bot.getGameMode() == GameMode.CREATIVE) {
            nms.gameMode.handleBlockBreakAction(targetPos,
                    ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                    side, nms.level().getMaxY(), -1);
            nms.swing(InteractionHand.MAIN_HAND);
            state.freeze = 5; state.currentPos = null;
            if (state.once) stopMining(fp.getUuid());
            return;
        }

        // Survival - progressive
        if (state.currentPos == null || !state.currentPos.equals(targetPos)) {
            if (state.currentPos != null)
                nms.gameMode.handleBlockBreakAction(state.currentPos,
                        ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK,
                        side, nms.level().getMaxY(), -1);

            nms.gameMode.handleBlockBreakAction(targetPos,
                    ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                    side, nms.level().getMaxY(), -1);

            if (state.progress == 0) blockState.attack(nms.level(), targetPos, nms);

            float speed = blockState.getDestroyProgress(nms, nms.level(), targetPos);
            if (speed >= 1.0F) {
                state.currentPos = null; state.freeze = 5;
                nms.swing(InteractionHand.MAIN_HAND);
                if (state.once) stopMining(fp.getUuid());
                return;
            }
            state.currentPos = targetPos; state.progress = 0;

        } else {
            float speed = blockState.getDestroyProgress(nms, nms.level(), targetPos);
            state.progress += speed;
            if (state.progress >= 1.0F) {
                nms.gameMode.handleBlockBreakAction(targetPos,
                        ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
                        side, nms.level().getMaxY(), -1);
                state.currentPos = null; state.progress = 0; state.freeze = 5;
                nms.swing(InteractionHand.MAIN_HAND);
                if (state.once) stopMining(fp.getUuid());
                return;
            }
            nms.level().destroyBlockProgress(-1, targetPos, (int)(state.progress * 10));
        }

        nms.swing(InteractionHand.MAIN_HAND);
        nms.resetLastActionTime();
    }

    private void abortMining(ServerPlayer nms, MiningState state) {
        if (state.currentPos == null) return;
        nms.level().destroyBlockProgress(-1, state.currentPos, -1);
        nms.gameMode.handleBlockBreakAction(state.currentPos,
                ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK,
                Direction.DOWN, nms.level().getMaxY(), -1);
        state.currentPos = null; state.progress = 0;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Raycast from the bot's eye; returns the first solid non-air block within 5 blocks. */
    private BlockPos getTargetBlock(Player bot) {
        try {
            Location eye = bot.getEyeLocation();
            BlockIterator iter = new BlockIterator(bot.getWorld(), eye.toVector(),
                    eye.getDirection(), 0, 5);
            while (iter.hasNext()) {
                org.bukkit.block.Block b = iter.next();
                if (!b.getType().isAir() && b.getType().isSolid())
                    return new BlockPos(b.getX(), b.getY(), b.getZ());
            }
        } catch (IllegalStateException ignored) {}
        return null;
    }

    private static double xzDist(Location a, Location b) {
        return xzDistRaw(a.getX(), a.getZ(), b.getX(), b.getZ());
    }

    private static double xzDistRaw(double ax, double az, double bx, double bz) {
        double dx = ax - bx, dz = az - bz;
        return Math.sqrt(dx * dx + dz * dz);
    }

    // ── Internal state ────────────────────────────────────────────────────────

    private static class MiningState {
        BlockPos currentPos = null;
        float    progress   = 0;
        int      freeze     = 0;
        boolean  once       = false;
    }
}


