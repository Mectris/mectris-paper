package com.mectris.mectrispaper.metrics;

import org.bukkit.Bukkit;
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

    private record Active(Instant joinedAt, String name) {}

    private final ConcurrentHashMap<UUID, Active> activeSessions = new ConcurrentHashMap<>();
    private final Queue<PlayerSession> pendingSessions = new ConcurrentLinkedQueue<>();

    /** Capture players who were already online when the plugin loaded (e.g. after /reload). */
    public void seedOnlinePlayers() {
        for (var player : Bukkit.getOnlinePlayers()) {
            activeSessions.putIfAbsent(player.getUniqueId(), new Active(Instant.now(), player.getName()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        activeSessions.put(player.getUniqueId(), new Active(Instant.now(), player.getName()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        var active = activeSessions.remove(event.getPlayer().getUniqueId());
        if (active != null) {
            enqueueSession(event.getPlayer().getUniqueId(), active);
        }
    }

    /** Flush all currently active sessions (call on plugin disable / disconnect). */
    public void flushActiveSessions() {
        activeSessions.forEach(this::enqueueSession);
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

    private void enqueueSession(UUID uuid, Active active) {
        var leftAt = Instant.now();
        var duration = (int) (leftAt.getEpochSecond() - active.joinedAt().getEpochSecond());
        pendingSessions.add(new PlayerSession(
                uuid.toString(),
                active.name(),
                active.joinedAt().toString(),
                leftAt.toString(),
                duration
        ));
    }
}
