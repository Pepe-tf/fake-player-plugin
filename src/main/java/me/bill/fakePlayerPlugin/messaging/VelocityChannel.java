package me.bill.fakePlayerPlugin.messaging;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import me.bill.fakePlayerPlugin.FakePlayerPlugin;
import me.bill.fakePlayerPlugin.config.Config;
import me.bill.fakePlayerPlugin.fakeplayer.BotBroadcast;
import me.bill.fakePlayerPlugin.fakeplayer.BotChatAI;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayer;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayerManager;
import me.bill.fakePlayerPlugin.fakeplayer.PacketHelper;
import me.bill.fakePlayerPlugin.fakeplayer.RemoteBotCache;
import me.bill.fakePlayerPlugin.fakeplayer.RemoteBotEntry;
import me.bill.fakePlayerPlugin.util.FppLogger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

public final class VelocityChannel implements PluginMessageListener {

  public static final String SUBCHANNEL_BOT_SPAWN = "BOT_SPAWN";
  public static final String SUBCHANNEL_BOT_DESPAWN = "BOT_DESPAWN";
  public static final String SUBCHANNEL_BOT_UPDATE = "BOT_UPDATE";
  public static final String SUBCHANNEL_CHAT = "CHAT";
  public static final String SUBCHANNEL_ALERT = "ALERT";
  public static final String SUBCHANNEL_JOIN = "JOIN";
  public static final String SUBCHANNEL_LEAVE = "LEAVE";
  public static final String SUBCHANNEL_SYNC = "SYNC";

  public static final String CHANNEL = "fpp:main";

  private static final String BUNGEE_CHANNEL = "BungeeCord";

  private final FakePlayerPlugin plugin;

  @SuppressWarnings("unused")
  private final FakePlayerManager manager;

  private final java.util.Set<String> recentIds = ConcurrentHashMap.newKeySet();

  public VelocityChannel(FakePlayerPlugin plugin, FakePlayerManager manager) {
    this.plugin = plugin;
    this.manager = manager;
  }

  private String generateAndTrackId() {
    String id = System.currentTimeMillis() + "-" + ThreadLocalRandom.current().nextInt(1_000_000);
    recentIds.add(id);
    Bukkit.getScheduler().runTaskLater(plugin, () -> recentIds.remove(id), 100L);
    return id;
  }

  private boolean isDuplicate(String msgId, String originServer) {
    return recentIds.contains(msgId) || Config.serverId().equals(originServer);
  }

  private void trackIncoming(String msgId) {
    recentIds.add(msgId);
    Bukkit.getScheduler().runTaskLater(plugin, () -> recentIds.remove(msgId), 100L);
  }

  public void sendPluginMessage(String subchannel, String... data) {
    var online = Bukkit.getOnlinePlayers();
    if (online.isEmpty()) {
      Config.debugNetwork("[VelocityChannel] dropped (no players online): " + subchannel);
      return;
    }
    try {
      ByteArrayOutputStream innerBuf = new ByteArrayOutputStream();
      DataOutputStream innerOut = new DataOutputStream(innerBuf);
      innerOut.writeUTF(subchannel);
      for (String f : data) innerOut.writeUTF(f != null ? f : "");
      byte[] innerBytes = innerBuf.toByteArray();

      ByteArrayOutputStream outerBuf = new ByteArrayOutputStream();
      DataOutputStream outerOut = new DataOutputStream(outerBuf);
      outerOut.writeUTF("Forward");
      outerOut.writeUTF("ALL");
      outerOut.writeUTF(CHANNEL);
      outerOut.writeShort(innerBytes.length);
      outerOut.write(innerBytes);

      online.iterator().next().sendPluginMessage(plugin, BUNGEE_CHANNEL, outerBuf.toByteArray());
      Config.debugNetwork(
          "[VelocityChannel] Sent '" + subchannel + "' (" + innerBytes.length + " bytes).");
    } catch (IOException e) {
      FppLogger.warn("[VelocityChannel] send failed: " + e.getMessage());
    }
  }

