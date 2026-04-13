package me.bill.fakePlayerPlugin.listener;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import java.util.*;
import java.util.stream.Collectors;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayer;
import me.bill.fakePlayerPlugin.fakeplayer.FakePlayerManager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ServerListListener implements Listener {

  private static final int MAX_SAMPLE = 12;

  private final FakePlayerManager manager;

  public ServerListListener(FakePlayerManager manager) {
    this.manager = manager;
  }

  @EventHandler(priority = EventPriority.NORMAL)
  public void onPing(PaperServerListPingEvent event) {

    Map<UUID, String> botNames =
        manager.getActivePlayers().stream()
            .collect(Collectors.toMap(FakePlayer::getUuid, FakePlayer::getName));

    List<PaperServerListPingEvent.ListedPlayerInfo> freshSample = new ArrayList<>();

    for (Map.Entry<UUID, String> e : botNames.entrySet()) {
      String name = e.getValue();
      if (name != null && !name.isBlank()) {
        freshSample.add(new PaperServerListPingEvent.ListedPlayerInfo(name, e.getKey()));
      }
    }

    for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
      if (botNames.containsKey(p.getUniqueId())) continue;
      String name = p.getName();
      if (!name.isBlank()) {
        freshSample.add(new PaperServerListPingEvent.ListedPlayerInfo(name, p.getUniqueId()));
      }
    }

    if (freshSample.isEmpty()) return;

    Collections.shuffle(freshSample);
    if (freshSample.size() > MAX_SAMPLE) freshSample = freshSample.subList(0, MAX_SAMPLE);

    List<PaperServerListPingEvent.ListedPlayerInfo> listed = event.getListedPlayers();
    listed.clear();
    listed.addAll(freshSample);
  }
}
