package com.mectris.mectrispaper.metrics;

import com.mectris.mectrispaper.Mectris;
import com.mectris.mectrispaper.api.MectrisApiClient;

import java.util.UUID;

public class MetricsScheduler implements Runnable {

    private final MectrisApiClient apiClient;
    private final MetricsCollector collector;
    private final PlayerSessionTracker sessionTracker;
    private final String apiKey;
    private final UUID installationId;

    public MetricsScheduler(
            MectrisApiClient apiClient,
            MetricsCollector collector,
            PlayerSessionTracker sessionTracker,
            String apiKey,
            UUID installationId
    ) {
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
                    collector.getUsedMemory()
            );
        } catch (Exception e) {
            Mectris.getInstance().getLogger().warning("Failed to send metrics: " + e.getMessage());
        }

        try {
            var pending = sessionTracker.drainPending();
            if (!pending.isEmpty()) {
                apiClient.sendPlayerSessions(apiKey, installationId, pending);
            }
        } catch (Exception e) {
            Mectris.getInstance().getLogger().warning("Failed to send player sessions: " + e.getMessage());
        }
    }
}