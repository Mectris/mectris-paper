package com.mectris.mectrispaper.api;

import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.UUID;

public class MectrisApiClient {

    private static final Gson GSON = new Gson();

    private final HttpClient http;
    private final String apiUrl;

    public MectrisApiClient(String apiUrl) {
        this.apiUrl = apiUrl;
        this.http = HttpClient.newHttpClient();
    }

    public ClaimResponse completeClaim(String claimToken, UUID installationId) throws Exception {
        var body = GSON.toJson(new ClaimRequest(claimToken, installationId.toString()));

        var request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + "/api/v1/claim"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        var response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new RuntimeException("Claim failed (HTTP " + response.statusCode() + "): " + response.body());

        return GSON.fromJson(response.body(), ClaimResponse.class);
    }

    public void sendMetrics(String apiKey, @NotNull UUID installationId, double tps, double mspt, int onlinePlayers, long usedMemory) throws Exception {
        var payload = new MetricsPayload(Instant.now().toString(), tps, mspt, onlinePlayers, usedMemory);
        var body = GSON.toJson(payload);

        var request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + "/api/v1/ingest/metrics"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .header("X-Installation-Id", installationId.toString())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        var response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 202) throw new RuntimeException("Ingest failed (HTTP " + response.statusCode() + ")");
    }

    public void sendDisconnect(String apiKey, UUID installationId) throws Exception {
        var request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + "/api/v1/ingest/disconnect"))
                .header("Authorization", "Bearer " + apiKey)
                .header("X-Installation-Id", installationId.toString())
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        http.send(request, HttpResponse.BodyHandlers.discarding());
    }

    private record ClaimRequest(String claimToken, String installationId) {}
    public record ClaimResponse(String apiKey, String serverId, String installationId) {}
    private record MetricsPayload(String timestamp, double tps, double mspt, int onlinePlayers, long usedMemory) {}
}