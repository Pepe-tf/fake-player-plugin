package me.bill.fakePlayerPlugin.command;

import me.bill.fakePlayerPlugin.FakePlayerPlugin;
import me.bill.fakePlayerPlugin.config.Config;
import me.bill.fakePlayerPlugin.database.DatabaseManager;
import me.bill.fakePlayerPlugin.lang.Lang;
import me.bill.fakePlayerPlugin.permission.Perm;
import me.bill.fakePlayerPlugin.util.BackupManager;
import me.bill.fakePlayerPlugin.util.ConfigMigrator;
import me.bill.fakePlayerPlugin.util.DataMigrator;
import me.bill.fakePlayerPlugin.util.TextUtil;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.util.List;

/**
 * {@code /fpp migrate} — Administration command for the plugin update/migration system.
 *
 * <h3>Subcommands</h3>
 * <pre>
 *  /fpp migrate backup             — Create a manual timestamped backup now
 *  /fpp migrate backups            — List stored backups
 *  /fpp migrate status             — Show config version, DB stats and backup count
 *  /fpp migrate config             — Re-run the config migration chain
 *  /fpp migrate db merge [file]    — Merge an old fpp.db into the current database
 *  /fpp migrate db export          — Export all session history to CSV
 *  /fpp migrate db tomysql         — Migrate SQLite data into configured MySQL
 * </pre>
 *
 * <p>Requires permission {@code fpp.admin.migrate} (child of {@code fpp.*}).
 */
public class MigrateCommand implements FppCommand {

    private static final String COLOR   = "<#0079FF>";
    private static final String C_CLOSE = "</#0079FF>";
    private static final String GRAY    = "<gray>";
    private static final String GREEN   = "<green>";
    private static final String RED     = "<red>";

    private final FakePlayerPlugin plugin;

    public MigrateCommand(FakePlayerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String getName()        { return "migrate"; }
    @Override public String getUsage()       { return "<backup|status|config|db>"; }
    @Override public String getDescription() { return "Manages config/data migration and backups."; }
    @Override public String getPermission()  { return Perm.ADMIN_MIGRATE; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission(Perm.ADMIN_MIGRATE)) {
            sender.sendMessage(Lang.get("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "backup"  -> doBackup(sender);
            case "backups" -> doBackupsList(sender);
            case "status"  -> doStatus(sender);
            case "config"  -> doConfig(sender);
            case "db"      -> doDb(sender, args);
            default        -> sendHelp(sender);
        }
        return true;
    }

    // ── Subcommand handlers ───────────────────────────────────────────────────

    private void doBackup(CommandSender sender) {
        msg(sender, GRAY + "Creating backup of all plugin files…");
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            File dir = BackupManager.createFullBackup(plugin, "manual");
            long bytes = BackupManager.totalBackupSizeBytes(plugin);
            sync(sender, GREEN + "✔ Backup created: " + GRAY + "backups/" + dir.getName());
            sync(sender, GRAY + "  Total backup storage: " + GRAY + formatBytes(bytes));
        });
    }

    private void doBackupsList(CommandSender sender) {
        List<String> backups = BackupManager.listBackups(plugin);
        if (backups.isEmpty()) {
            msg(sender, GRAY + "No backups found.");
            return;
        }
        msg(sender, COLOR + "ꜱᴛᴏʀᴇᴅ ʙᴀᴄᴋᴜᴘꜱ" + C_CLOSE + GRAY + " (" + backups.size() + " total):");
        backups.stream().limit(15).forEach(b ->
                msg(sender, GRAY + "  • " + b));
        if (backups.size() > 15) {
            msg(sender, GRAY + "  … and " + (backups.size() - 15) + " more.");
        }
        msg(sender, GRAY + "  Total size: " + formatBytes(BackupManager.totalBackupSizeBytes(plugin)));
    }

