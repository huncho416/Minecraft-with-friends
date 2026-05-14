package net.mythicpvp.core.punishment;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public enum PunishmentCategory {
    WARN(Material.YELLOW_WOOL),
    MUTE(Material.ORANGE_WOOL),
    BAN(Material.RED_WOOL),
    BLACKLIST(Material.BLACK_WOOL);

    private final Material material;

    PunishmentCategory(@NotNull Material material) {
        this.material = material;
    }

    @NotNull
    public Material material() {
        return material;
    }

    @NotNull
    public static PunishmentCategory parse(@NotNull String input) {
        return valueOf(input.trim().toUpperCase(Locale.ROOT));
    }
}
