package com.mectris.mectrispaper;

import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import com.github.Anon8281.universalScheduler.scheduling.tasks.MyScheduledTask;
import com.mectris.mectrispaper.api.MectrisApiClient;
import com.mectris.mectrispaper.config.MectrisConfig;
import com.mectris.mectrispaper.metrics.MetricsCollector;
import com.mectris.mectrispaper.metrics.MetricsScheduler;
import com.mectris.mectrispaper.storage.MectrisStorage;
import lombok.Getter;
import revxrsal.zapper.ZapperJavaPlugin;

import java.util.UUID;
import java.util.logging.Level;

public final class Mectris extends ZapperJavaPlugin {

    @Getter private static Mectris instance;
    @Getter private static TaskScheduler scheduler;

    private MectrisConfig mectrisConfig;
    private MectrisStorage storage;
    private MectrisApiClient apiClient;
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

        var claimToken = mectrisConfig.getClaimToken();
        if (!claimToken.isBlank()) {
            getLogger().info("Claim token detected, connecting to Mectris...");
            runClaimAsync(claimToken);
        } else {
            startIfConnected();
        }
    }

    private void runClaimAsync(String claimToken) {
        scheduler.runTaskAsynchronously(() -> {
            try {
                var installationId = UUID.randomUUID();
                var response = apiClient.completeClaim(claimToken, installationId);

                storage.saveCredentials(response.apiKey(), UUID.fromString(response.serverId()), UUID.fromString(response.installationId()));
                mectrisConfig.clearClaimToken();

                getLogger().info("Connected! Server ID: " + response.serverId());
                scheduler.runTask(() -> startMetricsTask(response.apiKey(), UUID.fromString(response.installationId())));
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Claim failed — check your token and api-url in config.yml: " + e.getMessage());
            }
        });
    }

    private void startIfConnected() {
        try {
            var creds = storage.loadCredentials();
            if (creds.isPresent()) {
                getLogger().info("Connected. Server ID: " + creds.get().serverId());
                startMetricsTask(creds.get().apiKey(), creds.get().installationId());
            } else {
                getLogger().warning("Not connected — paste your claim token into config.yml and restart.");
            }
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to load credentials", e);
        }
    }

    private void startMetricsTask(String apiKey, UUID installationId) {
        var intervalTicks = (long) mectrisConfig.getMetricsInterval() * 20L;

        metricsTask = scheduler.runTaskTimerAsynchronously(
                new MetricsScheduler(apiClient, new MetricsCollector(), apiKey, installationId),
                intervalTicks,
                intervalTicks
        );

        getLogger().info("Metrics reporting started (every " + mectrisConfig.getMetricsInterval() + "s).");
    }

    @Override
    public void onDisable() {
        if (metricsTask != null) metricsTask.cancel();
        if (storage != null) storage.close();
    }
}