    private void doStatus(CommandSender sender) {
        int configVer = plugin.getConfig().getInt("config-version", 0);
        boolean configCurrent = configVer >= ConfigMigrator.CURRENT_VERSION;

        msg(sender, COLOR + "ᴍɪɢʀᴀᴛɪᴏɴ ꜱᴛᴀᴛᴜꜱ" + C_CLOSE);

        // Config
        msg(sender, GRAY + "  Config version : " + configVer + " / " + ConfigMigrator.CURRENT_VERSION
                + (configCurrent ? "  " + GREEN + "✔ current" : "  " + RED + "✘ outdated — run /fpp migrate config"));

        // Database
        DatabaseManager db = plugin.getDatabaseManager();
        if (db != null) {
            var stats = db.getStats();
            msg(sender, GRAY + "  DB backend     : " + stats.backend());
            msg(sender, GRAY + "  Sessions       : " + stats.totalSessions() + " total, "
                    + stats.activeSessions() + " active");
            msg(sender, GRAY + "  Unique bots    : " + stats.uniqueBots());
            msg(sender, GRAY + "  Unique spawners: " + stats.uniqueSpawners());
            if (stats.totalUptimeMs() > 0) {
                msg(sender, GRAY + "  Combined uptime: " + stats.formattedUptime());
            }
        } else {
            msg(sender, RED + "  Database       : offline");
        }

        // Backups
        List<String> backups = BackupManager.listBackups(plugin);
        msg(sender, GRAY + "  Backups stored : " + backups.size() + " (max 10 kept)");
        if (!backups.isEmpty()) {
            msg(sender, GRAY + "  Latest backup  : " + backups.get(0));
        }
        msg(sender, GRAY + "  Backup storage : " + formatBytes(BackupManager.totalBackupSizeBytes(plugin)));
    }

    private void doConfig(CommandSender sender) {
        int current = plugin.getConfig().getInt("config-version", 0);
        if (current >= ConfigMigrator.CURRENT_VERSION) {
            msg(sender, GREEN + "✔ Config is already at the latest version (v"
                    + ConfigMigrator.CURRENT_VERSION + "). Nothing to do.");
            return;
        }
        msg(sender, GRAY + "Running config migration from v" + current
                + " → v" + ConfigMigrator.CURRENT_VERSION + "…");
        // Reset stored version so the chain runs fully
        plugin.getConfig().set("config-version", 0);
        plugin.saveConfig();

        boolean migrated = ConfigMigrator.migrateIfNeeded(plugin);

        // Reload the in-memory config so the rest of the server picks up changes
        Config.reload();

        if (migrated) {
            msg(sender, GREEN + "✔ Config migrated to v" + ConfigMigrator.CURRENT_VERSION
                    + " and reloaded. Check console for details.");
        } else {
            msg(sender, GREEN + "✔ Config stamped as v" + ConfigMigrator.CURRENT_VERSION
                    + " (defaults filled).");
        }
    }

    private void doDb(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendHelp(sender);
            return;
        }
        DatabaseManager db = plugin.getDatabaseManager();

