package com.mectris.mectrispaper.metrics;

public record PlayerSession(
        String playerUuid,
        String playerName,
        String country,
        String joinedAt,
        String leftAt,
        int durationSeconds
) {}