  public void broadcastBotSpawn(FakePlayer fp) {
    if (!Config.isNetworkMode()) return;
    me.bill.fakePlayerPlugin.fakeplayer.SkinProfile skin = fp.getResolvedSkin();
    String skinValue = (skin != null) ? skin.getValue() : "";
    String skinSignature = (skin != null) ? skin.getSignature() : "";
    String msgId = generateAndTrackId();
    sendPluginMessage(
        SUBCHANNEL_BOT_SPAWN,
        msgId,
        Config.serverId(),
        fp.getUuid().toString(),
        fp.getName(),
        fp.getDisplayName(),
        fp.getPacketProfileName(),
        skinValue,
        skinSignature);
    Config.debugNetwork(
        "[VelocityChannel] BOT_SPAWN sent for '"
            + fp.getName()
            + "' (skin="
            + !skinValue.isEmpty()
            + ").");
  }

  public void broadcastBotDisplayNameUpdate(FakePlayer fp) {
    if (!Config.isNetworkMode()) return;
    String msgId = generateAndTrackId();
    sendPluginMessage(
        SUBCHANNEL_BOT_UPDATE,
        msgId,
        Config.serverId(),
        fp.getUuid().toString(),
        fp.getDisplayName());
    Config.debugNetwork("[VelocityChannel] BOT_UPDATE sent for '" + fp.getName() + "'.");
  }

  public void broadcastConfigUpdated(String fileName) {
    if (!Config.isNetworkMode()) return;
    String msgId = generateAndTrackId();
    sendPluginMessage(SUBCHANNEL_SYNC, msgId, Config.serverId(), "config_updated", fileName);
    Config.debugNetwork("[VelocityChannel] SYNC/config_updated sent for '" + fileName + "'.");
  }

  public void broadcastBotDespawn(UUID uuid) {
    if (!Config.isNetworkMode()) return;
    String msgId = generateAndTrackId();
    sendPluginMessage(SUBCHANNEL_BOT_DESPAWN, msgId, Config.serverId(), uuid.toString());
    Config.debugNetwork("[VelocityChannel] BOT_DESPAWN sent for " + uuid + ".");
  }

  public void sendChatToNetwork(
      String botName, String botDisplayName, String message, String prefix, String suffix) {
    if (!Config.isNetworkMode()) return;
    String msgId = generateAndTrackId();
    sendPluginMessage(SUBCHANNEL_CHAT, msgId, botName, botDisplayName, message, prefix, suffix);
  }

  public void broadcastJoinToNetwork(FakePlayer fp) {
    if (!Config.isNetworkMode()) return;
    String msgId = generateAndTrackId();
    sendPluginMessage(SUBCHANNEL_JOIN, msgId, fp.getDisplayName(), Config.serverId());
  }

  public void broadcastLeaveToNetwork(String displayName) {
    if (!Config.isNetworkMode()) return;
    String msgId = generateAndTrackId();
    sendPluginMessage(SUBCHANNEL_LEAVE, msgId, displayName, Config.serverId());
  }

  public void broadcastGlobalAlert(String message) {
    String msgId = generateAndTrackId();
    broadcastAlertLocally(message);
    sendPluginMessage(SUBCHANNEL_ALERT, msgId, message);
    Config.debugNetwork("[VelocityChannel] Global alert sent (id=" + msgId + ").");
  }

