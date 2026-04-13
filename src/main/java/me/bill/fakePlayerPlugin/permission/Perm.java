package me.bill.fakePlayerPlugin.permission;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@SuppressWarnings("unused")
public final class Perm {

  private Perm() {}

  public static final String COMMAND = "fpp.command";

  public static final String PLUGIN_INFO = "fpp.plugininfo";

  public static final String OP = "fpp.op";

  public static final String USE = "fpp.use";

  public static final String USER_SPAWN = "fpp.spawn.user";

  public static final String USER_TPH = "fpp.tph";

  public static final String TP = "fpp.tp";

  public static final String USER_XP = "fpp.xp";

  public static final String USER_INFO = "fpp.info.user";

  public static final String BOT_LIMIT_PREFIX = "fpp.spawn.limit.";

  public static final String HELP = "fpp.help";

  public static final String SPAWN = "fpp.spawn";

  public static final String SPAWN_MULTIPLE = "fpp.spawn.multiple";

  public static final String SPAWN_CUSTOM_NAME = "fpp.spawn.name";

  public static final String DELETE = "fpp.delete";

  public static final String DELETE_ALL = "fpp.delete.all";

  public static final String LIST = "fpp.list";

  public static final String CHAT = "fpp.chat";

  public static final String RELOAD = "fpp.reload";

  public static final String INFO = "fpp.info";

  public static final String FREEZE = "fpp.freeze";

  public static final String STATS = "fpp.stats";

  public static final String MIGRATE = "fpp.migrate";

  public static final String LP_INFO = "fpp.lpinfo";

  public static final String RANK = "fpp.rank";

  public static final String ALERT = "fpp.alert";

  public static final String SYNC = "fpp.sync";

  public static final String SWAP = "fpp.swap";

  public static final String PEAKS = "fpp.peaks";

  public static final String SETTINGS = "fpp.settings";

  public static final String MOVE = "fpp.move";

  public static final String INVENTORY = "fpp.inventory";

  public static final String CMD = "fpp.cmd";

  public static final String MINE = "fpp.mine";

  public static final String STORAGE = "fpp.storage";

  public static final String PLACE = "fpp.place";

  public static final String USE_CMD = "fpp.useitem";

  public static final String WAYPOINT = "fpp.waypoint";

  public static final String BADWORD = "fpp.badword";

  public static final String PERSONALITY = "fpp.personality";

  public static final String RENAME = "fpp.rename";

  public static final String RENAME_OWN = "fpp.rename.own";

  public static final String NOTIFY = "fpp.notify";

  public static final String BYPASS_MAX = "fpp.bypass.max";

  public static final String BYPASS_COOLDOWN = "fpp.bypass.cooldown";

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

  public static int resolveUserBotLimit(CommandSender sender) {
    int best = -1;
    for (int i = 1; i <= 100; i++) {
      if (sender.hasPermission(BOT_LIMIT_PREFIX + i)) {
        best = i;
      }
    }
    return best;
  }
}
