package me.bill.fakePlayerPlugin.permission;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@SuppressWarnings("unused")
public final class Perm {

    private Perm() {}

    // ── Visibility / access ──────────────────────────────────────────────────
    public static final String COMMAND      = "fpp.command";
    public static final String PLUGIN_INFO  = "fpp.plugininfo";

    // ── Wildcard tiers ───────────────────────────────────────────────────────
    /** Full admin wildcard — grants every permission. Default: op. */
    public static final String OP           = "fpp.op";
    /** Alias for {@link #OP} — new preferred name. Identical in effect. */
    public static final String ADMIN        = "fpp.admin";
    /** User wildcard — spawn(1), tph, xp, info(own). Default: everyone. */
    public static final String USE          = "fpp.use";

    // ── Spawn ────────────────────────────────────────────────────────────────
    /** Admin spawn — no personal limit, multi-bot, custom names. */
    public static final String SPAWN              = "fpp.spawn";
    /** User spawn — respects personal bot limit. Included in {@link #USE}. */
    public static final String USER_SPAWN         = "fpp.spawn.user";
    /** Spawn more than one bot at a time. Child of {@link #SPAWN}. */
    public static final String SPAWN_MULTIPLE     = "fpp.spawn.multiple";
    /** Alias for {@link #SPAWN_MULTIPLE}. */
    public static final String SPAWN_MASS         = "fpp.spawn.mass";
    /** Use --name flag for custom bot name. Child of {@link #SPAWN}. */
    public static final String SPAWN_CUSTOM_NAME  = "fpp.spawn.name";
    /** Spawn with explicit world / coordinate arguments. */
    public static final String SPAWN_COORDS       = "fpp.spawn.coords";
    /** Prefix for per-player bot-limit nodes — e.g. fpp.spawn.limit.5. */
    public static final String BOT_LIMIT_PREFIX   = "fpp.spawn.limit.";

    // ── Despawn ──────────────────────────────────────────────────────────────
    /** Despawn bots. New preferred name. */
    public static final String DESPAWN             = "fpp.despawn";
    /** Legacy name for {@link #DESPAWN} — still checked in code. */
    public static final String DELETE              = "fpp.delete";
    /** Despawn all bots at once. Child of {@link #DELETE}. */
    public static final String DELETE_ALL          = "fpp.delete.all";
    /** Alias for {@link #DELETE_ALL}. */
    public static final String DESPAWN_BULK        = "fpp.despawn.bulk";
    /** Despawn bots owned by the sender only. */
    public static final String DESPAWN_OWN         = "fpp.despawn.own";

    // ── Help / list / stats ──────────────────────────────────────────────────
    public static final String HELP   = "fpp.help";
    public static final String LIST   = "fpp.list";
    public static final String STATS  = "fpp.stats";

    // ── Info ─────────────────────────────────────────────────────────────────
    /** Full admin database query — all bots, all history. */
    public static final String INFO      = "fpp.info";
    /** User info — own bots only (world, coords, uptime). */
    public static final String USER_INFO = "fpp.info.user";

    // ── Teleport ─────────────────────────────────────────────────────────────
    /** Teleport yourself to any bot. Admin. */
    public static final String TP       = "fpp.tp";
    /** Teleport own bot(s) to you. Included in {@link #USE}. */
    public static final String USER_TPH = "fpp.tph";

    // ── XP ───────────────────────────────────────────────────────────────────
    /** Collect XP from a bot. Included in {@link #USE}. */
    public static final String USER_XP = "fpp.xp";

    // ── Chat ─────────────────────────────────────────────────────────────────
    /** Top-level chat control — grants all chat sub-permissions. */
    public static final String CHAT        = "fpp.chat";
    /** Toggle global chat on/off. Sub-node of {@link #CHAT}. */
    public static final String CHAT_GLOBAL = "fpp.chat.global";
    /** Change a bot's activity tier. Sub-node of {@link #CHAT}. */
    public static final String CHAT_TIER   = "fpp.chat.tier";
    /** Mute a bot (temporarily or permanently). Sub-node of {@link #CHAT}. */
    public static final String CHAT_MUTE   = "fpp.chat.mute";
    /** Force a bot to say a message. Sub-node of {@link #CHAT}. */
    public static final String CHAT_SAY    = "fpp.chat.say";

