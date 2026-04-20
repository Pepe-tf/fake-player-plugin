package me.bill.fakePlayerPlugin.fakeplayer;

import me.bill.fakePlayerPlugin.config.Config;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Fence;
import org.bukkit.block.data.type.Gate;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.block.data.Waterlogged;

import java.util.*;

/**
 * Server-side A* grid pathfinder for bot navigation.
 * <p>
 * Inspired by Baritone's movement system but adapted for server-side NMS bots.
 * Supports: walk, diagonal, ascend, descend, fall, parkour (1-4 gap sprint jumps),
 * pillar (climb up), swim, break blocks, and place blocks.
 * <p>
 * Handles slabs, stairs, fences, doors, water, lava, trapdoors, and other
 * non-trivial block shapes. Avoids hazards (lava, cactus, fire, magma, sweet berry bush).
 */
public final class BotPathfinder {

    private BotPathfinder() {}

    // ── Cost constants ─────────────────────────────────────────────────────────

    private static final int WALK      = 10;
    private static final int DIAGONAL  = 14;  // √2 ≈ 1.414
    private static final int ASCEND    = 12;  // walk + jump overhead
    private static final int FALL_PER  = 3;   // per block fallen
    private static final int PARKOUR_C = 20;  // jump penalty
    private static final int BREAK_C   = 30;  // break penalty per block
    private static final int PLACE_C   = 20;  // place penalty
    private static final int PILLAR_C  = 18;  // pillar up (jump + place below)
    private static final int SWIM_C    = 14;  // swimming cost per block
    private static final int WATER_PEN = 6;   // walking through shallow water penalty

    // Hazardous blocks the bot should never walk into
    private static final Set<Material> HAZARDS = EnumSet.of(
            Material.LAVA,
            Material.FIRE,
            Material.SOUL_FIRE,
            Material.CACTUS,
            Material.SWEET_BERRY_BUSH,
            Material.MAGMA_BLOCK,
            Material.CAMPFIRE,
            Material.SOUL_CAMPFIRE,
            Material.WITHER_ROSE,
            Material.POWDER_SNOW,
            Material.POINTED_DRIPSTONE
    );

    // Blocks that slow movement significantly
    private static final Set<Material> SLOW_BLOCKS = EnumSet.of(
            Material.SOUL_SAND,
            Material.HONEY_BLOCK,
            Material.COBWEB
    );

    // Cardinal + diagonal direction offsets
    private static final int[][] DIRS = {
        {1, 0, WALK}, {-1, 0, WALK},
        {0, 1, WALK}, {0, -1, WALK},
        {1, 1, DIAGONAL}, {1, -1, DIAGONAL},
        {-1, 1, DIAGONAL}, {-1, -1, DIAGONAL}
    };

    // Cardinal only (for parkour, pillar, etc.)
    private static final int[][] CARDINAL = {
        {1, 0}, {-1, 0}, {0, 1}, {0, -1}
    };

    // ── Public types ───────────────────────────────────────────────────────────

    public record Pos(int x, int y, int z) {}

    public enum MoveType {
        WALK,
        ASCEND,
        DESCEND,
        PARKOUR,
        BREAK,
        PLACE,
        PILLAR,
        SWIM
    }

    public record Move(int x, int y, int z, MoveType type) {
        public Pos toPos() {
            return new Pos(x, y, z);
        }
    }

    public record PathOptions(boolean parkour, boolean breakBlocks, boolean placeBlocks) {
        public static final PathOptions DEFAULT = new PathOptions(false, false, false);

        public boolean anyEnabled() {
            return parkour || breakBlocks || placeBlocks;
        }
    }

    // ── Internal A* node ───────────────────────────────────────────────────────

    private record Node(Pos pos, Node parent, int g, int h, MoveType action) {
        int f() {
            return g + h;
        }
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Find a path from (sx,sy,sz) to (tx,ty,tz) returning a list of Moves with
     * action metadata, or null if no path found.
     */
    public static List<Move> findPathMoves(
            World world, int sx, int sy, int sz, int tx, int ty, int tz, PathOptions opts) {

        int configuredMaxRange = Config.pathfindingMaxRange();
        if (Math.abs(sx - tx) + Math.abs(sy - ty) + Math.abs(sz - tz) > configuredMaxRange * 3) {
            return null;
        }

        Pos start = snap(world, sx, sy, sz);
        Pos goal = snap(world, tx, ty, tz);
        if (start == null || goal == null) return null;
        if (start.equals(goal))
            return List.of(new Move(start.x(), start.y(), start.z(), MoveType.WALK));

        int nodeLimit =
                opts.anyEnabled()
                        ? Config.pathfindingMaxNodesExtended()
                        : Config.pathfindingMaxNodes();

        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingInt(Node::f));
        // Use long keys (packed x/y/z) instead of Pos records to avoid per-entry object allocation.
        Map<Long, Integer> best = new HashMap<>(nodeLimit);