  @Override
  public void onPluginMessageReceived(String channel, Player player, byte[] message) {
    if (!CHANNEL.equals(channel)) return;
    try {
      DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));
      String subchannel = in.readUTF();
      Config.debugNetwork("[VelocityChannel] Recv '" + subchannel + "' via " + player.getName());
      switch (subchannel) {
        case SUBCHANNEL_BOT_SPAWN -> handleBotSpawn(in);
        case SUBCHANNEL_BOT_DESPAWN -> handleBotDespawn(in);
        case SUBCHANNEL_BOT_UPDATE -> handleBotUpdate(in);
        case SUBCHANNEL_CHAT -> handleChat(in);
        case SUBCHANNEL_ALERT -> handleAlert(in);
        case SUBCHANNEL_JOIN -> handleJoin(in);
        case SUBCHANNEL_LEAVE -> handleLeave(in);
        case SUBCHANNEL_SYNC -> handleSync(in);
        default -> FppLogger.warn("[VelocityChannel] Unknown subchannel: '" + subchannel + "'.");
      }
    } catch (IOException e) {
      FppLogger.warn(
          "[VelocityChannel] Parse error from " + player.getName() + ": " + e.getMessage());
    }
  }

  private void handleBotSpawn(DataInputStream in) throws IOException {
    String msgId = in.readUTF();
    String originServer = in.readUTF();
    UUID uuid = UUID.fromString(in.readUTF());
    String name = in.readUTF();
    String displayName = in.readUTF();
    String packetName = in.readUTF();
    String skinValue = in.readUTF();
    String skinSignature = in.readUTF();

    if (isDuplicate(msgId, originServer)) {
      Config.debugNetwork("[VelocityChannel] BOT_SPAWN echo suppressed: " + name);
      return;
    }
    trackIncoming(msgId);

    Config.debugNetwork("[VelocityChannel] BOT_SPAWN '" + name + "' from '" + originServer + "'.");

    String safePacketName = (packetName == null || packetName.isBlank()) ? name : packetName;

    RemoteBotEntry entry =
        new RemoteBotEntry(
            originServer, uuid, name, displayName, safePacketName, skinValue, skinSignature);

    RemoteBotCache cache = plugin.getRemoteBotCache();
    if (cache != null) cache.add(entry);

    if (Config.tabListEnabled()) {
      for (Player online : Bukkit.getOnlinePlayers()) {
        PacketHelper.sendTabListAddRaw(
            online, uuid, safePacketName, displayName, skinValue, skinSignature);
      }
    }
  }

  private void handleBotDespawn(DataInputStream in) throws IOException {
    String msgId = in.readUTF();
    String originServer = in.readUTF();
    UUID uuid = UUID.fromString(in.readUTF());

    if (isDuplicate(msgId, originServer)) {
      Config.debugNetwork("[VelocityChannel] BOT_DESPAWN echo suppressed: " + uuid);
      return;
    }
    trackIncoming(msgId);

    Config.debugNetwork("[VelocityChannel] BOT_DESPAWN " + uuid + " from '" + originServer + "'.");

    RemoteBotCache cache = plugin.getRemoteBotCache();
    if (cache != null) cache.remove(uuid);

    for (Player online : Bukkit.getOnlinePlayers()) {
      PacketHelper.sendTabListRemoveByUuid(online, uuid);
    }
  }

  private void handleChat(DataInputStream in) throws IOException {
    String msgId = in.readUTF();
    String botName = in.readUTF();
    String botDisplayName = in.readUTF();
    String message = in.readUTF();
    String prefix = in.readUTF();
    String suffix = in.readUTF();

    if (recentIds.contains(msgId)) {
      Config.debugNetwork("[VelocityChannel] CHAT echo suppressed.");
      return;
    }
    BotChatAI.broadcastRemote(botName, botDisplayName, message, prefix, suffix);
  }

  private void handleAlert(DataInputStream in) throws IOException {
    String msgId = in.readUTF();
    String message = in.readUTF();

    if (recentIds.contains(msgId)) {
      Config.debugNetwork("[VelocityChannel] ALERT echo suppressed.");
      return;
    }
    trackIncoming(msgId);
    broadcastAlertLocally(message);
  }

  private void handleJoin(DataInputStream in) throws IOException {
    String msgId = in.readUTF();
    String displayName = in.readUTF();
    String originServer = in.readUTF();

    if (isDuplicate(msgId, originServer)) {
      Config.debugNetwork("[VelocityChannel] JOIN echo suppressed.");
      return;
    }
    trackIncoming(msgId);
    if (!Config.joinMessage()) return;
    BotBroadcast.broadcastJoinByDisplayName(displayName);
  }

  private void handleLeave(DataInputStream in) throws IOException {
    String msgId = in.readUTF();
    String displayName = in.readUTF();
    String originServer = in.readUTF();

    if (isDuplicate(msgId, originServer)) {
      Config.debugNetwork("[VelocityChannel] LEAVE echo suppressed.");
      return;
    }
    trackIncoming(msgId);
    if (!Config.leaveMessage()) return;
    BotBroadcast.broadcastLeaveByDisplayName(displayName);
  }

  private void handleBotUpdate(DataInputStream in) throws IOException {
    String msgId = in.readUTF();
    String originServer = in.readUTF();
    UUID uuid = UUID.fromString(in.readUTF());
    String newDisplayName = in.readUTF();

    if (isDuplicate(msgId, originServer)) {
      Config.debugNetwork("[VelocityChannel] BOT_UPDATE echo suppressed: " + uuid);
      return;
    }
    trackIncoming(msgId);

    Config.debugNetwork(
        "[VelocityChannel] BOT_UPDATE "
            + uuid
            + " displayName='"
            + newDisplayName
            + "' from '"
            + originServer
            + "'.");

    RemoteBotCache cache = plugin.getRemoteBotCache();
    if (cache == null) return;

    RemoteBotEntry existing = cache.get(uuid);
    if (existing == null) return;

    RemoteBotEntry updated =
        new RemoteBotEntry(
            existing.serverId(),
            existing.uuid(),
            existing.name(),
            newDisplayName,
            existing.packetProfileName(),
            existing.skinValue(),
            existing.skinSignature());
    cache.add(updated);

    if (Config.tabListEnabled()) {
      for (Player online : Bukkit.getOnlinePlayers()) {
        PacketHelper.sendTabListDisplayNameUpdate(online, uuid, newDisplayName);
      }
    }
  }

  private void handleSync(DataInputStream in) throws IOException {
    String msgId = in.readUTF();
    String originServer = in.readUTF();
    String key = in.readUTF();
    String value = in.readUTF();

    if (isDuplicate(msgId, originServer)) {
      Config.debugNetwork("[VelocityChannel] SYNC echo suppressed.");
      return;
    }
    trackIncoming(msgId);

    Config.debugNetwork(
        "[VelocityChannel] SYNC - " + key + "='" + value + "' from '" + originServer + "'.");

    if ("config_updated".equals(key) && Config.configSyncMode().equalsIgnoreCase("AUTO_PULL")) {
      var csm = plugin.getConfigSyncManager();
      if (csm != null) {

        Bukkit.getScheduler()
            .runTaskAsynchronously(
                plugin,
                () -> {
                  boolean pulled = csm.pull(value, false);
                  if (pulled) {
                    FppLogger.info(
                        "[ConfigSync] Reactive pull applied for '"
                            + value
                            + "' (pushed by "
                            + originServer
                            + ").");

                    Bukkit.getScheduler().runTask(plugin, () -> reloadSubsystemForFile(value));
                  }
                });
      }
    }
  }

  private void reloadSubsystemForFile(String fileName) {
    switch (fileName) {
      case "config.yml" -> {
        me.bill.fakePlayerPlugin.config.Config.reload();
        Config.debugConfigSync("[ConfigSync] config.yml reloaded after reactive pull.");
      }
      case "bot-names.yml" -> {
        me.bill.fakePlayerPlugin.config.BotNameConfig.reload();
        Config.debugConfigSync("[ConfigSync] bot-names.yml reloaded after reactive pull.");
      }
      case "bot-messages.yml" -> {
        me.bill.fakePlayerPlugin.config.BotMessageConfig.reload();
        Config.debugConfigSync("[ConfigSync] bot-messages.yml reloaded after reactive pull.");
      }
      case "language/en.yml" -> {
        me.bill.fakePlayerPlugin.lang.Lang.reload();
        Config.debugConfigSync("[ConfigSync] language/en.yml reloaded after reactive pull.");
      }
      default ->
          Config.debugConfigSync("[ConfigSync] Unknown file for reactive reload: " + fileName);
    }
  }

  private void broadcastAlertLocally(String message) {
    net.kyori.adventure.text.Component line =
        me.bill.fakePlayerPlugin.lang.Lang.get("alert-received", "message", message);
    Bukkit.getServer().broadcast(line);
  }
}