    // ── Move ─────────────────────────────────────────────────────────────────
    /** Top-level navigation — grants all move sub-permissions. */
    public static final String MOVE          = "fpp.move";
    /** Follow a player (--to). Sub-node of {@link #MOVE}. */
    public static final String MOVE_TO       = "fpp.move.to";
    /** Patrol a waypoint route (--wp). Sub-node of {@link #MOVE}. */
    public static final String MOVE_WAYPOINT = "fpp.move.waypoint";
    /** Stop navigation (--stop). Sub-node of {@link #MOVE}. */
    public static final String MOVE_STOP     = "fpp.move.stop";

    // ── Freeze ───────────────────────────────────────────────────────────────
    public static final String FREEZE = "fpp.freeze";

    // ── Rename ───────────────────────────────────────────────────────────────
    /** Rename any bot. Admin. */
    public static final String RENAME     = "fpp.rename";
    /** Rename only own bots. User opt-in. */
    public static final String RENAME_OWN = "fpp.rename.own";

    // ── Ping ─────────────────────────────────────────────────────────────────
    /** Top-level ping control — grants all ping sub-permissions. */
    public static final String PING        = "fpp.ping";
    /** Set a fixed ping value (--ping). Sub-node of {@link #PING}. */
    public static final String PING_SET    = "fpp.ping.set";
    /** Assign random ping (--random). Sub-node of {@link #PING}. */
    public static final String PING_RANDOM = "fpp.ping.random";
    /** Target multiple bots (--count). Sub-node of {@link #PING}. */
    public static final String PING_BULK   = "fpp.ping.bulk";

    // ── Inventory ────────────────────────────────────────────────────────────
    /** Top-level inventory — grants both cmd and right-click sub-nodes. */
    public static final String INVENTORY           = "fpp.inventory";
    /** Open bot inventory via /fpp inventory command. */
    public static final String INVENTORY_CMD       = "fpp.inventory.cmd";
    /** Open bot inventory by right-clicking the bot entity. */
    public static final String INVENTORY_RIGHTCLICK = "fpp.inventory.rightclick";

    // ── Cmd ──────────────────────────────────────────────────────────────────
    /** Execute commands as a bot / bind right-click command. Admin only. */
    public static final String CMD       = "fpp.cmd.admin";
    /** Legacy node — kept for backward compatibility (maps to fpp.cmd.admin). */
    public static final String CMD_LEGACY = "fpp.cmd";

    // ── Mine ─────────────────────────────────────────────────────────────────
    /** Top-level mine — grants all mine sub-permissions. */
    public static final String MINE       = "fpp.mine";
    /** Start continuous mining. Sub-node of {@link #MINE}. */
    public static final String MINE_START = "fpp.mine.start";
    /** Mine once (--once). Sub-node of {@link #MINE}. */
    public static final String MINE_ONCE  = "fpp.mine.once";
    /** Stop mining (--stop). Sub-node of {@link #MINE}. */
    public static final String MINE_STOP  = "fpp.mine.stop";
    /** Area-selection mining (--pos1/--pos2/--start). Sub-node of {@link #MINE}. */
    public static final String MINE_AREA  = "fpp.mine.area";

    // ── Use ──────────────────────────────────────────────────────────────────
    /** Legacy node for /fpp use. Checked in code. */
    public static final String USE_CMD       = "fpp.useitem";
    /** New alias for {@link #USE_CMD}. */
    public static final String USE_ACTION    = "fpp.use.cmd";
    /** Continuous right-click. Sub-node of {@link #USE_CMD}. */
    public static final String USE_START     = "fpp.useitem.start";
    /** Single right-click (--once). Sub-node of {@link #USE_CMD}. */
    public static final String USE_ONCE      = "fpp.useitem.once";
    /** Stop using (--stop). Sub-node of {@link #USE_CMD}. */
    public static final String USE_STOP      = "fpp.useitem.stop";

