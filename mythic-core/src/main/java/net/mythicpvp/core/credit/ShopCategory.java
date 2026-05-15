package net.mythicpvp.core.credit;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record ShopCategory(
        @NotNull String id,
        @NotNull String displayName,
        @NotNull Material material,
        int slot,
        @NotNull List<ShopItem> items) {}
