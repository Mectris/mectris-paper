package com.mectris.mectrispaper.models.api;

public record MetricsPayload(String timestamp, double tps, double mspt, int onlinePlayers,
        long usedMemory, Double cpuUsage, long maxMemory, int maxPlayers) {}