package me.bill.fakePlayerPlugin.util;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import me.bill.fakePlayerPlugin.FakePlayerPlugin;

public final class BackupManager {

  private static final DateTimeFormatter DATE_FMT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

  private static final int MAX_BACKUPS = 10;

  private BackupManager() {}

  public static File createConfigFilesBackup(FakePlayerPlugin plugin, String reason) {
    String safeReason = reason.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    String timestamp = LocalDateTime.now().format(DATE_FMT);
    File backupDir = new File(plugin.getDataFolder(), "backups/" + timestamp + "_" + safeReason);
    backupDir.mkdirs();

    File dataFolder = plugin.getDataFolder();

    copyFile(new File(dataFolder, "config.yml"), new File(backupDir, "config.yml"));
    copyFile(new File(dataFolder, "bot-names.yml"), new File(backupDir, "bot-names.yml"));
    copyFile(new File(dataFolder, "bot-messages.yml"), new File(backupDir, "bot-messages.yml"));

    File langDir = new File(dataFolder, "language");
    if (langDir.isDirectory()) {
      File langBackup = new File(backupDir, "language");
      langBackup.mkdirs();
      File[] langFiles = langDir.listFiles((d, n) -> n.endsWith(".yml"));
      if (langFiles != null) {
        for (File lf : langFiles) copyFile(lf, new File(langBackup, lf.getName()));
      }
    }

    writeManifest(backupDir, plugin, reason);
    pruneOldBackups(new File(dataFolder, "backups"));
    FppLogger.success("Config-files backup created → backups/" + backupDir.getName() + "/");
    return backupDir;
  }

  public static File createFullBackup(FakePlayerPlugin plugin, String reason) {
    return createFullBackup(plugin, reason, true);
  }

  public static File createFullBackup(FakePlayerPlugin plugin, String reason, boolean announce) {
    String safeReason = reason.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    String timestamp = LocalDateTime.now().format(DATE_FMT);
    File backupDir = new File(plugin.getDataFolder(), "backups/" + timestamp + "_" + safeReason);
    backupDir.mkdirs();

    File dataFolder = plugin.getDataFolder();

    copyFile(new File(dataFolder, "config.yml"), new File(backupDir, "config.yml"));
    copyFile(new File(dataFolder, "bot-names.yml"), new File(backupDir, "bot-names.yml"));
    copyFile(new File(dataFolder, "bot-messages.yml"), new File(backupDir, "bot-messages.yml"));

    File langDir = new File(dataFolder, "language");
    if (langDir.isDirectory()) {
      File langBackup = new File(backupDir, "language");
      langBackup.mkdirs();
      File[] langFiles = langDir.listFiles((d, n) -> n.endsWith(".yml"));
      if (langFiles != null) {
        for (File lf : langFiles) copyFile(lf, new File(langBackup, lf.getName()));
      }
    }

    copyFile(new File(dataFolder, "data/active-bots.yml"), new File(backupDir, "active-bots.yml"));

    File dbFile = new File(dataFolder, "data/fpp.db");
    if (dbFile.exists()) {
      copyFile(dbFile, new File(backupDir, "fpp.db"));

      copyFile(new File(dataFolder, "data/fpp.db-wal"), new File(backupDir, "fpp.db-wal"));
      copyFile(new File(dataFolder, "data/fpp.db-shm"), new File(backupDir, "fpp.db-shm"));
    }

    writeManifest(backupDir, plugin, reason);

    pruneOldBackups(new File(dataFolder, "backups"));

    if (announce) {
      FppLogger.success("Backup created → backups/" + backupDir.getName() + "/");
    } else {
      FppLogger.debug("Backup created → backups/" + backupDir.getName() + "/");
    }
    return backupDir;
  }

  public static List<String> listBackups(FakePlayerPlugin plugin) {
    File backupsDir = new File(plugin.getDataFolder(), "backups");
    if (!backupsDir.isDirectory()) return List.of();

    File[] dirs = backupsDir.listFiles(File::isDirectory);
    if (dirs == null || dirs.length == 0) return List.of();

    Arrays.sort(dirs, Comparator.comparing(File::getName).reversed());
    List<String> names = new ArrayList<>(dirs.length);
    for (File d : dirs) names.add(d.getName());
    return Collections.unmodifiableList(names);
  }

  public static long totalBackupSizeBytes(FakePlayerPlugin plugin) {
    File backupsDir = new File(plugin.getDataFolder(), "backups");
    return dirSize(backupsDir);
  }

  private static void copyFile(File src, File dst) {
    if (!src.exists()) return;
    try {
      dst.getParentFile().mkdirs();
      Files.copy(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      FppLogger.debug("BackupManager: could not copy " + src.getName() + ": " + e.getMessage());
    }
  }

  private static void writeManifest(File backupDir, FakePlayerPlugin plugin, String reason) {
    File manifest = new File(backupDir, "MANIFEST.txt");
    try (PrintWriter pw = new PrintWriter(new FileWriter(manifest))) {
      pw.println("FakePlayerPlugin Backup Manifest");
      pw.println("================================");
      pw.println("Plugin version : " + plugin.getPluginMeta().getVersion());
      pw.println("Backup reason  : " + reason);
      pw.println("Timestamp      : " + LocalDateTime.now());
      pw.println("Server version : " + plugin.getServer().getVersion());
    } catch (IOException e) {
      FppLogger.debug("BackupManager: could not write manifest: " + e.getMessage());
    }
  }

  private static void pruneOldBackups(File backupsDir) {
    if (!backupsDir.isDirectory()) return;
    File[] dirs = backupsDir.listFiles(File::isDirectory);
    if (dirs == null || dirs.length <= MAX_BACKUPS) return;

    Arrays.sort(dirs, Comparator.comparing(File::getName));
    int toDelete = dirs.length - MAX_BACKUPS;
    for (int i = 0; i < toDelete; i++) {
      deleteDirectory(dirs[i]);
      FppLogger.debug("BackupManager: pruned old backup: " + dirs[i].getName());
    }
  }

  private static void deleteDirectory(File dir) {
    File[] files = dir.listFiles();
    if (files != null) {
      for (File f : files) {
        if (f.isDirectory()) deleteDirectory(f);
        else f.delete();
      }
    }
    dir.delete();
  }

  private static long dirSize(File dir) {
    if (!dir.exists()) return 0L;
    long size = 0L;
    File[] files = dir.listFiles();
    if (files != null) {
      for (File f : files) {
        size += f.isDirectory() ? dirSize(f) : f.length();
      }
    }
    return size;
  }
}
