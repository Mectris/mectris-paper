package com.mectris.mectrispaper.models;

public record PlayerSession(
        String playerUuid,
        String playerName,
        String country,
        String joinedAt,
        String leftAt,
        int durationSeconds
) {}