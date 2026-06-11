package com.mectris.mectrispaper.models;

import java.util.UUID;

public record Credentials(String apiKey, UUID serverId, UUID installationId) {}