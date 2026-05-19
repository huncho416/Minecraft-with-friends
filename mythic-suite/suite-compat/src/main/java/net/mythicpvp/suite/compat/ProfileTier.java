package net.mythicpvp.suite.compat;

public enum ProfileTier {
    MODERN,
    MIDLEGACY,
    LEGACY,
    BEDROCK;

    public boolean needsHexDowngrade() {
        return this == LEGACY || this == BEDROCK;
    }

    public boolean needsMiniMessageStrip() {
        return this == LEGACY || this == BEDROCK;
    }

    public boolean rendersFully() {
        return this == MODERN;
    }
}
