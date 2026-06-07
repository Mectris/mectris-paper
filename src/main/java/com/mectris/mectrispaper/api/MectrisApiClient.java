package com.mectris.mectrispaper.api;

import com.google.gson.Gson;
import com.mectris.mectrispaper.metrics.PlayerSession;
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

    private static final Gson GSON = new Gson();
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient http;
    private final String apiUrl;

    public MectrisApiClient(String apiUrl) {
        this.apiUrl = apiUrl;
        // Bounded timeouts so a slow/hung backend never blocks the calling thread
        // indefinitely — important because disconnect()/onDisable() send on the main thread.
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public ClaimResponse completeClaim(String claimToken, UUID installationId) throws Exception {
        var body = GSON.toJson(new ClaimRequest(claimToken, installationId.toString()));

        var request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + "/api/v1/claim"))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        var response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new RuntimeException("Claim failed (HTTP " + response.statusCode() + "): " + response.body());

        return GSON.fromJson(response.body(), ClaimResponse.class);
    }

    public void sendMetrics(
            String apiKey,
            @NotNull UUID installationId,
            double tps,
            double mspt,
            int onlinePlayers,
            long usedMemory,
            long maxMemory,
            double cpuUsage,
            int maxPlayers
    ) throws Exception {
        var payload = new MetricsPayload(
                Instant.now().toString(),
                tps, mspt, onlinePlayers, usedMemory,
                cpuUsage >= 0 ? cpuUsage : null,
                maxMemory,
                maxPlayers
        );
        var body = GSON.toJson(payload);

        var request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + "/api/v1/ingest/metrics"))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .header("X-Installation-Id", installationId.toString())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        var response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 202) throw new RuntimeException("Ingest failed (HTTP " + response.statusCode() + ")");
    }

    public void sendServerInfo(
            String apiKey,
            @NotNull UUID installationId,
            String serverSoftware,
            String serverVersion,
            String jvmVersion,
            String osInfo,
            int pluginCount
    ) throws Exception {
        var payload = new ServerInfoPayload(serverSoftware, serverVersion, jvmVersion, osInfo, pluginCount);
        var body = GSON.toJson(payload);

        var request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + "/api/v1/ingest/server-info"))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .header("X-Installation-Id", installationId.toString())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        http.send(request, HttpResponse.BodyHandlers.discarding());
    }

    public void sendPlayerSessions(String apiKey, @NotNull UUID installationId, List<PlayerSession> sessions) throws Exception {
        if (sessions.isEmpty()) return;

        var body = GSON.toJson(sessions);

        var request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + "/api/v1/ingest/player-sessions"))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .header("X-Installation-Id", installationId.toString())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        var response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 202) {
            throw new RuntimeException("Player session ingest failed (HTTP " + response.statusCode() + ")");
        }
    }

    public void sendDisconnect(String apiKey, UUID installationId) throws Exception {
        var request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + "/api/v1/ingest/disconnect"))
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", "Bearer " + apiKey)
                .header("X-Installation-Id", installationId.toString())
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        http.send(request, HttpResponse.BodyHandlers.discarding());
    }

    private record ClaimRequest(String claimToken, String installationId) {}
    public record ClaimResponse(String apiKey, String serverId, String installationId) {}
    private record MetricsPayload(
            String timestamp,
            double tps,
            double mspt,
            int onlinePlayers,
            long usedMemory,
            Double cpuUsage,
            long maxMemory,
            int maxPlayers
    ) {}
    private record ServerInfoPayload(
            String serverSoftware,
            String serverVersion,
            String jvmVersion,
            String osInfo,
            int pluginCount
    ) {}
}
