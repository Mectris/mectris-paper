package com.mectris.mectrispaper.task;

import com.mectris.mectrispaper.api.MectrisApiClient;
import com.mectris.mectrispaper.collector.ServerInfoCollector;
import com.mectris.mectrispaper.utils.LoggerUtils;

import java.util.UUID;

public class ServerInfoTask implements Runnable {

    private final MectrisApiClient apiClient;
    private final String apiKey;
    private final UUID installationId;

    public ServerInfoTask(MectrisApiClient apiClient, String apiKey, UUID installationId) {
        this.apiClient = apiClient;
        this.apiKey = apiKey;
        this.installationId = installationId;
    }

    @Override
    public void run() {
        try {
            var info = new ServerInfoCollector();
            apiClient.sendServerInfo(
                    apiKey, installationId,
                    info.getServerSoftware(),
                    info.getServerVersion(),
                    info.getJvmVersion(),
                    info.getOsInfo(),
                    info.getPluginCount()
            );
        } catch (Exception e) {
            LoggerUtils.warn("Failed to send server info: {}", e.getMessage());
        }
    }
}
