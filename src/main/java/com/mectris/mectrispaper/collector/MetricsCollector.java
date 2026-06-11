package com.mectris.mectrispaper.collector;

import com.sun.management.OperatingSystemMXBean;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.lang.management.ManagementFactory;

public class MetricsCollector {

    public double getTps() {
        return Math.min(20.0, Bukkit.getServer().getTPS()[0]);
    }

    public double[] getTpsHistory() {
        return Bukkit.getServer().getTPS();
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
        if (osBean instanceof OperatingSystemMXBean sunBean) {
            double load = sunBean.getProcessCpuLoad();
            return load < 0 ? -1.0 : Math.min(100.0, load * 100.0);
        }

        return -1.0;
    }

    public int getWorldCount() {
        return Bukkit.getWorlds().size();
    }

    public int getLoadedChunks() {
        int chunks = 0;
        for (World world : Bukkit.getWorlds()) {
            chunks += world.getLoadedChunks().length;
        }
        return chunks;
    }

    public int getEntityCount() {
        int entities = 0;
        for (World world : Bukkit.getWorlds()) {
            entities += world.getEntities().size();
        }
        return entities;
    }

    public int getThreadCount() {
        return ManagementFactory.getThreadMXBean().getThreadCount();
    }

    public String getServerSoftware() {
        return Bukkit.getName();
    }

    public String getServerVersion() {
        return Bukkit.getMinecraftVersion();
    }

    public String getUptime() {
        long seconds = ManagementFactory.getRuntimeMXBean().getUptime() / 1000L;
        long days = seconds / 86400L;
        long hours = (seconds % 86400L) / 3600L;
        long minutes = (seconds % 3600L) / 60L;

        StringBuilder builder = new StringBuilder();
        if (days > 0) builder.append(days).append("d ");
        if (hours > 0 || days > 0) builder.append(hours).append("h ");
        builder.append(minutes).append("m");
        return builder.toString();
    }
}
