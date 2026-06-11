package com.mectris.mectrispaper.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@UtilityClass
public class GeoIpResolver {

    private static final Gson GSON = new Gson();

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    public void resolve(InetAddress address, Consumer<String> callback) {
        if (address == null
                || address.isLoopbackAddress()
                || address.isAnyLocalAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()) {
            callback.accept(null);
            return;
        }

        var ip = address.getHostAddress();
        var cached = cache.get(ip);
        if (cached != null) {
            callback.accept(cached.isEmpty() ? null : cached);
            return;
        }

        var request = HttpRequest.newBuilder()
                .uri(URI.create("http://ip-api.com/json/" + ip + "?fields=status,countryCode"))
                .timeout(java.time.Duration.ofSeconds(8))
                .GET()
                .build();

        http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(GeoIpResolver::parseCountry)
                .exceptionally(e -> null)
                .thenAccept(code -> {
                    cache.put(ip, code == null ? "" : code);
                    callback.accept(code);
                });
    }

    @Nullable
    private String parseCountry(String body) {
        try {
            var json = GSON.fromJson(body, JsonObject.class);
            if (json != null && json.has("status") && "success".equals(json.get("status").getAsString()) && json.has("countryCode")) {
                var cc = json.get("countryCode").getAsString();
                if (cc != null && cc.length() == 2) return cc.toUpperCase();
            }
        } catch (Exception ignored) {
        }

        return null;
    }
}