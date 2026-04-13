package me.bill.fakePlayerPlugin.fakeplayer;

import me.bill.fakePlayerPlugin.config.Config;
import me.bill.fakePlayerPlugin.util.FppLogger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public final class FakePlayerBody {

  public static final String VISUAL_PDC_VALUE = "fpp-visual";

  public static final String NAMETAG_PDC_VALUE = "fpp-nametag";

  private FakePlayerBody() {}

  public static Player spawn(FakePlayer fp, Location loc) {
    if (loc == null || loc.getWorld() == null) return null;
    try {

      Player player =
          NmsPlayerSpawner.spawnFakePlayer(
              fp.getUuid(),
              fp.getName(),
              fp.getResolvedSkin(),
              loc.getWorld(),
              loc.getX(),
              loc.getY(),
              loc.getZ());

      if (player == null) {
        FppLogger.warn("FakePlayerBody.spawn: NmsPlayerSpawner returned null for " + fp.getName());
        return null;
      }

      try {
        if (FakePlayerManager.FAKE_PLAYER_KEY != null) {
          player
              .getPersistentDataContainer()
              .set(
                  FakePlayerManager.FAKE_PLAYER_KEY,
                  PersistentDataType.STRING,
                  VISUAL_PDC_VALUE + ":" + fp.getName());
        }
      } catch (Exception e) {
        FppLogger.debug(
            "FakePlayerBody.spawn: PDC tag failed for " + fp.getName() + ": " + e.getMessage());
      }

      player.setGravity(true);

      player.setInvulnerable(false);
      player.setCollidable(true);

      player.setCanPickupItems(fp.isPickUpItemsEnabled());

      String displayName =
          fp.getRawDisplayName() != null ? fp.getRawDisplayName() : fp.getDisplayName();
      if (displayName != null && !displayName.isEmpty()) {
        try {
          player.displayName(me.bill.fakePlayerPlugin.util.TextUtil.colorize(displayName));
        } catch (Exception e) {
          FppLogger.debug("Failed to set display name for " + fp.getName() + ": " + e.getMessage());
        }
      }

      try {
        var maxHpAttr = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        if (maxHpAttr != null) {
          double hp = Config.maxHealth();
          maxHpAttr.setBaseValue(hp);
          player.setHealth(hp);
        }
      } catch (Exception ignored) {
      }

      player.setAllowFlight(false);
      player.setFlying(false);

      boolean fluidSpawn =
          loc.getBlock().isLiquid() || (loc.clone().add(0, 1, 0).getBlock().isLiquid());
      if (!fluidSpawn) {
        player.setVelocity(new org.bukkit.util.Vector(0, -0.001, 0));
      }

      Config.debug(
          "FakePlayerBody: spawned "
              + fp.getName()
              + " (gravity=true, damageable="
              + Config.bodyDamageable()
              + ", flying=false)");

      applyPaperSkin(player, fp.getResolvedSkin());

      return player;

    } catch (Exception e) {
      FppLogger.error("FakePlayerBody.spawn failed for " + fp.getName() + ": " + e.getMessage());
      return null;
    }
  }

  public static void removeAll(FakePlayer fp) {
    if (fp == null) return;
    try {
      Player player = fp.getPlayer();
      if (player != null && player.isOnline()) {
        NmsPlayerSpawner.removeFakePlayer(player);
      }
    } catch (Exception e) {
      FppLogger.error(
          "FakePlayerBody.removeAll failed for "
              + (fp.getName() != null ? fp.getName() : "?")
              + ": "
              + e.getMessage());
    }
  }

  public static void resolveAndFinish(
      Plugin plugin,
      FakePlayer fp,
      Location loc,
      Runnable onReady,
      @org.jetbrains.annotations.Nullable Runnable onSkinApplied) {
    String mode = me.bill.fakePlayerPlugin.config.Config.skinMode();

    if ("off".equals(mode) || "disabled".equals(mode)) {
      onReady.run();
      return;
    }

    SkinProfile cached = SkinRepository.get().getSessionCached(fp.getName());
    if (cached != null) {
      fp.setResolvedSkin(cached);
    }

    onReady.run();

    SkinRepository.get()
        .resolve(
            fp.getName(),
            skin -> {
              if (skin == null || !skin.isValid()) return;
              fp.setResolvedSkin(skin);

              Bukkit.getScheduler()
                  .runTaskLater(
                      plugin,
                      () -> {
                        Player body = fp.getPlayer();
                        if (body == null || !body.isOnline()) return;
                        applyPaperSkin(body, skin);
                        if (onSkinApplied != null) onSkinApplied.run();
                      },
                      3L);
            });
  }

  public static void resolveAndFinish(
      Plugin plugin, FakePlayer fp, Location loc, Runnable onReady) {
    resolveAndFinish(plugin, fp, loc, onReady, null);
  }

  public static void applyResolvedSkin(
      Plugin plugin, FakePlayer fp, org.bukkit.entity.Entity body) {
    if (!(body instanceof Player player)) return;
    applyPaperSkin(player, fp.getResolvedSkin());
  }

  private static void applyPaperSkin(Player bot, SkinProfile skin) {
    if (skin == null || !skin.isValid()) return;
    try {
      var profile = bot.getPlayerProfile();
      profile.removeProperty("textures");
      profile.setProperty(
          new com.destroystokyo.paper.profile.ProfileProperty(
              "textures", skin.getValue(), skin.getSignature() != null ? skin.getSignature() : ""));
      bot.setPlayerProfile(profile);
      Config.debugSkin(
          "FakePlayerBody: paper skin applied to " + bot.getName() + " (" + skin.getSource() + ")");

      NmsPlayerSpawner.forceAllSkinParts(bot);
    } catch (Exception e) {
      FppLogger.debug(
          "FakePlayerBody: paper skin apply failed for " + bot.getName() + ": " + e.getMessage());
    }
  }

  public static void removeNametag(FakePlayer fp) {}

  public static org.bukkit.entity.Entity spawnNametag(
      FakePlayer fp, org.bukkit.entity.Entity body) {

    return null;
  }

  public static void removeOrphanedNametags(String reason) {}

  public static void removeOrphanedBodies(String reason) {}
}