        open.add(new Node(start, null, 0, heuristic(start, goal), MoveType.WALK));
        best.put(posKey(start.x(), start.y(), start.z()), 0);

        int explored = 0;
        while (!open.isEmpty() && explored++ < nodeLimit) {
            Node cur = open.poll();

            long curKey = posKey(cur.pos().x(), cur.pos().y(), cur.pos().z());
            Integer bestG = best.get(curKey);
            if (bestG != null && cur.g() > bestG) continue;

            // Goal proximity check: within 1 block XZ and same Y
            if (Math.abs(cur.pos().x() - goal.x()) <= 1
                    && cur.pos().y() == goal.y()
                    && Math.abs(cur.pos().z() - goal.z()) <= 1) {
                return buildPathMoves(cur);
            }

            for (int[] nb : neighbors(world, cur.pos().x(), cur.pos().y(), cur.pos().z(), opts)) {
                Pos np = new Pos(nb[0], nb[1], nb[2]);
                long npKey = posKey(nb[0], nb[1], nb[2]);
                int newG = cur.g() + nb[3];
                MoveType mt = MOVE_TYPES[nb[4]];

                Integer existing = best.get(npKey);
                if (existing == null || newG < existing) {
                    best.put(npKey, newG);
                    open.add(new Node(np, cur, newG, heuristic(np, goal), mt));
                }
            }
        }

        return null;
    }

    /**
     * Backward-compat: returns a list of Pos (no action metadata).
     */
    public static List<Pos> findPath(
            World world, int sx, int sy, int sz, int tx, int ty, int tz, PathOptions opts) {
        List<Move> moves = findPathMoves(world, sx, sy, sz, tx, ty, tz, opts);
        if (moves == null) return null;
        List<Pos> result = new ArrayList<>(moves.size());
        for (Move m : moves) result.add(m.toPos());
        return result;
    }

    // Cached MoveType values array — avoids allocating a new array on every MoveType.values() call.
    private static final MoveType[] MOVE_TYPES = MoveType.values();

    // ── Position key helper ────────────────────────────────────────────────────
    // Encodes block coordinates as a long for O(1) HashMap lookups without Pos allocation.
    private static final long X_BIAS = 1 << 19;
    private static final long Z_BIAS = 1 << 19;
    private static final long Y_BIAS = 1 << 11;

    /**
     * Encodes an (x, y, z) block coordinate as a single long key for use in HashMap.
     * Avoids Pos record allocation in the A* best-cost map.
     * Supports x/z in [-524288, 524287] and y in [-2048, 2047].
     */
    private static long posKey(int x, int y, int z) {
        return ((long)(x + X_BIAS) << 32) | ((long)(y + Y_BIAS) << 20) | (z + Z_BIAS);
    }

