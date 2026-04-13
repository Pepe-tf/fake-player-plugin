package me.bill.fakePlayerPlugin.listener;

import me.bill.fakePlayerPlugin.FakePlayerPlugin;
import me.bill.fakePlayerPlugin.ai.BotConversationManager;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayer;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayerManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public final class BotMessageListener implements Listener {

  private final FakePlayerPlugin plugin;
  private final FakePlayerManager botManager;
  private final BotConversationManager conversationManager;

  public BotMessageListener(
      FakePlayerPlugin plugin,
      FakePlayerManager botManager,
      BotConversationManager conversationManager) {
    this.plugin = plugin;
    this.botManager = botManager;
    this.conversationManager = conversationManager;
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
    Player sender = event.getPlayer();
    String message = event.getMessage();

    if (botManager.getByUuid(sender.getUniqueId()) != null) {
      return;
    }

    String[] parts = message.split("\\s+", 3);
    if (parts.length < 3) return;

    String command = parts[0].toLowerCase().substring(1);

    if (!isDirectMessageCommand(command)) {
      return;
    }

    String targetName = parts[1];
    String messageContent = parts[2];

    FakePlayer targetBot = botManager.getByName(targetName);
    if (targetBot == null) {
      return;
    }

    event.setCancelled(true);

    conversationManager.handleDirectMessage(targetBot, sender, messageContent);

    sender.sendMessage("§7[me → " + targetBot.getName() + "] §f" + messageContent);
  }

  private boolean isDirectMessageCommand(String command) {
    return switch (command) {
      case "msg",
          "tell",
          "whisper",
          "w",
          "m",
          "t",
          "pm",
          "dm",
          "message",
          "emsg",
          "etell",
          "ewhisper" ->
          true;
      default -> false;
    };
  }
}
