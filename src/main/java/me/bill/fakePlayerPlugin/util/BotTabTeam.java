package me.bill.fakePlayerPlugin.util;

import me.bill.fakePlayerPlugin.config.Config;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayer;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public final class BotTabTeam {

    public static final String TEAM_NAME = "~fpp";

    private final Set<String> botEntries = new HashSet<>();

    public BotTabTeam() {}

    public void init() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            ensureTeamExists(p);
        }

        applyCollisionRule(Config.bodyPushable());
        FppLogger.debug(
                "[BotTabTeam] Initialized team '"
                        + TEAM_NAME
                        + "' on "
                        + Bukkit.getOnlinePlayers().size()
                        + " player scoreboard(s).");
    }

    private void ensureTeamExists(Player player) {
        if (player == null) return;
        try {
            Scoreboard board = player.getScoreboard();
            if (board.getTeam(TEAM_NAME) == null) {
                Team t = board.registerNewTeam(TEAM_NAME);

                applyCollisionRuleToTeam(t);
                Config.debug(
                        "[BotTabTeam] Created team '"
                                + TEAM_NAME
                                + "' on "
                                + player.getName()
                                + "'s scoreboard");
            }
        } catch (Exception e) {
            Config.debug(
                    "[BotTabTeam] Error creating team for "
                            + player.getName()
                            + ": "
                            + e.getMessage());
        }
    }

    public void applyCollisionRule(boolean pushable) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            Team team = p.getScoreboard().getTeam(TEAM_NAME);
            if (team != null) applyCollisionRuleToTeam(team);
        }
        FppLogger.debug(
                "[BotTabTeam] Collision rule set to "
                        + (pushable ? "ALWAYS" : "NEVER")
                        + " on "
                        + Bukkit.getOnlinePlayers().size()
                        + " scoreboard(s).");
    }

    private static void applyCollisionRuleToTeam(Team team) {
        boolean pushable = Config.bodyPushable();
        team.setOption(
                Team.Option.COLLISION_RULE,
                pushable ? Team.OptionStatus.ALWAYS : Team.OptionStatus.NEVER);
    }

    public void addBot(FakePlayer fp) {
        if (!Config.tabListEnabled()) return;
        String entry = fp.getPacketProfileName();
        botEntries.add(entry);
        for (Player p : Bukkit.getOnlinePlayers()) {
            ensureTeamExists(p);
            Team team = p.getScoreboard().getTeam(TEAM_NAME);
            if (team != null && !team.hasEntry(entry)) team.addEntry(entry);
        }
        Config.debug(
                "[BotTabTeam] + '"
                        + entry
                        + "' to "
                        + Bukkit.getOnlinePlayers().size()
                        + " scoreboards");
    }

    public void removeBot(FakePlayer fp) {
        removeEntry(fp.getPacketProfileName());
    }

    public void removeEntry(String entry) {
        if (entry == null) return;
        botEntries.remove(entry);
        for (Player p : Bukkit.getOnlinePlayers()) {
            Team team = p.getScoreboard().getTeam(TEAM_NAME);
            if (team != null && team.hasEntry(entry)) team.removeEntry(entry);
        }
        Config.debug(
                "[BotTabTeam] - '"
                        + entry
                        + "' from "
                        + Bukkit.getOnlinePlayers().size()
                        + " scoreboards");
    }

    public void rebuild(Collection<FakePlayer> activeBots) {
        botEntries.clear();
        for (Player p : Bukkit.getOnlinePlayers()) {
            ensureTeamExists(p);
            Team team = p.getScoreboard().getTeam(TEAM_NAME);
            if (team == null) continue;
            for (String e : new ArrayList<>(team.getEntries())) team.removeEntry(e);
            if (Config.tabListEnabled()) {
                for (FakePlayer fp : activeBots) {

                    String name = fp.getPacketProfileName();
                    botEntries.add(name);
                    team.addEntry(name);
                }
            }
        }
        FppLogger.debug(
                "[BotTabTeam] Rebuilt with "
                        + activeBots.size()
                        + " bot(s) on "
                        + Bukkit.getOnlinePlayers().size()
                        + " scoreboard(s)"
                        + (Config.tabListEnabled()
                                ? ""
                                : " (tab-list disabled - entries not added)")
                        + ".");
    }

    public void clearAll() {
        botEntries.clear();
        for (Player p : Bukkit.getOnlinePlayers()) {
            Team team = p.getScoreboard().getTeam(TEAM_NAME);
            if (team == null) continue;
            for (String e : new ArrayList<>(team.getEntries())) team.removeEntry(e);
        }
        FppLogger.debug(
                "[BotTabTeam] Cleared all entries from "
                        + Bukkit.getOnlinePlayers().size()
                        + " scoreboards");
    }

    public void destroy() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            Team team = p.getScoreboard().getTeam(TEAM_NAME);
            if (team != null) {
                try {
                    team.unregister();
                } catch (Exception ignored) {
                }
            }
        }
        botEntries.clear();
        FppLogger.debug("[BotTabTeam] Team '" + TEAM_NAME + "' unregistered from all scoreboards.");
    }

    public void syncToPlayer(Player player) {
        if (!Config.tabListEnabled() || botEntries.isEmpty()) return;
        try {
            ensureTeamExists(player);
            Team team = player.getScoreboard().getTeam(TEAM_NAME);
            if (team == null) return;

            applyCollisionRuleToTeam(team);
            int added = 0;
            for (String entry : botEntries) {
                if (!team.hasEntry(entry)) {
                    team.addEntry(entry);
                    added++;
                }
            }
            Config.debug(
                    "[BotTabTeam] Synced "
                            + added
                            + "/"
                            + botEntries.size()
                            + " bot(s) to "
                            + player.getName()
                            + "'s scoreboard");
        } catch (Exception e) {
            Config.debug(
                    "[BotTabTeam] syncToPlayer failed for "
                            + player.getName()
                            + ": "
                            + e.getMessage());
        }
    }

    public boolean isActive() {
        return !botEntries.isEmpty();
    }

    public void dumpTeamState() {
        FppLogger.info("═══════════════════════════════════════════════");
        FppLogger.info("[BotTabTeam] TEAM STATE DUMP - tracked entries: " + botEntries.size());
        if (!botEntries.isEmpty()) FppLogger.info("  Entries: " + String.join(", ", botEntries));
        for (Player p : Bukkit.getOnlinePlayers()) {
            Scoreboard board = p.getScoreboard();
            Team team = board.getTeam(TEAM_NAME);
            FppLogger.info(
                    "Player "
                            + p.getName()
                            + " | board="
                            + board.getClass().getSimpleName()
                            + " | team="
                            + (team == null
                                    ? "NOT FOUND"
                                    : "EXISTS size="
                                            + team.getSize()
                                            + " entries="
                                            + (team.getEntries().isEmpty()
                                                    ? "(none)"
                                                    : String.join(", ", team.getEntries()))));
        }
        FppLogger.info("═══════════════════════════════════════════════");
    }
}
