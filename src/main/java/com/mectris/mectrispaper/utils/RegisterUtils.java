package com.mectris.mectrispaper.utils;

import com.mectris.mectrispaper.Mectris;
import com.mectris.mectrispaper.commands.MectrisCommand;
import lombok.experimental.UtilityClass;
import revxrsal.commands.bukkit.BukkitLamp;

@UtilityClass
public class RegisterUtils {

    public void registerCommands() {
        var lamp = BukkitLamp.builder(Mectris.getInstance()).build();
        lamp.register(new MectrisCommand());
    }
}