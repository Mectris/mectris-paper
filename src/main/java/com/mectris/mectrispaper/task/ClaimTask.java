package com.mectris.mectrispaper.task;

import com.mectris.mectrispaper.Mectris;
import com.mectris.mectrispaper.identifiers.ConfigKeys;
import com.mectris.mectrispaper.service.MectrisService;

import java.util.UUID;
import java.util.function.Consumer;

public class ClaimTask implements Runnable {

    private final MectrisService service;
    private final String token;
    private final Consumer<String> onSuccess;
    private final Consumer<String> onFailure;

    public ClaimTask(MectrisService service, String token, Consumer<String> onSuccess, Consumer<String> onFailure) {
        this.service = service;
        this.token = token;
        this.onSuccess = onSuccess;
        this.onFailure = onFailure;
    }

    @Override
    public void run() {
        try {
            var installationId = UUID.randomUUID();
            var response = service.getApiClient().completeClaim(token, installationId);

            service.getStorage().saveCredentials(
                    response.apiKey(),
                    UUID.fromString(response.serverId()),
                    UUID.fromString(response.installationId())
            );

            ConfigKeys.CLAIM_TOKEN.set("");

            Mectris.getScheduler().runTask(() -> {
                service.startMetricsReporting(response.apiKey(), UUID.fromString(response.installationId()));
                onSuccess.accept(response.serverId());
            });
        } catch (Exception e) {
            onFailure.accept(e.getMessage());
        }
    }
}
