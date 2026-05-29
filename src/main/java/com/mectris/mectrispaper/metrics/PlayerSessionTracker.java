package com.mectris.mectrispaper.metrics;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PlayerSessionTracker implements Listener {

    private final ConcurrentHashMap<UUID, Instant> activeSessions = new ConcurrentHashMap<>();
    private final Queue<PlayerSession> pendingSessions = new ConcurrentLinkedQueue<>();

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        activeSessions.put(event.getPlayer().getUniqueId(), Instant.now());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        var joinTime = activeSessions.remove(event.getPlayer().getUniqueId());
        if (joinTime != null) {
            enqueueSession(event.getPlayer().getUniqueId(), joinTime);
        }
    }

    /** Flush all currently active sessions (call on plugin disable / disconnect). */
    public void flushActiveSessions() {
        activeSessions.forEach((uuid, joinTime) -> enqueueSession(uuid, joinTime));
        activeSessions.clear();
    }

    /** Drain and return all pending completed sessions, clearing the queue. */
    public List<PlayerSession> drainPending() {
        var batch = new ArrayList<PlayerSession>();
        PlayerSession session;
        while ((session = pendingSessions.poll()) != null) {
            batch.add(session);
        }
        return batch;
    }

    private void enqueueSession(UUID uuid, Instant joinTime) {
        var leftAt = Instant.now();
        var duration = (int) (leftAt.getEpochSecond() - joinTime.getEpochSecond());
        pendingSessions.add(new PlayerSession(
                uuid.toString(),
                joinTime.toString(),
                leftAt.toString(),
                duration
        ));
    }
}