package com.mectris.mectrispaper;

import lombok.Getter;
import revxrsal.zapper.ZapperJavaPlugin;

public final class Mectris extends ZapperJavaPlugin {

    @Getter private static Mectris instance;

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }
}