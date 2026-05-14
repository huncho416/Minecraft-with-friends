package net.mythicpvp.core.punishment;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record PunishmentFlow(
        @NotNull UUID targetUuid,
        @NotNull String targetName,
        PunishmentTemplate template,
        @NotNull String proof,
        boolean silent,
        boolean clearInventory
) {
    @NotNull
    public PunishmentFlow template(@NotNull PunishmentTemplate nextTemplate) {
        return new PunishmentFlow(targetUuid, targetName, nextTemplate, proof, silent, clearInventory);
    }

    @NotNull
    public PunishmentFlow proof(@NotNull String nextProof) {
        return new PunishmentFlow(targetUuid, targetName, template, nextProof, silent, clearInventory);
    }

    @NotNull
    public PunishmentFlow silent(boolean nextSilent) {
        return new PunishmentFlow(targetUuid, targetName, template, proof, nextSilent, clearInventory);
    }

    @NotNull
    public PunishmentFlow clearInventory(boolean nextClearInventory) {
        return new PunishmentFlow(targetUuid, targetName, template, proof, silent, nextClearInventory);
    }
}
