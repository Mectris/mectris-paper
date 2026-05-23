package com.mectris.mectrispaper.config;

import com.mectris.mectrispaper.Mectris;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;

import java.io.File;
import java.io.IOException;

public class MectrisConfig {

    private final YamlDocument document;

    public MectrisConfig() throws IOException {
        document = YamlDocument.create(
                new File(Mectris.getInstance().getDataFolder(), "config.yml"),
                Mectris.getInstance().getResource("config.yml"),
                GeneralSettings.DEFAULT,
                LoaderSettings.builder().setAutoUpdate(true).build(),
                DumperSettings.DEFAULT,
                UpdaterSettings.builder().setVersioning(new BasicVersioning("config-version")).build()
        );
    }

    public String getApiUrl() {
        return document.getString("api-url", "http://localhost:8080");
    }

    public String getClaimToken() {
        return document.getString("claim-token", "");
    }

    public void clearClaimToken() throws IOException {
        document.set("claim-token", "");
        document.save();
    }

    public int getMetricsInterval() {
        return document.getInt("metrics-interval", 30);
    }
}