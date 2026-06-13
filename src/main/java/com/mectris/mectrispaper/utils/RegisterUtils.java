package com.mectris.mectrispaper.utils;

import com.mectris.mectrispaper.Mectris;
import com.mectris.mectrispaper.commands.MectrisCommand;
import lombok.experimental.UtilityClass;
import revxrsal.commands.bukkit.BukkitLamp;

@UtilityClass
public class RegisterUtils {

    private final Mectris plugin = Mectris.getInstance();

    public void registerCommands() {
        var lamp = BukkitLamp.builder(plugin).build();
        lamp.register(new MectrisCommand());
    }

    public void registerListeners() {
        plugin.getServer().getPluginManager().registerEvents(plugin.getSessionTracker(), plugin);
    }
}