        switch (args[1].toLowerCase()) {
            case "merge" -> {
                // Resolve filename — default to "fpp.db", allow custom filename or absolute path
                String filename = args.length > 2 ? args[2] : "fpp.db";
                File file = resolveDbFile(filename);

                if (!file.exists()) {
                    msg(sender, RED + "File not found: " + filename);
                    msg(sender, GRAY + "Tip: place the old fpp.db inside "
                            + "plugins/FakePlayerPlugin/data/ and run:");
                    msg(sender, GRAY + "  /fpp migrate db merge " + filename);
                    return;
                }
                if (db == null) { msg(sender, RED + "Database is offline."); return; }

                msg(sender, GRAY + "Merging " + file.getName()
                        + " into current database… (this may take a moment)");

                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                    int merged = DataMigrator.mergeFromSQLite(plugin, db, file);
                    if (merged >= 0) {
                        sync(sender, GREEN + "✔ Merge complete — " + merged + " row(s) inserted.");
                        sync(sender, GRAY + "  Total sessions now: " + db.countSessions());
                    } else {
                        sync(sender, RED + "✘ Merge failed. Check console for details.");
                    }
                });
            }

            case "export" -> {
                if (db == null) { msg(sender, RED + "Database is offline."); return; }
                msg(sender, GRAY + "Exporting session history to CSV…");
                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                    File exported = DataMigrator.exportSessionsCsv(plugin, db);
                    if (exported != null) {
                        sync(sender, GREEN + "✔ Exported → "
                                + GRAY + "exports/" + exported.getName());
                    } else {
                        sync(sender, RED + "✘ Export failed. Check console for details.");
                    }
                });
            }

            case "tomysql" -> {
                if (!plugin.getConfig().getBoolean("database.mysql-enabled", false)) {
                    msg(sender, RED + "MySQL is not enabled in config.yml.");
                    msg(sender, GRAY + "Set database.mysql-enabled: true and fill in your credentials,"
                            + " then run /fpp reload before using this command.");
                    return;
                }
                if (db == null) { msg(sender, RED + "Database is offline."); return; }
                if (!db.isMysql()) {
                    msg(sender, RED + "The active database is still SQLite — run /fpp reload first"
                            + " so the plugin connects to MySQL, then retry.");
                    return;
                }
                msg(sender, GRAY + "Migrating SQLite → MySQL… (this may take a moment)");
                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                    int count = DataMigrator.migrateToMysql(plugin, db);
                    if (count >= 0) {
                        sync(sender, GREEN + "✔ Migration complete — " + count + " row(s) transferred.");
                    } else {
                        sync(sender, RED + "✘ Migration failed. Check console for details.");
                    }
                });
            }

            default -> sendHelp(sender);
        }
    }

    // ── Tab complete ──────────────────────────────────────────────────────────

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (!sender.hasPermission(Perm.ADMIN_MIGRATE)) return List.of();
        if (args.length == 1) return filter(List.of("backup", "backups", "status", "config", "db"), args[0]);
        if (args.length == 2 && args[0].equalsIgnoreCase("db"))
            return filter(List.of("merge", "export", "tomysql"), args[1]);
        if (args.length == 3 && args[0].equalsIgnoreCase("db") && args[1].equalsIgnoreCase("merge"))
            return filter(List.of("fpp.db", "fpp_old.db", "fpp_backup.db"), args[2]);
        return List.of();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void sendHelp(CommandSender sender) {
        msg(sender, COLOR + "ᴍɪɢʀᴀᴛɪᴏɴ & ʙᴀᴄᴋᴜᴘ ꜱʏꜱᴛᴇᴍ" + C_CLOSE);
        row(sender, "/fpp migrate backup",          "Create a manual backup now");
        row(sender, "/fpp migrate backups",          "List stored backups");
        row(sender, "/fpp migrate status",           "Show migration & DB status");
        row(sender, "/fpp migrate config",           "Re-run config migration chain");
        row(sender, "/fpp migrate db merge [file]",  "Merge old fpp.db into current DB");
        row(sender, "/fpp migrate db export",        "Export sessions to CSV");
        row(sender, "/fpp migrate db tomysql",       "Migrate SQLite → MySQL");
    }

    private void row(CommandSender sender, String cmd, String desc) {
        msg(sender, GRAY + "  " + COLOR + cmd + C_CLOSE + " " + GRAY + "— " + desc);
    }

    private void msg(CommandSender sender, String mm) {
        sender.sendMessage(TextUtil.colorize(mm));
    }

    /** Run {@code task} on the main thread (safe for sending messages from async). */
    private void sync(CommandSender sender, String mm) {
        plugin.getServer().getScheduler().runTask(plugin, () -> msg(sender, mm));
    }

    private File resolveDbFile(String filename) {
        // Try as relative path inside data/ directory first
        File inData = new File(plugin.getDataFolder(), "data/" + filename);
        if (inData.exists()) return inData;
        // Try plugins root
        File inPlugins = new File(plugin.getDataFolder().getParentFile(), filename);
        if (inPlugins.exists()) return inPlugins;
        // Try absolute path
        File abs = new File(filename);
        if (abs.isAbsolute()) return abs;
        // Default to data/filename (may not exist — caller checks)
        return inData;
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024)       return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }

    private static List<String> filter(List<String> options, String partial) {
        String p = partial.toLowerCase();
        return options.stream().filter(o -> o.toLowerCase().startsWith(p)).toList();
    }
}



