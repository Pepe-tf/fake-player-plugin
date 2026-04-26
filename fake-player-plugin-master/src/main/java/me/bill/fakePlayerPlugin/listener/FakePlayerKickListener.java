package me.bill.fakePlayerPlugin.listener;

import me.bill.fakePlayerPlugin.fakeplayer.FakePlayerManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;

public class FakePlayerKickListener implements Listener {

  private final FakePlayerManager manager;

  public FakePlayerKickListener(FakePlayerManager manager) {
    this.manager = manager;
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onPlayerKick(PlayerKickEvent event) {

    if (manager.getByName(event.getPlayer().getName()) == null) return;

    Component reason = event.reason();
    String plainReason =
        reason == null ? "" : PlainTextComponentSerializer.plainText().serialize(reason);

    if (plainReason.isEmpty()) return;

    event.setCancelled(true);
  }
}
