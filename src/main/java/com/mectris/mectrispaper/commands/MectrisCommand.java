package com.mectris.mectrispaper.commands;

import com.mectris.mectrispaper.Mectris;
import com.mectris.mectrispaper.api.MectrisApiClient;
import com.mectris.mectrispaper.collector.MetricsCollector;
import com.mectris.mectrispaper.identifiers.MessageKeys;
import com.mectris.mectrispaper.service.MectrisService;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.annotation.*;
import revxrsal.commands.bukkit.annotation.CommandPermission;

@Command("mectris")
@CommandPermission("mectris.admin")
@SuppressWarnings("unused")
public class MectrisCommand {

    @CommandPlaceholder
    public void help(@NotNull CommandSender sender) {
        sender.sendMessage(MessageKeys.HELP.getMessage());
    }

    @Subcommand("status")
    @Description("Show connection status and config")
    public void status(CommandSender sender) {
        var plugin = Mectris.getInstance();

        try {
            var creds = plugin.getStorage().loadCredentials();
            if (creds.isPresent()) {
                sender.sendMessage(MessageKeys.STATUS_CONNECTED.getMessage(
                        "{url}", MectrisApiClient.BASE_URL,
                        "{interval}", String.valueOf(MectrisService.METRICS_INTERVAL_SECONDS),
                        "{server_id}", creds.get().serverId().toString(),
                        "{installation_id}", creds.get().installationId().toString()));
            } else {
                sender.sendMessage(MessageKeys.STATUS_DISCONNECTED.getMessage(
                        "{url}", MectrisApiClient.BASE_URL,
                        "{interval}", String.valueOf(MectrisService.METRICS_INTERVAL_SECONDS)));
            }
        } catch (Exception e) {
            sender.sendMessage(MessageKeys.STATUS_ERROR.getMessage("{error}", String.valueOf(e.getMessage())));
        }
    }

    @Subcommand("metrics")
    @Description("Show current live server metrics")
    public void metrics(@NotNull CommandSender sender) {
        var collector = new MetricsCollector();
        double[] tps = collector.getTpsHistory();
        long usedMb = collector.getUsedMemory() / 1048576L;
        long maxMb = collector.getMaxMemory() / 1048576L;
        int memPercent = maxMb == 0 ? 0 : (int) Math.round(usedMb * 100.0 / maxMb);
        double cpu = collector.getCpuUsage();

        sender.sendMessage(MessageKeys.METRICS.getMessage(
                "{tps_1m}", String.format("%.2f", Math.min(20.0, tps[0])),
                "{tps_5m}", String.format("%.2f", Math.min(20.0, tps[1])),
                "{tps_15m}", String.format("%.2f", Math.min(20.0, tps[2])),
                "{mspt}", String.format("%.2f", collector.getMspt()),
                "{cpu}", cpu < 0 ? "N/A" : String.format("%.1f", cpu),
                "{mem_used}", String.valueOf(usedMb),
                "{mem_max}", String.valueOf(maxMb),
                "{mem_percent}", String.valueOf(memPercent),
                "{online}", String.valueOf(collector.getOnlinePlayers()),
                "{max_players}", String.valueOf(collector.getMaxPlayers()),
                "{worlds}", String.valueOf(collector.getWorldCount()),
                "{chunks}", String.valueOf(collector.getLoadedChunks()),
                "{entities}", String.valueOf(collector.getEntityCount()),
                "{threads}", String.valueOf(collector.getThreadCount()),
                "{software}", collector.getServerSoftware(),
                "{version}", collector.getServerVersion(),
                "{uptime}", collector.getUptime()));
    }

    @Subcommand("claim")
    @Description("Connect this server using a claim token")
    public void claim(@NotNull CommandSender sender, String token) {
        sender.sendMessage(MessageKeys.CLAIM.getMessage());

        Mectris.getInstance().getService().claimAsync(
                token,
                serverId -> sender.sendMessage(MessageKeys.CLAIM_SUCCESS.getMessage("{server_id}", serverId)),
                err -> sender.sendMessage(MessageKeys.CLAIM_FAILED.getMessage("{error}", err))
        );
    }

    @Subcommand("disconnect")
    @Description("Disconnect and clear stored credentials")
    public void disconnect(CommandSender sender) {
        try {
            Mectris.getInstance().getService().disconnect();
            sender.sendMessage(MessageKeys.DISCONNECT.getMessage());
        } catch (Exception e) {
            sender.sendMessage(MessageKeys.DISCONNECT_FAILED.getMessage("{error}", String.valueOf(e.getMessage())));
        }
    }

    @Subcommand("reload")
    @Description("Reload config and restart metrics reporting")
    public void reload(CommandSender sender) {
        try {
            Mectris.getInstance().getService().reload();
            Mectris.getInstance().getConfiguration().reload();
            Mectris.getInstance().getLanguage().reload();

            sender.sendMessage(MessageKeys.RELOAD.getMessage());
        } catch (Exception e) {
            sender.sendMessage(MessageKeys.RELOAD_FAILED.getMessage("{error}", String.valueOf(e.getMessage())));
        }
    }
}