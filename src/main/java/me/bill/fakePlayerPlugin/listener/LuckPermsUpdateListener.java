package me.bill.fakePlayerPlugin.listener;

import me.bill.fakePlayerPlugin.FakePlayerPlugin;
import me.bill.fakePlayerPlugin.config.Config;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayerManager;
import me.bill.fakePlayerPlugin.util.LuckPermsHelper;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.event.EventSubscription;
import net.luckperms.api.event.group.GroupDataRecalculateEvent;
import net.luckperms.api.event.node.NodeAddEvent;
import net.luckperms.api.event.node.NodeRemoveEvent;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;

/**
 * Listens for LuckPerms data changes and automatically updates bot prefixes
 * without requiring a respawn or server restart.
 * <p>
 * Handles:
 * <ul>
 *   <li>Group prefix/suffix/weight changes</li>
 *   <li>User group changes (when a spawner's group changes)</li>
 *   <li>Permission node additions/removals that affect prefixes</li>
 * </ul>
 * <p>
 * When a relevant change is detected:
 * <ol>
 *   <li>Invalidates the LuckPerms cache</li>
 *   <li>Schedules a sync task to update all active bot display names</li>
 *   <li>Updates bot nametags and tab-list entries in real-time</li>
 * </ol>
 */
public final class LuckPermsUpdateListener {

    private final FakePlayerPlugin plugin;
    private final FakePlayerManager fakePlayerManager;
    private final EventBus eventBus;
    private final List<EventSubscription<?>> subscriptions = new ArrayList<>();

    public LuckPermsUpdateListener(FakePlayerPlugin plugin, FakePlayerManager fakePlayerManager, LuckPerms luckPerms) {
        this.plugin = plugin;
        this.fakePlayerManager = fakePlayerManager;
        this.eventBus = luckPerms.getEventBus();
    }

    /**
     * Registers all LuckPerms event handlers.
     * Safe to call multiple times — only registers once.
     */
    public void register() {
        if (!subscriptions.isEmpty()) return;

        // Group data recalculate (fires when group meta changes)
        subscriptions.add(eventBus.subscribe(plugin, GroupDataRecalculateEvent.class, this::onGroupDataRecalculate));

        // User data recalculate (fires when user's groups change)
        subscriptions.add(eventBus.subscribe(plugin, UserDataRecalculateEvent.class, this::onUserDataRecalculate));

        // Node add/remove (fires when prefix nodes are added/removed)
        subscriptions.add(eventBus.subscribe(plugin, NodeAddEvent.class, this::onNodeAdd));
        subscriptions.add(eventBus.subscribe(plugin, NodeRemoveEvent.class, this::onNodeRemove));

        Config.debug("[LP-Auto-Update] Registered " + subscriptions.size() + " LuckPerms event listeners");
    }

    /**
     * Unregisters all event handlers.
     */
    public void unregister() {
        if (subscriptions.isEmpty()) return;
        
        for (EventSubscription<?> subscription : subscriptions) {
            subscription.close();
        }
        subscriptions.clear();
        
        Config.debug("[LP-Auto-Update] Unregistered LuckPerms event listeners");
    }

    // ── Event Handlers ────────────────────────────────────────────────────────

    private void onGroupDataRecalculate(GroupDataRecalculateEvent event) {
        String groupName = event.getGroup().getName();
        Config.debug("[LP-Auto-Update] Group '" + groupName + "' data recalculated");
        scheduleUpdate("group " + groupName + " changed");
    }

    private void onUserDataRecalculate(UserDataRecalculateEvent event) {
        String userName = event.getUser().getUsername();
        Config.debug("[LP-Auto-Update] User '" + (userName != null ? userName : event.getUser().getUniqueId()) 
                + "' data recalculated");
        scheduleUpdate("user group changed");
    }

    private void onNodeAdd(NodeAddEvent event) {
        if (isRelevantNode(event)) {
            Config.debug("[LP-Auto-Update] Prefix/meta node added");
            scheduleUpdate("prefix node added");
        }
    }

    private void onNodeRemove(NodeRemoveEvent event) {
        if (isRelevantNode(event)) {
            Config.debug("[LP-Auto-Update] Prefix/meta node removed");
            scheduleUpdate("prefix node removed");
        }
    }

    // ── Update Logic ──────────────────────────────────────────────────────────

    /**
     * Schedules a sync task to update all bot display names.
     * Debounced to avoid multiple rapid updates.
     */
    private void scheduleUpdate(String reason) {
        // Invalidate cache immediately
        LuckPermsHelper.invalidateCache();

        // Schedule update on next tick (gives LP time to propagate changes)
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                Config.debug("[LP-Auto-Update] Updating all bots (" + reason + ")");
                int updated = fakePlayerManager.updateAllBotPrefixes();
                if (updated > 0) {
                    Config.debug("[LP-Auto-Update] Updated " + updated + " bot(s)");
                }
            } catch (Exception e) {
                Config.debug("[LP-Auto-Update] Error updating bots: " + e.getMessage());
            }
        });
    }

    /**
     * Checks if a node event is relevant (prefix, suffix, or meta nodes).
     */
    private boolean isRelevantNode(Object event) {
        try {
            if (event instanceof NodeAddEvent addEvent) {
                String key = addEvent.getNode().getKey();
                return key.startsWith("prefix.") || key.startsWith("suffix.") || key.startsWith("meta.");
            } else if (event instanceof NodeRemoveEvent removeEvent) {
                String key = removeEvent.getNode().getKey();
                return key.startsWith("prefix.") || key.startsWith("suffix.") || key.startsWith("meta.");
            }
        } catch (Exception ignored) {}
        return false;
    }
}


