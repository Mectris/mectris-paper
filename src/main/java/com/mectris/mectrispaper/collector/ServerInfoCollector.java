package com.mectris.mectrispaper.collector;

import org.bukkit.Bukkit;

public class ServerInfoCollector {

    public String getServerSoftware() {
        var version = Bukkit.getVersion();
        if (version.toLowerCase().contains("paper"))  return "Paper";
        if (version.toLowerCase().contains("purpur")) return "Purpur";
        if (version.toLowerCase().contains("folia"))  return "Folia";
        if (version.toLowerCase().contains("spigot")) return "Spigot";
        if (version.toLowerCase().contains("craftbukkit")) return "CraftBukkit";
        if (version.toLowerCase().contains("leaf")) return "Leaf";
        if (version.toLowerCase().contains("advancedslimepaper")) return "AdvancedSlimePaper";
        if (version.toLowerCase().contains("uspigot")) return "UniversalSpigot";
        if (version.toLowerCase().contains("pufferfish")) return "Pufferfish";
        return "Unknown";
    }

    public String getServerVersion() {
        return Bukkit.getBukkitVersion();
    }

    public String getJvmVersion() {
        return System.getProperty("java.version", "unknown");
    }

    public String getOsInfo() {
        var name = System.getProperty("os.name", "unknown");
        var arch = System.getProperty("os.arch", "");

        return arch.isEmpty() ? name : name + " " + arch;
    }

    public int getPluginCount() {
        return Bukkit.getPluginManager().getPlugins().length;
    }
}
