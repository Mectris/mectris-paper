package com.mectris.mectrispaper.task;

import com.mectris.mectrispaper.api.MectrisApiClient;
import com.mectris.mectrispaper.collector.MetricsCollector;
import com.mectris.mectrispaper.collector.PlayerSessionCollector;
import com.mectris.mectrispaper.utils.LoggerUtils;

import java.util.UUID;

public class MetricsTask implements Runnable {

    private final MectrisApiClient apiClient;
    private final MetricsCollector collector;
    private final PlayerSessionCollector sessionTracker;
    private final String apiKey;
    private final UUID installationId;

    public MetricsTask(MectrisApiClient apiClient, MetricsCollector collector, PlayerSessionCollector sessionTracker, String apiKey, UUID installationId) {
        this.apiClient = apiClient;
        this.collector = collector;
        this.sessionTracker = sessionTracker;
        this.apiKey = apiKey;
        this.installationId = installationId;
    }

    @Override
    public void run() {
        try {
            apiClient.sendMetrics(
                    apiKey,
                    installationId,
                    collector.getTps(),
                    collector.getMspt(),
                    collector.getOnlinePlayers(),
                    collector.getUsedMemory(),
                    collector.getMaxMemory(),
                    collector.getCpuUsage(),
                    collector.getMaxPlayers()
            );
        } catch (Exception e) {
            LoggerUtils.warn("Failed to send metrics: {}", e.getMessage());
        }

        try {
            var pending = sessionTracker.drainPending();
            if (!pending.isEmpty()) {
                apiClient.sendPlayerSessions(apiKey, installationId, pending);
            }
        } catch (Exception e) {
            LoggerUtils.warn("Failed to send player sessions: {}", e.getMessage());
        }
    }
}
