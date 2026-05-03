package me.bill.fakePlayerPlugin.listener;

import me.bill.fakePlayerPlugin.fakeplayer.FakePlayerManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class FakePlayerConnectionMessageListener implements Listener {

  private final FakePlayerManager manager;

  public FakePlayerConnectionMessageListener(FakePlayerManager manager) {
    this.manager = manager;
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onJoin(PlayerJoinEvent event) {
    if (manager.isBotOrPending(event.getPlayer())) event.setJoinMessage(null);
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onQuit(PlayerQuitEvent event) {
    if (manager.isBotOrPending(event.getPlayer())) event.setQuitMessage(null);
  }
}
