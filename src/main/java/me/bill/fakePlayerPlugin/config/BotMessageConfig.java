package me.bill.fakePlayerPlugin.config;

import me.bill.fakePlayerPlugin.FakePlayerPlugin;
import me.bill.fakePlayerPlugin.util.YamlFileSyncer;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public final class BotMessageConfig {

    private static FakePlayerPlugin plugin;
    private static FileConfiguration cfg;

    private BotMessageConfig() {}

    public static void init(FakePlayerPlugin instance) {
        plugin = instance;
        reload();
    }

    public static void reload() {

        YamlFileSyncer.syncMissingKeys(plugin, "bot-messages.yml", "bot-messages.yml");

        File file = new File(plugin.getDataFolder(), "bot-messages.yml");
        if (!file.exists()) {
            plugin.saveResource("bot-messages.yml", false);
        }

        FileConfiguration disk = YamlConfiguration.loadConfiguration(file);
        disk.options().copyDefaults(true);

        InputStream jarStream = plugin.getResource("bot-messages.yml");
        if (jarStream != null) {
            YamlConfiguration jarDefaults =
                    YamlConfiguration.loadConfiguration(
                            new InputStreamReader(jarStream, StandardCharsets.UTF_8));
            disk.setDefaults(jarDefaults);
        }

        cfg = disk;
        Config.debug(
                "BotMessageConfig loaded: "
                        + file.getPath()
                        + " ("
                        + getMessages().size()
                        + " messages)");
    }

    public static List<String> getMessages() {
        if (cfg == null) return fallback();
        List<String> msgs = cfg.getStringList("messages");
        return msgs.isEmpty() ? fallback() : msgs;
    }

    public static List<String> getReplyMessages() {
        if (cfg == null) return fallbackReplies();
        List<String> msgs = cfg.getStringList("replies");
        return msgs.isEmpty() ? fallbackReplies() : msgs;
    }

    public static List<String> getBurstMessages() {
        if (cfg == null) return fallbackBursts();
        List<String> msgs = cfg.getStringList("burst-followups");
        return msgs.isEmpty() ? fallbackBursts() : msgs;
    }

    public static List<String> getJoinReactionMessages() {
        if (cfg == null) return List.of();
        return cfg.getStringList("join-reactions");
    }

    public static List<String> getDeathReactionMessages() {
        if (cfg == null) return List.of();
        return cfg.getStringList("death-reactions");
    }

    public static List<String> getLeaveReactionMessages() {
        if (cfg == null) return List.of();
        return cfg.getStringList("leave-reactions");
    }

    public static List<String> getKeywordReactionMessages(String key) {
        if (cfg == null || key == null) return List.of();
        return cfg.getStringList("keyword-reactions." + key.toLowerCase());
    }

    public static List<String> getBotToBotReplyMessages() {
        if (cfg == null) return List.of();
        List<String> msgs = cfg.getStringList("bot-to-bot-replies");
        return msgs.isEmpty() ? getReplyMessages() : msgs;
    }

    public static List<String> getAdvancementReactionMessages() {
        if (cfg == null) return List.of();
        return cfg.getStringList("advancement-reactions");
    }

    public static List<String> getFirstJoinReactionMessages() {
        if (cfg == null) return List.of();
        List<String> msgs = cfg.getStringList("first-join-reactions");
        return msgs.isEmpty() ? getJoinReactionMessages() : msgs;
    }

    public static List<String> getKillReactionMessages() {
        if (cfg == null) return List.of();
        return cfg.getStringList("kill-reactions");
    }

    public static List<String> getHighLevelReactionMessages() {
        if (cfg == null) return List.of();
        return cfg.getStringList("high-level-reactions");
    }

    public static List<String> getPlayerChatReactionMessages() {
        if (cfg == null) return List.of();
        List<String> msgs = cfg.getStringList("player-chat-reactions");
        return msgs.isEmpty() ? getReplyMessages() : msgs;
    }

    private static List<String> fallback() {
        return Arrays.asList("gg", "let's go!", "hey everyone", "what's up", "nice server");
    }

    private static List<String> fallbackReplies() {
        return Arrays.asList("yeah?", "sup", "what?", "hm?", "here!");
    }

    private static List<String> fallbackBursts() {
        return Arrays.asList("lol", "fr", "ngl", "no cap", "lmao");
    }
}
