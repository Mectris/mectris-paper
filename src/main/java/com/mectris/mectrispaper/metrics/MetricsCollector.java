package com.mectris.mectrispaper.metrics;

import org.bukkit.Bukkit;

import java.lang.management.ManagementFactory;

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

    public long getMaxMemory() {
        return Runtime.getRuntime().maxMemory();
    }

    public int getMaxPlayers() {
        return Bukkit.getMaxPlayers();
    }

    public double getCpuUsage() {
        var osBean = ManagementFactory.getOperatingSystemMXBean();
        if (osBean instanceof com.sun.management.OperatingSystemMXBean sunBean) {
            double load = sunBean.getProcessCpuLoad();
            return load < 0 ? -1.0 : Math.min(100.0, load * 100.0);
        }
        return -1.0;
    }
}
