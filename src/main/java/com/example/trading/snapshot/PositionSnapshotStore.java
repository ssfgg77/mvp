package com.example.trading.snapshot;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PositionSnapshotStore {

  public record Snapshot(Instant timestamp, Object positionsPayload) {}

  private final long ttlMs;
  private final Map<String, Snapshot> store = new ConcurrentHashMap<>();

  public PositionSnapshotStore(@Value("${app.snapshot.ttl-ms:300000}") long ttlMs) {
    this.ttlMs = ttlMs;
  }

  private String key(long userId, long accountId) {
    return userId + ":" + accountId;
  }

  public void put(long userId, long accountId, Object positionsPayload) {
    store.put(key(userId, accountId), new Snapshot(Instant.now(), positionsPayload));
  }

  public Snapshot get(long userId, long accountId) {
    return store.get(key(userId, accountId));
  }

  public boolean isStale(Snapshot snapshot) {
    if (snapshot == null) return true;
    long age = Instant.now().toEpochMilli() - snapshot.timestamp().toEpochMilli();
    return age > ttlMs;
  }
}
