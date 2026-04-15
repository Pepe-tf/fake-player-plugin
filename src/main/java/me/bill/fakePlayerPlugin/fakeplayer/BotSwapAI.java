package me.bill.fakePlayerPlugin.fakeplayer;

import me.bill.fakePlayerPlugin.FakePlayerPlugin;
import me.bill.fakePlayerPlugin.config.Config;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public final class BotSwapAI {

    private final FakePlayerPlugin plugin;
    private final FakePlayerManager manager;

    private static final List<String> FAREWELLS =
            List.of(
                    "gtg",
                    "brb",
                    "bye!",
                    "cya",
                    "gotta go",
                    "peace ✌",
                    "afk for a bit",
                    "dinner time lol",
                    "gonna log off",
                    "see ya",
                    "later",
                    "gn everyone",
                    "logging off",
                    "gotta do stuff",
                    "be back in a bit",
                    "ttyl",
                    "bbs",
                    "ok gtg now",
                    "bye byee",
                    "seeya around",
                    "taking a break",
                    "stepping out for a sec",
                    "gonna grab food",
                    "one sec brb",
                    "lag is killing me lol",
                    "my pc is dying",
                    "phone call brb",
                    "back in a few",
                    "gonna touch grass real quick",
                    "grabbing dinner brb",
                    "afk",
                    "need a break",
                    "stepping away",
                    "be right back",
                    "heading out",
                    "gotta run",
                    "gotta bounce",
                    "logging for a bit",
                    "bbl",
                    "see everyone later",
                    "rq afk",
                    "bio brb",
                    "someone's at the door brb",
                    "gotta eat",
                    "mom called lol",
                    "getting some water brb",
                    "stretching real quick",
                    "hands cramps lol gtg",
                    "gotta feed the dog",
                    "back soon",
                    "short break",
                    "tired lol bye",
                    "need to do smth brb",
                    "stepping away for a min",
                    "cya in a bit");

    private static final List<String> GREETINGS =
            List.of(
                    "back",
                    "hey",
                    "yo",
                    "hi",
                    "wassup",
                    "I'm back",
                    "missed me?",
                    "what did I miss?",
                    "heyy",
                    "yo what's good",
                    "back at it",
                    "ready to grind",
                    "let's go",
                    "sup everyone",
                    "finally back",
                    "ok I'm here",
                    "heyo",
                    "back again lol",
                    "hi everyone",
                    "just reconnected",
                    "connection dropped lol",
                    "stupid internet",
                    "wifi fixed",
                    "alright I'm back",
                    "what happened while I was gone?",
                    "did anything cool happen?",
                    "back from dinner",
                    "ok ready now",
                    "lets get it",
                    "recharged and back",
                    "I return",
                    "back from the void",
                    "here again lol",
                    "connection issues smh",
                    "lol I'm back",
                    "took a bit longer than expected",
                    "that took forever lol",
                    "finally",
                    "ok I'm alive",
                    "back online",
                    "hiii",
                    "yo I'm here",
                    "missed u all",
                    "back from the break",
                    "refreshed and ready",
                    "aight I'm back",
                    "let's do this",
                    "o/ back",
                    "heyyy what did I miss",
                    "took a quick break",
                    "back at it again",
                    "ran into some stuff brb over now");

    public enum Personality {
        CASUAL(1.0),

        GRINDER(1.6),

        SOCIAL(0.65),

        LURKER(2.2),

        ACTIVE(0.45),

        SPORADIC(1.1);

        public final double sessionMultiplier;

        Personality(double mult) {
            sessionMultiplier = mult;
        }
    }

    private final Map<UUID, Integer> sessionTimers = new ConcurrentHashMap<>();

    private final Map<UUID, Integer> packingTimers = new ConcurrentHashMap<>();

    private final Map<UUID, Integer> rejoinTimers = new ConcurrentHashMap<>();

    private final Map<UUID, Personality> personalities = new ConcurrentHashMap<>();

    private final Map<UUID, Integer> swapCounts = new ConcurrentHashMap<>();

    private final AtomicInteger swappedOut = new AtomicInteger(0);

    private final Map<UUID, Long> sessionExpiry = new ConcurrentHashMap<>();

    public BotSwapAI(FakePlayerPlugin plugin, FakePlayerManager manager) {
        this.plugin = plugin;
        this.manager = manager;

        Bukkit.getScheduler().runTaskTimer(plugin, this::syncBotSchedules, 40L, 20L);
    }

    private void syncBotSchedules() {
        if (!Config.swapEnabled()) return;

        for (FakePlayer fp : manager.getActivePlayers()) {
            UUID id = fp.getUuid();

            if (fp.getBotType() == BotType.PVP) continue;
            if (!sessionTimers.containsKey(id)) {
                assignPersonality(id);
                schedule(fp);
            }
        }

        sessionTimers
                .entrySet()
                .removeIf(
                        e -> {
                            if (manager.getByUuid(e.getKey()) == null) {
                                Bukkit.getScheduler().cancelTask(e.getValue());
                                cleanupState(e.getKey());
                                return true;
                            }
                            return false;
                        });
        packingTimers
                .entrySet()
                .removeIf(
                        e -> {
                            if (manager.getByUuid(e.getKey()) == null) {
                                Bukkit.getScheduler().cancelTask(e.getValue());
                                return true;
                            }
                            return false;
                        });
    }

    public void schedule(FakePlayer fp) {
        if (!Config.swapEnabled()) return;
        if (fp.getBotType() == BotType.PVP) return;

        UUID id = fp.getUuid();

        Integer prev = sessionTimers.remove(id);
        if (prev != null) Bukkit.getScheduler().cancelTask(prev);

        assignPersonality(id);
        long delay = sessionDurationTicks(id);
        sessionExpiry.put(id, System.currentTimeMillis() + (delay * 50L));

        int taskId =
                Bukkit.getScheduler()
                        .runTaskLater(
                                plugin,
                                () -> {
                                    sessionTimers.remove(id);
                                    FakePlayer current = manager.getByUuid(id);
                                    if (current != null) doLeave(current);
                                },
                                delay)
                        .getTaskId();

        sessionTimers.put(id, taskId);
        Config.debugSwap(
                "[SwapAI] Session scheduled for "
                        + fp.getName()
                        + " (delay="
                        + (delay / 20)
                        + "s, personality="
                        + personalities.getOrDefault(id, Personality.CASUAL).name().toLowerCase()
                        + ")");
    }

    public void cancel(UUID uuid) {
        Integer tid = sessionTimers.remove(uuid);
        if (tid != null) Bukkit.getScheduler().cancelTask(tid);
        Integer ptid = packingTimers.remove(uuid);
        if (ptid != null) Bukkit.getScheduler().cancelTask(ptid);
        Integer rid = rejoinTimers.remove(uuid);
        if (rid != null) {
            Bukkit.getScheduler().cancelTask(rid);
            swappedOut.decrementAndGet();
        }
        cleanupState(uuid);
    }

    public void cancelAll() {
        sessionTimers.values().forEach(Bukkit.getScheduler()::cancelTask);
        packingTimers.values().forEach(Bukkit.getScheduler()::cancelTask);
        rejoinTimers.values().forEach(Bukkit.getScheduler()::cancelTask);
        sessionTimers.clear();
        packingTimers.clear();
        rejoinTimers.clear();
        personalities.clear();
        swapCounts.clear();
        sessionExpiry.clear();
        swappedOut.set(0);
    }

    public boolean triggerNow(String botName) {
        FakePlayer fp = manager.getByName(botName);
        if (fp == null) return false;
        UUID id = fp.getUuid();
        Integer tid = sessionTimers.remove(id);
        if (tid != null) Bukkit.getScheduler().cancelTask(tid);

        Integer ptid = packingTimers.remove(id);
        if (ptid != null) Bukkit.getScheduler().cancelTask(ptid);
        doLeave(fp);
        return true;
    }

    public int getSwappedOutCount() {
        return swappedOut.get();
    }

    public int getActiveSessionCount() {
        return sessionTimers.size();
    }

    public Set<UUID> getActiveSessions() {
        return Collections.unmodifiableSet(new HashSet<>(sessionTimers.keySet()));
    }

    public long getNextSwapSeconds() {
        long now = System.currentTimeMillis();
        long soonest = Long.MAX_VALUE;
        for (long expiry : sessionExpiry.values()) {
            if (expiry < soonest) soonest = expiry;
        }
        if (soonest == Long.MAX_VALUE) return -1;
        return Math.max(0, (soonest - now) / 1000L);
    }

    public long getSessionExpiry(UUID uuid) {
        return sessionExpiry.getOrDefault(uuid, -1L);
    }

    public String getPersonalityLabel(UUID uuid) {
        Personality p = personalities.get(uuid);
        return p != null ? p.name().toLowerCase() : "unset";
    }

    public int getSwapCount(UUID uuid) {
        return swapCounts.getOrDefault(uuid, 0);
    }

    private void doLeave(FakePlayer fp) {
        if (!Config.swapEnabled()) return;

        int minOnline = Config.swapMinOnline();
        if (minOnline > 0 && manager.getActivePlayers().size() <= minOnline) {
            Config.debugSwap(
                    "[SwapAI] "
                            + fp.getName()
                            + " swap skipped (would go below min-online="
                            + minOnline
                            + "), rescheduling.");
            schedule(fp);
            return;
        }

        int maxOut = Config.swapMaxSwappedOut();
        if (maxOut > 0 && swappedOut.get() >= maxOut) {
            Config.debugSwap(
                    "[SwapAI] "
                            + fp.getName()
                            + " leave skipped (maxSwappedOut="
                            + maxOut
                            + "), rescheduling.");
            schedule(fp);
            return;
        }

        UUID leavingUuid = fp.getUuid();
        Personality p = personalities.getOrDefault(leavingUuid, Personality.CASUAL);
        int newCount = swapCounts.getOrDefault(leavingUuid, 0) + 1;
        Location lastLoc = fp.getLiveLocation();
        String oldName = fp.getName();

        Config.debugSwap(
                "[SwapAI] "
                        + oldName
                        + " starting leave (swap #"
                        + newCount
                        + ", personality="
                        + p.name().toLowerCase()
                        + ")");

        if (Config.swapFarewellChat() && shouldChat()) {
            sendBotChat(fp, randomFrom(FAREWELLS));
        }

        int leaveMin = Config.leaveDelayMin();
        int leaveMax = Math.max(leaveMin, Config.leaveDelayMax());
        long leaveDelayTicks;
        if (leaveMax <= 0) {
            leaveDelayTicks = 0L;
        } else {
            int spread = leaveMax - leaveMin;
            int ticks =
                    leaveMin + (spread > 0 ? ThreadLocalRandom.current().nextInt(spread + 1) : 0);
            leaveDelayTicks = Math.max(1L, (long) ticks);
        }
        long preDeleteDelay = 20L + ThreadLocalRandom.current().nextInt(80) + leaveDelayTicks;

        int packingId =
                Bukkit.getScheduler()
                        .runTaskLater(
                                plugin,
                                () -> {
                                    packingTimers.remove(leavingUuid);

                                    if (!Config.swapEnabled()) return;
                                    if (manager.getByName(oldName) == null) return;

                                    sessionTimers.remove(leavingUuid);
                                    sessionExpiry.remove(leavingUuid);
                                    swappedOut.incrementAndGet();

                                    int absMin = Config.swapAbsenceMin();
                                    int absMax = Math.max(absMin, Config.swapAbsenceMax());
                                    int absSec =
                                            absMin
                                                    + (absMax > absMin
                                                            ? ThreadLocalRandom.current()
                                                                    .nextInt(absMax - absMin + 1)
                                                            : 0);
                                    long absenceTicks = Math.max(40L, (long) absSec * 20L);
                                    long leaveBuffer =
                                            (long)
                                                            Math.max(
                                                                    Config.leaveDelayMin(),
                                                                    Config.leaveDelayMax())
                                                    + 10L;
                                    long totalRejoinDelay = absenceTicks + leaveBuffer;

                                    int rid =
                                            Bukkit.getScheduler()
                                                    .runTaskLater(
                                                            plugin,
                                                            () -> {
                                                                rejoinTimers.remove(leavingUuid);
                                                                doRejoin(
                                                                        leavingUuid,
                                                                        lastLoc,
                                                                        oldName,
                                                                        newCount,
                                                                        p);
                                                            },
                                                            totalRejoinDelay)
                                                    .getTaskId();

                                    Config.debugSwap(
                                            "[SwapAI] "
                                                    + oldName
                                                    + " offline for ~"
                                                    + absSec
                                                    + "s - rejoining in "
                                                    + (totalRejoinDelay / 20)
                                                    + "s.");

                                    manager.delete(oldName);
                                    rejoinTimers.put(leavingUuid, rid);
                                },
                                preDeleteDelay)
                        .getTaskId();

        packingTimers.put(leavingUuid, packingId);
    }

    private void doRejoin(
            UUID leavingUuid, Location loc, String oldName, int newSwapCount, Personality p) {
        swappedOut.decrementAndGet();

        if (!Config.swapEnabled()) return;
        if (loc == null || loc.getWorld() == null) return;

        String customName = null;
        if (Config.swapSameNameOnRejoin() && !manager.isNameUsed(oldName)) {
            customName = oldName;
        }

        Set<UUID> before = manager.getActiveUUIDs();

        int result = manager.spawn(loc, 1, null, customName, true);
        if (result <= 0) {
            Config.debugSwap(
                    "[SwapAI] Rejoin failed for ex-bot '"
                            + oldName
                            + "' (spawn result="
                            + result
                            + ").");

            if (Config.swapRetryRejoin()) {
                long retryTicks = Math.max(400L, (long) Config.swapRetryDelay() * 20L);
                Config.debugSwap(
                        "[SwapAI] Will retry '" + oldName + "' in " + (retryTicks / 20) + "s.");
                swappedOut.incrementAndGet();
                int retryId =
                        Bukkit.getScheduler()
                                .runTaskLater(
                                        plugin,
                                        () -> {
                                            rejoinTimers.remove(leavingUuid);
                                            doRejoin(leavingUuid, loc, oldName, newSwapCount, p);
                                        },
                                        retryTicks)
                                .getTaskId();
                rejoinTimers.put(leavingUuid, retryId);
            }
            return;
        }

        final String resolvedName = customName;
        FakePlayer newBot = resolvedName != null ? manager.getByName(resolvedName) : null;

        if (newBot == null) {

            for (FakePlayer fp : manager.getActivePlayers()) {
                if (!before.contains(fp.getUuid())) {
                    newBot = fp;
                    break;
                }
            }
        }

        if (newBot == null) {
            Config.debugSwap(
                    "[SwapAI] Rejoin: could not find new bot after spawn for '" + oldName + "'");
            return;
        }

        personalities.put(newBot.getUuid(), p);
        swapCounts.put(newBot.getUuid(), newSwapCount);

        Config.debugSwap(
                "[SwapAI] "
                        + newBot.getName()
                        + " rejoined (swap #"
                        + newSwapCount
                        + ", personality="
                        + p.name().toLowerCase()
                        + ")");

        if (Config.swapGreetingChat() && shouldChat()) {
            UUID newId = newBot.getUuid();
            long greetDelay = 20L + ThreadLocalRandom.current().nextInt(60);
            Bukkit.getScheduler()
                    .runTaskLater(
                            plugin,
                            () -> {
                                FakePlayer b = manager.getByUuid(newId);
                                if (b != null) sendBotChat(b, randomFrom(GREETINGS));
                            },
                            greetDelay);
        }

        schedule(newBot);
    }

    private long sessionDurationTicks(UUID botUuid) {
        Personality p = personalities.getOrDefault(botUuid, Personality.CASUAL);
        int minSec = Config.swapSessionMin();
        int maxSec = Math.max(minSec, Config.swapSessionMax());
        int spread = maxSec - minSec;
        int baseSec = minSec + (spread > 0 ? ThreadLocalRandom.current().nextInt(spread + 1) : 0);

        int count = swapCounts.getOrDefault(botUuid, 0);
        double growth = 1.0 + (Math.min(count, 5) * 0.08);

        double sporadic =
                p == Personality.SPORADIC
                        ? 0.6 + ThreadLocalRandom.current().nextDouble() * 0.8
                        : 1.0;
        long ticks = (long) (baseSec * p.sessionMultiplier * growth * sporadic * 20.0);
        return Math.max(200L, ticks);
    }

    private boolean shouldChat() {
        if (!Config.fakeChatEnabled()) return false;
        for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
            if (manager.getByUuid(p.getUniqueId()) == null) return true;
        }

        return !Config.fakeChatRequirePlayer() && ThreadLocalRandom.current().nextDouble() < 0.70;
    }

    private void sendBotChat(FakePlayer bot, String message) {
        org.bukkit.entity.Player entity = bot.getPlayer();
        if (entity == null || !entity.isValid() || !entity.isOnline()) return;
        BotChatAI.dispatchChat(entity, message);
    }

    private void assignPersonality(UUID botUuid) {
        personalities.computeIfAbsent(botUuid, k -> randomPersonality());
    }

    private void cleanupState(UUID uuid) {
        personalities.remove(uuid);
        swapCounts.remove(uuid);
        sessionExpiry.remove(uuid);
    }

    private static Personality randomPersonality() {
        double r = ThreadLocalRandom.current().nextDouble();
        if (r < 0.15) return Personality.GRINDER;
        else if (r < 0.30) return Personality.SOCIAL;
        else if (r < 0.44) return Personality.LURKER;
        else if (r < 0.58) return Personality.ACTIVE;
        else if (r < 0.68) return Personality.SPORADIC;
        else return Personality.CASUAL;
    }

    private static <T> T randomFrom(List<T> list) {
        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }
}
