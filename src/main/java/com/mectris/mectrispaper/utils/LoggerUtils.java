package com.mectris.mectrispaper.utils;

import com.mectris.mectrispaper.Mectris;
import lombok.experimental.UtilityClass;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class LoggerUtils {
    private final Logger logger = LogManager.getLogger("McPlayerShop");

    public void info(@NotNull String msg, @NotNull Object... objs) {
        logger.info(msg, objs);
    }

    public void warn(@NotNull String msg, @NotNull Object... objs) {
        logger.warn(msg, objs);
    }

    public void error(@NotNull String msg, @NotNull Object... objs) {
        logger.error(msg, objs);
    }

    public void printStartup() {
        String main = "\u001B[38;2;132;145;248m";
        String reset = "\u001B[0m";
        String software = Mectris.getInstance().getServer().getName();
        String version = Mectris.getInstance().getServer().getVersion();

        info("");
        info("{}                        _        _       {}", main, reset);
        info("{}                       | |      (_)     {}", main, reset);
        info("{}    _ __ ___   ___  ___| |_ _ __ _ ___ {}", main, reset);
        info("{}   | '_ ` _ \\ / _ \\/ __| __| '__| / __| {}", main, reset);
        info("{}   | | | | | |  __/ (__| |_| |  | \\__ \\ {}", main, reset);
        info("{}   |_| |_| |_|\\___|\\___|\\__|_|  |_|___/ {}", main, reset);
        info("");
        info("{}   The plugin successfully started.{}", main, reset);
        info("{}   Mectris {} {}{}", main, software, version, reset);
        info("{}   Discord @ dc.mectris.com{}", main, reset);
        info("");
    }
}