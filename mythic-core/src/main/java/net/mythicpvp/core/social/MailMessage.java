package net.mythicpvp.core.social;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record MailMessage(
        long id,
        @NotNull UUID recipient,
        @NotNull UUID sender,
        @NotNull String subject,
        @NotNull String body,
        @NotNull String attachmentsJson,
        boolean read,
        long sentAtMillis,
        long readAtMillis
) {
    @NotNull
    public MailMessage markRead(long readAtMillis) {
        return new MailMessage(id, recipient, sender, subject, body, attachmentsJson, true, sentAtMillis, readAtMillis);
    }
}
