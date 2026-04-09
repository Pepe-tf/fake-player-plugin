package me.bill.fakePlayerPlugin.command;

import me.bill.fakePlayerPlugin.FakePlayerPlugin;
import me.bill.fakePlayerPlugin.config.Config;
import me.bill.fakePlayerPlugin.fakeplayer.BotPathfinder;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayer;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayerManager;
import me.bill.fakePlayerPlugin.fakeplayer.NmsPlayerSpawner;
import me.bill.fakePlayerPlugin.lang.Lang;
import me.bill.fakePlayerPlugin.permission.Perm;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Navigates a bot to an online player using an A* grid pathfinder,
 * OR patrols a bot through a user-defined list of waypoints in a loop.
 *
 * <h3>Player-follow mode</h3>
 * {@code /fpp move <bot> <player>} - navigates continuously toward the player.
 * Recalculates path when target moves &gt;{@value #RECALC_DIST} blocks or every
 * {@value #RECALC_INTERVAL} ticks.
 *
 * <h3>Waypoint patrol mode</h3>
 * <ol>
 *   <li>Create a named route with {@code /fpp wp add <route>} (run multiple times
 *       to build a list of positions).</li>
 *   <li>{@code /fpp move <bot> --wp <route>} - start the patrol loop (bot visits
 *       position 1 → 2 → … → N → 1 → …, forever).</li>
 *   <li>{@code /fpp move <bot> --stop} - stop navigation.</li>
 * </ol>
 *
 * <h3>Navigation loop (1-tick interval)</h3>
 * <ol>
 *   <li>Handles WALK/ASCEND/DESCEND normally (face + forward + conditional jump).</li>
 *   <li>Handles PARKOUR: sprint + request jump when 1–3.5 blocks from landing.</li>
 *   <li>Handles BREAK: stop, face block, wait {@value #BREAK_TICKS} ticks, break it.</li>
 *   <li>Handles PLACE: stop, wait {@value #PLACE_TICKS} ticks, place bridge block.</li>
 *   <li>Stuck detection ({@value #STUCK_TICKS} ticks) forces jump + recalc.
 *       Suppressed while breaking or placing.</li>
 *   <li>Falls back to direct face-and-walk when no path is found.</li>
 * </ol>
 */
public final class MoveCommand implements FppCommand {

    // ── Tuning constants ──────────────────────────────────────────────────────

    private static final double ARRIVAL_DIST        = 2.0;
    /** Radius within which the bot considers itself "at" a patrol waypoint. */
    private static final double PATROL_ARRIVE_DIST  = 1.5;
    private static final double SPRINT_DIST         = 8.0;
    private static final double WP_ARRIVE_XZ        = 0.65;
    private static final double RECALC_DIST         = 3.5;
    private static final int    RECALC_INTERVAL     = 60;
    private static final int    STUCK_TICKS         = 8;
    private static final double STUCK_THRESHOLD     = 0.04;

    /**
     * Ticks a bot spends "mining" a blocking block before it disappears.
     * This is a fixed bot-behaviour wait (not physics-accurate hardness),
     * chosen so that navigation feels snappy (~0.75 s) without being jarring.
     */
    private static final int BREAK_TICKS = 15;

    /**
     * Ticks a bot waits before placing a bridge block.
     * Short pause to animate the "look-down-and-place" gesture.
     */
    private static final int PLACE_TICKS = 5;

    // ── Fields ────────────────────────────────────────────────────────────────

    private final FakePlayerPlugin  plugin;
    private final FakePlayerManager manager;
    private final Map<UUID, BukkitTask> navTasks     = new ConcurrentHashMap<>();
    /**
     * Per-bot randomized waypoint order for `--random` patrols.
     * Maps botUuid → shuffled list of waypoint indices (0..N-1).
     * Each bot gets its own unique random sequence at patrol start.
     */
    private final Map<UUID, List<Integer>> randomWpOrder = new ConcurrentHashMap<>();
    /**
     * Route name currently being patrolled per bot (null when in player-follow or idle).
     * Used by BotPersistence to save patrol state across restarts.
     */
    private final Map<UUID, String>  activeRouteNames  = new ConcurrentHashMap<>();
    /** Whether the active patrol for each bot is in --random order. */
    private final Map<UUID, Boolean> activeRandomFlags = new ConcurrentHashMap<>();
    /**
     * Global named waypoint route registry.  Injected via {@link #setWaypointStore(WaypointStore)}.
     * Used by the {@code --wp <name>} flag.
     */
    private WaypointStore waypointStore;

    public MoveCommand(FakePlayerPlugin plugin, FakePlayerManager manager) {
        this.plugin  = plugin;
        this.manager = manager;
    }

    /** Injects the global waypoint store so {@code --wp <name>} can look up destinations. */
    public void setWaypointStore(WaypointStore store) {
        this.waypointStore = store;
    }

    // ── FppCommand ────────────────────────────────────────────────────────────

    @Override public String getName()        { return "move"; }
    @Override public String getUsage()       { return "<bot|all> <player>  |  <bot|all> --wp <route> [--random]  |  <bot|all> --stop"; }
    @Override public String getDescription() { return "Navigate a bot (or all bots) to a player or patrol a named waypoint route in a loop."; }
    @Override public String getPermission()  { return Perm.MOVE; }
    @Override public boolean canUse(CommandSender sender) { return Perm.has(sender, Perm.MOVE); }

    // ── execute ───────────────────────────────────────────────────────────────

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Lang.get("move-usage"));
            return true;
        }

        // ── "all" target: apply command to every active bot ───────────────────
        if (args[0].equalsIgnoreCase("all")) {
            return executeAll(sender, args);
        }

        FakePlayer fp = manager.getByName(args[0]);
        if (fp == null) {
            sender.sendMessage(Lang.get("move-bot-not-found", "name", args[0]));
            return true;
        }

        Player bot = fp.getPlayer();
        if (bot == null || !bot.isOnline()) {
            sender.sendMessage(Lang.get("move-bot-not-online", "name", args[0]));
            return true;
        }

        // ── Flag sub-commands (--wp / --stop) ────────────────────────────────
        if (args.length >= 2 && args[1].startsWith("--")) {
            String flag = args[1].toLowerCase();

            if (flag.equals("--stop")) {
                if (!navTasks.containsKey(bot.getUniqueId())) {
                    sender.sendMessage(Lang.get("move-not-navigating", "name", fp.getDisplayName()));
                } else {
                    cancelNavigation(bot.getUniqueId()); // releases lock, clears jump, stops movement
                    sender.sendMessage(Lang.get("move-stopped", "name", fp.getDisplayName()));
                }
                return true;
            }

            if (flag.equals("--wp")) {
                if (waypointStore == null || args.length < 3) {
                    sender.sendMessage(Lang.get("move-wp-usage"));
                    return true;
                }
                String wpName = args[2];
                boolean randomOrder = args.length >= 4 && args[3].equalsIgnoreCase("--random");

                List<Location> route = waypointStore.getRoute(wpName);
                if (route == null || route.isEmpty()) {
                    sender.sendMessage(Lang.get("move-wp-not-found", "name", wpName));
                    return true;
                }
                // Verify at least the first waypoint is in the same world
                Location first = route.get(0);
                if (first.getWorld() == null || !first.getWorld().equals(bot.getWorld())) {
                    sender.sendMessage(Lang.get("move-wp-different-world",
                            "name", fp.getDisplayName(), "waypoint", wpName));
                    return true;
                }
                cancelNavigation(bot.getUniqueId());
                // Record route state for persistence BEFORE starting the task
                activeRouteNames.put(bot.getUniqueId(), wpName);
                activeRandomFlags.put(bot.getUniqueId(), randomOrder);
                if (randomOrder) {
                    startRandomPatrol(bot, route);
                    sender.sendMessage(Lang.get("move-wp-started-random",
                            "name", fp.getDisplayName(), "waypoint", wpName,
                            "count", String.valueOf(route.size())));
                } else {
                    startPatrol(bot, route);
                    sender.sendMessage(Lang.get("move-wp-started",
                            "name", fp.getDisplayName(), "waypoint", wpName,
                            "count", String.valueOf(route.size())));
                }
                return true;
            }

            // Unknown flag - fall through to usage
            sender.sendMessage(Lang.get("move-usage"));
            return true;
        }

        // ── Player-follow mode: /fpp move <bot> <player> ──────────────────────
        if (args.length >= 2) {
            Player target = Bukkit.getPlayer(args[1]);  // Case-insensitive lookup
            if (target == null) {
                sender.sendMessage(Lang.get("player-not-found", "player", args[1]));
                return true;
            }

            if (!bot.getWorld().equals(target.getWorld())) {
                sender.sendMessage(Lang.get("move-different-world",
                        "name", fp.getDisplayName(), "player", target.getName()));
                return true;
            }

            cancelNavigation(bot.getUniqueId());
            startNavigation(bot, target);

            sender.sendMessage(Lang.get("move-navigating",
                    "name", fp.getDisplayName(), "player", target.getName()));
            return true;
        }

        // ── No second arg - show usage ────────────────────────────────────────
        sender.sendMessage(Lang.get("move-usage"));
        return true;
    }

    // ── "all" bulk handler ────────────────────────────────────────────────────

    /**
     * Handles {@code /fpp move all <flag...>} - applies the navigation command
     * to every currently-active bot.  Bots that are offline or in a different
     * world than the first waypoint / target player are silently skipped and
     * counted in the feedback message.
     */
    private boolean executeAll(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Lang.get("move-usage"));
            return true;
        }

        String flag = args[1].toLowerCase();

        // ── all --stop ────────────────────────────────────────────────────────
        if (flag.equals("--stop")) {
            int stopped = 0;
            for (FakePlayer fp : manager.getActivePlayers()) {
                Player bot = fp.getPlayer();
                if (bot == null) continue;
                if (navTasks.containsKey(bot.getUniqueId())) {
                    cancelNavigation(bot.getUniqueId());
                    stopped++;
                }
            }
            sender.sendMessage(Lang.get("move-all-stopped", "count", String.valueOf(stopped)));
            return true;
        }

        // ── all --wp <route> [--random] ───────────────────────────────────────
        if (flag.equals("--wp")) {
            if (waypointStore == null || args.length < 3) {
                sender.sendMessage(Lang.get("move-wp-usage"));
                return true;
            }
            String wpName = args[2];
            boolean randomOrder = args.length >= 4 && args[3].equalsIgnoreCase("--random");

            List<Location> route = waypointStore.getRoute(wpName);
            if (route == null || route.isEmpty()) {
                sender.sendMessage(Lang.get("move-wp-not-found", "name", wpName));
                return true;
            }

            int started = 0, skipped = 0;
            for (FakePlayer fp : manager.getActivePlayers()) {
                Player bot = fp.getPlayer();
                if (bot == null || !bot.isOnline()) { skipped++; continue; }
                Location first = route.get(0);
                if (first.getWorld() == null || !first.getWorld().equals(bot.getWorld())) { skipped++; continue; }
                cancelNavigation(bot.getUniqueId());
                activeRouteNames.put(bot.getUniqueId(), wpName);
                activeRandomFlags.put(bot.getUniqueId(), randomOrder);
                if (randomOrder) startRandomPatrol(bot, route);
                else             startPatrol(bot, route);
                started++;
            }

            if (randomOrder) {
                sender.sendMessage(Lang.get("move-all-wp-started-random",
                        "waypoint", wpName, "count", String.valueOf(started), "skipped", String.valueOf(skipped)));
            } else {
                sender.sendMessage(Lang.get("move-all-wp-started",
                        "waypoint", wpName, "count", String.valueOf(started), "skipped", String.valueOf(skipped)));
            }
            return true;
        }

        // ── all <player> (player-follow) ──────────────────────────────────────
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Lang.get("player-not-found", "player", args[1]));
            return true;
        }

        int started = 0, skipped = 0;
        for (FakePlayer fp : manager.getActivePlayers()) {
            Player bot = fp.getPlayer();
            if (bot == null || !bot.isOnline()) { skipped++; continue; }
            if (!bot.getWorld().equals(target.getWorld())) { skipped++; continue; }
            cancelNavigation(bot.getUniqueId());
            startNavigation(bot, target);
            started++;
        }
        sender.sendMessage(Lang.get("move-all-navigating",
                "player", target.getName(), "count", String.valueOf(started), "skipped", String.valueOf(skipped)));
        return true;
    }

    // ── Navigation state machine ──────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void startNavigation(@NotNull Player bot, @NotNull Player target) {
        final UUID botUuid    = bot.getUniqueId();
        final UUID targetUuid = target.getUniqueId();

        // Lock bot for navigation - disables head-AI so it doesn't look at players
        manager.lockForNavigation(botUuid);

        // ── Mutable navigation state ──────────────────────────────────────────
        final List<BotPathfinder.Move>[] pathRef  = new List[]{null};
        final int[]      wpIdx     = {0};
        final Location[] lastCalc  = {target.getLocation().clone()};
        final int[]      recalcIn  = {0};
        final int[]      stuckFor  = {0};
        final double[]   prevX     = {bot.getLocation().getX()};
        final double[]   prevZ     = {bot.getLocation().getZ()};

        // ── Action state: BREAK ───────────────────────────────────────────────
        final boolean[]  isBreaking  = {false};
        final int[]      breakLeft   = {0};
        final Location[] breakLoc    = {null};

        // ── Action state: PLACE ───────────────────────────────────────────────
        final boolean[]  isPlacing   = {false};
        final int[]      placeLeft   = {0};

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                Player botPlayer    = Bukkit.getPlayer(botUuid);
                Player targetPlayer = Bukkit.getPlayer(targetUuid);

                if (botPlayer == null || !botPlayer.isOnline()) { cleanup(null); return; }
                if (targetPlayer == null || !targetPlayer.isOnline()) { cleanup(botPlayer); return; }

                Location botLoc    = botPlayer.getLocation();
                Location targetLoc = targetPlayer.getLocation();

                if (!botLoc.getWorld().equals(targetLoc.getWorld())) { cleanup(botPlayer); return; }

                // ── Final arrival ─────────────────────────────────────────────
                double distToTarget = xzDist(botLoc, targetLoc);
                if (distToTarget <= ARRIVAL_DIST) { cleanup(botPlayer); return; }

                // ── Path recalculation ─────────────────────────────────────────
                boolean targetMoved   = lastCalc[0].distanceSquared(targetLoc) > RECALC_DIST * RECALC_DIST;
                boolean pathExhausted = (pathRef[0] == null || wpIdx[0] >= pathRef[0].size());
                boolean heartbeat     = (--recalcIn[0] <= 0);

                if (targetMoved || pathExhausted || heartbeat) {
                    recalcIn[0] = RECALC_INTERVAL;
                    lastCalc[0] = targetLoc.clone();

                    // Reset action states when a new path is computed
                    isBreaking[0] = false; breakLoc[0] = null;
                    isPlacing[0]  = false;

                    BotPathfinder.PathOptions opts = new BotPathfinder.PathOptions(
                            Config.pathfindingParkour(),
                            Config.pathfindingBreakBlocks(),
                            Config.pathfindingPlaceBlocks());

                    List<BotPathfinder.Move> newPath = BotPathfinder.findPathMoves(
                            botLoc.getWorld(),
                            botLoc.getBlockX(), botLoc.getBlockY(), botLoc.getBlockZ(),
                            targetLoc.getBlockX(), targetLoc.getBlockY(), targetLoc.getBlockZ(),
                            opts);

                    pathRef[0] = newPath;
                    wpIdx[0]   = (newPath != null && newPath.size() > 1) ? 1 : 0;
                    stuckFor[0] = 0;
                }

                // ── Fallback: direct walk when no A* path ─────────────────────
                List<BotPathfinder.Move> path = pathRef[0];
                if (path == null || path.isEmpty() || wpIdx[0] >= path.size()) {
                    walkToward(botPlayer, targetLoc, distToTarget);
                    return;
                }

                BotPathfinder.Move wp = path.get(wpIdx[0]);
                double wpCX = wp.x() + 0.5, wpCZ = wp.z() + 0.5;

                // ── BREAK action ──────────────────────────────────────────
                if (wp.type() == BotPathfinder.MoveType.BREAK) {
                    if (!isBreaking[0]) {
                        Location breakTarget = findBreakTarget(botLoc, wp);
                        if (breakTarget != null) {
                            isBreaking[0] = true;
                            breakLeft[0]  = BREAK_TICKS;
                            breakLoc[0]   = breakTarget;
                        } else {
                            // Can't find block to break - recalc
                            recalcIn[0] = 0;
                            return;
                        }
                    }
                    // Suppress stuck detection while mining
                    stuckFor[0] = 0;
                    NmsPlayerSpawner.setMovementForward(botPlayer, 0f);
                    botPlayer.setSprinting(false);
                    // Face the block
                    Location blk = breakLoc[0];
                    if (blk != null) {
                        double bdx = blk.getX() - botLoc.getX();
                        double bdz = blk.getZ() - botLoc.getZ();
                        double bdy = blk.getY() - botLoc.getY();
                        float bYaw   = (float) Math.toDegrees(Math.atan2(-bdx, bdz));
                        float bPitch = (float) -Math.toDegrees(Math.atan2(bdy,
                                Math.sqrt(bdx * bdx + bdz * bdz)));
                        botPlayer.setRotation(bYaw, bPitch);
                        NmsPlayerSpawner.setHeadYaw(botPlayer, bYaw);
                    }
                    if (--breakLeft[0] <= 0) {
                        if (breakLoc[0] != null) {
                            breakLoc[0].getBlock().breakNaturally();
                            breakLoc[0] = null;
                        }
                        isBreaking[0] = false;
                        recalcIn[0]   = 0;  // recalc so new path avoids the now-open space
                        stuckFor[0]   = 0;
                    }
                    prevX[0] = botLoc.getX(); prevZ[0] = botLoc.getZ();
                    return;
                }

                // ── PLACE action ──────────────────────────────────────────────
                if (wp.type() == BotPathfinder.MoveType.PLACE) {
                    if (!isPlacing[0]) {
                        isPlacing[0] = true;
                        placeLeft[0] = PLACE_TICKS;
                    }
                    // Suppress stuck detection while placing
                    stuckFor[0] = 0;
                    NmsPlayerSpawner.setMovementForward(botPlayer, 0f);
                    botPlayer.setSprinting(false);
                    // Look down at gap
                    double pdx = (wp.x() + 0.5) - botLoc.getX();
                    double pdz = (wp.z() + 0.5) - botLoc.getZ();
                    float pYaw = (float) Math.toDegrees(Math.atan2(-pdx, pdz));
                    botPlayer.setRotation(pYaw, 70f);  // look steeply down
                    NmsPlayerSpawner.setHeadYaw(botPlayer, pYaw);

                    if (--placeLeft[0] <= 0) {
                        Block gapBlock = botPlayer.getWorld().getBlockAt(wp.x(), wp.y() - 1, wp.z());
                        if (gapBlock.isPassable()) {
                            Material mat = resolvePlaceMaterial();
                            gapBlock.setType(mat);
                        }
                        isPlacing[0] = false;
                        recalcIn[0]  = 0;  // recalc so new path uses the placed block as floor
                        stuckFor[0]  = 0;
                    }
                    prevX[0] = botLoc.getX(); prevZ[0] = botLoc.getZ();
                    return;
                }

                // ── Waypoint advance ──────────────────────────────────────────
                double wpXZDist = xzDistRaw(botLoc.getX(), botLoc.getZ(), wpCX, wpCZ);
                boolean wpYClose = Math.abs(botLoc.getY() - wp.y()) < 1.2;

                if (wpXZDist < WP_ARRIVE_XZ && wpYClose) {
                    wpIdx[0]++;
                    if (wpIdx[0] >= path.size()) {
                        recalcIn[0] = 0;
                        return;
                    }
                    wp    = path.get(wpIdx[0]);
                    wpCX  = wp.x() + 0.5;
                    wpCZ  = wp.z() + 0.5;
                    wpXZDist = xzDistRaw(botLoc.getX(), botLoc.getZ(), wpCX, wpCZ);
                }

                // ── Face the waypoint ─────────────────────────────────────────
                double dx = wpCX - botLoc.getX(), dz = wpCZ - botLoc.getZ();
                float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
                botPlayer.setRotation(yaw, 0f);
                NmsPlayerSpawner.setHeadYaw(botPlayer, yaw);

                // ── Walk / sprint ─────────────────────────────────────────────
                botPlayer.setSprinting(distToTarget > SPRINT_DIST
                        || wp.type() == BotPathfinder.MoveType.PARKOUR);
                NmsPlayerSpawner.setMovementForward(botPlayer, 1.0f);

                // ── Jump / parkour ────────────────────────────────────────────
                if (!botPlayer.isInWater() && !botPlayer.isInLava()) {
                    if (wp.y() > botLoc.getBlockY()) {
                        // Ascend - step-up jump
                        manager.requestNavJump(botUuid);
                    } else if (wp.type() == BotPathfinder.MoveType.PARKOUR
                            && wpXZDist >= 1.0 && wpXZDist <= 3.5) {
                        // Parkour - jump when within sprint range of the landing block.
                        // Combined with sprint=true above, this carries the bot across gaps.
                        // If the jump is missed, stuck detection recalculates.
                        manager.requestNavJump(botUuid);
                    }
                }

                // ── Stuck detection (suppressed during break/place) ────────────
                double moved = xzDistRaw(botLoc.getX(), botLoc.getZ(), prevX[0], prevZ[0]);
                if (moved < STUCK_THRESHOLD) {
                    if (++stuckFor[0] >= STUCK_TICKS) {
                        if (!botPlayer.isInWater() && !botPlayer.isInLava()) {
                            manager.requestNavJump(botUuid);
                        }
                        recalcIn[0] = 0;
                        stuckFor[0] = 0;
                    }
                } else {
                    stuckFor[0] = 0;
                }

                prevX[0] = botLoc.getX();
                prevZ[0] = botLoc.getZ();
            }

            // ── Inner helpers ─────────────────────────────────────────────────

            private void cleanup(@Nullable Player botPlayer) {
                manager.clearNavJump(botUuid);
                manager.unlockNavigation(botUuid);  // Re-enable head-AI
                activeRouteNames.remove(botUuid);
                activeRandomFlags.remove(botUuid);
                isBreaking[0] = false; breakLoc[0] = null;
                isPlacing[0]  = false;
                if (botPlayer != null) {
                    NmsPlayerSpawner.setMovementForward(botPlayer, 0f);
                    NmsPlayerSpawner.setJumping(botPlayer, false);
                    botPlayer.setSprinting(false);
                }
                cancel();
                navTasks.remove(botUuid);
            }

            private void walkToward(Player botPlayer, Location targetLoc, double dist) {
                Location bl = botPlayer.getLocation();
                float yaw = (float) Math.toDegrees(
                        Math.atan2(-(targetLoc.getX() - bl.getX()), targetLoc.getZ() - bl.getZ()));
                botPlayer.setRotation(yaw, 0f);
                NmsPlayerSpawner.setHeadYaw(botPlayer, yaw);
                botPlayer.setSprinting(dist > SPRINT_DIST);
                NmsPlayerSpawner.setMovementForward(botPlayer, 1.0f);
            }

        }.runTaskTimer(plugin, 0L, 1L);

        navTasks.put(botUuid, task);
    }

    // ── Patrol state machine ──────────────────────────────────────────────────

    /**
     * Starts an indefinite patrol loop through the given waypoints.
     * The bot walks waypoint[0] → waypoint[1] → … → waypoint[N-1] → waypoint[0] → …
     * using A* pathfinding between each stop.  The task runs until
     * {@link #cancelNavigation(UUID)} is called (e.g. by a new command or bot removal).
     *
     * <p>A snapshot copy of {@code waypoints} is taken at start time so
     * adding/removing entries from the source list mid-patrol has no effect
     * on the running patrol - issue a new {@code /fpp move <bot>} to pick
     * up changes.
     */
    @SuppressWarnings("unchecked")
    private void startPatrol(@NotNull Player bot, @NotNull List<Location> waypoints) {
        final UUID botUuid  = bot.getUniqueId();
        final List<Location> wps = new ArrayList<>(waypoints); // immutable snapshot

        // Lock bot for navigation - disables head-AI so it doesn't look at players
        manager.lockForNavigation(botUuid);

        // ── Mutable patrol state ──────────────────────────────────────────────
        final int[]    wpSetIdx = {0};                   // index into wps[]
        final List<BotPathfinder.Move>[] pathRef = new List[]{null};
        final int[]    wpIdx    = {0};
        final int[]    recalcIn = {0};
        final int[]    stuckFor = {0};
        final double[] prevX    = {bot.getLocation().getX()};
        final double[] prevZ    = {bot.getLocation().getZ()};
        final int[]    failedRecalcs = {0};  // Track consecutive failed pathfinds

        // ── Action state: BREAK ───────────────────────────────────────────────
        final boolean[]  isBreaking = {false};
        final int[]      breakLeft  = {0};
        final Location[] breakLoc   = {null};

        // ── Action state: PLACE ───────────────────────────────────────────────
        final boolean[] isPlacing = {false};
        final int[]     placeLeft = {0};

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                Player botPlayer = Bukkit.getPlayer(botUuid);
                if (botPlayer == null || !botPlayer.isOnline()) { cleanup(null); return; }

                Location botLoc = botPlayer.getLocation();
                Location dest   = wps.get(wpSetIdx[0]);

                // Cancel patrol if bot is in different world (e.g., teleported away)
                if (!botLoc.getWorld().equals(dest.getWorld())) {
                    cleanup(botPlayer);
                    return;
                }

                double distToDest = xzDist(botLoc, dest);

                // ── Arrived at current waypoint → advance to next ─────────────
                if (distToDest <= PATROL_ARRIVE_DIST) {
                    wpSetIdx[0] = (wpSetIdx[0] + 1) % wps.size();
                    pathRef[0]    = null;
                    wpIdx[0]      = 0;
                    recalcIn[0]   = 0;
                    stuckFor[0]   = 0;
                    isBreaking[0] = false; breakLoc[0] = null;
                    isPlacing[0]  = false;
                    NmsPlayerSpawner.setMovementForward(botPlayer, 0f);
                    return;
                }

                // ── Path recalculation ────────────────────────────────────────
                boolean pathExhausted = (pathRef[0] == null || wpIdx[0] >= pathRef[0].size());
                boolean heartbeat     = (--recalcIn[0] <= 0);

                if (pathExhausted || heartbeat) {
                    recalcIn[0]   = RECALC_INTERVAL;
                    isBreaking[0] = false; breakLoc[0] = null;
                    isPlacing[0]  = false;

                    BotPathfinder.PathOptions opts = new BotPathfinder.PathOptions(
                            Config.pathfindingParkour(),
                            Config.pathfindingBreakBlocks(),
                            Config.pathfindingPlaceBlocks());

                    List<BotPathfinder.Move> newPath = BotPathfinder.findPathMoves(
                            botLoc.getWorld(),
                            botLoc.getBlockX(), botLoc.getBlockY(), botLoc.getBlockZ(),
                            dest.getBlockX(),   dest.getBlockY(),   dest.getBlockZ(),
                            opts);

                    if (newPath == null) {
                        failedRecalcs[0]++;
                        // Skip unreachable waypoints after 10 failed attempts
                        if (failedRecalcs[0] >= 10) {
                            wpSetIdx[0] = (wpSetIdx[0] + 1) % wps.size();
                            failedRecalcs[0] = 0;
                            pathRef[0]    = null;
                            wpIdx[0]      = 0;
                            stuckFor[0]   = 0;
                            return;
                        }
                    } else {
                        failedRecalcs[0] = 0;
                    }

                    pathRef[0]  = newPath;
                    wpIdx[0]    = (newPath != null && newPath.size() > 1) ? 1 : 0;
                    stuckFor[0] = 0;
                }

                // ── Fallback: direct walk when no A* path ─────────────────────
                List<BotPathfinder.Move> path = pathRef[0];
                if (path == null || path.isEmpty() || wpIdx[0] >= path.size()) {
                    walkToward(botPlayer, dest, distToDest);
                    return;
                }

                BotPathfinder.Move wp = path.get(wpIdx[0]);
                double wpCX = wp.x() + 0.5, wpCZ = wp.z() + 0.5;

                // ── BREAK action ──────────────────────────────────────────────
                if (wp.type() == BotPathfinder.MoveType.BREAK) {
                    if (!isBreaking[0]) {
                        Location blk = findBreakTarget(botLoc, wp);
                        if (blk != null) {
                            isBreaking[0] = true;
                            breakLeft[0]  = BREAK_TICKS;
                            breakLoc[0]   = blk;
                        } else {
                            recalcIn[0] = 0;
                            return;
                        }
                    }
                    stuckFor[0] = 0;
                    NmsPlayerSpawner.setMovementForward(botPlayer, 0f);
                    botPlayer.setSprinting(false);
                    Location blk = breakLoc[0];
                    if (blk != null) {
                        double bdx = blk.getX() - botLoc.getX();
                        double bdz = blk.getZ() - botLoc.getZ();
                        double bdy = blk.getY() - botLoc.getY();
                        float bYaw   = (float) Math.toDegrees(Math.atan2(-bdx, bdz));
                        float bPitch = (float) -Math.toDegrees(Math.atan2(bdy,
                                Math.sqrt(bdx * bdx + bdz * bdz)));
                        botPlayer.setRotation(bYaw, bPitch);
                        NmsPlayerSpawner.setHeadYaw(botPlayer, bYaw);
                    }
                    if (--breakLeft[0] <= 0) {
                        if (breakLoc[0] != null) { breakLoc[0].getBlock().breakNaturally(); breakLoc[0] = null; }
                        isBreaking[0] = false;
                        recalcIn[0]   = 0;
                        stuckFor[0]   = 0;
                    }
                    prevX[0] = botLoc.getX(); prevZ[0] = botLoc.getZ();
                    return;
                }

                // ── PLACE action ──────────────────────────────────────────────
                if (wp.type() == BotPathfinder.MoveType.PLACE) {
                    if (!isPlacing[0]) { isPlacing[0] = true; placeLeft[0] = PLACE_TICKS; }
                    stuckFor[0] = 0;
                    NmsPlayerSpawner.setMovementForward(botPlayer, 0f);
                    botPlayer.setSprinting(false);
                    double pdx = (wp.x() + 0.5) - botLoc.getX();
                    double pdz = (wp.z() + 0.5) - botLoc.getZ();
                    float pYaw = (float) Math.toDegrees(Math.atan2(-pdx, pdz));
                    botPlayer.setRotation(pYaw, 70f);
                    NmsPlayerSpawner.setHeadYaw(botPlayer, pYaw);
                    if (--placeLeft[0] <= 0) {
                        Block gapBlock = botPlayer.getWorld().getBlockAt(wp.x(), wp.y() - 1, wp.z());
                        if (gapBlock.isPassable()) gapBlock.setType(resolvePlaceMaterial());
                        isPlacing[0] = false;
                        recalcIn[0]  = 0;
                        stuckFor[0]  = 0;
                    }
                    prevX[0] = botLoc.getX(); prevZ[0] = botLoc.getZ();
                    return;
                }

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

                // ── Face the waypoint ─────────────────────────────────────────
                double dx = wpCX - botLoc.getX(), dz = wpCZ - botLoc.getZ();
                float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
                botPlayer.setRotation(yaw, 0f);
                NmsPlayerSpawner.setHeadYaw(botPlayer, yaw);

                // ── Walk / sprint ─────────────────────────────────────────────
                botPlayer.setSprinting(distToDest > SPRINT_DIST
                        || wp.type() == BotPathfinder.MoveType.PARKOUR);
                NmsPlayerSpawner.setMovementForward(botPlayer, 1.0f);

                // ── Jump / parkour ────────────────────────────────────────────
                if (!botPlayer.isInWater() && !botPlayer.isInLava()) {
                    if (wp.y() > botLoc.getBlockY()) {
                        manager.requestNavJump(botUuid);
                    } else if (wp.type() == BotPathfinder.MoveType.PARKOUR
                            && wpXZDist >= 1.0 && wpXZDist <= 3.5) {
                        manager.requestNavJump(botUuid);
                    }
                }

                // ── Stuck detection ───────────────────────────────────────────
                double moved = xzDistRaw(botLoc.getX(), botLoc.getZ(), prevX[0], prevZ[0]);
                if (moved < STUCK_THRESHOLD) {
                    if (++stuckFor[0] >= STUCK_TICKS) {
                        if (!botPlayer.isInWater() && !botPlayer.isInLava())
                            manager.requestNavJump(botUuid);
                        recalcIn[0] = 0;
                        stuckFor[0] = 0;
                    }
                } else {
                    stuckFor[0] = 0;
                }

                prevX[0] = botLoc.getX();
                prevZ[0] = botLoc.getZ();
            }

            private void cleanup(@Nullable Player botPlayer) {
                manager.clearNavJump(botUuid);
                manager.unlockNavigation(botUuid);  // Re-enable head-AI
                activeRouteNames.remove(botUuid);
                activeRandomFlags.remove(botUuid);
                isBreaking[0] = false; breakLoc[0] = null;
                isPlacing[0]  = false;
                if (botPlayer != null) {
                    NmsPlayerSpawner.setMovementForward(botPlayer, 0f);
                    NmsPlayerSpawner.setJumping(botPlayer, false);
                    botPlayer.setSprinting(false);
                }
                cancel();
                navTasks.remove(botUuid);
            }

            private void walkToward(Player botPlayer, Location targetLoc, double dist) {
                Location bl = botPlayer.getLocation();
                float yaw = (float) Math.toDegrees(
                        Math.atan2(-(targetLoc.getX() - bl.getX()), targetLoc.getZ() - bl.getZ()));
                botPlayer.setRotation(yaw, 0f);
                NmsPlayerSpawner.setHeadYaw(botPlayer, yaw);
                botPlayer.setSprinting(dist > SPRINT_DIST);
                NmsPlayerSpawner.setMovementForward(botPlayer, 1.0f);
            }

        }.runTaskTimer(plugin, 0L, 1L);

        navTasks.put(botUuid, task);
    }

    // ── Random-patrol state machine ──────────────────────────────────────────

    /**
     * Starts an indefinite patrol loop through the given waypoints in a
     * <em>randomised</em> order that is unique to this bot.
     *
     * <ul>
     *   <li>At start, a shuffled copy of waypoint indices is stored in
     *       {@link #randomWpOrder} under the bot's UUID.</li>
     *   <li>The bot visits waypoints in that shuffled order.  When the full cycle
     *       completes the list is <em>re-shuffled</em> so consecutive cycles
     *       never produce the same sequence.</li>
     *   <li>Every bot that calls this method gets its own independent
     *       {@link Random} instance, so two bots on the same route will walk
     *       different random paths simultaneously.</li>
     * </ul>
     *
     * <p>A snapshot copy of {@code waypoints} is taken at start time - changes
     * to the source list mid-patrol have no effect.
     */
    @SuppressWarnings("unchecked")
    private void startRandomPatrol(@NotNull Player bot, @NotNull List<Location> waypoints) {
        final UUID botUuid = bot.getUniqueId();
        final List<Location> wps = new ArrayList<>(waypoints); // immutable snapshot

        // Build initial shuffled order unique to this bot
        final Random rng = new Random();
        List<Integer> shuffled = new ArrayList<>();
        for (int i = 0; i < wps.size(); i++) shuffled.add(i);
        Collections.shuffle(shuffled, rng);
        randomWpOrder.put(botUuid, shuffled);

        // Lock bot for navigation - disables head-AI so it doesn't look at players
        manager.lockForNavigation(botUuid);

        // ── Mutable patrol state ──────────────────────────────────────────────
        final int[]    cycleIdx     = {0};   // position within the shuffled order list
        final List<BotPathfinder.Move>[] pathRef = new List[]{null};
        final int[]    wpIdx        = {0};
        final int[]    recalcIn     = {0};
        final int[]    stuckFor     = {0};
        final double[] prevX        = {bot.getLocation().getX()};
        final double[] prevZ        = {bot.getLocation().getZ()};
        final int[]    failedRecalcs = {0};

        // ── Action state: BREAK ───────────────────────────────────────────────
        final boolean[]  isBreaking = {false};
        final int[]      breakLeft  = {0};
        final Location[] breakLoc   = {null};

        // ── Action state: PLACE ───────────────────────────────────────────────
        final boolean[] isPlacing = {false};
        final int[]     placeLeft = {0};

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                Player botPlayer = Bukkit.getPlayer(botUuid);
                if (botPlayer == null || !botPlayer.isOnline()) { cleanup(null); return; }

                List<Integer> order = randomWpOrder.get(botUuid);
                if (order == null) { cleanup(botPlayer); return; }

                int actualIdx = order.get(cycleIdx[0]);
                Location dest   = wps.get(actualIdx);
                Location botLoc = botPlayer.getLocation();

                if (!botLoc.getWorld().equals(dest.getWorld())) {
                    cleanup(botPlayer);
                    return;
                }

                double distToDest = xzDist(botLoc, dest);

                // ── Arrived → advance to next random waypoint ─────────────────
                if (distToDest <= PATROL_ARRIVE_DIST) {
                    cycleIdx[0]++;
                    // Cycle complete - re-shuffle for the next round
                    if (cycleIdx[0] >= order.size()) {
                        Collections.shuffle(order, rng);
                        cycleIdx[0] = 0;
                    }
                    pathRef[0]    = null;
                    wpIdx[0]      = 0;
                    recalcIn[0]   = 0;
                    stuckFor[0]   = 0;
                    isBreaking[0] = false; breakLoc[0] = null;
                    isPlacing[0]  = false;
                    NmsPlayerSpawner.setMovementForward(botPlayer, 0f);
                    return;
                }

                // ── Path recalculation ────────────────────────────────────────
                boolean pathExhausted = (pathRef[0] == null || wpIdx[0] >= pathRef[0].size());
                boolean heartbeat     = (--recalcIn[0] <= 0);

                if (pathExhausted || heartbeat) {
                    recalcIn[0]   = RECALC_INTERVAL;
                    isBreaking[0] = false; breakLoc[0] = null;
                    isPlacing[0]  = false;

                    BotPathfinder.PathOptions opts = new BotPathfinder.PathOptions(
                            Config.pathfindingParkour(),
                            Config.pathfindingBreakBlocks(),
                            Config.pathfindingPlaceBlocks());

                    List<BotPathfinder.Move> newPath = BotPathfinder.findPathMoves(
                            botLoc.getWorld(),
                            botLoc.getBlockX(), botLoc.getBlockY(), botLoc.getBlockZ(),
                            dest.getBlockX(),   dest.getBlockY(),   dest.getBlockZ(),
                            opts);

                    if (newPath == null) {
                        failedRecalcs[0]++;
                        if (failedRecalcs[0] >= 10) {
                            // Skip unreachable waypoint - advance to next random stop
                            cycleIdx[0]++;
                            if (cycleIdx[0] >= order.size()) {
                                Collections.shuffle(order, rng);
                                cycleIdx[0] = 0;
                            }
                            failedRecalcs[0] = 0;
                            pathRef[0] = null;
                            wpIdx[0]   = 0;
                            stuckFor[0] = 0;
                            return;
                        }
                    } else {
                        failedRecalcs[0] = 0;
                    }

                    pathRef[0]  = newPath;
                    wpIdx[0]    = (newPath != null && newPath.size() > 1) ? 1 : 0;
                    stuckFor[0] = 0;
                }

                // ── Fallback: direct walk when no A* path ─────────────────────
                List<BotPathfinder.Move> path = pathRef[0];
                if (path == null || path.isEmpty() || wpIdx[0] >= path.size()) {
                    walkToward(botPlayer, dest, distToDest);
                    return;
                }

                BotPathfinder.Move wp = path.get(wpIdx[0]);
                double wpCX = wp.x() + 0.5, wpCZ = wp.z() + 0.5;

                // ── BREAK action ──────────────────────────────────────────────
                if (wp.type() == BotPathfinder.MoveType.BREAK) {
                    if (!isBreaking[0]) {
                        Location blk = findBreakTarget(botLoc, wp);
                        if (blk != null) {
                            isBreaking[0] = true;
                            breakLeft[0]  = BREAK_TICKS;
                            breakLoc[0]   = blk;
                        } else {
                            recalcIn[0] = 0;
                            return;
                        }
                    }
                    stuckFor[0] = 0;
                    NmsPlayerSpawner.setMovementForward(botPlayer, 0f);
                    botPlayer.setSprinting(false);
                    Location blk = breakLoc[0];
                    if (blk != null) {
                        double bdx = blk.getX() - botLoc.getX();
                        double bdz = blk.getZ() - botLoc.getZ();
                        double bdy = blk.getY() - botLoc.getY();
                        float bYaw   = (float) Math.toDegrees(Math.atan2(-bdx, bdz));
                        float bPitch = (float) -Math.toDegrees(Math.atan2(bdy,
                                Math.sqrt(bdx * bdx + bdz * bdz)));
                        botPlayer.setRotation(bYaw, bPitch);
                        NmsPlayerSpawner.setHeadYaw(botPlayer, bYaw);
                    }
                    if (--breakLeft[0] <= 0) {
                        if (breakLoc[0] != null) { breakLoc[0].getBlock().breakNaturally(); breakLoc[0] = null; }
                        isBreaking[0] = false;
                        recalcIn[0]   = 0;
                        stuckFor[0]   = 0;
                    }
                    prevX[0] = botLoc.getX(); prevZ[0] = botLoc.getZ();
                    return;
                }

                // ── PLACE action ──────────────────────────────────────────────
                if (wp.type() == BotPathfinder.MoveType.PLACE) {
                    if (!isPlacing[0]) { isPlacing[0] = true; placeLeft[0] = PLACE_TICKS; }
                    stuckFor[0] = 0;
                    NmsPlayerSpawner.setMovementForward(botPlayer, 0f);
                    botPlayer.setSprinting(false);
                    double pdx = (wp.x() + 0.5) - botLoc.getX();
                    double pdz = (wp.z() + 0.5) - botLoc.getZ();
                    float pYaw = (float) Math.toDegrees(Math.atan2(-pdx, pdz));
                    botPlayer.setRotation(pYaw, 70f);
                    NmsPlayerSpawner.setHeadYaw(botPlayer, pYaw);
                    if (--placeLeft[0] <= 0) {
                        Block gapBlock = botPlayer.getWorld().getBlockAt(wp.x(), wp.y() - 1, wp.z());
                        if (gapBlock.isPassable()) gapBlock.setType(resolvePlaceMaterial());
                        isPlacing[0] = false;
                        recalcIn[0]  = 0;
                        stuckFor[0]  = 0;
                    }
                    prevX[0] = botLoc.getX(); prevZ[0] = botLoc.getZ();
                    return;
                }

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

                // ── Face the waypoint ─────────────────────────────────────────
                double dx = wpCX - botLoc.getX(), dz = wpCZ - botLoc.getZ();
                float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
                botPlayer.setRotation(yaw, 0f);
                NmsPlayerSpawner.setHeadYaw(botPlayer, yaw);

                // ── Walk / sprint ─────────────────────────────────────────────
                botPlayer.setSprinting(distToDest > SPRINT_DIST
                        || wp.type() == BotPathfinder.MoveType.PARKOUR);
                NmsPlayerSpawner.setMovementForward(botPlayer, 1.0f);

                // ── Jump / parkour ────────────────────────────────────────────
                if (!botPlayer.isInWater() && !botPlayer.isInLava()) {
                    if (wp.y() > botLoc.getBlockY()) {
                        manager.requestNavJump(botUuid);
                    } else if (wp.type() == BotPathfinder.MoveType.PARKOUR
                            && wpXZDist >= 1.0 && wpXZDist <= 3.5) {
                        manager.requestNavJump(botUuid);
                    }
                }

                // ── Stuck detection ───────────────────────────────────────────
                double moved = xzDistRaw(botLoc.getX(), botLoc.getZ(), prevX[0], prevZ[0]);
                if (moved < STUCK_THRESHOLD) {
                    if (++stuckFor[0] >= STUCK_TICKS) {
                        if (!botPlayer.isInWater() && !botPlayer.isInLava())
                            manager.requestNavJump(botUuid);
                        recalcIn[0] = 0;
                        stuckFor[0] = 0;
                    }
                } else {
                    stuckFor[0] = 0;
                }

                prevX[0] = botLoc.getX();
                prevZ[0] = botLoc.getZ();
            }

            private void cleanup(@Nullable Player botPlayer) {
                manager.clearNavJump(botUuid);
                manager.unlockNavigation(botUuid);
                randomWpOrder.remove(botUuid);
                isBreaking[0] = false; breakLoc[0] = null;
                isPlacing[0]  = false;
                if (botPlayer != null) {
                    NmsPlayerSpawner.setMovementForward(botPlayer, 0f);
                    NmsPlayerSpawner.setJumping(botPlayer, false);
                    botPlayer.setSprinting(false);
                }
                cancel();
                navTasks.remove(botUuid);
            }

            private void walkToward(Player botPlayer, Location targetLoc, double dist) {
                Location bl = botPlayer.getLocation();
                float yaw = (float) Math.toDegrees(
                        Math.atan2(-(targetLoc.getX() - bl.getX()), targetLoc.getZ() - bl.getZ()));
                botPlayer.setRotation(yaw, 0f);
                NmsPlayerSpawner.setHeadYaw(botPlayer, yaw);
                botPlayer.setSprinting(dist > SPRINT_DIST);
                NmsPlayerSpawner.setMovementForward(botPlayer, 1.0f);
            }

        }.runTaskTimer(plugin, 0L, 1L);

        navTasks.put(botUuid, task);
    }

    // ── Break / Place helpers ─────────────────────────────────────────────────

    /**
     * Finds the first non-passable block in the direct path from bot to the BREAK waypoint.
     * Checks, in order: destination feet, destination head, source head (for ascend-break).
     */
    @Nullable
    private static Location findBreakTarget(Location botLoc, BotPathfinder.Move wp) {
        org.bukkit.World w = botLoc.getWorld();
        int wx = wp.x(), wy = wp.y(), wz = wp.z();

        // Destination feet
        if (!w.getBlockAt(wx, wy, wz).isPassable())
            return new Location(w, wx + 0.5, wy + 0.5, wz + 0.5);

        // Destination head
        if (!w.getBlockAt(wx, wy + 1, wz).isPassable())
            return new Location(w, wx + 0.5, wy + 1.5, wz + 0.5);

        // Source head (ascend-break: can't step up because y+2 is blocked)
        int by = botLoc.getBlockY();
        int bx = botLoc.getBlockX(), bz = botLoc.getBlockZ();
        if (!w.getBlockAt(bx, by + 2, bz).isPassable())
            return new Location(w, bx + 0.5, by + 2.5, bz + 0.5);

        return null;
    }

    /**
     * Returns the material to use when placing a bridge block.
     * Falls back to DIRT if the configured value is invalid.
     */
    private Material resolvePlaceMaterial() {
        String raw = Config.pathfindingPlaceMaterial();
        Material mat = Material.matchMaterial(raw.toUpperCase());
        if (mat != null && mat.isBlock() && mat.isSolid()) return mat;
        return Material.DIRT;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static double xzDist(Location a, Location b) {
        return xzDistRaw(a.getX(), a.getZ(), b.getX(), b.getZ());
    }

    private static double xzDistRaw(double ax, double az, double bx, double bz) {
        double dx = ax - bx, dz = az - bz;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * Cancels the active navigation task for a bot and fully releases all navigation state:
     * nav-jump, navigation lock (re-enables head-AI), and movement inputs.
     * Safe to call even when no task is running.
     */
    private void cancelNavigation(@NotNull UUID botUuid) {
        BukkitTask t = navTasks.remove(botUuid);
        if (t != null && !t.isCancelled()) t.cancel();
        // Release navigation lock so head-AI resumes and the bot stops moving.
        randomWpOrder.remove(botUuid);
        activeRouteNames.remove(botUuid);
        activeRandomFlags.remove(botUuid);
        manager.clearNavJump(botUuid);
        manager.unlockNavigation(botUuid);
        Player botPlayer = Bukkit.getPlayer(botUuid);
        if (botPlayer != null) {
            NmsPlayerSpawner.setMovementForward(botPlayer, 0f);
            NmsPlayerSpawner.setJumping(botPlayer, false);
            botPlayer.setSprinting(false);
        }
    }

    /** Cancels all active navigation tasks. Called from {@code onDisable}. */
    public void cancelAll() {
        new ArrayList<>(navTasks.keySet()).forEach(this::cancelNavigation);
    }

    /**
     * Cleans up all navigation state for a specific bot.
     * Call this when a bot is despawned/deleted to prevent memory leaks.
     *
     * @param botUuid the bot's UUID
     */
    public void cleanupBot(@NotNull UUID botUuid) {
        cancelNavigation(botUuid);
    }

    // ── Tab-complete ──────────────────────────────────────────────────────────

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            String in = args[0].toLowerCase();
            // "all" bulk target
            if ("all".startsWith(in)) out.add("all");
            for (FakePlayer fp : manager.getActivePlayers())
                if (fp.getName().toLowerCase().startsWith(in)) out.add(fp.getName());
        } else if (args.length == 2) {
            String in = args[1].toLowerCase();
            // Flag options
            for (String flag : List.of("--wp", "--stop"))
                if (flag.startsWith(in)) out.add(flag);
            // Online player names (for player-follow mode) - exclude bots
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (manager.getByName(p.getName()) == null && p.getName().toLowerCase().startsWith(in))
                    out.add(p.getName());
            }
        } else if (args.length == 3 && args[1].equalsIgnoreCase("--wp")) {
            // Suggest known waypoint names
            if (waypointStore != null) {
                String in = args[2].toLowerCase();
                for (String name : waypointStore.getNames())
                    if (name.startsWith(in)) out.add(name);
            }
        } else if (args.length == 4 && args[1].equalsIgnoreCase("--wp")) {
            // Suggest --random flag after the waypoint name
            String in = args[3].toLowerCase();
            if ("--random".startsWith(in)) out.add("--random");
        }
        return out;
    }

    // ── Persistence API (used by BotPersistence to save/restore patrol state) ──

    /**
     * Returns the name of the waypoint route currently being patrolled by the given bot,
     * or {@code null} when the bot is in player-follow mode or not navigating.
     */
    @Nullable
    public String getActiveRouteForBot(@NotNull UUID botUuid) {
        return activeRouteNames.get(botUuid);
    }

    /**
     * Returns {@code true} if the bot's active patrol is in {@code --random} order.
     */
    public boolean isRandomPatrolForBot(@NotNull UUID botUuid) {
        Boolean v = activeRandomFlags.get(botUuid);
        return v != null && v;
    }

    /**
     * Resumes a waypoint patrol for a bot that was restored from persistence.
     * Cancels any existing navigation first, records the route name/random flag,
     * then starts the appropriate patrol loop.
     *
     * @param bot       the bot's Bukkit {@link Player}
     * @param route     snapshot of the waypoint positions (from {@link WaypointStore})
     * @param random    {@code true} to shuffle patrol order
     * @param routeName the route name (stored in {@link #activeRouteNames} for next save)
     */
    public void resumePatrol(@NotNull Player bot, @NotNull List<Location> route,
                             boolean random, @NotNull String routeName) {
        UUID uid = bot.getUniqueId();
        cancelNavigation(uid);
        activeRouteNames.put(uid, routeName);
        activeRandomFlags.put(uid, random);
        if (random) startRandomPatrol(bot, route);
        else        startPatrol(bot, route);
    }
}