    private static List<int[]> neighbors(World world, int x, int y, int z, PathOptions opts) {
        List<int[]> out = new ArrayList<>(48); // pre-size: 8 dirs × ~3 moves each + swim/pillar

        final int WK = MoveType.WALK.ordinal(),
                AS = MoveType.ASCEND.ordinal(),
                DE = MoveType.DESCEND.ordinal(),
                PK = MoveType.PARKOUR.ordinal(),
                BK = MoveType.BREAK.ordinal(),
                PL = MoveType.PLACE.ordinal(),
                PI = MoveType.PILLAR.ordinal(),
                SW = MoveType.SWIM.ordinal();

        int maxFall = Config.pathfindingMaxFall();

        // ── Standard movement (walk, ascend, descend, diagonal) ──

        for (int[] d : DIRS) {
            int dx = d[0], dz = d[1], base = d[2];
            int nx = x + dx, nz = z + dz;
            boolean isDiag = (dx != 0 && dz != 0);

            // Diagonal corner clearance check
            if (isDiag) {
                if (!canPassThrough(world, x + dx, y, z)
                        || !canPassThrough(world, x + dx, y + 1, z)
                        || !canPassThrough(world, x, y, z + dz)
                        || !canPassThrough(world, x, y + 1, z + dz)) continue;
            }

            boolean feetClear = canPassThrough(world, nx, y, nz);
            boolean headClear = canPassThrough(world, nx, y + 1, nz);
            boolean floorSolid = canStandOn(world, nx, y - 1, nz);

            // ── Flat walk ──
            if (feetClear && headClear && floorSolid) {
                int cost = base;
                // Penalty for walking through water or slow blocks
                if (isWater(world, nx, y, nz)) cost += WATER_PEN;
                else if (isSlowBlock(world, nx, y, nz)) cost += WATER_PEN;
                // Hazard avoidance
                if (!isHazard(world, nx, y, nz) && !isHazard(world, nx, y - 1, nz)) {
                    out.add(new int[] {nx, y, nz, cost, WK});
                }
            }
            // ── Break blocks to walk (cardinal only) ──
            else if (!isDiag && opts.breakBlocks() && floorSolid) {
                int cost = base;
                if (!feetClear && canBreak(world, nx, y, nz)) cost += BREAK_C;
                else if (!feetClear) continue;
                if (!headClear && canBreak(world, nx, y + 1, nz)) cost += BREAK_C;
                else if (!headClear) continue;
                if (cost > base) {
                    out.add(new int[] {nx, y, nz, cost, BK});
                }
            }

            // ── Place blocks to bridge (cardinal only) ──
            if (!isDiag && opts.placeBlocks() && !floorSolid && feetClear && headClear) {
                // Need an adjacent solid block to place against
                if (hasAdjacentSolid(world, nx, y - 1, nz)) {
                    if (!isHazard(world, nx, y, nz)) {
                        out.add(new int[] {nx, y, nz, base + PLACE_C, PL});
                    }
                }
            }

            // ── Ascend (step up 1 block) ──
            boolean srcHeadClear = canPassThrough(world, x, y + 2, z);
            boolean tgtFeetClear = canPassThrough(world, nx, y + 1, nz);
            boolean tgtHeadClear = canPassThrough(world, nx, y + 2, nz);
            boolean tgtFloorSolid = canStandOn(world, nx, y, nz);

            if (srcHeadClear && tgtFeetClear && tgtHeadClear && tgtFloorSolid) {
                if (!isHazard(world, nx, y + 1, nz)) {
                    int cost = base + ASCEND;
                    if (isSlowBlock(world, x, y, z)) cost += 4; // harder to jump from soul sand etc.
                    out.add(new int[] {nx, y + 1, nz, cost, AS});
                }
            }
            // Ascend + break overhead (cardinal only)
            else if (!isDiag && opts.breakBlocks()
                    && tgtFeetClear && tgtFloorSolid
                    && !srcHeadClear && canBreak(world, x, y + 2, z)
                    && tgtHeadClear) {
                out.add(new int[] {nx, y + 1, nz, base + ASCEND + BREAK_C, BK});
            }

            // ── Descend / Fall ──
            if (feetClear && headClear) {
                for (int drop = 1; drop <= maxFall; drop++) {
                    int ny = y - drop;
                    if (!inBounds(world, ny)) break;
                    if (canStandOn(world, nx, ny - 1, nz)
                            && canPassThrough(world, nx, ny, nz)
                            && canPassThrough(world, nx, ny + 1, nz)) {
                        if (!isHazard(world, nx, ny, nz) && !isHazard(world, nx, ny - 1, nz)) {
                            int fallCost = base + drop * FALL_PER;
                            // Extra cost for dangerous falls (fall damage starts at 4+)
                            if (drop >= 4) fallCost += (drop - 3) * 8;
                            out.add(new int[] {nx, ny, nz, fallCost, DE});
                        }
                        break;
                    }
                    if (!canPassThrough(world, nx, ny, nz)) break;
                }
            }

            // ── Swim (water navigation) ──
            if (isWater(world, nx, y, nz) && isWater(world, nx, y + 1, nz)) {
                // Horizontal swim
                out.add(new int[] {nx, y, nz, SWIM_C, SW});
                // Swim up
                if (canPassThrough(world, nx, y + 2, nz) || isWater(world, nx, y + 2, nz)) {
                    out.add(new int[] {nx, y + 1, nz, SWIM_C + 2, SW});
                }
                // Swim down
                if (isWater(world, nx, y - 1, nz) || canPassThrough(world, nx, y - 1, nz)) {
                    out.add(new int[] {nx, y - 1, nz, SWIM_C - 1, SW});
                }
            }

            // ── Parkour jumps (cardinal only) ──
            if (!isDiag && opts.parkour()) {
                tryParkour(world, x, y, z, dx, dz, out, PK, feetClear, headClear);
            }
        }

        // ── Pillar up (jump + place below, cardinal only) ──
        if (opts.placeBlocks() && canPassThrough(world, x, y + 2, z)) {
            // Can we jump up and stand?
            if (canPassThrough(world, x, y + 1, z) && canPassThrough(world, x, y + 2, z)) {
                out.add(new int[] {x, y + 1, z, PILLAR_C, PI});
            }
        }

        // ── Straight swim up/down from current position ──
        if (isWater(world, x, y, z)) {
            if (isWater(world, x, y + 1, z) || canPassThrough(world, x, y + 1, z)) {
                out.add(new int[] {x, y + 1, z, SWIM_C, SW});
            }
            if (isWater(world, x, y - 1, z) || canPassThrough(world, x, y - 1, z)) {
                out.add(new int[] {x, y - 1, z, SWIM_C - 1, SW});
            }
        }

        return out;
    }

