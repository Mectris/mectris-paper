package com.mectris.mectrispaper.identifiers;

import com.mectris.mectrispaper.Mectris;
import com.mectris.mectrispaper.config.Config;
import com.mectris.mectrispaper.processor.MessageProcessor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Getter
public enum MessageKeys {
    HELP("messages.help"),

    STATUS_CONNECTED("messages.status-connected"),
    STATUS_DISCONNECTED("messages.status-disconnected"),
    STATUS_ERROR("messages.status-error"),

    METRICS("messages.metrics"),

    CLAIM("messages.claim"),
    CLAIM_SUCCESS("messages.claim-success"),
    CLAIM_FAILED("messages.claim-failed"),

    DISCONNECT("messages.disconnect"),
    DISCONNECT_FAILED("messages.disconnect-failed"),

    RELOAD("messages.reload"),
    RELOAD_FAILED("messages.reload-failed");

    private final String path;
    private static final Config config = Mectris.getInstance().getLanguage();

    MessageKeys(@NotNull String path) {
        this.path = path;
    }

    public @NotNull String getMessage(@NotNull String... replacements) {
        List<String> lines = config.getStringList(path);
        String raw = lines.isEmpty() ? config.getString(path) : String.join("\n", lines);

        String message = MessageProcessor.process(raw)
                .replace("%prefix%", MessageProcessor.process(config.getString("prefix")));

        for (int i = 0; i + 1 < replacements.length; i += 2) {
            message = message.replace(replacements[i], replacements[i + 1]);
        }
        return message;
    }
}
