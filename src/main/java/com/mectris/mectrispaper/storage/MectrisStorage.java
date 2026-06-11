package com.mectris.mectrispaper.storage;

import com.google.gson.Gson;
import com.mectris.mectrispaper.Mectris;
import com.mectris.mectrispaper.models.Credentials;
import com.mectris.mectrispaper.models.CredentialsData;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

public class MectrisStorage {

    private static final Gson GSON = new Gson();

    private final File file;

    public MectrisStorage() {
        this.file = new File(Mectris.getInstance().getDataFolder(), "credentials.json");
    }

    public Optional<Credentials> loadCredentials() throws IOException {
        if (!file.exists()) return Optional.empty();

        try (var reader = new FileReader(file)) {
            var data = GSON.fromJson(reader, CredentialsData.class);
            if (data == null || data.apiKey() == null || data.serverId() == null || data.installationId() == null) {
                return Optional.empty();
            }

            return Optional.of(new Credentials(data.apiKey(), UUID.fromString(data.serverId()), UUID.fromString(data.installationId())));
        }
    }

    public void saveCredentials(String apiKey, @NotNull UUID serverId, @NotNull UUID installationId) throws IOException {
        try (var writer = new FileWriter(file)) {
            GSON.toJson(new CredentialsData(apiKey, serverId.toString(), installationId.toString()), writer);
        }
    }

    public void clearCredentials() {
        file.delete();
    }

    public void close() {}
}