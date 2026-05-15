package net.mythicpvp.core.credit;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record ShopItem(
        @NotNull String id,
        @NotNull String displayName,
        @NotNull Material material,
        long cost,
        @NotNull ShopItemType type,
        @NotNull String value,
        @Nullable String requiresRank,
        @NotNull List<String> lore) {

    public enum ShopItemType {
        RANK,
        CRATE,
        COSMETIC,
        RANK_UPGRADE
    }
}
