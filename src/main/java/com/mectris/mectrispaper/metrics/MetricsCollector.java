package com.mectris.mectrispaper.metrics;

import org.bukkit.Bukkit;

public class MetricsCollector {

    public double getTps() {
        return Math.min(20.0, Bukkit.getServer().getTPS()[0]);
    }

    public double getMspt() {
        return Bukkit.getServer().getAverageTickTime();
    }

    public int getOnlinePlayers() {
        return Bukkit.getServer().getOnlinePlayers().size();
    }

    public long getUsedMemory() {
        var runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
}