    /**
     * Tries parkour jumps of 2, 3, and 4 blocks in a cardinal direction.
     * Inspired by Baritone's MovementParkour — checks gap, clearance, and landing.
     */
    private static void tryParkour(
            World world, int x, int y, int z,
            int dx, int dz, List<int[]> out, int PK,
            boolean gap1FeetClear, boolean gap1HeadClear) {

        // Can't jump from soul sand (maxJump = 2 → only 1-gap)
        // Can't jump from water/slow blocks
        if (isSlowBlock(world, x, y - 1, z)) return;
        if (isWater(world, x, y, z)) return;

        // Head clearance at source (y+2)
        if (!canPassThrough(world, x, y + 2, z)) return;

        int maxJump = 4; // sprint: 4 blocks

        // Verify each gap distance
        for (int dist = 2; dist <= maxJump; dist++) {
            int gx = x + dx * (dist - 1), gz = z + dz * (dist - 1);

            // All intermediate positions must have clearance
            if (!canPassThrough(world, gx, y, gz)
                    || !canPassThrough(world, gx, y + 1, gz)
                    || !canPassThrough(world, gx, y + 2, gz)) break;

            // The gap must actually be a gap (no floor)
            if (dist == 2 && !gap1FeetClear) break;

            int lx = x + dx * dist, lz = z + dz * dist;

            // Check landing clearance
            if (!canPassThrough(world, lx, y, lz) || !canPassThrough(world, lx, y + 1, lz)) {
                // Ascend parkour: land on top of a block at y+1
                if (dist <= 3 && canStandOn(world, lx, y, lz)
                        && canPassThrough(world, lx, y + 1, lz)
                        && canPassThrough(world, lx, y + 2, lz)) {
                    if (!isHazard(world, lx, y + 1, lz)) {
                        out.add(new int[] {lx, y + 1, lz, WALK * dist + PARKOUR_C + ASCEND, PK});
                    }
                }
                break;
            }

            // Flat landing
            if (canStandOn(world, lx, y - 1, lz)) {
                if (!isHazard(world, lx, y, lz) && !isHazard(world, lx, y - 1, lz)) {
                    out.add(new int[] {lx, y, lz, WALK * dist + PARKOUR_C, PK});
                }
            }
        }
    }

    // ── Block classification helpers ───────────────────────────────────────────
    // These replace the simple isPassable() checks with proper handling of
    // slabs, stairs, fences, walls, doors, water, lava, trapdoors, etc.

