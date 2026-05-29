package com.mectris.mectrispaper.metrics;

public record PlayerSession(
        String playerUuid,
        String joinedAt,
        String leftAt,
        int durationSeconds
) {}