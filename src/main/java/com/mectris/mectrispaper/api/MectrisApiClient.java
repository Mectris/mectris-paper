package com.mectris.mectrispaper.api;

import com.google.gson.Gson;
import com.mectris.mectrispaper.models.PlayerSession;
import com.mectris.mectrispaper.models.api.ClaimRequest;
import com.mectris.mectrispaper.models.api.ClaimResponse;
import com.mectris.mectrispaper.models.api.MetricsPayload;
import com.mectris.mectrispaper.models.api.ServerInfoPayload;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class MectrisApiClient {

    public static final String BASE_URL = "https://api.mectris.com";

    private static final Gson GSON = new Gson();
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final String JSON = "application/json";

    private final HttpClient http;

    public MectrisApiClient() {
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public ClaimResponse completeClaim(String claimToken, @NotNull UUID installationId) throws Exception {
        var request = jsonRequest("/api/v1/claim")
                .POST(jsonBody(new ClaimRequest(claimToken, installationId.toString())))
                .build();

        var response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Claim failed (HTTP " + response.statusCode() + "): " + response.body());
        }

        return GSON.fromJson(response.body(), ClaimResponse.class);
    }

    public void sendMetrics(String apiKey, @NotNull UUID installationId, double tps, double mspt, int onlinePlayers, long usedMemory, long maxMemory, double cpuUsage, int maxPlayers) throws Exception {
        var payload = new MetricsPayload(
                Instant.now().toString(),
                tps, mspt, onlinePlayers, usedMemory,
                cpuUsage >= 0 ? cpuUsage : null,
                maxMemory,
                maxPlayers
        );

        var request = authedJsonRequest("/api/v1/ingest/metrics", apiKey, installationId)
                .POST(jsonBody(payload))
                .build();

        sendExpecting(request, 202, "Ingest");
    }

    public void sendServerInfo(String apiKey, @NotNull UUID installationId, String serverSoftware, String serverVersion, String jvmVersion, String osInfo, int pluginCount) throws Exception {
        var payload = new ServerInfoPayload(serverSoftware, serverVersion, jvmVersion, osInfo, pluginCount);
        var request = authedJsonRequest("/api/v1/ingest/server-info", apiKey, installationId)
                .POST(jsonBody(payload))
                .build();

        http.send(request, HttpResponse.BodyHandlers.discarding());
    }

    public void sendPlayerSessions(String apiKey, @NotNull UUID installationId, @NotNull List<PlayerSession> sessions) throws Exception {
        if (sessions.isEmpty()) return;

        var request = authedJsonRequest("/api/v1/ingest/player-sessions", apiKey, installationId)
                .POST(jsonBody(sessions))
                .build();

        sendExpecting(request, 202, "Player session ingest");
    }

    public void sendDisconnect(String apiKey, UUID installationId) throws Exception {
        var request = authedRequest("/api/v1/ingest/disconnect", apiKey, installationId)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        http.send(request, HttpResponse.BodyHandlers.discarding());
    }

    private HttpRequest.Builder baseRequest(String path) {
        return HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .timeout(REQUEST_TIMEOUT);
    }

    private HttpRequest.Builder jsonRequest(String path) {
        return baseRequest(path).header("Content-Type", JSON);
    }

    private HttpRequest.Builder authedRequest(String path, String apiKey, @NotNull UUID installationId) {
        return baseRequest(path)
                .header("Authorization", "Bearer " + apiKey)
                .header("X-Installation-Id", installationId.toString());
    }

    private HttpRequest.Builder authedJsonRequest(String path, String apiKey, UUID installationId) {
        return authedRequest(path, apiKey, installationId).header("Content-Type", JSON);
    }

    @NotNull
    @Contract("_ -> new")
    private static HttpRequest.BodyPublisher jsonBody(Object payload) {
        return HttpRequest.BodyPublishers.ofString(GSON.toJson(payload));
    }

    private void sendExpecting(HttpRequest request, int expectedStatus, String action) throws Exception {
        var response = http.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != expectedStatus) {
            throw new RuntimeException(action + " failed (HTTP " + response.statusCode() + ")");
        }
    }
}