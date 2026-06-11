package com.mectris.mectrispaper.models.api;

public record ServerInfoPayload(String serverSoftware, String serverVersion, String jvmVersion,
        String osInfo, int pluginCount) {}