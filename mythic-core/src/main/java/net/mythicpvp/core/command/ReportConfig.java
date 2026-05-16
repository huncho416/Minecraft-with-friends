package net.mythicpvp.core.command;

import net.mythicpvp.suite.config.MythicConfig;
import org.jetbrains.annotations.NotNull;

public final class ReportConfig {

    private static final long DEFAULT_REPORT_COOLDOWN = 600L;
    private static final long DEFAULT_HELPOP_COOLDOWN = 600L;

    private final MythicConfig config;

    public ReportConfig(@NotNull MythicConfig config) {
        this.config = config;
    }

    public long cooldownSeconds() {
        return Math.max(0L, config.getConfig().getLong("reports.cooldown-seconds", DEFAULT_REPORT_COOLDOWN));
    }

    public long helpopCooldownSeconds() {
        return Math.max(0L, config.getConfig().getLong("helpop.cooldown-seconds", DEFAULT_HELPOP_COOLDOWN));
    }
}
