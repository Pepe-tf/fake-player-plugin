package me.bill.fakePlayerPlugin.listener;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayer;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayerManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Adjusts the server-list ping response so fake players are counted in the
 * online player total and shown in the hover sample (up to 12, shuffled).
 *
 * <p>Vanilla Minecraft only shows the first 12 entries in the hover list,
 * so we shuffle the combined list to give real and fake players equal
 * visibility across multiple refreshes.
 */
public class ServerListListener implements Listener {

    /** Vanilla client shows at most 12 names in the server-list hover. */
    private static final int MAX_SAMPLE = 12;

    private final FakePlayerManager manager;

    public ServerListListener(FakePlayerManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPing(PaperServerListPingEvent event) {
        int botCount = manager.getCount();
        if (botCount == 0) return;

        // Bump the reported online count
        event.setNumPlayers(event.getNumPlayers() + botCount);

        // Build a combined sample list then shuffle + cap at MAX_SAMPLE
        List<PaperServerListPingEvent.ListedPlayerInfo> sample = new ArrayList<>(event.getListedPlayers());

        for (FakePlayer fp : manager.getActivePlayers()) {
            try {
                sample.add(new PaperServerListPingEvent.ListedPlayerInfo(fp.getName(), fp.getUuid()));
            } catch (Exception ignored) {}
        }

        Collections.shuffle(sample);
        if (sample.size() > MAX_SAMPLE) sample = sample.subList(0, MAX_SAMPLE);

        event.getListedPlayers().clear();
        event.getListedPlayers().addAll(sample);
    }
}

