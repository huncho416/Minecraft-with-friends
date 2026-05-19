package net.mythicpvp.suite.compat;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record ClientProfile(@NotNull UUID uuid, int protocol, boolean bedrock, @NotNull ProfileTier tier) {

    public static final int PROTOCOL_1_21 = 767;
    public static final int PROTOCOL_1_16 = 735;
    public static final int PROTOCOL_1_13 = 393;

    public static final ClientProfile UNKNOWN_MODERN = new ClientProfile(new UUID(0L, 0L), Integer.MAX_VALUE, false, ProfileTier.MODERN);

    @NotNull
    public static ProfileTier tierFor(int protocol, boolean bedrock) {
        if (bedrock) return ProfileTier.BEDROCK;
        if (protocol >= PROTOCOL_1_21) return ProfileTier.MODERN;
        if (protocol >= PROTOCOL_1_16) return ProfileTier.MIDLEGACY;
        if (protocol >= PROTOCOL_1_13) return ProfileTier.LEGACY;
        return ProfileTier.LEGACY;
    }
}
