package net.mythicpvp.core.report;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public enum ReportCategory {
    CHEATING("Cheating", Material.IRON_SWORD, "Hacking, X-ray, killaura, fly, etc."),
    GRIEFING("Griefing", Material.TNT, "Block destruction, sabotage, raiding."),
    HARASSMENT("Harassment", Material.BARRIER, "Targeted insults, threats, bullying."),
    RACISM("Racism", Material.RED_BANNER, "Racial slurs or hateful conduct."),
    SLURS("Slurs", Material.PAPER, "Slurs, hate speech, or discriminatory terms."),
    INAPPROPRIATE_BUILD("Inappropriate Build", Material.BRICKS, "Offensive or NSFW structures."),
    SPAM("Spam", Material.BOOK, "Repeated messages or advertising."),
    SCAMMING("Scamming", Material.GOLD_INGOT, "Trade scams or false promises."),
    EXPLOITING("Exploiting", Material.COMMAND_BLOCK, "Glitch abuse, dupes, server exploits."),
    INAPPROPRIATE_SKIN("Inappropriate Skin/Cape", Material.PLAYER_HEAD, "Offensive skin or NSFW cape."),
    OTHER("Other", Material.NAME_TAG, "Anything not covered above.");

    private final String displayName;
    private final Material icon;
    private final String description;

    ReportCategory(@NotNull String displayName, @NotNull Material icon, @NotNull String description) {
        this.displayName = displayName;
        this.icon = icon;
        this.description = description;
    }

    @NotNull
    public String displayName() {
        return displayName;
    }

    @NotNull
    public Material icon() {
        return icon;
    }

    @NotNull
    public String description() {
        return description;
    }

    @NotNull
    public static List<ReportCategory> all() {
        return List.of(values());
    }
}
