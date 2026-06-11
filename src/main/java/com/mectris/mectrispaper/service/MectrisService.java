package com.mectris.mectrispaper.service;

import com.github.Anon8281.universalScheduler.scheduling.tasks.MyScheduledTask;
import com.mectris.mectrispaper.Mectris;
import com.mectris.mectrispaper.api.MectrisApiClient;
import com.mectris.mectrispaper.collector.MetricsCollector;
import com.mectris.mectrispaper.collector.PlayerSessionCollector;
import com.mectris.mectrispaper.models.Credentials;
import com.mectris.mectrispaper.storage.MectrisStorage;
import com.mectris.mectrispaper.task.ClaimTask;
import com.mectris.mectrispaper.task.MetricsTask;
import com.mectris.mectrispaper.task.ServerInfoTask;
import com.mectris.mectrispaper.utils.LoggerUtils;
import lombok.Getter;

import java.util.UUID;
import java.util.function.Consumer;

public class MectrisService {

    public static final int METRICS_INTERVAL_SECONDS = 180;

    @Getter private final MectrisStorage storage;
    private final PlayerSessionCollector sessionTracker;

    @Getter private MectrisApiClient apiClient;
    private MyScheduledTask metricsTask;

    public MectrisService(MectrisStorage storage, MectrisApiClient apiClient, PlayerSessionCollector sessionTracker) {
        this.storage = storage;
        this.apiClient = apiClient;
        this.sessionTracker = sessionTracker;
    }

    public void startIfConnected() {
        try {
            var creds = storage.loadCredentials();
            if (creds.isPresent()) {
                LoggerUtils.info("Connected. Server ID: {}", creds.get().serverId());
                startMetricsReporting(creds.get().apiKey(), creds.get().installationId());
            } else {
                LoggerUtils.warn("Not connected — run /mectris claim <token> or paste the token into config.yml and restart.");
            }
        } catch (Exception e) {
            LoggerUtils.error("Failed to load credentials", e);
        }
    }

    public void claimAsync(String token, Consumer<String> onSuccess, Consumer<String> onFailure) {
        Mectris.getScheduler().runTaskAsynchronously(new ClaimTask(this, token, onSuccess, onFailure));
    }

    public void startMetricsReporting(String apiKey, UUID installationId) {
        var intervalTicks = (long) METRICS_INTERVAL_SECONDS * 20L;

        metricsTask = Mectris.getScheduler().runTaskTimerAsynchronously(
                new MetricsTask(apiClient, new MetricsCollector(), sessionTracker, apiKey, installationId),
                intervalTicks,
                intervalTicks
        );

        LoggerUtils.info("Metrics reporting started (every {}s).", METRICS_INTERVAL_SECONDS);

        Mectris.getScheduler().runTaskAsynchronously(new ServerInfoTask(apiClient, apiKey, installationId));
    }

    public void disconnect() {
        cancelMetricsTask();

        try {
            var creds = storage.loadCredentials();
            if (creds.isPresent()) {
                flushSessions(creds.get());
                apiClient.sendDisconnect(creds.get().apiKey(), creds.get().installationId());
            }
        } catch (Exception ignored) {}

        storage.clearCredentials();
    }

    public void reload() {
        cancelMetricsTask();
        apiClient = new MectrisApiClient();
        startIfConnected();
    }

    public void shutdown() {
        cancelMetricsTask();

        try {
            var creds = storage.loadCredentials();
            if (creds.isPresent()) {
                flushSessions(creds.get());
            }
        } catch (Exception e) {
            LoggerUtils.warn("Failed to flush player sessions on disable: {}", e.getMessage());
        }
    }

    private void cancelMetricsTask() {
        if (metricsTask != null) {
            metricsTask.cancel();
            metricsTask = null;
        }
    }

    private void flushSessions(Credentials creds) throws Exception {
        sessionTracker.flushActiveSessions();
        var pending = sessionTracker.drainPending();
        if (!pending.isEmpty()) {
            apiClient.sendPlayerSessions(creds.apiKey(), creds.installationId(), pending);
        }
    }
}