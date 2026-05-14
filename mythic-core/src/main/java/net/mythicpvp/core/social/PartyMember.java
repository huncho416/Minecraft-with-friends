package net.mythicpvp.core.social;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record PartyMember(
        long id,
        long partyId,
        @NotNull UUID player,
        long joinedAtMillis
) {}
