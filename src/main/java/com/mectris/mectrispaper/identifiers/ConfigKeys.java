package com.mectris.mectrispaper.identifiers;

import com.mectris.mectrispaper.Mectris;
import com.mectris.mectrispaper.config.Config;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public enum ConfigKeys {
    CLAIM_TOKEN("claim-token");

    private static final Config config = Mectris.getInstance().getConfiguration();
    private final String path;

    ConfigKeys(@NotNull String path) {
        this.path = path;
    }

    public static @NotNull String getString(@NotNull String path) {
        return config.getString(path);
    }

    public @NotNull String getString() {
        return config.getString(path);
    }

    public void set(Object value) {
        config.set(path, value);
        config.save();
    }

    public boolean getBoolean() {
        return config.getBoolean(path);
    }

    public int getInt() {
        return config.getInt(path);
    }

    public double getDouble() {
        return config.getDouble(path);
    }

    public long getLong() {
        return config.getLong(path);
    }

    public List<String> getList() {
        return config.getList(path);
    }

    public @NotNull Section getSection() {
        return config.getSection(path);
    }
}