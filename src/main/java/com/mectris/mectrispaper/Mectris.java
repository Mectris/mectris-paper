package com.mectris.mectrispaper;

import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import com.github.Anon8281.universalScheduler.scheduling.tasks.MyScheduledTask;
import com.mectris.mectrispaper.api.MectrisApiClient;
import com.mectris.mectrispaper.config.MectrisConfig;
import com.mectris.mectrispaper.metrics.MetricsCollector;
import com.mectris.mectrispaper.metrics.MetricsScheduler;
import com.mectris.mectrispaper.metrics.PlayerSessionTracker;
import com.mectris.mectrispaper.storage.MectrisStorage;
import com.mectris.mectrispaper.utils.RegisterUtils;
import lombok.Getter;
import revxrsal.zapper.ZapperJavaPlugin;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;

public final class Mectris extends ZapperJavaPlugin {

    @Getter private static Mectris instance;
    @Getter private static TaskScheduler scheduler;

    @Getter private MectrisConfig mectrisConfig;
    @Getter private MectrisStorage storage;
    @Getter private MectrisApiClient apiClient;
    @Getter private PlayerSessionTracker sessionTracker;
    private MyScheduledTask metricsTask;

    @Override
    public void onLoad() {
        instance = this;
        scheduler = UniversalScheduler.getScheduler(this);
    }

    @Override
    public void onEnable() {
        getDataFolder().mkdirs();

        try {
            mectrisConfig = new MectrisConfig();
            storage = new MectrisStorage();
            apiClient = new MectrisApiClient(mectrisConfig.getApiUrl());
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize Mectris", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        sessionTracker = new PlayerSessionTracker();
        getServer().getPluginManager().registerEvents(sessionTracker, this);

        RegisterUtils.registerCommands();

        var claimToken = mectrisConfig.getClaimToken();
        if (!claimToken.isBlank()) {
            getLogger().info("Claim token detected, connecting to Mectris...");
            claimAsync(claimToken,
                    serverId -> getLogger().info("Connected! Server ID: " + serverId),
                    err      -> getLogger().severe("Claim failed — check your token and api-url: " + err)
            );
        } else {
            startIfConnected();
        }
    }

    public void claimAsync(String token, Consumer<String> onSuccess, Consumer<String> onFailure) {
        scheduler.runTaskAsynchronously(() -> {
            try {
                var installationId = UUID.randomUUID();
                var response = apiClient.completeClaim(token, installationId);

                storage.saveCredentials(response.apiKey(), UUID.fromString(response.serverId()), UUID.fromString(response.installationId()));
                mectrisConfig.clearClaimToken();

                scheduler.runTask(() -> {
                    startMetricsTask(response.apiKey(), UUID.fromString(response.installationId()));
                    onSuccess.accept(response.serverId());
                });
            } catch (Exception e) {
                onFailure.accept(e.getMessage());
            }
        });
    }

    public void disconnect() throws Exception {
        if (metricsTask != null) {
            metricsTask.cancel();
            metricsTask = null;
        }
        try {
            var creds = storage.loadCredentials();
            if (creds.isPresent()) {
                sessionTracker.flushActiveSessions();
                var pending = sessionTracker.drainPending();
                if (!pending.isEmpty()) {
                    apiClient.sendPlayerSessions(creds.get().apiKey(), creds.get().installationId(), pending);
                }
                apiClient.sendDisconnect(creds.get().apiKey(), creds.get().installationId());
            }
        } catch (Exception ignored) {}
        storage.clearCredentials();
    }

    public void reload() throws Exception {
        if (metricsTask != null) {
            metricsTask.cancel();
            metricsTask = null;
        }
        mectrisConfig.reload();
        apiClient = new MectrisApiClient(mectrisConfig.getApiUrl());
        startIfConnected();
    }

    private void startIfConnected() {
        try {
            var creds = storage.loadCredentials();
            if (creds.isPresent()) {
                getLogger().info("Connected. Server ID: " + creds.get().serverId());
                startMetricsTask(creds.get().apiKey(), creds.get().installationId());
            } else {
                getLogger().warning("Not connected — run /mectris claim <token> or paste the token into config.yml and restart.");
            }
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to load credentials", e);
        }
    }

    private void startMetricsTask(String apiKey, UUID installationId) {
        var intervalTicks = (long) mectrisConfig.getMetricsInterval() * 20L;

        metricsTask = scheduler.runTaskTimerAsynchronously(
                new MetricsScheduler(apiClient, new MetricsCollector(), sessionTracker, apiKey, installationId),
                intervalTicks,
                intervalTicks
        );

        getLogger().info("Metrics reporting started (every " + mectrisConfig.getMetricsInterval() + "s).");
    }

    @Override
    public void onDisable() {
        if (metricsTask != null) metricsTask.cancel();

        try {
            if (sessionTracker != null && storage != null) {
                var creds = storage.loadCredentials();
                if (creds.isPresent()) {
                    sessionTracker.flushActiveSessions();
                    var pending = sessionTracker.drainPending();
                    if (!pending.isEmpty()) {
                        apiClient.sendPlayerSessions(creds.get().apiKey(), creds.get().installationId(), pending);
                    }
                }
            }
        } catch (Exception e) {
            getLogger().warning("Failed to flush player sessions on disable: " + e.getMessage());
        }

        if (storage != null) storage.close();
    }
}