package me.bill.fakePlayerPlugin.command;

import me.bill.fakePlayerPlugin.config.Config;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayerManager;
import me.bill.fakePlayerPlugin.lang.Lang;
import me.bill.fakePlayerPlugin.permission.Perm;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code /fpp spawn [amount] [--name <botname>]}
 *
 * <p><b>Admin tier</b> ({@code fpp.spawn}):
 * <ul>
 *   <li>Any amount up to global max (or unlimited with fpp.bypass.maxbots)</li>
 *   <li>{@code --name <name>} flag (requires fpp.spawn.name)</li>
 *   <li>Tab-complete: 1 5 10 15 20 --name</li>
 * </ul>
 *
 * <p><b>User tier</b> ({@code fpp.user.spawn}):
 * <ul>
 *   <li>Up to personal bot limit (fpp.bot.&lt;num&gt; or config user-bot-limit)</li>
 *   <li>No custom names — always "[bot] PlayerName" format</li>
 *   <li>Tab-complete: only "1" (never multi-spawn, never --name)</li>
 * </ul>
 */
public class SpawnCommand implements FppCommand {

    // User-tier only ever sees "1" — multi-spawn is admin-only
    private static final List<String> USER_COUNT_SUGGESTIONS = List.of("1");

    private final FakePlayerManager manager;

    public SpawnCommand(FakePlayerManager manager) {
        this.manager = manager;
    }

    @Override public String getName()        { return "spawn"; }
    @Override public String getDescription() { return "Spawns fake player(s) at your location."; }

    /** Usage adapts to whether the user is admin or user-tier. */
    @Override public String getUsage() {
        // This is shown in help — generic because we can't know the viewer here.
        // HelpCommand renders it per-sender so we keep it generic.
        return "[amount] [--name <name>]";
    }

    /**
     * No single permission node — dual-tier (fpp.spawn OR fpp.user.spawn).
     * @return null — permission is checked inside execute() and canUse().
     */
    @Override public String getPermission() { return null; }

    /**
     * Visible/usable if the sender has EITHER the admin spawn OR the user spawn perm.
     * This controls tab-complete visibility and help-menu filtering.
     */
    @Override
    public boolean canUse(CommandSender sender) {
        return Perm.has(sender, Perm.SPAWN) || Perm.has(sender, Perm.USER_SPAWN);
    }

