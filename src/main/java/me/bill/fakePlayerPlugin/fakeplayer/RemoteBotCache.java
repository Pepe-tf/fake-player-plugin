package me.bill.fakePlayerPlugin.fakeplayer;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RemoteBotCache {

  private final ConcurrentHashMap<UUID, RemoteBotEntry> entries = new ConcurrentHashMap<>();

  public void add(RemoteBotEntry entry) {
    entries.put(entry.uuid(), entry);
  }

  public void remove(UUID uuid) {
    entries.remove(uuid);
  }

  public void removeAllFromServer(String serverId) {
    entries.values().removeIf(e -> serverId.equals(e.serverId()));
  }

  public void clear() {
    entries.clear();
  }

  public RemoteBotEntry get(UUID uuid) {
    return entries.get(uuid);
  }

  public Collection<RemoteBotEntry> getAll() {
    return Collections.unmodifiableCollection(entries.values());
  }

  public int count() {
    return entries.size();
  }
}
