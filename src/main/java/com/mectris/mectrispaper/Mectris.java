package com.mectris.mectrispaper;

import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import com.mectris.mectrispaper.api.MectrisApiClient;
import com.mectris.mectrispaper.collector.PlayerSessionCollector;
import com.mectris.mectrispaper.config.Config;
import com.mectris.mectrispaper.identifiers.ConfigKeys;
import com.mectris.mectrispaper.service.MectrisService;
import com.mectris.mectrispaper.storage.MectrisStorage;
import com.mectris.mectrispaper.utils.LoggerUtils;
import com.mectris.mectrispaper.utils.RegisterUtils;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import lombok.Getter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import revxrsal.zapper.ZapperJavaPlugin;

import java.io.File;

public final class Mectris extends ZapperJavaPlugin {

    @Getter private static Mectris instance;
    @Getter private static TaskScheduler scheduler;

    @Getter private MectrisStorage storage;
    @Getter private PlayerSessionCollector sessionTracker;
    @Getter private MectrisService service;

    @Getter private Config language;
    Config config;

    @Override
    public void onLoad() {
        instance = this;
        scheduler = UniversalScheduler.getScheduler(this);
    }

    @Override
    public void onEnable() {
        initializeComponents();

        storage = new MectrisStorage();
        sessionTracker = new PlayerSessionCollector();
        service = new MectrisService(storage, new MectrisApiClient(), sessionTracker);

        sessionTracker.seedOnlinePlayers();

        RegisterUtils.registerCommands();
        RegisterUtils.registerListeners();

        var claimToken = ConfigKeys.CLAIM_TOKEN.getString();
        if (!claimToken.isBlank()) {
            LoggerUtils.info("Claim token detected, connecting to Mectris...");
            service.claimAsync(claimToken,
                    serverId -> LoggerUtils.info("Connected! Server ID: {}", serverId),
                    err -> LoggerUtils.error("Claim failed — check your token and api-url: {}", err)
            );
        } else {
            service.startIfConnected();
        }

        LoggerUtils.printStartup();
    }

    @Override
    public void onDisable() {
        if (service != null) service.shutdown();
        if (storage != null) storage.close();
    }

    public Config getConfiguration() {
        return config;
    }

    private void initializeComponents() {
        final GeneralSettings generalSettings = GeneralSettings.builder()
                .setUseDefaults(false)
                .build();

        final LoaderSettings loaderSettings = LoaderSettings.builder()
                .setAutoUpdate(true)
                .build();

        final UpdaterSettings updaterSettings = UpdaterSettings.builder()
                .setKeepAll(true)
                .setVersioning(new BasicVersioning("version"))
                .build();

        config = loadConfig("config.yml", generalSettings, loaderSettings, updaterSettings);
        language = loadConfig("messages.yml", generalSettings, loaderSettings, updaterSettings);
    }

    @NotNull
    @Contract("_, _, _, _ -> new")
    private Config loadConfig(@NotNull String fileName, @NotNull GeneralSettings generalSettings, @NotNull LoaderSettings loaderSettings, @NotNull UpdaterSettings updaterSettings) {
        return new Config(
                new File(getDataFolder(), fileName),
                getResource(fileName),
                generalSettings,
                loaderSettings,
                DumperSettings.DEFAULT,
                updaterSettings
        );
    }
}