    /**
     * Can a player body (feet or head) pass through this block position?
     * More accurate than Block.isPassable() — handles slabs, trapdoors, fences, etc.
     */
    public static boolean canPassThrough(World world, int x, int y, int z) {
        if (y < world.getMinHeight() || y > world.getMaxHeight()) return true;
        try {
            if (!world.isChunkLoaded(x >> 4, z >> 4)) return false;
            Block block = world.getBlockAt(x, y, z);
            Material mat = block.getType();

            // Air is always passable
            if (mat.isAir()) return true;

            // Hazards are passable for pathfinding geometry but should be avoided by cost
            // (handled in neighbor generation)

            // Water is passable (for swim)
            if (mat == Material.WATER) return true;

            // Lava is NOT passable (hazard, don't walk through)
            if (mat == Material.LAVA) return false;

            // Fences and walls block movement (1.5 blocks tall)
            if (block.getBlockData() instanceof Fence) return false;
            if (mat.name().contains("WALL") && !mat.name().contains("WALL_")) return false;
            if (mat.name().contains("_WALL") || mat == Material.COBBLESTONE_WALL
                    || mat == Material.MOSSY_COBBLESTONE_WALL) return false;

            // Fence gates: passable only when open
            if (block.getBlockData() instanceof Gate gate) {
                return gate.isOpen();
            }

            // Trapdoors: passable only when open
            if (block.getBlockData() instanceof TrapDoor trapDoor) {
                return trapDoor.isOpen();
            }

            // Top slabs block head, bottom slabs don't block feet at that Y
            if (block.getBlockData() instanceof Slab slab) {
                return slab.getType() == Slab.Type.BOTTOM;
            }

            // Cobweb: not passable (too slow)
            if (mat == Material.COBWEB) return false;

            // Fallback to Bukkit's isPassable
            return block.isPassable();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Can a player stand on this block (solid ground underneath feet)?
     * Handles slabs, stairs, fences, walls, chests, leaves, etc.
     */
    public static boolean canStandOn(World world, int x, int y, int z) {
        if (y < world.getMinHeight() || y > world.getMaxHeight()) return false;
        try {
            if (!world.isChunkLoaded(x >> 4, z >> 4)) return false;
            Block block = world.getBlockAt(x, y, z);
            Material mat = block.getType();

            // Air can't be stood on
            if (mat.isAir()) return false;

            // Full solid blocks (most common case)
            if (mat.isSolid() && mat.isOccluding()) return true;

            // Slabs: top slabs and double slabs are walkable; bottom slabs are too
            if (block.getBlockData() instanceof Slab slab) {
                return true; // all slab types can be stood on
            }

            // Stairs can be stood on
            if (mat.name().contains("STAIRS")) return true;

            // Fences and walls (1.5 blocks high — can stand on top)
            if (block.getBlockData() instanceof Fence) return true;
            if (mat.name().contains("WALL")) return true;

            // Glass, stained glass, glass panes
            if (mat == Material.GLASS || mat.name().contains("STAINED_GLASS")
                    && !mat.name().contains("PANE")) return true;

            // Chests, ender chests, barrels
            if (mat == Material.CHEST || mat == Material.TRAPPED_CHEST
                    || mat == Material.ENDER_CHEST || mat == Material.BARREL) return true;

            // Leaves (can stand on in a pinch)
            if (mat.name().contains("LEAVES")) return true;

            // Farmland, dirt path, soul sand
            if (mat == Material.FARMLAND || mat == Material.DIRT_PATH
                    || mat == Material.SOUL_SAND) return true;

            // Honey block (walkable but slow — cost handled separately)
            if (mat == Material.HONEY_BLOCK) return true;

            // Beds
            if (mat.name().contains("_BED")) return true;

            // Scaffolding
            if (mat == Material.SCAFFOLDING) return true;

            // Trapdoors: closed top trapdoor can be stood on
            if (block.getBlockData() instanceof TrapDoor trapDoor) {
                return !trapDoor.isOpen() && trapDoor.getHalf() == org.bukkit.block.data.Bisected.Half.TOP;
            }

            // Water: can be stood on only if there's a solid block below or it's the surface
            if (mat == Material.WATER) return false;

            // Magma block: solid but hazardous (cost handled in neighbor gen)
            if (mat == Material.MAGMA_BLOCK) return true;

            // Fallback: if not passable, it's probably solid enough to stand on
            return !block.isPassable();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Legacy compat: alias for canStandOn + clearance check.
     */
    public static boolean walkable(World world, int x, int y, int z) {
        if (!inBounds(world, y) || !inBounds(world, y + 1)) return false;
        return canStandOn(world, x, y - 1, z)
                && canPassThrough(world, x, y, z)
                && canPassThrough(world, x, y + 1, z);
    }

    /**
     * Legacy compat: alias for canPassThrough.
     */
    public static boolean passable(World world, int x, int y, int z) {
        return canPassThrough(world, x, y, z);
    }

    /**
     * Is this block a hazard the bot should avoid walking into?
     */
    private static boolean isHazard(World world, int x, int y, int z) {
        if (y < world.getMinHeight() || y > world.getMaxHeight()) return false;
        try {
            if (!world.isChunkLoaded(x >> 4, z >> 4)) return false;
            return HAZARDS.contains(world.getBlockAt(x, y, z).getType());
        } catch (Exception e) {
            return true; // be safe
        }
    }

    /**
     * Is this block water?
     */
    private static boolean isWater(World world, int x, int y, int z) {
        if (y < world.getMinHeight() || y > world.getMaxHeight()) return false;
        try {
            if (!world.isChunkLoaded(x >> 4, z >> 4)) return false;
            Block block = world.getBlockAt(x, y, z);
            if (block.getType() == Material.WATER) return true;
            // Check waterlogged blocks
            if (block.getBlockData() instanceof Waterlogged wl) {
                return wl.isWaterlogged();
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Is this a movement-slowing block (soul sand, honey, cobweb)?
     */
    private static boolean isSlowBlock(World world, int x, int y, int z) {
        if (y < world.getMinHeight() || y > world.getMaxHeight()) return false;
        try {
            if (!world.isChunkLoaded(x >> 4, z >> 4)) return false;
            return SLOW_BLOCKS.contains(world.getBlockAt(x, y, z).getType());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Can this block be broken for path clearing?
     * Returns false for bedrock, obsidian, reinforced deepslate, etc.
     */
    private static boolean canBreak(World world, int x, int y, int z) {
        if (y < world.getMinHeight() || y > world.getMaxHeight()) return false;
        try {
            if (!world.isChunkLoaded(x >> 4, z >> 4)) return false;
            Material mat = world.getBlockAt(x, y, z).getType();
            if (mat.isAir()) return false; // nothing to break
            // Unbreakable blocks
            if (mat == Material.BEDROCK || mat == Material.END_PORTAL_FRAME
                    || mat == Material.END_PORTAL || mat == Material.BARRIER
                    || mat == Material.COMMAND_BLOCK || mat == Material.CHAIN_COMMAND_BLOCK
                    || mat == Material.REPEATING_COMMAND_BLOCK
                    || mat == Material.STRUCTURE_BLOCK || mat == Material.JIGSAW
                    || mat == Material.REINFORCED_DEEPSLATE) return false;
            // Very hard blocks (would take too long)
            if (mat == Material.OBSIDIAN || mat == Material.CRYING_OBSIDIAN
                    || mat == Material.RESPAWN_ANCHOR || mat == Material.ANCIENT_DEBRIS
                    || mat == Material.NETHERITE_BLOCK || mat == Material.ENDER_CHEST) return false;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Does the position at (x,y,z) have at least one adjacent solid block?
     * Used for PLACE validation — need something to place against.
     */
    private static boolean hasAdjacentSolid(World world, int x, int y, int z) {
        return canStandOn(world, x, y - 1, z)  // below
                || canStandOn(world, x + 1, y, z)
                || canStandOn(world, x - 1, y, z)
                || canStandOn(world, x, y, z + 1)
                || canStandOn(world, x, y, z - 1);
    }

    // ── Snap, heuristic, bounds ────────────────────────────────────────────────

    private static Pos snap(World world, int x, int y, int z) {
        for (int dy = 0; dy <= 3; dy++) {
            if (dy == 0 && walkable(world, x, y, z)) return new Pos(x, y, z);
            if (dy > 0 && walkable(world, x, y + dy, z)) return new Pos(x, y + dy, z);
            if (dy > 0 && walkable(world, x, y - dy, z)) return new Pos(x, y - dy, z);
        }
        // Try snapping in water (for swim-based paths)
        if (isWater(world, x, y, z)) return new Pos(x, y, z);
        return null;
    }

    private static int heuristic(Pos a, Pos b) {
        int dx = Math.abs(a.x() - b.x());
        int dy = Math.abs(a.y() - b.y());
        int dz = Math.abs(a.z() - b.z());
        int maxXZ = Math.max(dx, dz), minXZ = Math.min(dx, dz);
        // Octile distance for XZ + vertical cost
        return (WALK * maxXZ) + ((DIAGONAL - WALK) * minXZ) + dy * ASCEND;
    }

    private static boolean inBounds(World world, int y) {
        return y > world.getMinHeight() && y < world.getMaxHeight() - 1;
    }

    private static List<Move> buildPathMoves(Node end) {
        List<Move> path = new ArrayList<>();
        for (Node n = end; n != null; n = n.parent()) {
            path.addFirst(new Move(n.pos().x(), n.pos().y(), n.pos().z(), n.action()));
        }
        return path;
    }
}
