package me.bill.fakePlayerPlugin.ai;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import me.bill.fakePlayerPlugin.FakePlayerPlugin;
import me.bill.fakePlayerPlugin.config.Config;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class BotConversationManager {

  private final FakePlayerPlugin plugin;
  private final AIProviderRegistry aiRegistry;

  private final Map<UUID, Map<UUID, Deque<AIProvider.ChatMessage>>> conversations =
      new ConcurrentHashMap<>();

  private final Map<UUID, String> botPersonalities = new ConcurrentHashMap<>();

  private final Map<UUID, String> botPersonalityNames = new ConcurrentHashMap<>();

  private final Map<String, Long> lastResponseTimes = new ConcurrentHashMap<>();

  public BotConversationManager(FakePlayerPlugin plugin, AIProviderRegistry aiRegistry) {
    this.plugin = plugin;
    this.aiRegistry = aiRegistry;
  }

  public void handleDirectMessage(FakePlayer bot, Player sender, String message) {
    if (!aiRegistry.isAvailable()) {
      if (Config.aiConversationsDebug()) {
        plugin.getLogger().warning("[AI] No provider available — cannot respond to DM");
      }
      return;
    }

    if (!Config.aiConversationsEnabled()) {
      return;
    }

    String key = bot.getUuid() + ":" + sender.getUniqueId();
    long now = System.currentTimeMillis();
    long cooldown = Config.aiConversationsCooldown() * 1000L;
    Long lastResponse = lastResponseTimes.get(key);
    if (lastResponse != null && (now - lastResponse) < cooldown) {
      if (Config.aiConversationsDebug()) {
        plugin
            .getLogger()
            .info("[AI] Rate limit active for " + sender.getName() + " → " + bot.getName());
      }
      return;
    }

    Map<UUID, Deque<AIProvider.ChatMessage>> botConversations =
        conversations.computeIfAbsent(bot.getUuid(), k -> new ConcurrentHashMap<>());
    Deque<AIProvider.ChatMessage> history =
        botConversations.computeIfAbsent(sender.getUniqueId(), k -> new ArrayDeque<>());

    history.addLast(new AIProvider.ChatMessage("user", message));

    int maxHistory = Config.aiConversationsMaxHistory();
    while (history.size() > maxHistory * 2) {
      history.pollFirst();
    }

    List<AIProvider.ChatMessage> messageList = new ArrayList<>(history);

    String personality = resolvePersonality(bot);

    aiRegistry
        .generateResponse(messageList, bot.getName(), personality)
        .thenAccept(
            response -> {
              long delayTicks = 0;
              if (Config.aiTypingDelayEnabled()) {
                double delaySecs =
                    Config.aiTypingDelayBase()
                        + (response.length() * Config.aiTypingDelayPerChar());
                delaySecs = Math.min(delaySecs, Config.aiTypingDelayMax());
                delayTicks = Math.max(1L, Math.round(delaySecs * 20));
              }

              Bukkit.getScheduler()
                  .runTaskLater(
                      plugin,
                      () -> {
                        if (sender.isOnline()) {

                          history.addLast(new AIProvider.ChatMessage("assistant", response));

                          sendBotReply(bot, sender, response);

                          lastResponseTimes.put(key, System.currentTimeMillis());

                          if (Config.aiConversationsDebug()) {
                            plugin
                                .getLogger()
                                .info(
                                    "[AI] "
                                        + bot.getName()
                                        + " → "
                                        + sender.getName()
                                        + ": "
                                        + response);
                          }
                        }
                      },
                      delayTicks);
            })
        .exceptionally(
            ex -> {
              plugin
                  .getLogger()
                  .warning(
                      "[AI] Failed to generate response for "
                          + bot.getName()
                          + ": "
                          + ex.getMessage());
              if (Config.aiConversationsDebug()) {
                ex.printStackTrace();
              }
              return null;
            });
  }

  private void sendBotReply(FakePlayer bot, Player recipient, String message) {
    Player botPlayer = bot.getPlayer();
    if (botPlayer != null && botPlayer.isOnline()) {

      Bukkit.dispatchCommand(
          Bukkit.getConsoleSender(),
          "minecraft:tellraw "
              + recipient.getName()
              + " [{\"text\":\"["
              + bot.getName()
              + " → me] \",\"color\":\"gray\"},"
              + "{\"text\":\""
              + escapeJson(message)
              + "\",\"color\":\"white\"}]");
    }
  }

  public java.util.concurrent.CompletableFuture<String> generatePublicChatReaction(
      FakePlayer bot, String playerName, String playerMessage) {

    if (!aiRegistry.isAvailable() || !Config.aiConversationsEnabled()) {
      return java.util.concurrent.CompletableFuture.failedFuture(
          new IllegalStateException("AI not available or disabled"));
    }

    String rateKey = "pc-react:" + bot.getUuid();
    long now = System.currentTimeMillis();
    int cooldownSec = Config.fakeChatOnPlayerChatAiCooldown();
    Long lastTime = lastResponseTimes.get(rateKey);
    if (lastTime != null && (now - lastTime) < cooldownSec * 1000L) {
      return java.util.concurrent.CompletableFuture.failedFuture(
          new IllegalStateException("Rate limited"));
    }

    lastResponseTimes.put(rateKey, now);

    String personality = resolvePersonality(bot);

    String chatPersonality =
        personality
            + "\n\nCURRENT CONTEXT: You are in a Minecraft server's PUBLIC CHAT."
            + " Player "
            + playerName
            + " just said: \""
            + playerMessage
            + "\"."
            + " React naturally as another player would in game chat."
            + " STRICT: 1-8 words max. No full sentences. No quotes around your response."
            + " Sound like a casual Minecraft player, not an AI assistant."
            + " Lowercase preferred. Optional: 1 typo. Match the energy of what they said.";

    List<AIProvider.ChatMessage> messages =
        List.of(new AIProvider.ChatMessage("user", playerName + ": " + playerMessage));

    if (Config.aiConversationsDebug()) {
      plugin
          .getLogger()
          .info(
              "[AI][pc-react] "
                  + bot.getName()
                  + " reacting to "
                  + playerName
                  + ": "
                  + playerMessage);
    }

    return aiRegistry
        .generateResponse(messages, bot.getName(), chatPersonality)
        .whenComplete(
            (result, err) -> {
              if (err != null) {

                lastResponseTimes.remove(rateKey);
                if (Config.aiConversationsDebug()) {
                  plugin
                      .getLogger()
                      .warning(
                          "[AI][pc-react] Failed for " + bot.getName() + ": " + err.getMessage());
                }
              } else if (Config.aiConversationsDebug()) {
                plugin.getLogger().info("[AI][pc-react] " + bot.getName() + " → " + result);
              }
            });
  }

  private String resolvePersonality(FakePlayer bot) {
    String personality;
    if (botPersonalities.containsKey(bot.getUuid())) {
      personality = botPersonalities.get(bot.getUuid());
    } else if (bot.getAiPersonality() != null) {
      String text =
          plugin.getPersonalityRepository() != null
              ? plugin.getPersonalityRepository().get(bot.getAiPersonality())
              : null;
      personality = (text != null) ? text : Config.aiConversationsDefaultPersonality();
    } else {
      personality = Config.aiConversationsDefaultPersonality();
    }
    return personality.replace("{bot_name}", bot.getName());
  }

  public void setBotPersonality(UUID botUuid, String personality) {
    if (personality == null || personality.isBlank()) {
      botPersonalities.remove(botUuid);
      botPersonalityNames.remove(botUuid);
    } else {
      botPersonalities.put(botUuid, personality);
      botPersonalityNames.remove(botUuid);
    }
  }

  public boolean setBotPersonalityByName(
      UUID botUuid, String personalityName, PersonalityRepository repo) {
    String text = repo.get(personalityName);
    if (text == null) return false;
    botPersonalities.put(botUuid, text);
    botPersonalityNames.put(botUuid, personalityName.toLowerCase(java.util.Locale.ROOT));
    return true;
  }

  public String getBotPersonality(UUID botUuid) {
    return botPersonalities.get(botUuid);
  }

  public String getBotPersonalityName(UUID botUuid) {
    return botPersonalityNames.get(botUuid);
  }

  public void clearConversation(UUID botUuid, UUID playerUuid) {
    Map<UUID, Deque<AIProvider.ChatMessage>> botConvos = conversations.get(botUuid);
    if (botConvos != null) {
      botConvos.remove(playerUuid);
    }
    lastResponseTimes.remove(botUuid + ":" + playerUuid);
  }

  public void clearBotConversations(UUID botUuid) {
    conversations.remove(botUuid);
    botPersonalities.remove(botUuid);
    botPersonalityNames.remove(botUuid);

    lastResponseTimes.keySet().removeIf(key -> key.startsWith(botUuid.toString()));
  }

  public void clearAll() {
    conversations.clear();
    botPersonalities.clear();
    botPersonalityNames.clear();
    lastResponseTimes.clear();
  }

  public int getConversationSize(UUID botUuid, UUID playerUuid) {
    Map<UUID, Deque<AIProvider.ChatMessage>> botConvos = conversations.get(botUuid);
    if (botConvos == null) return 0;
    Deque<AIProvider.ChatMessage> history = botConvos.get(playerUuid);
    return history != null ? history.size() : 0;
  }

  private String escapeJson(String text) {
    return text.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t");
  }
}
