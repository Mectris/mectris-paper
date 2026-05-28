package com.mectris.mectrispaper.commands;

import com.mectris.mectrispaper.Mectris;
import com.mectris.mectrispaper.metrics.MetricsCollector;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import revxrsal.commands.annotation.*;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.bukkit.annotation.CommandPermission;

@Command("mectris")
@CommandPermission("mectris.admin")
public class MectrisCommand {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    @CommandPlaceholder
    public void help(BukkitCommandActor actor) {
        send(actor, "<dark_gray>--- <aqua>Mectris Help</aqua> ---");
        send(actor, "<yellow>/mectris status</yellow> <gray>- Connection & config info");
        send(actor, "<yellow>/mectris metrics</yellow> <gray>- Live server metrics");
        send(actor, "<yellow>/mectris claim <token></yellow> <gray>- Connect with a claim token");
        send(actor, "<yellow>/mectris disconnect</yellow> <gray>- Clear credentials");
        send(actor, "<yellow>/mectris reload</yellow> <gray>- Reload config & restart metrics");
    }

    @Subcommand("status")
    @Description("Show connection status and config")
    public void status(BukkitCommandActor actor) {
        var plugin = Mectris.getInstance();
        var config = plugin.getMectrisConfig();

        send(actor, "<dark_gray>--- <aqua>Mectris Status</aqua> ---");
        send(actor, "<gray>API URL: <white>" + config.getApiUrl());
        send(actor, "<gray>Metrics interval: <white>" + config.getMetricsInterval() + "s");

        try {
            var creds = plugin.getStorage().loadCredentials();
            if (creds.isPresent()) {
                send(actor, "<gray>Status: <green>Connected");
                send(actor, "<gray>Server ID: <white>" + creds.get().serverId());
                send(actor, "<gray>Installation ID: <white>" + creds.get().installationId());
            } else {
                send(actor, "<gray>Status: <red>Not connected");
                send(actor, "<gray>Run <yellow>/mectris claim <token></yellow> to connect");
            }
        } catch (Exception e) {
            send(actor, "<red>Failed to read credentials: " + e.getMessage());
        }
    }

    @Subcommand("metrics")
    @Description("Show current live server metrics")
    public void metrics(BukkitCommandActor actor) {
        var collector = new MetricsCollector();
        send(actor, "<dark_gray>--- <aqua>Live Metrics</aqua> ---");
        send(actor, "<gray>TPS: <white>" + String.format("%.2f", collector.getTps()));
        send(actor, "<gray>MSPT: <white>" + String.format("%.2f", collector.getMspt()) + "ms");
        send(actor, "<gray>Online players: <white>" + collector.getOnlinePlayers());
        send(actor, "<gray>Used memory: <white>" + (collector.getUsedMemory() / 1024 / 1024) + " MB");
    }

    @Subcommand("claim")
    @Description("Connect this server using a claim token")
    public void claim(BukkitCommandActor actor, String token) {
        send(actor, "<gray>Claiming, please wait...");
        CommandSender sender = actor.sender();

        Mectris.getInstance().claimAsync(
                token,
                serverId -> sender.sendMessage(MM.deserialize("<green>Connected! Server ID: <white>" + serverId)),
                err     -> sender.sendMessage(MM.deserialize("<red>Claim failed: " + err))
        );
    }

    @Subcommand("disconnect")
    @Description("Disconnect and clear stored credentials")
    public void disconnect(BukkitCommandActor actor) {
        try {
            Mectris.getInstance().disconnect();
            send(actor, "<green>Disconnected. Credentials cleared.");
        } catch (Exception e) {
            send(actor, "<red>Failed to disconnect: " + e.getMessage());
        }
    }

    @Subcommand("reload")
    @Description("Reload config and restart metrics reporting")
    public void reload(BukkitCommandActor actor) {
        try {
            Mectris.getInstance().reload();
            send(actor, "<green>Config reloaded and metrics restarted.");
        } catch (Exception e) {
            send(actor, "<red>Reload failed: " + e.getMessage());
        }
    }

    private void send(BukkitCommandActor actor, String miniMessage) {
        actor.sender().sendMessage(MM.deserialize(miniMessage));
    }
}