    // ── Execute ──────────────────────────────────────────────────────────────

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Lang.get("player-only"));
            return true;
        }

        // Check admin tier first (includes OP check) — if they have fpp.spawn OR are OP, treat as admin
        boolean isAdmin = Perm.hasOrOp(sender, Perm.SPAWN);
        boolean isUser  = !isAdmin && Perm.has(sender, Perm.USER_SPAWN);

        if (!isAdmin && !isUser) {
            sender.sendMessage(Lang.get("no-permission"));
            return true;
        }

        int    amount     = 1;
        String customName = null;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            if (arg.equalsIgnoreCase("--name") || arg.equalsIgnoreCase("-n")) {
                // --name is admin-only (fpp.spawn.name)
                if (!isAdmin || Perm.missing(sender, Perm.SPAWN_CUSTOM_NAME)) {
                    sender.sendMessage(Lang.get("no-permission"));
                    return true;
                }
                if (i + 1 < args.length) {
                    customName = args[++i];
                } else {
                    sender.sendMessage(Lang.get("spawn-invalid"));
                    return true;
                }

            } else {
                try {
                    int parsed = Integer.parseInt(arg);
                    if (parsed < 1) {
                        sender.sendMessage(Lang.get("spawn-invalid"));
                        return true;
                    }
                    // Multi-spawn is admin-only
                    if (parsed > 1 && !isAdmin) {
                        sender.sendMessage(Lang.get("no-permission"));
                        return true;
                    }
                    amount = parsed;
                } catch (NumberFormatException e) {
                    sender.sendMessage(Lang.get("spawn-invalid"));
                    return true;
                }
            }
        }

        // ── Cooldown check (user and admin — bypass with fpp.bypass.cooldown) ──
        if (!Perm.has(sender, Perm.BYPASS_COOLDOWN) && manager.isOnCooldown(player.getUniqueId())) {
            long remaining = manager.getRemainingCooldown(player.getUniqueId());
            sender.sendMessage(Lang.get("spawn-cooldown", "seconds", String.valueOf(remaining)));
            return true;
        }

        // ── User-tier: enforce personal bot limit & use display-name format ──
        if (!isAdmin) {
            int perPlayerLimit = Perm.resolveUserBotLimit(sender);
            if (perPlayerLimit < 0) perPlayerLimit = Config.userBotLimit();

            int currentOwned = manager.getBotsOwnedBy(player.getUniqueId()).size();
            int available    = perPlayerLimit - currentOwned;

            if (available <= 0) {
                sender.sendMessage(Lang.get("spawn-user-limit-reached",
                        "limit", String.valueOf(perPlayerLimit)));
                return true;
            }
            amount = Math.min(amount, available);

            int spawned = manager.spawnUserBot(player.getLocation(), amount, player,
                    Perm.has(sender, Perm.BYPASS_MAX));
            if (spawned <= 0) {
                sender.sendMessage(Lang.get("spawn-max-reached", "max", String.valueOf(Config.maxBots())));
            } else {
                manager.recordSpawnCooldown(player.getUniqueId());
                sender.sendMessage(Lang.get("spawn-success",
                        "count", String.valueOf(spawned),
                        "total", String.valueOf(manager.getCount())));
            }
            return true;
        }

        // ── Admin tier: pool name or custom --name ───────────────────────────
        int spawned = manager.spawn(player.getLocation(), amount, player, customName,
                Perm.has(sender, Perm.BYPASS_MAX));
        switch (spawned) {
            case -2 -> sender.sendMessage(Lang.get("spawn-invalid-name"));
            case -1 -> sender.sendMessage(Lang.get("spawn-max-reached",
                    "max", String.valueOf(Config.maxBots())));
            case  0 -> sender.sendMessage(Lang.get("spawn-name-taken",
                    "name", customName));
            default -> {
                manager.recordSpawnCooldown(player.getUniqueId());
                sender.sendMessage(Lang.get("spawn-success",
                        "count", String.valueOf(spawned),
                        "total", String.valueOf(manager.getCount())));
            }
        }
        return true;
    }

    // ── Tab-complete ──────────────────────────────────────────────────────────

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        boolean isAdmin = Perm.has(sender, Perm.SPAWN);
        boolean isUser  = Perm.has(sender, Perm.USER_SPAWN);

        // No permission at all — return nothing (CommandManager already guards this,
        // but be defensive in case tabComplete is called directly)
        if (!isAdmin && !isUser) return List.of();

        String current = args.length > 0 ? args[args.length - 1] : "";
        String prev    = args.length >= 2 ? args[args.length - 2] : "";

        // ── After --name: suggest available pool names (admin only) ──────────
        if (prev.equalsIgnoreCase("--name") || prev.equalsIgnoreCase("-n")) {
            if (!isAdmin) return List.of(); // user-tier never gets --name
            return suggestNames(current);
        }

        // ── Scan args already typed ──────────────────────────────────────────
        boolean hasCount   = false;
        boolean hasName    = false;
        boolean expectName = false;

        for (int i = 0; i < args.length - 1; i++) {
            String a = args[i];
            if (a.equalsIgnoreCase("--name") || a.equalsIgnoreCase("-n")) {
                expectName = true;
            } else if (expectName) {
                hasName    = true;
                expectName = false;
            } else {
                try { Integer.parseInt(a); hasCount = true; }
                catch (NumberFormatException ignored) {}
            }
        }

        List<String> suggestions = new ArrayList<>();

        // ── Count presets ────────────────────────────────────────────────────
        if (!hasCount) {
            List<String> presets = isAdmin ? Config.spawnCountPresetsAdmin() : USER_COUNT_SUGGESTIONS;
            presets.stream()
                    .filter(n -> n.startsWith(current))
                    .forEach(suggestions::add);
        }

        // ── --name flag (admin + fpp.spawn.name only, not yet typed) ─────────
        if (isAdmin && Perm.has(sender, Perm.SPAWN_CUSTOM_NAME)
                && !hasName && "--name".startsWith(current.toLowerCase())) {
            suggestions.add("--name");
        }

        return suggestions;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<String> suggestNames(String prefix) {
        String lower  = prefix.toLowerCase();
        List<String> active = manager.getActiveNames();
        List<String> pool   = Config.namePool();
        List<String> result = new ArrayList<>();
        for (String name : pool) {
            if (name.toLowerCase().startsWith(lower) && !active.contains(name)) {
                result.add(name);
                if (result.size() >= 50) break;
            }
        }
        return result;
    }
}