    // ── Attack (PvE) ─────────────────────────────────────────────────────────
    /** Walk bot to sender then attack in look direction. Respects 1.9+ attack cooldown. */
    public static final String ATTACK = "fpp.attack";

    // ── Follow ──────────────────────────────────────────────────────────────
    /** Make a bot continuously follow a player. */
    public static final String FOLLOW = "fpp.follow";

    // ── Place ─────────────────────────────────────────────────────────────────    /** Top-level place — grants all place sub-permissions. */
    public static final String PLACE       = "fpp.place";
    /** Start continuous placement. Sub-node of {@link #PLACE}. */
    public static final String PLACE_START = "fpp.place.start";
    /** Place once (--once). Sub-node of {@link #PLACE}. */
    public static final String PLACE_ONCE  = "fpp.place.once";
    /** Stop placing (--stop). Sub-node of {@link #PLACE}. */
    public static final String PLACE_STOP  = "fpp.place.stop";

    // ── Storage ───────────────────────────────────────────────────────────────
    public static final String STORAGE = "fpp.storage";

    // ── Waypoint ──────────────────────────────────────────────────────────────
    public static final String WAYPOINT = "fpp.waypoint";

    // ── Rank (LuckPerms) ─────────────────────────────────────────────────────
    /** Assign LP groups — grants all rank sub-permissions. */
    public static final String RANK      = "fpp.rank";
    /** Assign a group with --set. Sub-node of {@link #RANK}. */
    public static final String RANK_SET  = "fpp.rank.set";
    /** Clear a bot's group. Sub-node of {@link #RANK}. */
    public static final String RANK_CLEAR = "fpp.rank.clear";
    /** Bulk random assignment. Sub-node of {@link #RANK}. */
    public static final String RANK_BULK  = "fpp.rank.bulk";

    // ── LuckPerms info ────────────────────────────────────────────────────────
    public static final String LP_INFO = "fpp.lpinfo";

    // ── AI / personality ─────────────────────────────────────────────────────
    public static final String PERSONALITY = "fpp.personality";

    // ── Badword filter ────────────────────────────────────────────────────────
    public static final String BADWORD = "fpp.badword";

    // ── Reload / migrate / sync / alert ──────────────────────────────────────
    public static final String RELOAD  = "fpp.reload";
    public static final String MIGRATE = "fpp.migrate";
    public static final String SYNC    = "fpp.sync";
    public static final String ALERT   = "fpp.alert";

    // ── Swap / peaks / settings ───────────────────────────────────────────────
    public static final String SWAP     = "fpp.swap";
    public static final String PEAKS    = "fpp.peaks";
    public static final String SETTINGS = "fpp.settings";

    // ── Bypass ────────────────────────────────────────────────────────────────
    /** Bypass global max-bots cap. */
    public static final String BYPASS_MAX      = "fpp.bypass.max";
    /** Bypass spawn cooldown. */
    public static final String BYPASS_COOLDOWN = "fpp.bypass.cooldown";

    // ── Notifications ─────────────────────────────────────────────────────────
    public static final String NOTIFY = "fpp.notify";

    // ── Helpers ───────────────────────────────────────────────────────────────

    public static boolean has(CommandSender sender, String permission) {
        return sender.hasPermission(permission);
    }

    public static boolean hasOrOp(CommandSender sender, String permission) {
        if (sender instanceof Player p && p.isOp()) return true;
        return sender.hasPermission(permission);
    }

    public static boolean missing(CommandSender sender, String permission) {
        return !has(sender, permission);
    }

    public static boolean hasAny(CommandSender sender, String... permissions) {
        for (String perm : permissions) {
            if (sender.hasPermission(perm)) return true;
        }
        return false;
    }

    /**
     * Resolves the highest personal bot limit from {@code fpp.spawn.limit.1} …
     * {@code fpp.spawn.limit.100}. Returns {@code -1} if no limit node is set
     * (caller should fall back to the config default).
     */
    public static int resolveUserBotLimit(CommandSender sender) {
        int best = -1;
        for (int i = 1; i <= 100; i++) {
            if (sender.hasPermission(BOT_LIMIT_PREFIX + i)) best = i;
        }
        return best;
    }
}
