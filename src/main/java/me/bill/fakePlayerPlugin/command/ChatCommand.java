package me.bill.fakePlayerPlugin.command;

import me.bill.fakePlayerPlugin.FakePlayerPlugin;
import me.bill.fakePlayerPlugin.config.Config;
import me.bill.fakePlayerPlugin.lang.Lang;
import me.bill.fakePlayerPlugin.permission.Perm;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.List;

/**
 * {@code /fpp chat [on|off]} — toggles bot fake-chat on or off at runtime.
 *
 * <p>The change is written back to {@code config.yml} immediately so it
 * survives a {@code /fpp reload} and a server restart.
 */
public class ChatCommand implements FppCommand {

    private final FakePlayerPlugin plugin;

    public ChatCommand(FakePlayerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String getName()        { return "chat"; }
    @Override public String getUsage()       { return "[on|off]"; }
    @Override public String getDescription() { return "Toggles or shows bot fake-chat status."; }
    @Override public String getPermission()  { return Perm.CHAT; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            // No argument — show current state and toggle
            boolean current = Config.fakeChatEnabled();
            sender.sendMessage(Lang.get(current ? "chat-status-on" : "chat-status-off"));
            return true;
        }

        boolean enable;
        switch (args[0].toLowerCase()) {
            case "on",  "true",  "yes", "1" -> enable = true;
            case "off", "false", "no",  "0" -> enable = false;
            case "status" -> {
                boolean on = Config.fakeChatEnabled();
                sender.sendMessage(Lang.get(on ? "chat-status-on" : "chat-status-off"));
                return true;
            }
            default -> {
                sender.sendMessage(Lang.get("chat-invalid"));
                return true;
            }
        }

        // Write to in-memory config AND disk, then reload
        plugin.getConfig().set("fake-chat.enabled", enable);
        plugin.saveConfig();
        Config.reload();

        sender.sendMessage(Lang.get(enable ? "chat-enabled" : "chat-disabled"));
        Config.debug("fake-chat.enabled set to " + enable + " by " + sender.getName());
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("on", "off", "status");
        }
        return List.of();
    }
}
